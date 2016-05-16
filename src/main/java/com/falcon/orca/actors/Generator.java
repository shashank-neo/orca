/**
 * Copyright 2016 Shwet Shashank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.falcon.orca.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.falcon.orca.commands.CollectorCommand;
import com.falcon.orca.commands.GeneratorCommand;
import com.falcon.orca.commands.ManagerCommand;
import com.falcon.orca.domain.DynDataStore;
import com.falcon.orca.enums.CollectorCommandType;
import com.falcon.orca.enums.HttpMethods;
import com.falcon.orca.enums.ManagerCommandType;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static com.falcon.orca.helpers.PrintHelper.printOnCmd;

/**
 * Created by shwet.s under project orca.<br/>
 * Created on  03/04/16. <br/>
 * Updated on 03/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Slf4j
public class Generator extends UntypedActor {
    private final ActorRef collector;
    private final CloseableHttpClient client;
    private final DynDataStore dataStore;
    private final boolean isBodyDynamic;
    private final HttpMethods method;
    private final String url;
    private final List<Header> headers;
    private final byte[] staticRequestData;
    private final boolean isUrlDynamic;


    public Generator(final ActorRef collector, final String url, final HttpMethods method, final byte[] data, final
    List<Header> headers, final List<Cookie> cookies, final boolean isBodyDynamic, final boolean isUrlDynamic,  final
    DynDataStore dataStore) throws
            URISyntaxException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.collector = collector;
        this.dataStore = dataStore;
        this.isBodyDynamic = isBodyDynamic;
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.staticRequestData = data != null? Arrays.copyOf(data, data.length): new byte[0];

        this.isUrlDynamic = isUrlDynamic;
        CookieStore cookieStore = new BasicCookieStore();
        if (cookies != null) {
            cookies.forEach(cookieStore::addCookie);
        }
        TrustStrategy trustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(null, trustStrategy).build();
        this.client = HttpClientBuilder.create().setSSLContext(sslContext).setSSLHostnameVerifier(new
                NoopHostnameVerifier()).setDefaultCookieStore(cookieStore).build();
    }

    public static Props props(final ActorRef collector, final String url, final HttpMethods method, final byte[] data,
                              final List<Header> headers, final List<Cookie> cookies, final boolean isBodyDynamic,
                              final boolean isUrlDynamic, final DynDataStore dataStore) {
        return Props.create(new Creator<Generator>() {
            private static final long serialVersionUID = 1L;

            public Generator create() throws Exception {
                return new Generator(collector, url, method, data, headers, cookies, isBodyDynamic, isUrlDynamic,
                        dataStore);
            }
        });
    }

    @Override
    public void onReceive(Object message) {
        try {
            if (message instanceof GeneratorCommand) {
                switch (((GeneratorCommand) message).getType()) {
                    case LOAD: {
                        makeHttpCall();
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.GEN_FREE);
                        getSender().tell(managerCommand, getSelf());
                        break;
                    }
                    case STOP: {
                        this.client.close();
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.GEN_STOPPED);
                        getSender().tell(managerCommand, getSelf());
                        break;
                    }
                }
            } else {
                unhandled(message);
            }
        } catch (Exception e) {
            if(StringUtils.isNotBlank(e.getMessage())) {
                printOnCmd("Error: " + e.getMessage());
            } else {
                printOnCmd("Unxpected Error occurred.");
            }
            log.error("Exception: ",e);
        }
    }

    private void makeHttpCall() {
        HttpResponse response = null;
        try {
            HttpUriRequest request = prepareRequest();
            long startTime = System.nanoTime();
            response = client.execute(request);
            long stopTime = System.nanoTime();
            sendDataToCollector(stopTime, startTime, response.getEntity().getContentLength(), response.getStatusLine
                    ().getStatusCode());
        } catch (Exception e) {
            System.out.println("Error in making http call " + e.getMessage());
        } finally {
            if (response != null && response.getEntity() != null) EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    private void sendDataToCollector(final Long stopTime, final Long startTime, final Long contentLength, final Integer
            statusCode) {
        CollectorCommand collectorCommand = new CollectorCommand();
        collectorCommand.putOnContext("responseTime", stopTime - startTime);
        collectorCommand.putOnContext("responseSize", contentLength);
        collectorCommand.putOnContext("isSuccess", statusCode >= 200 && statusCode < 300);
        collectorCommand.setCommandType(CollectorCommandType.COLLECT);
        collector.tell(collectorCommand, getSelf());
    }

    private HttpUriRequest prepareRequest() throws URISyntaxException, JsonProcessingException {
        HttpUriRequest request;
        switch (method) {
            case POST: {
                String postUrl = isUrlDynamic ? dataStore.fillURLWithData() : url;
                AbstractHttpEntity postData;
                try {
                    postData = isBodyDynamic ? new StringEntity(dataStore.fillTemplateWithData(),
                                StandardCharsets.UTF_8) : new ByteArrayEntity(staticRequestData == null ? new byte[0]
                            : staticRequestData);
                } catch (IllegalArgumentException ile) {
                    postData = new ByteArrayEntity(new byte[0]);
                    log.error("Post body is null, sending blank.");
                }
                request = new HttpPost(postUrl);
                ((HttpPost) request).setEntity(postData);
                break;
            }
            case GET: {
                String getUrl = isUrlDynamic ? dataStore.fillURLWithData() : url;
                request = new HttpGet(getUrl);
                break;
            }
            case PUT: {
                String putUrl = isUrlDynamic ? dataStore.fillURLWithData() : url;
                AbstractHttpEntity putData;
                try {
                    putData = isBodyDynamic ? new StringEntity(dataStore.fillTemplateWithData(),
                            StandardCharsets.UTF_8) : new ByteArrayEntity(staticRequestData == null ? new byte[0]
                            : staticRequestData);
                } catch (IllegalArgumentException ile) {
                    putData = new ByteArrayEntity(new byte[0]);
                    log.error("Post body is null, sending blank.");
                }
                request = new HttpPut(putUrl);
                ((HttpPut) request).setEntity(putData);
                break;
            }
            case DELETE: {
                String deleteUrl = isUrlDynamic ? dataStore.fillURLWithData() : url;
                request = new HttpDelete(deleteUrl);
                break;
            }
            case OPTIONS:{
                String optionsUrl = isUrlDynamic ? dataStore.fillURLWithData() : url;
                request = new HttpOptions(optionsUrl);
                break;
            }
            case HEAD: {
                String headUrl = isUrlDynamic ? dataStore.fillURLWithData() : url;
                request = new HttpHead(headUrl);
                break;
            }
            default:
                throw new URISyntaxException(url + ":" + method, "Wrong method supplied, available methods are POST, " +
                        "GET, PUT, DELETE, HEAD, OPTIONS");
        }
        if (headers != null) {
            headers.forEach(request::addHeader);
        }
        return request;
    }
}

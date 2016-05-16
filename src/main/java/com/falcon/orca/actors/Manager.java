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
import com.falcon.orca.commands.NodeManagerCommand;
import com.falcon.orca.domain.DynDataStore;
import com.falcon.orca.domain.HttpCookie;
import com.falcon.orca.domain.RunDetails;
import com.falcon.orca.domain.RunResult;
import com.falcon.orca.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.falcon.orca.helpers.PrintHelper.printJobStatus;
import static com.falcon.orca.helpers.PrintHelper.printOnCmd;

/**
 * Created by shwet.s under project orca. <br/> Created on  03/04/16. <br/> Updated on 03/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */
public class Manager extends UntypedActor {
    private final Integer noOfUsers;
    private final Long duration;
    private final List<ActorRef> generators;
    private final ActorRef collector;
    private final boolean durationMode;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ActorRef invoker;
    private Long startTime;
    private Long stopTime;
    private Long pausedTimeTotal = 0L;
    private Long pausedAt;
    private Integer numOfRepeats;
    private Integer totalRepeats;
    private Integer runningGenerators;
    private Boolean isPaused = false;


    public Manager(String url, Integer noOfUsers, Long duration, Integer numOfRepeats, HttpMethods httpMethod, String
            data, List<String> headersString, List<String> cookiesString, boolean isBodyDynamic, boolean isUrlDynamic,
                   DynDataStore dataStore) {
        this.noOfUsers = noOfUsers;
        this.duration = duration;
        this.numOfRepeats = numOfRepeats;
        this.totalRepeats = numOfRepeats;
        this.generators = new LinkedList<>();
        this.collector = getContext().actorOf(Collector.props());
        this.durationMode = (duration != null);
        final List<Header> headers = new ArrayList<>();
        if (headersString != null) {
            headersString.forEach((o) -> {
                if (o.contains(":")) {
                    String[] keyVal = o.split(":");
                    headers.add(new BasicHeader(keyVal[0], keyVal[1]));
                }
            });
        }
        final List<Cookie> cookies = new ArrayList<>();
        if (cookiesString != null) {
            cookiesString.forEach((o) -> {
                try {
                    HttpCookie cookie = objectMapper.readValue(o, HttpCookie.class);
                    BasicClientCookie basicCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                    basicCookie.setDomain(cookie.getDomain());
                    basicCookie.setPath(cookie.getPath());
                    cookies.add(basicCookie);
                } catch (IOException e) {
                    System.out.println("Wrong cookie format ignoring this cookie " + o);
                }
            });
        }
        for (int i = 0; i < this.noOfUsers; i++) {
            this.generators.add(getContext().actorOf(Generator.props(collector, url, httpMethod, data == null ?
                    null : data.getBytes(StandardCharsets.UTF_8), headers, cookies, isBodyDynamic, isUrlDynamic,
                    dataStore)));
        }
    }

    public static Props props(final RunDetails runDetails, final DynDataStore dataStore) {
        return Props.create(new Creator<Manager>() {
            private static final long serialVersionUID = 1L;

            public Manager create() throws Exception {
                return new Manager(runDetails.getUrl(), runDetails.getConcurrency(), runDetails.getDuration(),
                        runDetails.getRepeats(), HttpMethods.valueOf(runDetails.getHttpMethod()), runDetails.getData(),
                        runDetails.getHeaders(), runDetails.getCookies(), runDetails.isBodyDynamic(), runDetails
                        .isUrlDynamic(), dataStore);
            }
        });
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ManagerCommand) {
            switch (((ManagerCommand) message).getType()) {
                case START: {
                    printOnCmd("Starting job.");
                    CollectorCommand collectorCommand = new CollectorCommand();
                    collectorCommand.setCommandType(CollectorCommandType.RESET);
                    collector.tell(collectorCommand, getSelf());
                    invoker = getSender();
                    break;
                }
                case COLL_READY: {
                    this.startTime = System.nanoTime();
                    for (ActorRef actorRef : generators) {
                        GeneratorCommand generatorCommand = new GeneratorCommand();
                        generatorCommand.setType(GeneratorCommandType.LOAD);
                        actorRef.tell(generatorCommand, getSelf());
                    }
                    this.runningGenerators = noOfUsers;
                    break;
                }
                case GEN_FREE: {
                    if (!isPaused) {
                        if (durationMode) {
                            Long timeElapsed = System.nanoTime() - startTime - pausedTimeTotal;
                            if (timeElapsed <= duration * 1000000000) {
                                getSender().tell(createLoadCommand(), getSelf());
                                printJobStatus(timeElapsed, duration);
                            } else {
                                runningGenerators--;
                                if (runningGenerators <= 0) {
                                    completeTest();
                                }
                            }
                        } else {
                            if (numOfRepeats > 0) {
                                numOfRepeats--;
                                getSender().tell(createLoadCommand(), getSelf());
                                printJobStatus(numOfRepeats, totalRepeats);
                            } else {
                                runningGenerators--;
                                if (runningGenerators <= 0) {
                                    completeTest();
                                }
                            }
                        }
                    }
                    break;
                }
                case COL_STOPPED: {
                    RunResult runResult = (RunResult) ((ManagerCommand) message).getFromContext("runResult");
                    Double timeTaken = (stopTime - startTime - pausedTimeTotal) / 1000000000.0;
                    runResult.setTotalTimeTaken(timeTaken);
                    runResult.setAverageTimePerRequest(timeTaken / runResult.getTotalRequestCompleted());
                    runResult.setRequestsPerSecond(runResult.getTotalRequestCompleted() / timeTaken);
                    if (!invoker.equals(getSelf())) {
                        System.out.println("Result for this node, for combined result check master terminal : ");
                    }
                    System.out.println(runResult.toString());
                    if (!invoker.equals(getSelf())) {
                        NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                        nodeManagerCommand.setType(NodeManagerCommandType.LOAD_COMPLETE);
                        nodeManagerCommand.putOnContext("runResult", runResult);
                        nodeManagerCommand.putOnContext("responseTimes", ((ManagerCommand) message).getFromContext
                                ("responseTimes"));
                        invoker.tell(nodeManagerCommand, getSelf());
                    }
                    context().stop(getSender());
                    context().stop(getSelf());
                    break;
                }
                case GEN_STOPPED: {
                    runningGenerators--;
                    context().stop(getSender());
                    if (runningGenerators <= 0) {
                        CollectorCommand collectorCommand = new CollectorCommand();
                        collectorCommand.setCommandType(CollectorCommandType.STOP);
                        collector.tell(collectorCommand, getSelf());
                    }
                    break;
                }
                case STOP: {
                    this.stopTime = System.nanoTime();
                    for (ActorRef actorRef : generators) {
                        GeneratorCommand generatorCommand = new GeneratorCommand();
                        generatorCommand.setType(GeneratorCommandType.STOP);
                        actorRef.tell(generatorCommand, getSelf());
                    }
                    break;
                }
                case PAUSE: {
                    this.pausedAt = System.nanoTime();
                    this.isPaused = true;
                    break;
                }
                case RESUME: {
                    this.pausedTimeTotal += (System.nanoTime() - this.pausedAt);
                    this.isPaused = false;
                    generators.forEach((o) -> {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.GEN_FREE);
                        getSelf().tell(managerCommand, o);
                    });
                    break;
                }
            }
        } else {
            unhandled(message);
        }
    }

    private GeneratorCommand createLoadCommand() {
        GeneratorCommand generatorCommand = new GeneratorCommand();
        generatorCommand.setType(GeneratorCommandType.LOAD);
        return generatorCommand;
    }

    private void completeTest() {
        this.runningGenerators = this.noOfUsers;
        ManagerCommand managerCommand = new ManagerCommand();
        managerCommand.setType(ManagerCommandType.STOP);
        getSelf().tell(managerCommand, getSelf());
    }
}

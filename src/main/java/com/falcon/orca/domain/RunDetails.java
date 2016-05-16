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
package com.falcon.orca.domain;

import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shwet.s under project orca. <br/> Created on  06/04/16. <br/> Updated on 06/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */

@Getter
@Setter
public class RunDetails implements Serializable {
    private Integer concurrency;
    private Integer repeats;
    private Long duration;
    private boolean isDurationMode;
    private Long responseTimeOut;
    private List<String> headers = new ArrayList<>();
    private String httpMethod = "GET";
    private String url;
    private List<String> cookies;
    private String data;
    private String dataFilePath;
    private String templateFilePath;
    private boolean bodyDynamic = false;
    private boolean urlDynamic = false;
    @Override
    public String toString() {
        return String.format(
                "RunDetails:" +
                        "\t concurrency= %s%n" +
                        "\t repeats=%s%n" +
                        "\t duration=%s%n" +
                        "\t isDurationMode=%s%n" +
                        "\t responseTimeOut=%s%n" +
                        "\t dataFilePath=%s%n" +
                        "\t templateFilePath=%s%n" +
                        "\t bodyDynamic=%s%n" +
                        "\t urlDynamic=%s%n" +
                        "\t headers=%s%n" +
                        "\t httpMethod=%s%n" +
                        "\t cookies=%s%n" +
                        "\t data=%s%n", concurrency, repeats, duration, isDurationMode, responseTimeOut,
                dataFilePath, templateFilePath, bodyDynamic, urlDynamic, Joiner.on(",").join(headers), httpMethod,
                Joiner.on(",").join(cookies), data);
    }
}

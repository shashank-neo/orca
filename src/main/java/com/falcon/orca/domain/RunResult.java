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

import lombok.*;

import java.io.Serializable;

/**
 * Created by shwet.s under project orca. <br/> Created on  06/04/16. <br/> Updated on 06/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RunResult implements Serializable {
    private Double totalTimeTaken = 0.0;
    private Double averageTimePerRequest = 0.0;
    private Integer totalRequestCompleted = 0;
    private Integer failedRequests = 0;
    private Integer successFullRequests = 0;
    private Long totalDataTransfered = 0L;
    private Double requestsPerSecond = 0.0;
    private Double fiftyPercentile = 0.0;
    private Double seventyFifthPercentile = 0.0;
    private Double nintyPercentile = 0.0;
    private Double nintyFifthPercentile = 0.0;
    private Double nintyEighthPercentile = 0.0;
    private Double nintyNinethPercentile = 0.0;

    @Override
    public String toString() {
        return String.format("RunResult:%n" +
                        "\t totalTimeTaken\t=\t%s secs%n" +
                        "\t failedRequests\t=\t%s%n" +
                        "\t successFullRequests\t=\t%s%n" +
                        "\t averageTimePerRequest\t=\t%s secs%n" +
                        "\t totalRequestCompleted\t=\t%s%n" +
                        "\t totalDataTransfered\t=\t%s bytes%n" +
                        "\t requestsPerSecond\t=\t%s%n%n%n" +
                        "\t 50th Percentile\t=\t%s%n" +
                        "\t 75th Percentile\t=\t%s%n" +
                        "\t 90th Percentile\t=\t%s%n" +
                        "\t 95th Percentile\t=\t%s%n" +
                        "\t 98th Percentile\t=\t%s%n" +
                        "\t 99th Percentile\t=\t%s%n", totalTimeTaken, failedRequests, successFullRequests,
                averageTimePerRequest, totalRequestCompleted, totalDataTransfered, requestsPerSecond, fiftyPercentile,
                seventyFifthPercentile, nintyPercentile, nintyFifthPercentile, nintyEighthPercentile,
                nintyNinethPercentile);
    }
}

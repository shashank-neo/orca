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

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.falcon.orca.commands.CollectorCommand;
import com.falcon.orca.commands.ManagerCommand;
import com.falcon.orca.domain.RunResult;
import com.falcon.orca.enums.ManagerCommandType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by shwet.s under project orca. <br/> Created on  03/04/16. <br/> Updated on 03/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */
public class Collector extends UntypedActor {
    private final List<Long> responseTimes;
    private final Integer reservoirSize = 100000;
    private final Random random = new Random();
    private Integer failureCount;
    private Integer successCount;
    private Integer totalCount;
    private Long totalDataTransferred;

    public Collector() {
        this.responseTimes = new ArrayList<>();
        this.failureCount = 0;
        this.successCount = 0;
        this.totalCount = 0;
        this.totalDataTransferred = 0L;
    }

    public static Props props() {
        return Props.create(new Creator<Collector>() {
            private static final long serialVersionUID = 1L;

            public Collector create() throws Exception {
                return new Collector();
            }
        });
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof CollectorCommand) {
            switch (((CollectorCommand) message).getCommandType()) {
                case RESET:
                    this.responseTimes.clear();
                    this.successCount = 0;
                    this.failureCount = 0;
                    this.totalCount = 0;
                    this.totalDataTransferred = 0L;
                    ManagerCommand collReady = new ManagerCommand();
                    collReady.setType(ManagerCommandType.COLL_READY);
                    getSender().tell(collReady, getSelf());
                    break;
                case COLLECT: {
                    totalCount++;
                    sampleReservoir(responseTimes, (Long) ((CollectorCommand) message).getFromContext("responseTime"));
                    totalDataTransferred += (Long) ((CollectorCommand) message).getFromContext("responseSize");
                    if ((Boolean) ((CollectorCommand) message).getFromContext("isSuccess")) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    break;
                }
                case STOP: {
                    //Send an ack back to Manager
                    Collections.sort(responseTimes);
                    RunResult runResult = RunResult.builder()
                            .failedRequests(failureCount)
                            .successFullRequests(successCount)
                            .totalDataTransfered(totalDataTransferred)
                            .totalRequestCompleted(failureCount + successCount)
                            .fiftyPercentile(responseTimes.get((int) (responseTimes.size() * 0.5)) / 1000000.00)
                            .seventyFifthPercentile(responseTimes.get((int) (responseTimes.size() * 0.75)) / 1000000.00)
                            .nintyPercentile(responseTimes.get((int) (responseTimes.size() * 0.9)) / 1000000.00)
                            .nintyFifthPercentile(responseTimes.get((int) (responseTimes.size() * 0.95)) / 1000000.00)
                            .nintyEighthPercentile(responseTimes.get((int) (responseTimes.size() * 0.98)) / 1000000.00)
                            .nintyNinethPercentile(responseTimes.get((int) (responseTimes.size() * 0.99)) / 1000000.00)
                            .build();
                    ManagerCommand managerCommand = new ManagerCommand();
                    managerCommand.setType(ManagerCommandType.COL_STOPPED);
                    managerCommand.putOnContext("runResult", runResult);
                    managerCommand.putOnContext("responseTimes", responseTimes);
                    getSender().tell(managerCommand, getSelf());
                    break;
                }
            }
        } else {
            unhandled(message);
        }
    }

    private void sampleReservoir(final List<Long> reservoir, final Long number) {
        if (reservoir.size() < reservoirSize) {
            reservoir.add(number);
        } else {
            int j = random.nextInt(totalCount + 1);
            if (j < reservoirSize) {
                reservoir.set(j, number);
            }
        }
    }
}

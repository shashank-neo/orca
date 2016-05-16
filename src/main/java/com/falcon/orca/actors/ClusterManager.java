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
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.japi.Creator;
import com.falcon.orca.commands.ClusterManagerCommand;
import com.falcon.orca.commands.NodeManagerCommand;
import com.falcon.orca.data.readers.DataReader;
import com.falcon.orca.data.readers.impl.JsonFileReader;
import com.falcon.orca.domain.DynGenerator;
import com.falcon.orca.domain.RunDetails;
import com.falcon.orca.domain.RunResult;
import com.falcon.orca.enums.DynVarUseType;
import com.falcon.orca.enums.NodeManagerCommandType;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.falcon.orca.helpers.PrintHelper.*;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  14/04/16. <br/>
 * Updated on 14/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Slf4j
@SuppressWarnings(value = "unchecked")
public class ClusterManager extends UntypedActor {

    private final Cluster cluster = Cluster.get(getContext().system());
    private final List<ActorRef> nodeManagers = new ArrayList<>();
    private Integer minimumNodes = 1;
    private Boolean isBusy = false;
    private Boolean isPaused = false;
    private Integer busyNodes = 0;
    private Integer pausedNodes = 0;
    private List<RunResult> runResults = new ArrayList<>();
    private List<Long> mergedResponseTimes = new ArrayList<>();

    public ClusterManager(final Integer minimumNodes, final String hostname) {
        this.minimumNodes = minimumNodes;
        Cluster.get(getContext().system()).join(new Address("akka.tcp", "default", hostname, 2552));
    }

    public static Props props(final Integer minimumNodes, final String hostname) {
        return Props.create(new Creator<ClusterManager>() {
            private static final long serialVersionUID = 1L;

            public ClusterManager create() throws Exception {
                return new ClusterManager(minimumNodes, hostname);
            }
        });
    }

    @Override
    public void preStart() {
        cluster.subscribe(getSelf(), ClusterEvent.MemberUp.class);
    }

    @Override
    public void onReceive(Object message) {

        if (message instanceof ClusterEvent.CurrentClusterState) {
            log.info("Got message of type Current cluster state");
        } else if (message instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) message;
            //if(memberUp.member().hasRole("node_manager")) {
            NodeManagerCommand command = new NodeManagerCommand();
            command.setType(NodeManagerCommandType.REGISTER_TO_MASTER);
            getContext().actorSelection(memberUp.member().address() + "/user/node_manager").tell(command,
                    getSelf());
            //}
        } else if (message instanceof ClusterEvent.MemberExited) {
            nodeManagers.remove(getSender());
        } else if (message instanceof ClusterEvent.UnreachableMember) {
            log.info("Got message of type unreachable member " + getSender());
            nodeManagers.remove(getSender());
        } else if (message instanceof ClusterManagerCommand) {
            switch (((ClusterManagerCommand) message).getType()) {
                case REGISTER_NODE:
                    nodeManagers.add(getSender());
                    if (nodeManagers.size() >= minimumNodes) {
                        printOnCmd("Minimum number of nodes in cluster complete, you can run tests now.");
                    }
                    break;
                case UNREGISTER_NODE: {
                    printOnCmd("Got a node disconnect request, current size " + nodeManagers.size());
                    nodeManagers.remove(getSender());
                    printOnCmd("Removing node from cluster after removal size " + nodeManagers.size());
                    break;
                }
                case START_LOAD: {
                    try {
                        if (nodeManagers.size() < minimumNodes) {
                            printOnCmd("Not enough numbers of nodes, have patience.");
                        } else if (isBusy) {
                            printOnCmd("Already a run going on can't start another, wait for it to finish");
                        } else {
                            isBusy = true;
                            busyNodes = 0;
                            pausedNodes = 0;
                            mergedResponseTimes.clear();
                            runResults.clear();
                            nodeManagers.forEach((o) -> {
                                NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                nodeManagerCommand.setType(NodeManagerCommandType.CLEAR_LOAD_DATA);
                                o.tell(nodeManagerCommand, getSelf());
                            });
                            ClusterManagerCommand managerCommand = (ClusterManagerCommand) message;
                            RunDetails runDetails = (RunDetails) managerCommand.getFromContext("runDetails");
                            if (runDetails.isUrlDynamic() || runDetails.isBodyDynamic()) {
                                DataReader dataReader = new JsonFileReader(runDetails.getDataFilePath(), runDetails
                                        .getTemplateFilePath());
                                HashMap<String, HashMap<String, List<Object>>> dynDataFromFile = dataReader
                                        .readVariableValues();
                                HashMap<String, HashMap<String, DynVarUseType>> dynVarUseTypeFromFile = dataReader
                                        .readVariableUseType();
                                HashMap<String, DynGenerator> generators = dataReader.readGenerators();
                                if (generators != null) {
                                   generators.forEach((k,v) -> nodeManagers.forEach((o) -> {
                                       NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                       nodeManagerCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                       nodeManagerCommand.putOnContext("key", k);
                                       nodeManagerCommand.putOnContext("data", v);
                                       nodeManagerCommand.putOnContext("dataType", "generator");
                                       o.tell(nodeManagerCommand, getSelf());
                                   }));
                                }
                                final int[] nodeManagerIndex = {0};
                                if (runDetails.isBodyDynamic()) {
                                    HashMap<String, List<Object>> bodyParams = dynDataFromFile.get("bodyData");
                                    HashMap<String, DynVarUseType> bodyParamsUseType = dynVarUseTypeFromFile.get
                                            ("bodyVarUseType");
                                    String template = dataReader.readTemplate();
                                    if (!StringUtils.isBlank(template)) {
                                        NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                        nodeManagerCommand.putOnContext("template", template);
                                        nodeManagerCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                        nodeManagers.forEach((o) -> o.tell(nodeManagerCommand, getSelf()));
                                    }
                                    bodyParams.forEach((k, v) -> {
                                        List<List<Object>> partitions = Lists.partition(v, v.size() / nodeManagers.size());
                                        nodeManagerIndex[0] = 0;
                                        nodeManagers.forEach((o) -> {
                                            List<Object> partition = partitions.get(nodeManagerIndex[0]++);
                                            if (partition.size() < 10000) {
                                                NodeManagerCommand nodeDataCommand = new NodeManagerCommand();
                                                nodeDataCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                                nodeDataCommand.putOnContext("key", k);
                                                nodeDataCommand.putOnContext("data", new ArrayList(partition));
                                                nodeDataCommand.putOnContext("dataType", "body");
                                                nodeDataCommand.putOnContext("dataUseType", bodyParamsUseType.get(k));
                                                o.tell(nodeDataCommand, getSelf());
                                            } else {
                                                List<List<Object>> nodePartitions = Lists.partition(partition, 10000);
                                                for (List<Object> nodePartition : nodePartitions) {
                                                    NodeManagerCommand nodeDataCommand = new NodeManagerCommand();
                                                    nodeDataCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                                    nodeDataCommand.putOnContext("key", k);
                                                    nodeDataCommand.putOnContext("data", new ArrayList(nodePartition));
                                                    nodeDataCommand.putOnContext("dataType", "body");
                                                    nodeDataCommand.putOnContext("dataUseType", bodyParamsUseType
                                                            .get(k));
                                                    o.tell(nodeDataCommand, getSelf());
                                                }
                                            }
                                        });
                                    });
                                }
                                if (runDetails.isUrlDynamic()) {
                                    HashMap<String, List<Object>> urlParams = dynDataFromFile.get("urlData");
                                    HashMap<String, DynVarUseType> urlParamsUseType = dynVarUseTypeFromFile.get
                                            ("urlVarUseType");
                                    //Block to send urlTemplate to each node
                                    {
                                        NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                        nodeManagerCommand.putOnContext("urlTemplate", runDetails.getUrl());
                                        nodeManagerCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                        nodeManagers.forEach((o) -> o.tell(nodeManagerCommand, getSelf()));
                                    }


                                    urlParams.forEach((k, v) -> {
                                        List<List<Object>> partitions = Lists.partition(v, v.size() / nodeManagers
                                                .size());
                                        nodeManagerIndex[0] = 0;
                                        nodeManagers.forEach((o) -> {
                                            List<Object> partition = partitions.get(nodeManagerIndex[0]++);
                                            if (partition.size() < 10000) {
                                                NodeManagerCommand nodeDataCommand = new NodeManagerCommand();
                                                nodeDataCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                                nodeDataCommand.putOnContext("key", k);
                                                nodeDataCommand.putOnContext("data", new ArrayList<>(partition));
                                                nodeDataCommand.putOnContext("dataType", "url");
                                                nodeDataCommand.putOnContext("dataUseType", urlParamsUseType.get(k));
                                                o.tell(nodeDataCommand, getSelf());
                                            } else {
                                                List<List<Object>> nodePartitions = Lists.partition(partition, 10000);
                                                for (List<Object> nodePartition : nodePartitions) {
                                                    NodeManagerCommand nodeDataCommand = new NodeManagerCommand();
                                                    nodeDataCommand.setType(NodeManagerCommandType.TAKE_LOAD_DATA);
                                                    nodeDataCommand.putOnContext("key", k);
                                                    nodeDataCommand.putOnContext("data", new ArrayList<>
                                                            (nodePartition));
                                                    nodeDataCommand.putOnContext("dataType", "url");
                                                    nodeDataCommand.putOnContext("dataUseType", urlParamsUseType.get(k));
                                                    o.tell(nodeDataCommand, getSelf());
                                                }
                                            }
                                        });
                                    });
                                }
                                nodeManagers.forEach((o) -> {
                                    NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                    nodeManagerCommand.setType(NodeManagerCommandType.LOAD_DATA_COMPLETE);
                                    o.tell(nodeManagerCommand, getSelf());
                                });
                            }
                            nodeManagers.forEach((o) -> {
                                NodeManagerCommand command = new NodeManagerCommand();
                                command.putOnContext("runDetails", runDetails);
                                command.setType(NodeManagerCommandType.START_LOAD);
                                o.tell(command, getSelf());
                            });
                        }
                    } catch (IOException ie) {
                        printOnCmd("Datareader failed to read data from file, make sure template file, datafile pathe" +
                                " are correct.");
                        log.error("Datareader failed to load", ie);
                        busyNodes = 0;
                        isBusy = false;
                    }
                    break;
                }
                case LOAD_GENERATION_START: {
                    busyNodes++;
                    break;
                }
                case LOAD_GENERATION_COMPLETE: {
                    runResults.add((RunResult) ((ClusterManagerCommand) message).getFromContext("runResult"));
                    NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                    nodeManagerCommand.setType(NodeManagerCommandType.SEND_DATA);
                    getSender().tell(nodeManagerCommand, getSelf());
                    break;
                }
                case TAKE_DATA: {
                    mergedResponseTimes.addAll((List<Long>) ((ClusterManagerCommand) message).getFromContext(
                            "responseTimes"));
                    break;
                }
                case DATA_SEND_COMPLETE: {
                    busyNodes--;
                    if (busyNodes <= 0) {
                        isBusy = false;
                        printMergedResult(runResults, mergedResponseTimes);
                    }
                    break;
                }
                case STOP_LOAD: {
                    nodeManagers.forEach((o) -> {
                        NodeManagerCommand command = new NodeManagerCommand();
                        command.setType(NodeManagerCommandType.STOP_LOAD);
                        o.tell(command, getSelf());
                    });
                    break;
                }
                case PAUSE_LOAD: {
                    nodeManagers.forEach((o) -> {
                        NodeManagerCommand command = new NodeManagerCommand();
                        command.setType(NodeManagerCommandType.PAUSE_LOAD);
                        o.tell(command, getSelf());
                    });
                    break;
                }
                case LOAD_GENERATION_PAUSED: {
                    pausedNodes++;
                    if (Objects.equals(pausedNodes, busyNodes)) {
                        isPaused = true;
                    }
                    break;
                }
                case LOAD_GENERATION_RESUMED: {
                    pausedNodes--;
                    if (pausedNodes <= 0) {
                        isPaused = false;
                    }
                    break;
                }
                case RESUME_LOAD: {
                    nodeManagers.forEach((o) -> {
                        NodeManagerCommand command = new NodeManagerCommand();
                        command.setType(NodeManagerCommandType.RESUME_LOAD);
                        o.tell(command, getSelf());
                    });
                    break;
                }
                case CLUSTER_DETAILS:
                    printOnCmd(printClusterDetails(isBusy, isPaused, nodeManagers.size(), busyNodes));
                    break;
                case EXIT:
                    //kill all the node managers and kill the local actor system and then exit
                    nodeManagers.forEach((o) -> {
                        NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                        nodeManagerCommand.setType(NodeManagerCommandType.REMOTE_EXIT);
                        o.tell(nodeManagerCommand, getSelf());
                    });
                    context().stop(getSelf());
                    break;
                default:
                    unhandled(message);
            }
        }
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }
}

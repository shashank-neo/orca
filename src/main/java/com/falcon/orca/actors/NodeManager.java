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
import akka.japi.Creator;
import com.falcon.orca.commands.ClusterManagerCommand;
import com.falcon.orca.commands.ManagerCommand;
import com.falcon.orca.commands.NodeManagerCommand;
import com.falcon.orca.domain.DynDataStore;
import com.falcon.orca.domain.DynGenerator;
import com.falcon.orca.domain.RunDetails;
import com.falcon.orca.enums.ClustermanagerCommandType;
import com.falcon.orca.enums.DynVarUseType;
import com.falcon.orca.enums.ManagerCommandType;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.falcon.orca.helpers.PrintHelper.printOnCmd;

/**
 * Created by shwet.s under project orca. <br/> Created on  14/04/16. <br/> Updated on 14/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */
public class NodeManager extends UntypedActor {
    private final Cluster cluster = Cluster.get(getContext().system());
    private final String masterHost;
    private final Integer masterPort;
    private ActorRef manager;
    private ActorRef clusterManager;
    private List<Long> responseTimes;
    private DynDataStore dynDataStore;
    private HashMap<String, List<Object>> bodyParams;
    private HashMap<String, List<Object>> urlParams;
    private HashMap<String, DynVarUseType> bodyParamsUseType;
    private HashMap<String, DynVarUseType> urlParamsUseType;
    private HashMap<String, DynGenerator> dataGenerators;
    private String template;
    private String urlTemplate;

    public NodeManager(final String masterHost, final Integer masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.bodyParams = new HashMap<>();
        this.urlParams = new HashMap<>();
        this.bodyParamsUseType = new HashMap<>();
        this.urlParamsUseType = new HashMap<>();
        this.dataGenerators = new HashMap<>();
    }

    public static Props props(final String masterHost, final Integer masterPort) {
        return Props.create(new Creator<NodeManager>() {
            private static final long serialVersionUID = 1L;

            public NodeManager create() throws Exception {
                return new NodeManager(masterHost, masterPort);
            }
        });
    }

    @Override
    public void preStart() {
        Cluster.get(getContext().system()).join(new Address("akka.tcp", "default", masterHost, masterPort));
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void onReceive(Object message) {
        if (message instanceof NodeManagerCommand) {
            switch (((NodeManagerCommand) message).getType()) {
                case REGISTER_TO_MASTER: {
                    clusterManager = getSender();
                    ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                    clusterManagerCommand.setType(ClustermanagerCommandType.REGISTER_NODE);
                    clusterManager.tell(clusterManagerCommand, getSelf());
                    break;
                }
                case UNREGISTER_FROM_MASTER: {
                    if (manager != null && !manager.isTerminated()) {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.STOP);
                        manager.tell(managerCommand, manager);
                    }
                    ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                    clusterManagerCommand.setType(ClustermanagerCommandType.UNREGISTER_NODE);
                    clusterManager.tell(clusterManagerCommand, getSelf());
                    cluster.leave(cluster.selfAddress());
                    context().stop(getSelf());
                    break;
                }
                case START_LOAD:
                    if (manager != null && !manager.isTerminated()) {
                        printOnCmd("Already running a job, please wait!!");
                    } else {
                        NodeManagerCommand nodeManagerCommand = (NodeManagerCommand) message;
                        RunDetails runDetails = (RunDetails) nodeManagerCommand.getFromContext("runDetails");
                        manager = getContext().actorOf(Manager.props(runDetails, dynDataStore));
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.START);
                        manager.tell(managerCommand, getSelf());

                        ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                        clusterManagerCommand.setType(ClustermanagerCommandType.LOAD_GENERATION_START);
                        clusterManager.tell(clusterManagerCommand, getSelf());
                    }
                    break;
                case PAUSE_LOAD: {
                    if (manager != null && !manager.isTerminated()) {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.PAUSE);
                        manager.tell(managerCommand, getSelf());

                        ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                        clusterManagerCommand.setType(ClustermanagerCommandType.LOAD_GENERATION_PAUSED);
                        clusterManager.tell(clusterManagerCommand, getSelf());
                    } else {
                        printOnCmd("No active jobs to pause.");
                    }
                    break;
                }
                case RESUME_LOAD: {
                    if (manager != null && !manager.isTerminated()) {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.RESUME);
                        manager.tell(managerCommand, manager);

                        ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                        clusterManagerCommand.setType(ClustermanagerCommandType.LOAD_GENERATION_RESUMED);
                        clusterManager.tell(clusterManagerCommand, getSelf());
                    } else {
                        printOnCmd("No paused jobs to resume.");
                    }
                    break;
                }
                case STOP_LOAD: {
                    if (manager != null && !manager.isTerminated()) {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.STOP);
                        manager.tell(managerCommand, manager);

                        ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                        clusterManagerCommand.setType(ClustermanagerCommandType.LOAD_GENERATION_COMPLETE);
                        clusterManager.tell(clusterManagerCommand, getSelf());
                        printOnCmd("Job killed successfully.");
                    } else {
                        printOnCmd("No job running.");
                    }
                    break;
                }
                case EXIT: {
                    if (manager != null && !manager.isTerminated()) {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.STOP);
                        manager.tell(managerCommand, manager);
                    }
                    ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                    clusterManagerCommand.setType(ClustermanagerCommandType.UNREGISTER_NODE);
                    clusterManager.tell(clusterManagerCommand, getSelf());
                    cluster.leave(cluster.selfAddress());
                    context().stop(getSelf());
                    break;
                }
                case REMOTE_EXIT: {
                    if (manager != null && !manager.isTerminated()) {
                        ManagerCommand managerCommand = new ManagerCommand();
                        managerCommand.setType(ManagerCommandType.STOP);
                        manager.tell(managerCommand, manager);
                    }
                    cluster.leave(cluster.selfAddress());
                    context().stop(getSelf());
                    break;
                }
                case LOAD_COMPLETE: {
                    ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                    clusterManagerCommand.setType(ClustermanagerCommandType.LOAD_GENERATION_COMPLETE);
                    clusterManagerCommand.putOnContext("runResult", ((NodeManagerCommand) message).getFromContext
                            ("runResult"));
                    clusterManager.tell(clusterManagerCommand, getSelf());
                    responseTimes = (List<Long>) ((NodeManagerCommand) message).getFromContext("responseTimes");
                    break;
                }
                case SEND_DATA: {
                    List<List<Long>> responseTimePartitions = Lists.partition(responseTimes, 1000);
                    for (List<Long> responseTimePartition : responseTimePartitions) {
                        ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                        clusterManagerCommand.setType(ClustermanagerCommandType.TAKE_DATA);
                        clusterManagerCommand.putOnContext("responseTimes", new ArrayList<>(responseTimePartition));
                        getSender().tell(clusterManagerCommand, getSelf());
                    }
                    ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                    clusterManagerCommand.setType(ClustermanagerCommandType.DATA_SEND_COMPLETE);
                    getSender().tell(clusterManagerCommand, getSelf());
                    break;
                }
                case CLEAR_LOAD_DATA: {
                    this.bodyParams.clear();
                    this.urlParams.clear();
                    this.bodyParamsUseType.clear();
                    this.urlParamsUseType.clear();
                    this.dataGenerators.clear();
                    break;
                }
                case TAKE_LOAD_DATA: {
                    String template = (String) ((NodeManagerCommand) message).getFromContext("template");
                    if(!StringUtils.isBlank(template)) {
                        this.template = template;
                    }
                    String urlTemplate = (String) ((NodeManagerCommand) message).getFromContext("urlTemplate");
                    if(!StringUtils.isBlank(urlTemplate)) {
                        this.urlTemplate = urlTemplate;
                    }
                    String key = (String) ((NodeManagerCommand) message).getFromContext("key");
                    String dataType  = (String) ((NodeManagerCommand) message).getFromContext("dataType");

                    if(dataType!= null && dataType.equalsIgnoreCase("body")) {
                        List<Object> data = (List<Object>) ((NodeManagerCommand) message).getFromContext("data");
                        DynVarUseType varUseType = (DynVarUseType) ((NodeManagerCommand) message).getFromContext
                                ("dataUseType");
                        if(!bodyParams.containsKey(key)) {
                            if(varUseType.equals(DynVarUseType.USE_MULTIPLE)) {
                                bodyParams.put(key, new ArrayList<>());
                                bodyParamsUseType.put(key, DynVarUseType.USE_MULTIPLE);
                            } else if(varUseType.equals(DynVarUseType.USE_ONCE)){
                                bodyParams.put(key, new LinkedList<>());
                                bodyParamsUseType.put(key, DynVarUseType.USE_ONCE);
                            }
                        }
                        bodyParams.get(key).addAll(data);
                    } else if(dataType != null && dataType.equalsIgnoreCase("url")) {
                        List<Object> data = (List<Object>) ((NodeManagerCommand) message).getFromContext("data");
                        DynVarUseType varUseType = (DynVarUseType) ((NodeManagerCommand) message).getFromContext
                                ("dataUseType");
                        if(!urlParams.containsKey(key)) {
                            if(varUseType.equals(DynVarUseType.USE_MULTIPLE)) {
                                urlParams.put(key, new ArrayList<>());
                                urlParamsUseType.put(key, DynVarUseType.USE_MULTIPLE);
                            } else if(varUseType.equals(DynVarUseType.USE_ONCE)){
                                urlParams.put(key, new LinkedList<>());
                                urlParamsUseType.put(key, DynVarUseType.USE_ONCE);
                            }
                        }
                        urlParams.get(key).addAll(data);
                    } else if(dataType != null && dataType.equalsIgnoreCase("generator")) {
                        DynGenerator generator = (DynGenerator) ((NodeManagerCommand) message).getFromContext("data");
                        dataGenerators.put(key, generator);
                    }
                    break;
                }
                case LOAD_DATA_COMPLETE: {
                    this.dynDataStore = new DynDataStore(bodyParams, urlParams, bodyParamsUseType, urlParamsUseType,
                            dataGenerators, template, urlTemplate);
                    break;
                }
                default:
                    unhandled(message);
            }
        }
    }
}

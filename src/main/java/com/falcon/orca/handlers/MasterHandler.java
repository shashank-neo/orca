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

package com.falcon.orca.handlers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.falcon.orca.actors.ClusterManager;
import com.falcon.orca.actors.NodeManager;
import com.falcon.orca.commands.ClusterManagerCommand;
import com.falcon.orca.commands.NodeManagerCommand;
import com.falcon.orca.domain.RunDetails;
import com.falcon.orca.enums.ClustermanagerCommandType;
import com.falcon.orca.enums.NodeManagerCommandType;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.falcon.orca.helpers.CommandHelper.*;
import static com.falcon.orca.helpers.PrintHelper.printHelpMasterMode;
import static com.falcon.orca.helpers.PrintHelper.printOnCmd;

/**
 * Created by shwet.s under project orca. <br/> Created on  15/04/16. <br/> Updated on 15/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */
public class MasterHandler extends ModeHandler {

    private final Integer minimumNodes;
    private final String hostname;

    public MasterHandler(final ActorSystem actorSystem, final Integer minimumNodes, final String hostname) {
        this.actorSystem = actorSystem;
        this.minimumNodes = minimumNodes;
        this.hostname = hostname;
    }

    @Override
    public void handle() {

        final ActorRef clusterManager = actorSystem.actorOf(ClusterManager.props(this.minimumNodes, hostname));

        //Register the local nodeManager
        //TODO: host and port should not be required in local nodeManager case
        final ActorRef localNodeManager = actorSystem.actorOf(NodeManager.props(hostname, 2552));
        NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
        nodeManagerCommand.setType(NodeManagerCommandType.REGISTER_TO_MASTER);
        localNodeManager.tell(nodeManagerCommand, clusterManager);

        //Read the input on console and take decision.
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        Options options = createMasterOptions();
        printOnCmd("Welcome to ORCA type help to see what ORCA can do. You have started the node in master mode.");
        try {
            String command = br.readLine();
            while (command != null) {
                if (!StringUtils.isEmpty(command)) {
                    try {
                        String[] treatedCommandParts = treatCommands(command);
                        commandLine = commandLineParser.parse(options, treatedCommandParts);
                        if (commandLine.hasOption("start")) {
                            RunDetails runDetails = createRunDetails(commandLine);
                            ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                            clusterManagerCommand.setType(ClustermanagerCommandType.START_LOAD);
                            clusterManagerCommand.putOnContext("runDetails", runDetails);
                            clusterManager.tell(clusterManagerCommand, clusterManager);
                        } else if (commandLine.hasOption("stop")) {
                            ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                            clusterManagerCommand.setType(ClustermanagerCommandType.STOP_LOAD);
                            clusterManager.tell(clusterManagerCommand, clusterManager);
                        } else if (commandLine.hasOption("exit")) {
                            ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                            clusterManagerCommand.setType(ClustermanagerCommandType.EXIT);
                            clusterManager.tell(clusterManagerCommand, clusterManager);
                            actorSystem.shutdown();
                            actorSystem.awaitTermination(new FiniteDuration(1, TimeUnit.MINUTES));
                            break;
                        } else if (commandLine.hasOption("pause")) {
                            ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                            clusterManagerCommand.setType(ClustermanagerCommandType.PAUSE_LOAD);
                            clusterManager.tell(clusterManagerCommand, clusterManager);
                        } else if (commandLine.hasOption("resume")) {
                            ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                            clusterManagerCommand.setType(ClustermanagerCommandType.RESUME_LOAD);
                            clusterManager.tell(clusterManagerCommand, clusterManager);
                        } else if (commandLine.hasOption("clusterDetails")) {
                            ClusterManagerCommand clusterManagerCommand = new ClusterManagerCommand();
                            clusterManagerCommand.setType(ClustermanagerCommandType.CLUSTER_DETAILS);
                            clusterManager.tell(clusterManagerCommand, clusterManager);
                        } else {
                            printOnCmd(printHelpMasterMode());
                        }
                    } catch (ParseException pe) {
                        printOnCmd(printHelpMasterMode());
                    } catch (MalformedURLException me) {
                        me.printStackTrace();
                    }
                } else {
                    printOnCmd("", false);
                }
                command = br.readLine();
            }
        } catch (IOException e) {
            printOnCmd("Failed to read your command, try again.");
        }
    }
}

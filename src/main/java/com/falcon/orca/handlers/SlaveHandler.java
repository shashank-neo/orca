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
import com.falcon.orca.actors.NodeManager;
import com.falcon.orca.commands.NodeManagerCommand;
import com.falcon.orca.enums.NodeManagerCommandType;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.falcon.orca.helpers.CommandHelper.createSlaveOptions;
import static com.falcon.orca.helpers.CommandHelper.treatCommands;
import static com.falcon.orca.helpers.PrintHelper.printHelpSlaveMode;
import static com.falcon.orca.helpers.PrintHelper.printOnCmd;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/04/16. <br/>
 * Updated on 15/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class SlaveHandler extends ModeHandler {

    private ActorRef nodeManager;

    public SlaveHandler(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public void handle() {
        CommandLine commandLine;
        CommandLineParser commandLineParser = new DefaultParser();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        Options options = createSlaveOptions();
        printOnCmd("Welcome to ORCA type help to see what ORCA can do.");
        try {
            String command = br.readLine();
            while (command != null) {
                if (!StringUtils.isEmpty(command)) {
                    try {
                        String[] treatedCommandParts = treatCommands(command);
                        commandLine = commandLineParser.parse(options, treatedCommandParts);
                        if (commandLine.hasOption("connect")) {
                            String masterHost;
                            if (commandLine.hasOption("masterHost")) {
                                masterHost = commandLine.getOptionValue("masterHost");
                            } else {
                                throw new MissingArgumentException("Master host is required to connect");
                            }

                            Integer masterPort;
                            if (commandLine.hasOption("masterPort")) {
                                masterPort = Integer.valueOf(commandLine.getOptionValue("masterPort"));
                            } else {
                                throw new MissingArgumentException("Master port is required to connect");
                            }
                            nodeManager = actorSystem.actorOf(NodeManager.props(masterHost, masterPort),
                                    "node_manager");
                        } else if (commandLine.hasOption("disconnect")) {
                            if (nodeManager == null || nodeManager.isTerminated()) {
                                printOnCmd("node is not part of any cluster");
                            } else {
                                NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                nodeManagerCommand.setType(NodeManagerCommandType.UNREGISTER_FROM_MASTER);
                                nodeManager.tell(nodeManagerCommand, nodeManager);
                            }
                        } else if (commandLine.hasOption("exit")) {
                            if (nodeManager != null && !nodeManager.isTerminated()) {
                                NodeManagerCommand nodeManagerCommand = new NodeManagerCommand();
                                nodeManagerCommand.setType(NodeManagerCommandType.EXIT);
                                nodeManager.tell(nodeManagerCommand, nodeManager);
                            }
                            actorSystem.shutdown();
                            actorSystem.awaitTermination(new FiniteDuration(1, TimeUnit.MINUTES));
                            break;
                        } else {
                            printOnCmd(printHelpSlaveMode());
                        }
                    } catch (ParseException pe) {
                        printOnCmd(printHelpSlaveMode());
                    }
                } else {
                    printOnCmd("", false);
                }
                command = br.readLine();
            }
        } catch (IOException e) {
            printOnCmd("Failed to read input from command line, please try again.");
        }
    }
}

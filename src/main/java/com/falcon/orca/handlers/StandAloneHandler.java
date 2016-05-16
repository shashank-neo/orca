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
import com.falcon.orca.actors.Manager;
import com.falcon.orca.commands.ManagerCommand;
import com.falcon.orca.data.readers.DataReader;
import com.falcon.orca.data.readers.impl.JsonFileReader;
import com.falcon.orca.domain.DynDataStore;
import com.falcon.orca.domain.DynGenerator;
import com.falcon.orca.domain.RunDetails;
import com.falcon.orca.enums.DynVarUseType;
import com.falcon.orca.enums.ManagerCommandType;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.falcon.orca.helpers.CommandHelper.*;
import static com.falcon.orca.helpers.PrintHelper.printHelpStandAloneMode;
import static com.falcon.orca.helpers.PrintHelper.printOnCmd;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/04/16. <br/>
 * Updated on 15/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class StandAloneHandler extends ModeHandler {
    private ActorRef manager;

    public StandAloneHandler(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public void handle() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        Options options = createOptions();
        printOnCmd("Welcome to ORCA type --help | -h  to see what ORCA can do.");
        try {
            String command = br.readLine();
            while (command != null) {
                if (!StringUtils.isEmpty(command)) {
                    try {
                        String[] treatedCommandParts = treatCommands(command);
                        commandLine = commandLineParser.parse(options, treatedCommandParts);
                        if (commandLine.hasOption("start")) {
                            if (manager != null && !manager.isTerminated()) {
                                printOnCmd("Already running a job, please wait!!");
                            } else {
                                RunDetails runDetails = createRunDetails(commandLine);
                                if(runDetails.isBodyDynamic() || runDetails.isUrlDynamic()) {
                                    DataReader dataReader = new JsonFileReader(runDetails.getDataFilePath(), runDetails
                                            .getTemplateFilePath());
                                    HashMap<String, HashMap<String, List<Object>>> dynDataFromFile = dataReader
                                            .readVariableValues();
                                    HashMap<String, HashMap<String, DynVarUseType>> dynVarUseTypeFromFile = dataReader
                                            .readVariableUseType();
                                    HashMap<String, List<Object>> bodyParams = null;
                                    String template = null;
                                    HashMap<String, DynVarUseType> bodyParamsUseType = null;
                                    HashMap<String, List<Object>> urlParams = null;
                                    HashMap<String, DynVarUseType> urlParamsUseType = null;
                                    if(runDetails.isBodyDynamic()) {
                                        bodyParams = dynDataFromFile.get("bodyData");
                                        template = dataReader.readTemplate();
                                        bodyParamsUseType = dynVarUseTypeFromFile.get("bodyVarUseType");
                                    }
                                    if(runDetails.isUrlDynamic()) {
                                        urlParams = dynDataFromFile.get("urlData");
                                        urlParamsUseType = dynVarUseTypeFromFile.get("urlVarUseType");
                                    }
                                    HashMap<String, DynGenerator> generators = dataReader.readGenerators();
                                    DynDataStore dataStore = new DynDataStore(bodyParams, urlParams,
                                            bodyParamsUseType, urlParamsUseType, generators, template, runDetails
                                            .getUrl());
                                    manager = actorSystem.actorOf(Manager.props(runDetails, dataStore));
                                } else {
                                    manager = actorSystem.actorOf(Manager.props(runDetails, null));
                                }
                                ManagerCommand managerCommand = new ManagerCommand();
                                managerCommand.setType(ManagerCommandType.START);
                                manager.tell(managerCommand, manager);
                            }
                        } else if (commandLine.hasOption("stop")) {
                            if (manager != null && !manager.isTerminated()) {
                                ManagerCommand managerCommand = new ManagerCommand();
                                managerCommand.setType(ManagerCommandType.STOP);
                                manager.tell(managerCommand, manager);
                                printOnCmd("Job killed successfully.");
                            } else {
                                printOnCmd("No job running.");
                            }
                        } else if (commandLine.hasOption("exit")) {
                            if (manager != null && !manager.isTerminated()) {
                                ManagerCommand managerCommand = new ManagerCommand();
                                managerCommand.setType(ManagerCommandType.STOP);
                                manager.tell(managerCommand, manager);
                            }
                            printOnCmd("Exiting the system gracefully now.");
                            actorSystem.shutdown();
                            actorSystem.awaitTermination(new FiniteDuration(1, TimeUnit.MINUTES));
                            break;
                        } else if (commandLine.hasOption("pause")) {
                            if (manager != null && !manager.isTerminated()) {
                                ManagerCommand managerCommand = new ManagerCommand();
                                managerCommand.setType(ManagerCommandType.PAUSE);
                                manager.tell(managerCommand, manager);
                            } else {
                                printOnCmd("No active jobs to pause.");
                            }
                        } else if (commandLine.hasOption("resume")) {
                            if (manager != null && !manager.isTerminated()) {
                                ManagerCommand managerCommand = new ManagerCommand();
                                managerCommand.setType(ManagerCommandType.RESUME);
                                manager.tell(managerCommand, manager);
                            } else {
                                printOnCmd("No paused job to resume.");
                            }
                        } else {
                            printOnCmd(printHelpStandAloneMode());
                        }
                    } catch (ParseException pe) {
                        printOnCmd(printHelpStandAloneMode());
                    } catch (MalformedURLException me) {
                        me.printStackTrace();
                    }
                } else {
                    printOnCmd("", false);
                }
                command = br.readLine();
            }
        } catch (IOException e) {
            printOnCmd("Something went wrong in reading input, please try again.");
        }
    }
}

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
package com.falcon.orca;

import akka.actor.ActorSystem;
import com.falcon.orca.handlers.MasterHandler;
import com.falcon.orca.handlers.ModeHandler;
import com.falcon.orca.handlers.SlaveHandler;
import com.falcon.orca.handlers.StandAloneHandler;
import org.apache.commons.cli.*;

import java.io.IOException;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  02/04/16. <br/>
 * Updated on 02/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Option mode = Option.builder("mo").longOpt("mode").required(true).hasArg(true).desc("Mode to start the node " +
                "in, possible values are standalone master slave").build();
        Option host = Option.builder("ho").longOpt("host").hasArg(true).desc("Machine name").build();
        Options modeOptions = new Options();
        modeOptions.addOption(mode);
        modeOptions.addOption(host);
        CommandLineParser commandLineParser = new DefaultParser();
        ModeHandler handler;
        final ActorSystem actorSystem = ActorSystem.create();
        String machineName = "127.0.0.1";
        try {
            CommandLine commandLine = commandLineParser.parse(modeOptions, args);
            if (commandLine.hasOption("host")) {
                machineName = commandLine.getOptionValue("host");
            }
            if (commandLine.hasOption("mode")) {
                String modeValue = commandLine.getOptionValue("mode");
                if ("standalone".equalsIgnoreCase(modeValue)) {
                    handler = new StandAloneHandler(actorSystem);
                    handler.handle();
                } else if ("master".equalsIgnoreCase(modeValue)) {
                    handler = new MasterHandler(actorSystem, 1, machineName);
                    handler.handle();
                } else if ("slave".equalsIgnoreCase(modeValue)) {
                    handler = new SlaveHandler(actorSystem);
                    handler.handle();
                } else {
                    actorSystem.shutdown();
                    System.out.println("Mode is required, use -mo or --mode, possible values are standalone, master " +
                            "and slave");
                }
            } else {
                actorSystem.shutdown();
                System.out.println("Mode is required, use -mo or --mode, possible values are standalone, master and " +
                        "slave");
            }
        } catch (ParseException e) {
            actorSystem.shutdown();
            System.out.println("Mode is required, use -mo or --mode, possible values are standalone, master and slave");
        }
    }


}


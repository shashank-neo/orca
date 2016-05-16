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
package com.falcon.orca.helpers;

import com.falcon.orca.domain.RunDetails;
import com.google.common.base.Joiner;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;

import java.net.MalformedURLException;
import java.util.Arrays;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/04/16. <br/>
 * Updated on 15/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class CommandHelper {

    private CommandHelper() {
    }

    public static Options createSlaveOptions() {
        Option connect = Option.builder("cn").longOpt("connect").build();
        Option url = Option.builder("MH").longOpt("masterHost").hasArg(true).build();
        Option port = Option.builder("MP").longOpt("masterPort").hasArg(true).build();
        Option disconnect = Option.builder("dcn").longOpt("disconnect").build();
        Option exit = Option.builder("e").longOpt("exit").build();
        Options options = new Options();
        options.addOption(connect);
        options.addOption(url);
        options.addOption(port);
        options.addOption(disconnect);
        options.addOption(exit);
        return options;
    }

    public static Options createMasterOptions() {
        Option start = Option.builder("s").longOpt("start").build();
        Option stop = Option.builder("st").longOpt("stop").build();
        Option pause = Option.builder("p").longOpt("pause").build();
        Option continueOpt = Option.builder("re").longOpt("resume").build();
        Option exit = Option.builder("e").longOpt("exit").build();
        Option clusterDetails = Option.builder("cd").longOpt("clusterDetails").build();
        Option url = Option.builder("U").longOpt("url").hasArg(true).build();
        Option durationMode = Option.builder("DM").longOpt("durationMode").hasArg(true).build();
        Option duration = Option.builder("DU").longOpt("duration").hasArg(true).build();
        Option repeats = Option.builder("R").longOpt("repeats").hasArg(true).build();
        Option method = Option.builder("M").longOpt("method").hasArg(true).build();
        Option header = Option.builder("H").longOpt("header").hasArg(true).build();
        Option concurrency = Option.builder("C").longOpt("concurrency").hasArg(true).build();
        Option templateFile = Option.builder("TF").longOpt("template").hasArg(true).build();
        Option dataFile = Option.builder("DF").longOpt("dataFile").hasArg(true).build();
        Option cookies = Option.builder("CO").longOpt("cookie").hasArg(true).build();
        Option data = Option.builder("D").longOpt("data").hasArg(true).type(String.class).build();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(start);
        optionGroup.addOption(stop);
        optionGroup.addOption(pause);
        optionGroup.addOption(continueOpt);
        optionGroup.addOption(clusterDetails);
        optionGroup.addOption(exit);
        Options options = new Options();
        options.addOptionGroup(optionGroup);
        options.addOption(url);
        options.addOption(durationMode);
        options.addOption(duration);
        options.addOption(repeats);
        options.addOption(method);
        options.addOption(header);
        options.addOption(concurrency);
        options.addOption(cookies);
        options.addOption(data);
        options.addOption(templateFile);
        options.addOption(dataFile);
        return options;
    }

    public static Options createOptions() {
        Option start = Option.builder("s").longOpt("start").build();
        Option stop = Option.builder("st").longOpt("stop").build();
        Option pause = Option.builder("p").longOpt("pause").build();
        Option continueOpt = Option.builder("re").longOpt("resume").build();
        Option exit = Option.builder("e").longOpt("exit").build();
        Option url = Option.builder("U").longOpt("url").hasArg(true).build();
        Option durationMode = Option.builder("DM").longOpt("durationMode").hasArg(true).build();
        Option duration = Option.builder("DU").longOpt("duration").hasArg(true).build();
        Option repeats = Option.builder("R").longOpt("repeats").hasArg(true).build();
        Option method = Option.builder("M").longOpt("method").hasArg(true).build();
        Option header = Option.builder("H").longOpt("header").hasArg(true).build();
        Option concurrency = Option.builder("C").longOpt("concurrency").hasArg(true).build();
        Option templateFile = Option.builder("TF").longOpt("template").hasArg(true).build();
        Option dataFile = Option.builder("DF").longOpt("dataFile").hasArg(true).build();
        Option cookies = Option.builder("CO").longOpt("cookie").hasArg(true).build();
        Option data = Option.builder("D").longOpt("data").hasArg(true).type(String.class).build();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(start);
        optionGroup.addOption(stop);
        optionGroup.addOption(pause);
        optionGroup.addOption(continueOpt);
        optionGroup.addOption(exit);
        Options options = new Options();
        options.addOptionGroup(optionGroup);
        options.addOption(url);
        options.addOption(durationMode);
        options.addOption(duration);
        options.addOption(repeats);
        options.addOption(method);
        options.addOption(header);
        options.addOption(concurrency);
        options.addOption(cookies);
        options.addOption(data);
        options.addOption(templateFile);
        options.addOption(dataFile);
        return options;
    }

    public static RunDetails createRunDetails(final CommandLine commandLine) throws MissingArgumentException,
            MalformedURLException {
        RunDetails runDetails = new RunDetails();
        if (commandLine.hasOption("url") && !StringUtils.isBlank(commandLine.getOptionValue("url"))) {
            runDetails.setUrl(commandLine.getOptionValue("url"));
        } else {
            throw new MissingArgumentException("--url Please provide URL to hit.");
        }
        if(commandLine.hasOption("dataFile") && commandLine.hasOption("template")) {
            runDetails.setBodyDynamic(true);
        }
        if((commandLine.getOptionValue("url").contains("{{") && commandLine.hasOption("dataFile")) || (commandLine
                .getOptionValue("url").contains("@@") && commandLine.hasOption("dataFile"))) {
            runDetails.setUrlDynamic(true);
        }
        if (commandLine.hasOption("durationMode")) {
            runDetails.setDurationMode(Boolean.valueOf(
                    commandLine.getOptionValue("durationMode")));
        } else {
            runDetails.setDurationMode(false);
        }
        if (runDetails.isDurationMode()) {
            if (commandLine.hasOption("duration")) {
                runDetails.setDuration(Long.valueOf(commandLine.getOptionValue("duration")));
            } else {
                throw new MissingArgumentException("--duration duration of run in  " +
                        "seconds is required in duration mode");
            }
        } else {
            if (commandLine.hasOption("repeats")) {
                runDetails.setRepeats(Integer.valueOf(commandLine.getOptionValue
                        ("repeats")));
            } else {
                throw new MissingArgumentException("--repeats please provide number " +
                        "of repeats to make");
            }
        }
        if(commandLine.hasOption("template")) {
            runDetails.setTemplateFilePath(commandLine.getOptionValue("template"));
        }
        if(commandLine.hasOption("dataFile")) {
            runDetails.setDataFilePath(commandLine.getOptionValue("dataFile"));
        }
        if (commandLine.hasOption("method")) {
            runDetails.setHttpMethod(commandLine.getOptionValue("method"));
        } else {
            runDetails.setHttpMethod("GET");
        }
        if (commandLine.hasOption("header")) {
            runDetails.setHeaders(Arrays.asList(commandLine.getOptionValues("header")));
        }
        if (commandLine.hasOption("cookie")) {
            runDetails.setCookies(Arrays.asList(commandLine.getOptionValues("cookie")));
        }
        if (commandLine.hasOption("data")) {
            runDetails.setData(Joiner.on(' ').join(commandLine.getOptionValues("data")));
        }
        if (commandLine.hasOption("concurrency")) {
            runDetails.setConcurrency(
                    Integer.valueOf(commandLine.getOptionValue("concurrency")));
        } else {
            throw new MissingArgumentException("--concurrency Provide number of " +
                    "users to simulate.");
        }
        return runDetails;
    }

    public static String[] treatCommands(String command) {
        command = command.replaceAll("\\s+(?=((\\\\[\\\\\']|[^\\\\\'])*\'(\\\\[\\\\\']" +
                "|[^\\\\\'])*\')*(\\\\[\\\\\']|[^\\\\\'])*$)", "%20");
        String[] commandParts = command.split("%20");
        String[] treatedCommandParts = new String[commandParts.length];
        for (int i = 0; i < commandParts.length; i++) {
            treatedCommandParts[i] = commandParts[i].replaceAll("'", "");
        }
        return treatedCommandParts;
    }
}

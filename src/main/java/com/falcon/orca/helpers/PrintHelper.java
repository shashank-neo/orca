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

import com.falcon.orca.domain.RunResult;

import java.util.Collections;
import java.util.List;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/04/16. <br/>
 * Updated on 15/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class PrintHelper {

    public static String printHelpStandAloneMode() {
        return "Orca usage: (Stand alone mode) \n" +
                "\t--start | -s \t\tStart a new load job with following parameters\n" +
                "\t\t--url | -U \t\tRequest URL\n" +
                "\t\t--durationMode | -DM [true or false]\t\tLoad will run for some duration, default is false\n" +
                "\t\t--repeats | -R \t\tNumber of requests to make[Not required in duration mode]\n" +
                "\t\t--duration | -DU \t\tNumber of seconds for test to run[Not required in repeat mode]\n" +
                "\t\t--header | -H \t\tHeader to be sent while making request, for multiple headers use multiple " +
                "times.\n" +
                "\t\t--cookie | -CO \t\tCookie to be sent while making request, for multiple cookies use multiple " +
                "times.\n" +
                "\t\t--method | -M \t\tHttp method to use, possible values GET,POST,PUT,HEAD,OPTIONS, default is " +
                "GET.\n" +
                "\t\t--data | -D \t\tData to post to server, will be used only for POST, PUT, DELETE.\n" +
                "\t\t--template | -TF \t\tRequest Data template file path.\n" +
                "\t\t--dataFile | -DF \t\tData file path, which will be used to fill template.\n" +
                "\t--stop | -st \t\tStop the current running load job.\n" +
                "\t--pause | -p \t\tPause the current running load job.\n" +
                "\t--resume | -re \t\tContinue the currently paused load job.\n" +
                "\t--exit | -e \t\tExit the program.\n" +
                "\t--help | -h \t\tPrint this message.\n";
    }

    public static String printHelpMasterMode() {
        return "Orca usage: (Master mode) \n" +
                "\t--start | -s \t\tStart a new load job with following parameters\n" +
                "\t\t--url | -U \t\tRequest URL\n" +
                "\t\t--durationMode | -DM [true or false]\t\tLoad will run for some duration, default is false\n" +
                "\t\t--repeats | -R \t\tNumber of requests to make[Not required in duration mode]\n" +
                "\t\t--duration | -DU \t\tNumber of seconds for test to run[Not required in repeat mode]\n" +
                "\t\t--header | -H \t\tHeader to be sent while making request, for multiple headers use multiple " +
                "times.\n" +
                "\t\t--cookie | -CO \t\tCookie to be sent while making request, for multiple cookies use multiple " +
                "times.\n" +
                "\t\t--method | -M \t\tHttp method to use, possible values GET,POST,PUT,HEAD,OPTIONS, default is " +
                "GET.\n" +
                "\t\t--data | -D \t\tData to post to server, will be used only for POST, PUT, DELETE.\n" +
                "\t\t--template | -TF \t\tRequest Data template file path.\n" +
                "\t\t--dataFile | -DF \t\tData file path, which will be used to fill template.\n" +
                "\t--stop | -st \t\tStop the current running load job.\n" +
                "\t--clusterDetails | -cd \t\tPrint cluster details, running job details.\n" +
                "\t--pause | -p \t\tPause the current running load job.\n" +
                "\t--resume | -re \t\tContinue the currently paused load job.\n" +
                "\t--exit | -e \t\tExit the program.\n" +
                "\t--help | -h \t\tPrint this message.\n";
    }

    public static String printHelpSlaveMode() {
        return "Orca usage: (Slave mode) \n" +
                "\t--connect | -cn \t\tConect to a master cluster\n" +
                "\t\t--masterHost | -MH \t\tMaster host to connect to\n" +
                "\t\t--masterPort | -MP \t\tMaster port to connect to\n" +
                "\t--disconnect | -dcn \t\tLeave cluster gracefully.\n" +
                "\t--exit | -e \t\tExit the program.\n" +
                "\t--help | -h \t\tPrint this message.\n";
    }

    public static String printClusterDetails(boolean isBusy, boolean isPaused, int numberOfNodes, int busyNodes) {
        return String.format("Cluster Details: %n" +
                "\t status\t\t%s%n" +
                "\t totalNodes\t\t%s%n" +
                "\t busyNodes\t\t%s%n", isBusy ? "BUSY" : isPaused ? "PAUSED" : "FREE", numberOfNodes, busyNodes);
    }

    public static void printOnCmd(final String message, final boolean printOnSameLine) {
        if (printOnSameLine) {
            System.out.println("ORCA>> " + message);
            System.out.print("ORCA>> ");
        } else {
            System.out.print("ORCA>> " + message);
        }
    }

    public static void printJobStatus(final Long timeElapsed, final Long duration) {
        double percentage = ((timeElapsed * 100.0) / (duration * 1000000000.0));
        if (percentage % 10 == 0) {
            printOnCmd(percentage + "% job done.");
        }
    }

    public static void printJobStatus(final Integer leftRepeats, final Integer totalRepeats) {
        int completedRepeats = totalRepeats - leftRepeats;
        double percentage = ((completedRepeats * 100.0) / totalRepeats);
        if (percentage % 10 == 0) {
            printOnCmd(percentage + "% job done.");
        }
    }

    public static void printMergedResult(final List<RunResult> runResults, final List<Long> mergedResponseTimes) {
        RunResult mergedResult = new RunResult();
        mergedResult.setAverageTimePerRequest(runResults.stream().mapToDouble(RunResult::getAverageTimePerRequest)
                .summaryStatistics().getAverage());
        mergedResult.setFailedRequests((int) runResults.stream().mapToInt(RunResult::getFailedRequests)
                .summaryStatistics().getSum());
        mergedResult.setRequestsPerSecond(runResults.stream().mapToDouble(RunResult::getRequestsPerSecond)
                .summaryStatistics().getAverage());
        mergedResult.setSuccessFullRequests((int) runResults.stream().mapToInt(RunResult::getSuccessFullRequests)
                .summaryStatistics().getSum());
        mergedResult.setTotalDataTransfered(runResults.stream().mapToLong(RunResult::getTotalDataTransfered)
                .summaryStatistics().getSum());
        mergedResult.setTotalRequestCompleted((int) runResults.stream().mapToLong(RunResult::getTotalRequestCompleted)
                .summaryStatistics().getSum());
        mergedResult.setTotalTimeTaken(runResults.stream().mapToDouble(RunResult::getTotalTimeTaken)
                .summaryStatistics().getAverage());

        //Collections.sort(mergedResponseSizes);
        Collections.sort(mergedResponseTimes);

        mergedResult.setFiftyPercentile(mergedResponseTimes.get((int) (mergedResponseTimes.size() * 0.5)) / 1000000.00);
        mergedResult.setSeventyFifthPercentile(mergedResponseTimes.get((int) (mergedResponseTimes.size() * 0.75))
                / 1000000.00);
        mergedResult.setNintyPercentile(mergedResponseTimes.get((int) (mergedResponseTimes.size() * 0.9)) / 1000000.00);
        mergedResult.setNintyFifthPercentile(mergedResponseTimes.get((int) (mergedResponseTimes.size() * 0.95))
                / 1000000.00);
        mergedResult.setNintyEighthPercentile(mergedResponseTimes.get((int) (mergedResponseTimes.size() * 0.98))
                / 1000000.00);
        mergedResult.setNintyNinethPercentile(mergedResponseTimes.get((int) (mergedResponseTimes.size() * 0.99))
                / 1000000.00);
        System.out.println("Combined result from all nodes: ");
        System.out.println(mergedResult.toString());

    }

    public static void printOnCmd(final String message) {
        printOnCmd(message, true);
    }
}

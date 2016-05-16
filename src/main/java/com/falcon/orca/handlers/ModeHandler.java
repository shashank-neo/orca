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

import akka.actor.ActorSystem;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

/**
 * Created by shwet.s under project orca. <br/> Created on  15/04/16. <br/> Updated on 15/04/16.  <br/> Updated by
 * shwet.s. <br/>
 */
public abstract class ModeHandler {
    protected CommandLine commandLine = null;
    protected CommandLineParser commandLineParser = new DefaultParser();
    protected ActorSystem actorSystem;

    public abstract void handle();
}

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
package com.falcon.orca.enums;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  14/04/16. <br/> Updated on 14/04/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public enum ClustermanagerCommandType {
    REGISTER_NODE, STOP_LOAD, UNREGISTER_NODE, START_LOAD, EXIT, PAUSE_LOAD, CLUSTER_DETAILS,
    LOAD_GENERATION_START, LOAD_START_FAILED, LOAD_GENERATION_COMPLETE, LOAD_GENERATION_PAUSED,
    LOAD_GENERATION_RESUMED, RESUME_LOAD, DATA_SEND_COMPLETE, TAKE_DATA
}

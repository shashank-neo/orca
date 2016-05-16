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
package com.falcon.orca.domain;

import com.falcon.orca.helpers.DynVariable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  11/05/16. <br/>
 * Updated on 11/05/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Getter
@Setter
public class DynJsonData implements Serializable {
    private HashMap<String, DynVariable> urlParams;
    private HashMap<String, DynVariable> bodyParams;
    private HashMap<String, DynGenerator> generators;
}

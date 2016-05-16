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
package com.falcon.orca.data.readers.impl;

import com.falcon.orca.data.readers.DataReader;
import com.falcon.orca.domain.DynGenerator;
import com.falcon.orca.domain.DynJsonData;
import com.falcon.orca.enums.DynVarUseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  11/05/16. <br/>
 * Updated on 11/05/16.  <br/>
 * Updated by shwet.s. <br/>
 */
public class JsonFileReader implements DataReader {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] template;
    private HashMap<String, List<Object>> bodyData = new HashMap<>();
    private HashMap<String, List<Object>> urlData = new HashMap<>();
    private HashMap<String, DynVarUseType> bodyVarUseType = new HashMap<>();
    private HashMap<String, DynVarUseType> urlVarUseType = new HashMap<>();
    private final HashMap<String, DynGenerator> generators;



    public JsonFileReader(final String dataFilePath, final String templateFilePath) throws IOException {
        if(!StringUtils.isBlank(templateFilePath)) {
            this.template = Files.readAllBytes(Paths.get(templateFilePath));
        } else {
            this.template = new byte[0];
        }
        DynJsonData dynData = objectMapper.readValue(new File(dataFilePath), DynJsonData.class);
        dynData.getBodyParams().forEach((k,v) -> {
            switch (v.getType()) {
                case USE_ONCE: {
                    LinkedList<Object> data = new LinkedList<>();
                    data.addAll(v.getValues());
                    bodyData.put(k, data);
                    bodyVarUseType.put(k, DynVarUseType.USE_ONCE);
                    break;
                }
                case USE_MULTIPLE: {
                    ArrayList<Object> data = new ArrayList<>();
                    data.addAll(v.getValues());
                    bodyData.put(k, data);
                    bodyVarUseType.put(k, DynVarUseType.USE_MULTIPLE);
                    break;
                }
            }
        });
        dynData.getUrlParams().forEach((k,v) -> {
            switch (v.getType()) {
                case USE_ONCE: {
                    LinkedList<Object> data = new LinkedList<>();
                    data.addAll(v.getValues());
                    urlData.put(k, data);
                    urlVarUseType.put(k, DynVarUseType.USE_ONCE);
                    break;
                }
                case USE_MULTIPLE: {
                    ArrayList<Object> data = new ArrayList<>();
                    data.addAll(v.getValues());
                    urlData.put(k, data);
                    urlVarUseType.put(k, DynVarUseType.USE_MULTIPLE);
                    break;
                }
            }
        });
        generators = dynData.getGenerators();
    }

    @Override
    public String readTemplate() {
        return new String(template, StandardCharsets.UTF_8);
    }

    @Override
    public HashMap<String, HashMap<String, List<Object>>> readVariableValues() {
        HashMap<String, HashMap<String, List<Object>>> returnData = new HashMap<>();
        returnData.put("bodyData", bodyData);
        returnData.put("urlData", urlData);
        return returnData;
    }

    @Override
    public HashMap<String, HashMap<String, DynVarUseType>> readVariableUseType() {
        HashMap<String, HashMap<String, DynVarUseType>> returnData = new HashMap<>();
        returnData.put("bodyVarUseType", bodyVarUseType);
        returnData.put("urlVarUseType", urlVarUseType);
        return returnData;
    }

    @Override
    public HashMap<String, DynGenerator> readGenerators() {
        return generators;
    }
}

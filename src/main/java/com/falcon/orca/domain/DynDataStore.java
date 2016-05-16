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

import com.falcon.orca.data.generators.DataGenerator;
import com.falcon.orca.data.generators.impl.NumberGenerator;
import com.falcon.orca.data.generators.impl.StringGenerator;
import com.falcon.orca.enums.DynVarUseType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  09/05/16. <br/>
 * Updated on 09/05/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Slf4j
public class DynDataStore implements Serializable {

    private static final long serialVersionUID = 42L;
    private final HashMap<String, List<Object>> bodyParamsData;
    private final HashMap<String, List<Object>> urlParamsData;
    private final HashMap<String, DynVarUseType> bodyVariableType;
    private final HashMap<String, DynVarUseType> urlVariableType;
    private final HashMap<String, Integer> bodyDataCounters;
    private final HashMap<String, Integer> urlDataCounters;
    private final HashMap<String, DataGenerator> dataGenerators;
    private transient final Mustache bodyMustache;
    private transient final Mustache urlMustache;
    private final Pattern pattern = Pattern.compile("@@(.*)?@@");

    public DynDataStore(final HashMap<String, List<Object>> bodyParamsData, final HashMap<String, List<Object>>
            urlParamsData, final HashMap<String, DynVarUseType> bodyVariableType, final HashMap<String,
            DynVarUseType> urlVariableType, final HashMap<String, DynGenerator> dataGenerators, final String
            dataTemplate, final String urlTemplate ) {
        this.bodyParamsData = bodyParamsData;
        this.urlParamsData = urlParamsData;
        this.bodyVariableType = bodyVariableType;
        this.urlVariableType = urlVariableType;
        this.bodyDataCounters = new HashMap<>();
        this.urlDataCounters = new HashMap<>();
        this.dataGenerators = new HashMap<>();
        if(dataGenerators != null) {
            dataGenerators.forEach((k,v) -> {
                switch (v.getType()) {
                    case NUMERIC: {
                        DataGenerator<Long> generator = new NumberGenerator(Long.parseLong(v.getProperties().get
                                ("limit")), Long.parseLong(v.getProperties().get("offset")));
                        this.dataGenerators.put(k, generator);
                        break;
                    }
                    case ALPHA_NUMERIC: {
                        DataGenerator<String> generator = new StringGenerator(v.getProperties().get("characters"),
                                Integer.parseInt(StringUtils.isBlank(v.getProperties().get("length"))? "10" : v
                                        .getProperties().get("length")), true, true);
                        this.dataGenerators.put(k, generator);
                        break;
                    }
                    case STRING: {
                        DataGenerator<String> generator = new StringGenerator(v.getProperties().get("characters"),
                                Integer.parseInt(StringUtils.isBlank(v.getProperties().get("length"))? "10" : v
                                        .getProperties().get("length")), true, false);
                        this.dataGenerators.put(k, generator);
                        break;
                    }
                }
            });
        }
        if(bodyVariableType != null) {
            bodyVariableType.forEach((k, v) -> {
                if (v.equals(DynVarUseType.USE_MULTIPLE)) {
                    this.bodyDataCounters.put(k, 0);
                }
            });
        }
        if(urlVariableType != null){
            urlVariableType.forEach((k, v) -> {
                if (v.equals(DynVarUseType.USE_MULTIPLE)) {
                    this.urlDataCounters.put(k, 0);
                }
            });
        }
        MustacheFactory mf = new DefaultMustacheFactory();
        if(!StringUtils.isBlank(dataTemplate)) {
            bodyMustache = mf.compile(new StringReader(dataTemplate), "orca-body");
        } else {
            bodyMustache = null;
        }
        if(!StringUtils.isBlank(urlTemplate)) {
            urlMustache = mf.compile(new StringReader(urlTemplate), "orca-url");
        } else {
            urlMustache = null;
        }
    }

    public String fillTemplateWithData() throws JsonProcessingException {
        Map<String, Object> scopes = getDataMap();
        StringWriter stringWriter = new StringWriter();
        bodyMustache.execute(stringWriter, scopes);
        String data = stringWriter.toString().trim();
        Matcher m = pattern.matcher(data);
        if(m.find()) {
            int groupCount = m.groupCount();
            while(groupCount > 0) {
                String keyToReplace = m.group(groupCount);
                if(dataGenerators.containsKey(keyToReplace)) {
                    data = data.replace("@@" + keyToReplace + "@@", String.valueOf(dataGenerators.get(m.group
                            (groupCount)).next()));
                }
                groupCount--;
            }
        }
        return data;
    }

    public String fillURLWithData() throws JsonProcessingException {
        Map<String, Object> scope = getUrlDataMap();
        StringWriter stringWriter = new StringWriter();
        urlMustache.execute(stringWriter, scope);
        String url = stringWriter.toString().trim();
        Matcher m = pattern.matcher(url);
        if(m.find()) {
            int groupCount = m.groupCount();
            while(groupCount > 0) {
                String keyToReplace = m.group(groupCount);
                if(dataGenerators.containsKey(keyToReplace)) {
                    url = url.replace("@@" + keyToReplace + "@@", String.valueOf(dataGenerators.get(m.group(groupCount))
                            .next()));
                }
                groupCount--;
            }
        }
        return url;
    }

    private DynVarUseType getUseType(final String variableName, final String variableType) {
        if ("body".equalsIgnoreCase(variableType) && bodyVariableType != null && bodyVariableType.containsKey
                (variableName)) {
            return bodyVariableType.get(variableName);
        } else if("url".equalsIgnoreCase(variableType) && urlVariableType != null && urlVariableType.containsKey
                (variableName)) {
            return urlVariableType.get(variableName);
        }
        return null;
    }

    private Object getDynVariableData(final String variableName, final String variableType) {
        DynVarUseType useType = getUseType(variableName, variableType);
        if(useType == null) {
            log.error("Variable {} doesn't exists.", variableName);
            return null;
        }
        Object returnValue = null;
        switch (useType) {
            case USE_MULTIPLE: {
                if("body".equalsIgnoreCase(variableType)) {
                    List<Object> objects = bodyParamsData.get(variableName);
                    Integer counter = bodyDataCounters.get(variableName);
                    if (objects != null && objects.size() > counter) {
                        returnValue = objects.get(counter);
                        bodyDataCounters.put(variableName, (++counter) % objects.size());
                    }
                } else if("url".equalsIgnoreCase(variableType)) {
                    List<Object> objects = urlParamsData.get(variableName);
                    Integer counter = urlDataCounters.get(variableName);
                    if(objects != null && objects.size() > counter) {
                        returnValue = objects.get(counter);
                        urlDataCounters.put(variableName, (++counter) % objects.size());
                    }
                }
                break;
            }
            case USE_ONCE: {
                if("body".equalsIgnoreCase(variableType)) {
                    List<Object> objects = bodyParamsData.get(variableName);
                    if (objects != null && objects.size() > 0) {
                        returnValue = objects.remove(0);
                    }
                } else if("url".equalsIgnoreCase(variableType)) {
                    List<Object> objects = urlParamsData.get(variableName);
                    if (objects != null && objects.size() > 0) {
                        returnValue = objects.remove(0);
                    }
                }
                break;
            }
            default: {
                log.error("Can't find value for variable {}", variableName);
            }
        }
        return returnValue;
    }

    private Map<String, Object> getDataMap() throws JsonProcessingException {
        HashMap<String, Object> dataMap = new HashMap<>();
        bodyParamsData.keySet().forEach((o) -> dataMap.put(o, getDynVariableData(o, "body")));
        return dataMap;
    }

    private Map<String, Object> getUrlDataMap() throws JsonProcessingException {
        HashMap<String, Object> dataMap = new HashMap<>();
        urlParamsData.keySet().forEach((o) -> dataMap.put(o, getDynVariableData(o, "url")));
        return dataMap;
    }
}

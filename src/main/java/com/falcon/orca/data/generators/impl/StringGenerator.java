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
package com.falcon.orca.data.generators.impl;

import com.falcon.orca.data.generators.DataGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/05/16. <br/>
 * Updated on 15/05/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Data
@AllArgsConstructor
public class StringGenerator implements DataGenerator<String> {

    private String characters;
    private Integer length;
    private boolean letters;
    private boolean numbers;

    @Override
    public String next() {
        if(StringUtils.isBlank(characters)) {
            if(letters && numbers) {
                return RandomStringUtils.randomAlphanumeric(length);
            } else {
                if(letters) {
                    return RandomStringUtils.randomAlphabetic(length);
                } else if (numbers){
                    return RandomStringUtils.randomAlphanumeric(length);
                }
            }
        } else {
            return RandomStringUtils.random(length, 0, characters.length(), letters, numbers, characters.toCharArray());
        }
        return "";
    }
}

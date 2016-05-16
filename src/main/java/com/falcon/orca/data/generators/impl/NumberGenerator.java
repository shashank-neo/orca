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
import org.apache.commons.math3.random.RandomDataGenerator;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/05/16. <br/>
 * Updated on 15/05/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Data
@AllArgsConstructor
public class NumberGenerator implements DataGenerator<Long> {

    private Long limit = 9223372036854775807L;
    private Long offset = 0L;
    private final RandomDataGenerator random = new RandomDataGenerator();

    @Override
    public Long next() {
        return random.nextLong(offset, limit);
    }
}

package com.falcon.orca.domain;

import com.falcon.orca.data.generators.DataGeneratorType;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by shwet.s under project orca. <br/>
 * Created on  15/05/16. <br/>
 * Updated on 15/05/16.  <br/>
 * Updated by shwet.s. <br/>
 */
@Data
public class DynGenerator implements Serializable {
    private DataGeneratorType type;
    private HashMap<String, String> properties;
}

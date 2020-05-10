package org.yamcs.sle.udpslebridge;

import java.util.Properties;

public class Util {
    public static String getProperty(Properties props, String key) {
        if(!props.containsKey(key)) {
            throw new ConfigurationException("Cannot find property '"+key+"'");
        }
        return props.getProperty(key);
    }
}

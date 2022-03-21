package org.yamcs.sle.udpslebridge;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.yamcs.sle.provider.FrameSink;

/**
 * Initializes and keeps a reference to all frame sinks (e.g. UDP sender).
 */
public class FrameSinks {
    Map<String, FrameSink> sinks = new HashMap<>();
    static FrameSinks instance;

    public FrameSinks(Properties properties) {
        // load all sources
        Collection<String> ids = FrameSources.getIds(properties, "fsink");
        for (String id : ids) {
            String key = "fsink." + id + ".type";
            String type = Util.getProperty(properties, key);
            FrameSink fsink;
            if ("udp".equals(type)) {
                fsink = new UdpFrameSink(properties, id);
            } else {
                throw new ConfigurationException("Unknown frame sink type '" + type + "'");
            }
            fsink.startup();
            sinks.put(id, fsink);
        }
    }

    static public FrameSink getSink(String id) {
        return instance.sinks.get(id);
    }

    public static void init(Properties properties) {
        instance = new FrameSinks(properties);
    }
}

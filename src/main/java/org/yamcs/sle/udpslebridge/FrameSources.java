package org.yamcs.sle.udpslebridge;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.yamcs.sle.provider.FrameSource;

/**
 * Initializes and keeps a reference to all frame sources (e.g. UDP receiver or file reader).
 */
public class FrameSources {
    Map<String, FrameSource> sources = new HashMap<>();
    static FrameSources instance;

    public FrameSources(Properties properties) {
        // load all sources
        Collection<String> ids = getIds(properties, "fsource");
        for (String id : ids) {
            String key = "fsource." + id + ".type";
            String type = Util.getProperty(properties, key);
            FrameSource fsource;
            if ("udp".equals(type)) {
                fsource = new UdpFrameSource(properties, id);
            } else if ("file".equals(type)) {
                fsource = new FileFrameSource(properties, id);
            } else {
                throw new ConfigurationException("Unknown frame source type '" + type + "'");
            }
            fsource.startup();
            sources.put(id, fsource);
        }
    }

    static public FrameSource getSource(String id) {
        return instance.sources.get(id);
    }

    public static void init(Properties properties) {
        instance = new FrameSources(properties);
    }

    public FrameSources getInstance() {
        return instance;
    }

    /**
     * return a set of ids from all the properties of type prefix.id.xyz
     */
    static Collection<String> getIds(Properties props, String prefix) {
        Set<String> r = new HashSet<>();
        for (Object k : props.keySet()) {
            String[] a = ((String) k).split("\\.");
            if (a.length > 2 && prefix.equals(a[0])) {
                r.add(a[1]);
            }
        }
        return r;
    }
}

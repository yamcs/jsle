package org.yamcs.sle;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public final static int APP_ID_FWD_CLTU = 16;
    
    public final static Map<Integer, String> BIND_DIAGNOSTIC = new HashMap<>(); 
    static {
        BIND_DIAGNOSTIC.put(0, "access denied");
        BIND_DIAGNOSTIC.put(1, "service type not supported");
        BIND_DIAGNOSTIC.put(2, "version not supported");
        BIND_DIAGNOSTIC.put(3, "no such service instance");
        BIND_DIAGNOSTIC.put(4, "already bound");
        BIND_DIAGNOSTIC.put(5, "si not accessible to this initiator");
        BIND_DIAGNOSTIC.put(6, "inconsistent service type");
        BIND_DIAGNOSTIC.put(7, "invalid time");
        BIND_DIAGNOSTIC.put(8, "out of service");
        BIND_DIAGNOSTIC.put(127, "other reason");
    }
}

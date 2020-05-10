package org.yamcs.sle.udpslebridge;

import java.util.Optional;
import java.util.Properties;

import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.provider.AuthProvider;

import io.netty.buffer.ByteBufUtil;

import static org.yamcs.sle.udpslebridge.Util.*;

public class BridgeAuthProvider implements AuthProvider {
    Properties properties;
    String myUsername;
    byte[] myPass;
    
    public BridgeAuthProvider(Properties properties) {
        this.properties = properties;
        this.myUsername = Util.getProperty(properties, "sle.myUsername");
        this.myPass = ByteBufUtil.decodeHexDump(Util.getProperty(properties, "sle.myPassword"));
    }
    
    @Override
    public Isp1Authentication getAuth(String initiatorId) {
        //look for an entry where auth.x.initiatorId = initiatorId and return x
        Optional<String> x = properties.entrySet().stream()
        .filter(e-> initiatorId.equals(e.getValue()) 
                && ((String)e.getKey()).startsWith("auth.")
                && ((String)e.getKey()).endsWith(".initiatorId"))
        .map(e-> {
            String k = (String)e.getKey();
            return k.substring(5, k.length()-12);
        }).findFirst();
        
        if(!x.isPresent()) {
            return null;
        }
        String id = x.get();
        String peerUsername = properties.getProperty("auth."+id+".peerUsername", initiatorId);
        byte[] peerPass = ByteBufUtil.decodeHexDump(getProperty(properties, "auth."+id+".peerPassword"));
        String hashAlgorithm = properties.getProperty("auth."+id+".hashAlgorithm", "SHA-1");
        return new Isp1Authentication(myUsername, myPass, peerUsername, peerPass, hashAlgorithm);
    }

}

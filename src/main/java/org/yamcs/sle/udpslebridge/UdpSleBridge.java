package org.yamcs.sle.udpslebridge;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.LogManager;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

/**
 * This implements a simple UDP to SLE bridge for RAF and CLTU services
 * 
 * @author nm
 *
 */
public class UdpSleBridge {
    static Properties config = new Properties();
    
    static public void main(String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
        
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        Fcltu2Udp fclu2udp = new Fcltu2Udp(config);
        fclu2udp.start();
        
    }
}

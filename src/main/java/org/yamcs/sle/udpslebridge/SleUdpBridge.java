package org.yamcs.sle.udpslebridge;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.yamcs.sle.AuthLevel;
import org.yamcs.sle.Isp1Handler;
import org.yamcs.sle.provider.AuthProvider;
import org.yamcs.sle.provider.SleAttributes;
import org.yamcs.sle.provider.SleProvider;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

/**
 * This implements a simple UDP to SLE bridge for RAF and CLTU services
 * 
 * @author nm
 *
 */
public class SleUdpBridge {
    int slePort;
    AuthLevel authLevel;
    SleAttributes sleAttributes;
    static Logger logger = Logger.getLogger(SleUdpBridge.class.getName());
    BridgeServiceInitializer srvInitializer;
    AuthProvider authProvider;
    private String responderId;

    public SleUdpBridge(Properties properties) {
        this.slePort = Integer.valueOf(properties.getProperty("sle.port", "25711"));
        this.authLevel = AuthLevel.valueOf(properties.getProperty("sle.authLevel", "BIND"));
        this.responderId = properties.getProperty("sle.responderId");

        srvInitializer = new BridgeServiceInitializer(properties);
        authProvider = new BridgeAuthProvider(properties);
    }

    public void start() throws InterruptedException {

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    logger.fine("New client connected from " + ch.remoteAddress());
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(8192, 4, 4));
                    ch.pipeline().addLast(new Isp1Handler(false));
                    ch.pipeline().addLast(getProvider(ch));
                }

            });
            b.childOption(ChannelOption.SO_KEEPALIVE, true);

            // Start the client.
            ChannelFuture f = b.bind(slePort).sync();
            System.out.println("listening to SLE port " + slePort);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    private ChannelHandler getProvider(SocketChannel ch) {
        SleProvider csph = new SleProvider(authProvider, responderId, srvInitializer);
        // csph.addMonitor(new MyMonitor(csph));
        csph.setAuthLevel(authLevel);
        return csph;
    }

    static public void printUsageAndExit(int code) {
        System.out.println("Usage: sle-udp-bridge.sh -c bridge.properties [-l logging.properties]");
        System.exit(code);
    }

    static public void main(String[] args) throws Exception {

        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        String cfile = "bridge.properties";
        String lfile = "logging.properties";
        for (int i = 0; i < args.length; i++) {
            if ("-h".equals(args[i]) || "-help".equals(args[i]) || "--help".equals(args[i])) {
                printUsageAndExit(0);
            } else if ("-c".equals(args[i])) {
                if (i == args.length) {
                    printUsageAndExit(-1);
                }
                cfile = args[++i];
            } else if ("-l".equals(args[i])) {
                if (i == args.length) {
                    printUsageAndExit(-1);
                }
                lfile = args[++i];
                if (!new File(lfile).exists()) {
                    System.err.println("File no found: "+lfile);
                    printUsageAndExit(-1);
                }
            }
        }
        if (!new File(cfile).exists()) {
            System.err.println("Config file does not exist: "+cfile);
            printUsageAndExit(-1);
        }

        
        File f = new File(lfile);
        if (f.exists()) {
            LogManager.getLogManager().readConfiguration(new FileInputStream(f));
        }

        Properties props = new Properties();
        props.load(new FileInputStream(cfile));
        try {
            FrameSources.init(props);
            FrameSinks.init(props);

            SleUdpBridge bridge = new SleUdpBridge(props);
            bridge.start();
        } catch (ConfigurationException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}

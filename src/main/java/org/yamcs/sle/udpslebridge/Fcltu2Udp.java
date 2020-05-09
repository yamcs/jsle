package org.yamcs.sle.udpslebridge;

import java.util.Properties;
import java.util.logging.Logger;

import org.yamcs.sle.AuthLevel;
import org.yamcs.sle.AuthenticationException;
import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.ForwardDuStatus;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.Isp1Handler;
import org.yamcs.sle.SleMonitor;
import org.yamcs.sle.State;
import org.yamcs.sle.provider.CltuConsumer;
import org.yamcs.sle.provider.CltuServiceProviderHandler;
import org.yamcs.sle.provider.SleAttributes;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Receives commands via forward CLTU service and sends them via UDP
 * 
 * @author nm
 *
 */
public class Fcltu2Udp {
    // host/port to send the UDP packets to
    String udpHost;
    int udpPort;
    int slePort;
    AuthLevel authLevel;
    MyCltuCosumer cltuConsumer;
    SleAttributes sleAttributes;
    Isp1Authentication isp1Authentication;
    static Logger logger = Logger.getLogger(Fcltu2Udp.class.getName());
    
    public Fcltu2Udp(Properties properties) {
        this.udpHost = properties.getProperty("cltu.udpHost", "localhost");
        this.udpPort = Integer.valueOf(properties.getProperty("cltu.udpPort", "5000"));
        this.slePort = Integer.valueOf(properties.getProperty("cltu.slePort", "25711"));
        this.authLevel = AuthLevel.valueOf(properties.getProperty("authLevel", "BIND"));
        
        final String responderPortId = "Harness_Port_1";
        final String peerUsername = "mertens";
        final String myUsername = "proxy";
        final String responderId = "proxy";
        final String myPass = "AB0102030405060708090a0b0c0d0e0f";
        final String peerPass = "000102030405060708090a0b0c0d0e0f";
        
        isp1Authentication = new Isp1Authentication(myUsername, ByteBufUtil.decodeHexDump(myPass),
                peerUsername, ByteBufUtil.decodeHexDump(peerPass), "SHA-1");
        sleAttributes = new SleAttributes(responderPortId, responderId, "sagr=9999.spack=ESC11-PERM.rsl-fg=1.raf=onlt1");
        cltuConsumer = new MyCltuCosumer();
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
                    logger.fine("New client connected from "+ch.remoteAddress());
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(8192, 4, 4));
                    ch.pipeline().addLast(new Isp1Handler(false));
                    ch.pipeline().addLast(getProviderHandler(ch));
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

    private ChannelHandler getProviderHandler(SocketChannel ch) {
        CltuServiceProviderHandler csph = new CltuServiceProviderHandler(isp1Authentication, sleAttributes, cltuConsumer);
        csph.addMonitor(new MyMonitor(csph));
        csph.setAuthLevel(authLevel);
        return csph;
    }

    class MyCltuCosumer implements CltuConsumer {

        @Override
        public UplinkResult uplink(byte[] cltuData) {
            UplinkResult ur = new UplinkResult();
            ur.startTime = CcsdsTime.now();
            System.out.println("uplinking cltuof size "+cltuData.length);
            ur.stopTime = CcsdsTime.now();
            ur.cltuStatus = ForwardDuStatus.radiated;
            return ur;
        }
    }

    class MyMonitor implements SleMonitor {
        final CltuServiceProviderHandler csph;
        public MyMonitor(CltuServiceProviderHandler csph) {
            this.csph = csph;
        }
        @Override
        public void connected() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void disconnected() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void stateChanged(State newState) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void exceptionCaught(Throwable t) {
            if(t instanceof AuthenticationException) {
                logger.warning(t.toString()+"; Closing connection");
                csph.shutdown();
            } else {
                t.printStackTrace();
            }
            
        }

    }
}

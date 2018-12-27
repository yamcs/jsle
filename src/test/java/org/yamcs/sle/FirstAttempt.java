package org.yamcs.sle;



import org.yamcs.sle.CltuServiceUserHandler;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.Isp1Handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class FirstAttempt {
    
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 25711;
        final String responderPortId =  "Harness_Port_1";
        final String initiatorId = "mertens";
        
        Isp1Authentication isp1Authentication = new Isp1Authentication("mertens", ByteBufUtil.decodeHexDump("000102030405060708090a0b0c0d0e0f"),
                "mertens", "cucubau1".getBytes(), "SHA-1");
        CltuServiceUserHandler csuh = new CltuServiceUserHandler(isp1Authentication, responderPortId, initiatorId);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class); 
            b.option(ChannelOption.SO_KEEPALIVE, true); 
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                  //  hackChannel(ch);
                    
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2048, 4, 4));
                    ch.pipeline().addLast(new Isp1Handler(true));
                    ch.pipeline().addLast(csuh);
                }
            });
            
            // Start the client.
           ChannelFuture f = b.connect(host, port).sync();
           csuh.bind().get();
           System.out.println("yuhuu bound");
           csuh.start().get();
           System.out.println("yuhuu started");
           
         /*  CltuGetParameter cgpr = csuh.getParameter(ParameterName.deliveryMode).get();
           System.out.println(" got deliveryMode parameter: "+cgpr);
           
           CltuGetParameter cgpr = csuh.getParameter(ParameterName.bitLockRequired).get();
           System.out.println(" got bitlockrequired parameter: "+cgpr);
           
           CltuGetParameter cgpr = csuh.getParameter(ParameterName.expectedEventInvocationIdentification).get();
           System.out.println(" expectedEventInvocationIdentification got parameter: "+cgpr);
           
           CltuGetParameter cgpr = csuh.getParameter(ParameterName.maximumSlduLength).get();
           System.out.println(" maximumSlduLength got parameter: "+cgpr);
           
           cgpr = csuh.getParameter(ParameterName.modulationFrequency).get();
           System.out.println("modulationFrequency got parameter: "+cgpr);
           src/main/java/org/yamcs/sle/AbstractServiceUserHandler.java
           cgpr = csuh.getParameter(ParameterName.subcarrierToBitRateRatio).get();
           System.out.println("subcarrierToBitRateRatio got parameter: "+cgpr);
           
           cgpr = csuh.getParameter(ParameterName.expectedSlduIdentification).get();
           System.out.println("expectedSlduIdentification got parameter: "+cgpr);
           */
           csuh.schedulePeriodicStatusReport(10).get();
           System.out.println("reporting cycle configured");
           
           
           for(int i =0; i<30; i++) {
               csuh.transferCltu(new byte[100]);
               //Thread.sleep(100);
           }
           
           Thread.sleep(20000);
           csuh.stopPeriodicStatusReport().get();
           System.out.println("Periodic status report stopped");
           
           f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
    /*
    static void hackChannel(SocketChannel ch) {
        try {
            Field f = AbstractNioChannel.class.getDeclaredField("ch");
            f.setAccessible(true);
            Object o = f.get(ch);
            java.nio.channels.SocketChannel jch = (java.nio.channels.SocketChannel)o; 
            jch.socket().setOOBInline(true);
            ch.pipeline().addLast(new OobDetector());
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }*/
}

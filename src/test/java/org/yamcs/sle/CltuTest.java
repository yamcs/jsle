package org.yamcs.sle;



import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.Isp1Handler;
import org.yamcs.sle.user.CltuServiceUserHandler;
import org.yamcs.sle.user.CltuSleMonitor;
import org.yamcs.sle.user.SleAttributes;

import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuTransferData;
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

public class CltuTest {
    
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 25711;
        final String responderPortId =  "Harness_Port_1";
        final String initiatorId = "mertens";
        
        Isp1Authentication isp1Authentication = new Isp1Authentication(initiatorId, ByteBufUtil.decodeHexDump("000102030405060708090a0b0c0d0e0f"),
                "jsle-bridge", ByteBufUtil.decodeHexDump("AB0102030405060708090a0b0c0d0e0f"), "SHA-1");
        
        SleAttributes attr = new SleAttributes(responderPortId, initiatorId, "sagr=SAGR.spack=SPACK.fsl-fg=FSL-FG.cltu=cltu1");
        CltuServiceUserHandler csuh = new CltuServiceUserHandler(isp1Authentication, attr);
        csuh.addMonitor(new MyMonitor());
        csuh.setAuthLevel(AuthLevel.BIND);
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
           
           csuh.schedulePeriodicStatusReport(10);
           Thread.sleep(100000);
           
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
     //      csuh.schedulePeriodicStatusReport(10).get();
       //    System.out.println("reporting cycle configured");
           
           
           for(int i =0; i<30; i++) {
               System.out.println("before csuh transfer is connected: "+csuh.isConnected());
               csuh.transferCltu(new byte[100]);
               Thread.sleep(10000);
           }
           
           Thread.sleep(20000);
           csuh.stopPeriodicStatusReport().get();
           System.out.println("Periodic status report stopped");
           
           f.channel().closeFuture().sync();
        } catch(Exception e) {
           e.printStackTrace();
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
    
    static class MyMonitor implements CltuSleMonitor {

        @Override
        public void connected() {
            System.out.println("MyMonitor: connected");
        }

        @Override
        public void disconnected() {
            System.out.println("MyMonitor: disconnected");
        }

        @Override
        public void stateChanged(State newState) {
            System.out.println("MyMonitor: state changed: "+newState);
        }

        @Override
        public void exceptionCaught(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onCltuStatusReport(CltuStatusReportInvocation cltuStatusReportInvocation) {
            System.out.println("MyMonitor: onCltuStatusReport: "+cltuStatusReportInvocation);
            
        }

        @Override
        public void onAsyncNotify(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation) {
            System.out.println("MyMonitor: onAsyncNotify: "+cltuAsyncNotifyInvocation);
        }

        @Override
        public void onPositiveTransfer(int cltuId) {
            System.out.println("MyMonitor: onPositiveTransfer: cltuId = "+cltuId);
            
        }

        @Override
        public void onNegativeTransfer(int cltuId, DiagnosticCltuTransferData negativeResult) {
            System.out.println("MyMonitor: onNegativeTransfer: cltuId: "+cltuId+" negativeResult: "+negativeResult);
        }
        
    }
}

package org.yamcs.sle;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.DeliveryMode;
import org.yamcs.sle.Constants.FrameQuality;
import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.ProductionStatus;
import org.yamcs.sle.user.FrameConsumer;
import org.yamcs.sle.user.RacfStatusReport;
import org.yamcs.sle.user.RcfServiceUserHandler;
import org.yamcs.sle.user.SleAttributes;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.Isp1Handler;
import org.yamcs.sle.RacfSleMonitor;

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

public class RcfTest {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 25711;
        final String responderPortId = "Harness_Port_1";
        final String initiatorId = "mertens";

        Isp1Authentication isp1Authentication = new Isp1Authentication("mertens",
                ByteBufUtil.decodeHexDump("000102030405060708090a0b0c0d0e0f"),
                "jsle-bridge", ByteBufUtil.decodeHexDump("AB0102030405060708090a0b0c0d0e0f"), "SHA-1");
        MyConsumer c = new MyConsumer();
        SleAttributes attr = new SleAttributes(responderPortId, initiatorId, "sagr=SAGR.spack=SPACK.rsl-fg=RSL-FG.rcf=onlt1");
        RcfServiceUserHandler rsuh = new RcfServiceUserHandler(isp1Authentication, attr,  DeliveryMode.rtnTimelyOnline, c);
        rsuh.setAuthLevel(AuthLevel.BIND);
        
        MyMonitor m = new MyMonitor();
        rsuh.addMonitor(m);

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    // hackChannel(ch);

                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(8192, 4, 4));
                    ch.pipeline().addLast(new Isp1Handler(true));
                    ch.pipeline().addLast(rsuh);
                }
            });

            GVCID gvcid = new GVCID(0, 1, 1);
            // Start the client.
            ChannelFuture f = b.connect(host, port).sync();

            rsuh.bind().get();

            System.out.println("yuhuu bound");
            rsuh.start(gvcid).get();
            System.out.println("yuhuu started");

            /*
             * CltuGetParameter cgpr = csuh.getParameter(ParameterName.deliveryMode).get();
             * System.out.println(" got deliveryMode parameter: "+cgpr);
             * 
             * CltuGetParameter cgpr = csuh.getParameter(ParameterName.bitLockRequired).get();
             * System.out.println(" got bitlockrequired parameter: "+cgpr);
             * 
             * CltuGetParameter cgpr = csuh.getParameter(ParameterName.expectedEventInvocationIdentification).get();
             * System.out.println(" expectedEventInvocationIdentification got parameter: "+cgpr);
             * 
             * CltuGetParameter cgpr = csuh.getParameter(ParameterName.maximumSlduLength).get();
             * System.out.println(" maximumSlduLength got parameter: "+cgpr);
             * 
             * cgpr = csuh.getParameter(ParameterName.modulationFrequency).get();
             * System.out.println("modulationFrequency got parameter: "+cgpr);
             * src/main/java/org/yamcs/sle/AbstractServiceUserHandler.java
             * cgpr = csuh.getParameter(ParameterName.subcarrierToBitRateRatio).get();
             * System.out.println("subcarrierToBitRateRatio got parameter: "+cgpr);
             * 
             * cgpr = csuh.getParameter(ParameterName.expectedSlduIdentification).get();
             * System.out.println("expectedSlduIdentification got parameter: "+cgpr);
             */
            rsuh.schedulePeriodicStatusReport(10).get();
            System.out.println("reporting cycle configured");

            Thread.sleep(20000);
            rsuh.stopPeriodicStatusReport().get();
            System.out.println("Periodic status report stopped");

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
    /*
     * static void hackChannel(SocketChannel ch) {
     * try {
     * Field f = AbstractNioChannel.class.getDeclaredField("ch");
     * f.setAccessible(true);
     * Object o = f.get(ch);
     * java.nio.channels.SocketChannel jch = (java.nio.channels.SocketChannel)o;
     * jch.socket().setOOBInline(true);
     * ch.pipeline().addLast(new OobDetector());
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     */

    static class MyConsumer implements FrameConsumer {

        @Override
        public void onExcessiveDataBacklog() {
            System.out.println("onExcessiveDataBacklog");
        }

        @Override
        public void onProductionStatusChange(ProductionStatus productionStatusChange) {
            System.out.println("onProductionStatusChange: " + productionStatusChange);

        }

        @Override
        public void onLossFrameSync(CcsdsTime time, LockStatus carrier, LockStatus subcarrier, LockStatus symbolSync) {
            System.out.println("onLossFrameSync time: " + time + ": carrier: " + carrier + " subcarrier: " + subcarrier
                    + " symbolSync: " + symbolSync);
        }

        @Override
        public void onEndOfData() {
            System.out.println("onEndOfData");
        }

        @Override
        public void acceptFrame(CcsdsTime ert, AntennaId antennaId, int dataLinkContinuity, FrameQuality frameQuality,
                byte[] privAnn, byte[] data) {
            System.out.println(ert+" received frame of length " + data.length);

        }
    }

    static class MyMonitor implements RacfSleMonitor {


        @Override
        public void connected() {
            System.out.println("MyMonitor:connected");

        }

        @Override
        public void disconnected() {
            System.out.println("MyMonitor:disconnected");

        }

        @Override
        public void stateChanged(State newState) {
            System.out.println("MyMonitor:stateChanged " + newState);

        }

        @Override
        public void exceptionCaught(Throwable t) {
            t.printStackTrace();
            System.out.println("MyMonitor:exceptionCaught: " + t);

        }

        @Override
        public void onStatusReport(RacfStatusReport rsr) {
            System.out.println("MyMonitor:onRafStatusReport " + rsr);

            
        }

    }
}

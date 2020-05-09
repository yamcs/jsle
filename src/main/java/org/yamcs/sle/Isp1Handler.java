package org.yamcs.sle;

import java.util.concurrent.TimeUnit;

import com.beanit.jasn1.ber.ReverseByteArrayOutputStream;
import com.beanit.jasn1.ber.types.BerType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class Isp1Handler extends ChannelDuplexHandler {
    final static byte TYPE_SLE_PDU = 1;
    final static byte TYPE_TML_CONTEXT = 2;
    final static byte TYPE_TML_HEARBEAT = 3;
    final static int ISP1_PROTOCOL_ID = 0x49535031;

    // if true, then we send a context message when the connection is established and also start the hearbeat checker
    // if false, expect a context message to start a hearbeat sender
    final boolean initiator;
    int heartbeatInterval;
    int heartbeatDeadFactor;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Isp1Handler.class);

    long lastMessageReceivedTime;
    long lastMessageSentTime;

    boolean heartbeatInitialized = false;
    private ScheduledFuture<?> heartbeatFuture;
    final HeartbeatSettings hbSettings;

    public Isp1Handler(boolean initiator) {
        this(initiator, new HeartbeatSettings());
    }

    public Isp1Handler(boolean initiator, HeartbeatSettings hbSettings) {
        this.initiator = initiator;
        this.hbSettings = hbSettings;
        this.heartbeatInterval = hbSettings.heartbeatInterval;
        this.heartbeatDeadFactor = hbSettings.heartbeatDeadFactor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            throw new IllegalStateException("Unexpected message of type " + msg.getClass() + " encountered");
        }
        ByteBuf buf = (ByteBuf) msg;
        byte type = buf.readByte();
        buf.skipBytes(7);
        lastMessageReceivedTime = System.currentTimeMillis();
        switch (type) {
        case TYPE_SLE_PDU:
            ctx.fireChannelRead(buf);
            break;
        case TYPE_TML_CONTEXT:
            handleContextMessage(ctx, buf);
            break;
        case TYPE_TML_HEARBEAT:
            break;
        default:
            throw new DecoderException("Invalid ISP1 type received " + type);
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (initiator) {
            sendContextMessage(ctx);
            scheduleHeartbeats(ctx);
        } else {
            ctx.executor().schedule(() -> {
                if (!heartbeatInitialized) {
                    logger.debug("No context message received in {} seconds, closing the connection",
                            hbSettings.authenticationTimeout);
                    ctx.channel().close();
                }
            }, hbSettings.authenticationTimeout, TimeUnit.SECONDS);
        }

        super.channelActive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof BerType)) {
            throw new IllegalStateException("Unexpected message of type " + msg.getClass() + " encountered");
        }
        try {
            BerType ber = (BerType) msg;
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeByte(0x01);
            buf.writeZero(3);

            ReverseByteArrayOutputStream rbaos = new ReverseByteArrayOutputStream(128, true);
            ber.encode(rbaos);
            byte[] encodedBer = rbaos.getArray();
            buf.writeInt(encodedBer.length);
            buf.writeBytes(encodedBer);

            ctx.writeAndFlush(buf, promise);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleContextMessage(ChannelHandlerContext ctx, ByteBuf buf) {
        if (initiator || heartbeatInitialized) {
            logger.warn("Ignoring bogus context message");
            return;
        }

        int protocolId = buf.readInt();
        if (protocolId != ISP1_PROTOCOL_ID) {
            logger.warn("Received invalid context message protocol id: {}, expected: {}", protocolId, ISP1_PROTOCOL_ID);
            ctx.close();
            return;
        }
        int versionId = buf.readInt();
        if (versionId != 1) {
            logger.warn("Received invalid context message version id: {}, expected: 1", versionId);
            ctx.close();
            return;
        }
        heartbeatInterval = buf.readShort();
        heartbeatDeadFactor = buf.readShort();
        logger.debug("received context heartbeatInterval: {}, heartbeatDeadFactor: {}", heartbeatInterval,
                heartbeatDeadFactor);
        if (heartbeatInterval == 0) {// no heartbeat required
            heartbeatInitialized = true;
            return;
        }
        if (heartbeatInterval < hbSettings.minHeartbeatInterval) {
            logger.warn("Requested heartbeat interval {} seconds too short, closing the connection",
                    heartbeatInterval);
            ctx.close();
            return;
        }
        if (heartbeatDeadFactor < 1 || heartbeatDeadFactor > hbSettings.maxHeartbeatDeadFactor) {
            logger.warn("Requested heartbeat dead factor {} invalid, closing the connection", heartbeatDeadFactor);
            ctx.close();
            return;
        }
        heartbeatInitialized = true;
        scheduleHeartbeats(ctx);
    }

    private void scheduleHeartbeats(ChannelHandlerContext ctx) {
        lastMessageReceivedTime = System.currentTimeMillis();
        lastMessageSentTime = lastMessageReceivedTime;
        heartbeatFuture = ctx.executor().scheduleAtFixedRate(() -> {
            checkHeartbeat(ctx);
            sendHeartbeat(ctx);
        }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
    }

    private void sendHeartbeat(ChannelHandlerContext ctx) {
        long t = System.currentTimeMillis();
        if ((t - lastMessageSentTime) / 1000 >= heartbeatInterval) {
            ByteBuf buf = ctx.alloc().buffer(8);
            buf.writeByte(TYPE_TML_HEARBEAT);
            buf.writeZero(7);
            ctx.writeAndFlush(buf);
        }
    }

    private void checkHeartbeat(ChannelHandlerContext ctx) {
        long t = System.currentTimeMillis();
        if ((t - lastMessageReceivedTime) / 1000 >= heartbeatInterval * heartbeatDeadFactor) {
            logger.warn("No message received in the last {} seconds, closing the connection",
                    (t - lastMessageReceivedTime) / 1000);
            ctx.close();
        }
    }

    private void sendContextMessage(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(20);
        buf.writeByte(TYPE_TML_CONTEXT);
        buf.writeZero(3); // reserved (zer)
        buf.writeInt(12); // size
        buf.writeInt(0x49535031); // ISP1
        buf.writeZero(3); // reserved (zer)
        buf.writeByte(1); // version
        buf.writeShort(heartbeatInterval);
        buf.writeShort(heartbeatDeadFactor);
        ctx.writeAndFlush(buf);
    }

    static public class HeartbeatSettings {
        public int minHeartbeatInterval = 10;
        public int maxHeartbeatDeadFactor = 10;
        public int heartbeatInterval = 30;
        public int heartbeatDeadFactor = 3;

        // if server, wait maximum this number of seconds for the context message
        public int authenticationTimeout = 60;
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
    }

}

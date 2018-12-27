package org.yamcs.sle;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openmuc.jasn1.ber.ReverseByteArrayOutputStream;
import org.openmuc.jasn1.ber.types.BerType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class Isp1Handler extends ChannelDuplexHandler {
    final static byte TYPE_SLE_PDU = 1;
    final static byte TYPE_TML_CONTEXT = 2;
    final static byte TYPE_TML_HEARBEAT = 3;

    final static int HEARTBEAT_INTERVAL = 30;
    final static int HEARTBEAT_DEAD_FACTOR = 3;
    final static int MIN_HEARTBEAT_INTERVAL = 5;
    final static int MAX_HEARTBEAT_DEAD_FACTOR = 100;
    
    //if server, wait maximum this number of seconds for the context message
    final static int CTX_TIMEOUT = 60;

    // use classes produced by this supplier to decode incoming PDUs - it is reused since we don't process in parallel
    final Supplier<BerType> incomingBerSupplier;

    // if true, then we send a context message when the connection is established and also start the hearbeat checker
    // if false, expect a context message to start a hearbeat sender
    final boolean initiator;
    int heartbeatInterval = HEARTBEAT_INTERVAL;
    int heartbeatDeadFactor = HEARTBEAT_DEAD_FACTOR;
    int ctxTimeout = CTX_TIMEOUT;
    
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Isp1Handler.class);

    long lastMessageReceivedTime;
    long lastMessageSentTime;
    
    boolean heartbeatInitialized = false;
    
    public Isp1Handler(Supplier<BerType> incomingBer, boolean initiator) {
        this.incomingBerSupplier = incomingBer;
        this.initiator = initiator;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            throw new IllegalStateException("Unexpected message of type " + msg.getClass() + " encountered");
        }
        ByteBuf buf = (ByteBuf) msg;
        byte type = buf.readByte();
        buf.skipBytes(7);

        switch (type) {
        case TYPE_SLE_PDU:
            ctx.fireChannelRead(decodePdu(buf));
            break;
        case TYPE_TML_CONTEXT:
            handleContextMessage(ctx, buf);
            break;
        case TYPE_TML_HEARBEAT:
            lastMessageReceivedTime = System.currentTimeMillis();
            break;
        default:
            throw new DecoderException("Invalid ISP1 type received " + type);
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (initiator) {
            System.out.println("Channel active, sending context");
            sendContextMessage(ctx);
            scheduleHeartbeats(ctx);
        } else {
            ctx.executor().schedule(() -> {
                if(!heartbeatInitialized) {
                    logger.debug("No context message received in {} seconds, closing the connection", ctxTimeout);
                    ctx.close();
                }
            }, ctxTimeout, TimeUnit.SECONDS);
        }
        super.channelActive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println(Thread.currentThread()+" Sending " + msg);
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

            System.out.println("Sending message of size " + buf.readableBytes() + ":\n" + ByteBufUtil.hexDump(buf));
            ctx.writeAndFlush(buf, promise);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleContextMessage(ChannelHandlerContext ctx, ByteBuf buf) {
        if(initiator || heartbeatInitialized) {
            logger.warn("Ignoring bogus context message");
        } else {
            heartbeatInterval = buf.readShort();
            heartbeatDeadFactor = buf.readShort();
            logger.debug("received context heartbeatInterval: {}, heartbeatDeadFactor: {}", heartbeatInterval, heartbeatDeadFactor);
            if(heartbeatInterval==0) {//no heartbead required
                heartbeatInitialized = true;
                return;
            }
            if(heartbeatInterval<MIN_HEARTBEAT_INTERVAL) {
                logger.warn("Requested heartbeat interval {} seconds too short, closing the connection", heartbeatInterval);
                ctx.close();
                return;
            }
            if(heartbeatDeadFactor<1 || heartbeatDeadFactor>MAX_HEARTBEAT_DEAD_FACTOR) {
                logger.warn("Requested heartbeat dead factor {} invalid, closing the connection", heartbeatInterval);
                ctx.close();
                return;
            }
            scheduleHeartbeats(ctx);
        }
        System.out.println("received TLM_CONTEXT msg");

    }

    private void scheduleHeartbeats(ChannelHandlerContext ctx) {
        lastMessageReceivedTime = System.currentTimeMillis();
        lastMessageSentTime = lastMessageReceivedTime;
        ctx.executor().schedule(() -> sendHeartbeat(ctx), heartbeatInterval, TimeUnit.SECONDS);
        ctx.executor().schedule(() -> checkHeartbeat(ctx), heartbeatInterval, TimeUnit.SECONDS);
    }
    
    private void sendHeartbeat(ChannelHandlerContext ctx) {
        long t = System.currentTimeMillis();
        if((t-lastMessageSentTime) > heartbeatInterval*(heartbeatDeadFactor-1)) {
            ByteBuf buf = ctx.alloc().buffer(8);
            buf.writeByte(TYPE_TML_HEARBEAT);
            buf.writeZero(7);
            ctx.writeAndFlush(buf);
        }
    }
    
    
    private void checkHeartbeat(ChannelHandlerContext ctx) {
        long t = System.currentTimeMillis();
        if((t-lastMessageReceivedTime)/1000 > heartbeatInterval*heartbeatDeadFactor) {
            logger.warn("No message received in the last {} seconds, closing the connection", (t-lastMessageReceivedTime)/1000);
            ctx.close();
        }
    }

    private Object decodePdu(ByteBuf buf) {
        try {
            BerType ber = incomingBerSupplier.get();
            ber.decode(new ByteBufInputStream(buf));
            System.out.println(Thread.currentThread()+" received ber: "+ber);
            return ber;
        } catch (IOException e) {
            throw new DecoderException(e.getMessage());
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
}

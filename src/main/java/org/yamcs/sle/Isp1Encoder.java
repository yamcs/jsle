package org.yamcs.sle;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class Isp1Encoder extends MessageToByteEncoder<SlePdu>{

    @Override
    protected void encode(ChannelHandlerContext ctx, SlePdu msg, ByteBuf out) throws Exception {
        
    }

}

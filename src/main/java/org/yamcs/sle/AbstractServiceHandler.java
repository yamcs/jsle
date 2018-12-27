package org.yamcs.sle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AbstractServiceHandler extends ChannelInboundHandlerAdapter {
    protected ChannelHandlerContext channelHandlerContext;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.channelHandlerContext = ctx;
    }

}

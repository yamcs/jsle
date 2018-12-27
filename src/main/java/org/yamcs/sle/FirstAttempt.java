package org.yamcs.sle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.openmuc.jasn1.ber.ReverseByteArrayOutputStream;
import org.openmuc.jasn1.ber.types.BerObjectIdentifier;
import org.openmuc.jasn1.ber.types.BerOctetString;
import org.openmuc.jasn1.ber.types.string.BerVisibleString;

import ccsds.sle.transfer.service.bind.types.ApplicationIdentifier;
import ccsds.sle.transfer.service.bind.types.AuthorityIdentifier;
import ccsds.sle.transfer.service.bind.types.PortId;
import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.VersionNumber;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuUserToProviderPdu;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuProviderToUserPdu;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.service.instance.id.OidValues;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceAttribute;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
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
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2048, 4, 4));
                    ch.pipeline().addLast(new Isp1Handler(() -> new CltuProviderToUserPdu(), true));
                    ch.pipeline().addLast(csuh);
                }
            });
            
            // Start the client.
           ChannelFuture f = b.connect(host, port).sync();
           csuh.bind().get();
           System.out.println("yuhuu bound");
           csuh.start().get();
           System.out.println("yuhuu started");
           
           
           f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
    
   
    
}

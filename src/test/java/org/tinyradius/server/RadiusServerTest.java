package org.tinyradius.server;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RadiusServerTest {

    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);


    @Test
    void setupListeners() {
        final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

        final MockHandler authHandler = new MockHandler();
        final MockHandler acctHandler = new MockHandler();
        final RadiusServer<NioDatagramChannel> server = new RadiusServer<>(
                eventExecutors, channelFactory, null, authHandler, acctHandler, 0, 0);

        // todo
    }

    private static class MockHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private final AtomicInteger count = new AtomicInteger();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            count.incrementAndGet();
        }
    }
}
package org.tinyradius.server;

import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RadiusServerTest {

    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);
    private static final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

    @AfterAll
    static void afterAll() {
        eventExecutors.shutdownGracefully().syncUninterruptibly();
    }

    @Test
    void serverStartStop() throws InterruptedException {
        final MockHandler authHandler = new MockHandler();
        final MockHandler acctHandler = new MockHandler();
        final RadiusServer server = new RadiusServer(
                eventExecutors, channelFactory, authHandler, acctHandler, new InetSocketAddress(1024), new InetSocketAddress(1025));

        // not registered with eventLoop
        assertFalse(server.getAcctChannel().isRegistered());
        assertFalse(server.getAuthChannel().isRegistered());

        // not bound to socket
        assertNull(server.getAcctChannel().localAddress());
        assertNull(server.getAuthChannel().localAddress());

        // no handlers registered
        String TAIL_CONTEXT = "DefaultChannelPipeline$TailContext#0";
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAcctChannel().pipeline().names());
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAuthChannel().pipeline().names());

        server.start().syncUninterruptibly();

        // registered with eventLoop
        assertTrue(server.getAcctChannel().isRegistered());
        assertTrue(server.getAuthChannel().isRegistered());

        // bound to socket
        assertNotNull(server.getAcctChannel().localAddress());
        assertNotNull(server.getAuthChannel().localAddress());

        // handlers registered
        final String mockHandlerName = "RadiusServerTest$MockHandler#0";
        assertEquals(Arrays.asList(mockHandlerName, TAIL_CONTEXT), server.getAcctChannel().pipeline().names());
        assertEquals(Arrays.asList(mockHandlerName, TAIL_CONTEXT), server.getAuthChannel().pipeline().names());

        server.stop().syncUninterruptibly();
        Thread.sleep(500);

        // not registered with eventLoop
        assertFalse(server.getAcctChannel().isRegistered());
        assertFalse(server.getAuthChannel().isRegistered());

        // no handlers registered
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAcctChannel().pipeline().names());
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAuthChannel().pipeline().names());
    }

    private static class MockHandler extends HandlerAdapter<RadiusPacket> {

        private final AtomicInteger count = new AtomicInteger();

        MockHandler() {
            super(null, null, null, null, null);
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            count.incrementAndGet();
        }
    }
}
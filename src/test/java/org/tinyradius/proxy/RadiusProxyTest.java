package org.tinyradius.proxy;

import io.netty.channel.ChannelFactory;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RadiusProxyTest {

    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);
    private static final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

    @AfterAll
    static void afterAll() {
        eventExecutors.shutdownGracefully().syncUninterruptibly();
    }

    @Test
    void proxyStartStop() throws InterruptedException {
        final MockProxyHandlerAdapter mockProxyHandlerAdapter = new MockProxyHandlerAdapter();

        final RadiusProxy server = new RadiusProxy(
                eventExecutors, channelFactory, null, mockProxyHandlerAdapter, 0, 0);

        assertFalse(mockProxyHandlerAdapter.isStarted);

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

        assertTrue(mockProxyHandlerAdapter.isStarted);

        // registered with eventLoop
        assertTrue(server.getAcctChannel().isRegistered());
        assertTrue(server.getAuthChannel().isRegistered());

        // bound to socket
        assertNotNull(server.getAcctChannel().localAddress());
        assertNotNull(server.getAuthChannel().localAddress());

        // handlers registered
        final String mockHandlerName = "RadiusProxyTest$MockProxyHandlerAdapter#0";
        assertEquals(Arrays.asList(mockHandlerName, TAIL_CONTEXT), server.getAcctChannel().pipeline().names());
        assertEquals(Arrays.asList(mockHandlerName, TAIL_CONTEXT), server.getAuthChannel().pipeline().names());

        server.stop().syncUninterruptibly();
        Thread.sleep(500);

        assertFalse(mockProxyHandlerAdapter.isStarted);

        // not registered with eventLoop
        assertFalse(server.getAcctChannel().isRegistered());
        assertFalse(server.getAuthChannel().isRegistered());

        // no handlers registered
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAcctChannel().pipeline().names());
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAuthChannel().pipeline().names());
    }

    private static class MockProxyHandlerAdapter extends ProxyHandlerAdapter {

        private boolean isStarted = false;

        private MockProxyHandlerAdapter() {
            super(DefaultDictionary.INSTANCE, null, null, null);
        }

        @Override
        public Future<Void> start() {
            isStarted = true;
            return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
        }

        @Override
        public Future<Void> stop() {
            isStarted = false;
            return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
        }
    }
}
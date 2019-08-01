package org.tinyradius.proxy;

import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.proxy.handler.ProxyRequestHandler;
import org.tinyradius.util.RadiusEndpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyHandlerAdapterTest {

    private final PacketEncoder packetEncoder = new PacketEncoder(DefaultDictionary.INSTANCE);

    @Test
    void lifecycleCommands() {
        final HashedWheelTimer timer = new HashedWheelTimer();
        final MockProxyRequestHandler mockProxyRequestHandler = new MockProxyRequestHandler();

        final ProxyHandlerAdapter proxyHandlerAdapter =
                new ProxyHandlerAdapter(packetEncoder, mockProxyRequestHandler, timer, a -> "mysecret");

        assertFalse(mockProxyRequestHandler.isStarted);

        proxyHandlerAdapter.start().syncUninterruptibly();
        assertTrue(mockProxyRequestHandler.isStarted);

        proxyHandlerAdapter.stop().syncUninterruptibly();
        assertFalse(mockProxyRequestHandler.isStarted);

        timer.stop();
    }

    private static class MockProxyRequestHandler extends ProxyRequestHandler {

        private boolean isStarted = false;

        private MockProxyRequestHandler() {
            super(null);
        }

        @Override
        public RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
            return null;
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
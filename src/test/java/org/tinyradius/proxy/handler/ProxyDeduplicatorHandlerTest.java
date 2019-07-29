package org.tinyradius.proxy.handler;

import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import static org.junit.jupiter.api.Assertions.*;

class ProxyDeduplicatorHandlerTest {

    @Test
    void lifecycleCommands() {
        final HashedWheelTimer timer = new HashedWheelTimer();
        final MockProxyRequestHandler mockProxyRequestHandler = new MockProxyRequestHandler();
        final ProxyDeduplicatorHandler proxyDeduplicatorHandler = new ProxyDeduplicatorHandler(mockProxyRequestHandler, timer, 1000);

        assertFalse(mockProxyRequestHandler.isStarted);

        proxyDeduplicatorHandler.start().syncUninterruptibly();
        assertTrue(mockProxyRequestHandler.isStarted);

        proxyDeduplicatorHandler.stop().syncUninterruptibly();
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
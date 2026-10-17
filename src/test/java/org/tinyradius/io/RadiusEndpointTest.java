package org.tinyradius.io;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class RadiusEndpointTest {

    @Test
    void equalsHashCodeToString() {
        var addr1 = new InetSocketAddress("127.0.0.1", 1812);
        var addr2 = new InetSocketAddress("127.0.0.1", 1813);

        var ep1 = new RadiusEndpoint(addr1, "secret1");
        var ep1Same = new RadiusEndpoint(addr1, "secret1");
        var epDiffAddr = new RadiusEndpoint(addr2, "secret1");
        var epDiffSecret = new RadiusEndpoint(addr1, "secret2");

        assertEquals(addr1, ep1.address());
        assertEquals("secret1", ep1.secret());
        assertEquals(ep1, ep1Same);
        assertEquals(ep1.hashCode(), ep1Same.hashCode());
        assertNotEquals(ep1, epDiffAddr);
        assertNotEquals(ep1, epDiffSecret);
        assertTrue(ep1.toString().contains("RadiusEndpoint{address="));
        assertFalse(ep1.toString().contains("secret1"));
    }

    @Test
    void lifecycleCloseInterrupt() {
        var promise = new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE);
        var lifecycle = new RadiusLifecycle() {
            @Override
            public @NonNull Future<Void> isReady() {
                return promise;
            }

            @Override
            public @NonNull Future<Void> closeAsync() {
                Thread.currentThread().interrupt();
                return promise;
            }
        };

        assertSame(promise, lifecycle.isReady());
        lifecycle.close();
        assertTrue(Thread.interrupted()); // verify interrupted status was set and clear it
    }
}

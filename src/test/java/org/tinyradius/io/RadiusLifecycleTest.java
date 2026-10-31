package org.tinyradius.io;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiusLifecycleTest {

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

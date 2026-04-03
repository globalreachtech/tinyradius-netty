package org.tinyradius.io;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RadiusLifecycleTest {

    @Test
    void testRadiusLifecycleClose() {
        RadiusLifecycle lifecycle = mock(RadiusLifecycle.class);
        Promise<Void> closePromise = ImmediateEventExecutor.INSTANCE.newPromise();
        closePromise.setSuccess(null);

        when(lifecycle.closeAsync()).thenReturn(closePromise);
        doCallRealMethod().when(lifecycle).close();

        lifecycle.close();

        verify(lifecycle).closeAsync();
    }

    @Test
    void testRadiusLifecycleCloseInterrupted() throws InterruptedException {
        RadiusLifecycle lifecycle = mock(RadiusLifecycle.class);
        Future<Void> closeFuture = mock(Future.class);

        when(lifecycle.closeAsync()).thenReturn(closeFuture);
        when(closeFuture.sync()).thenThrow(new InterruptedException());
        doCallRealMethod().when(lifecycle).close();

        lifecycle.close();

        assertTrue(Thread.interrupted());
        verify(lifecycle).closeAsync();
    }
}

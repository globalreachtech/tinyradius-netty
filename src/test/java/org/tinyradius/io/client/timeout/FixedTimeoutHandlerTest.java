package org.tinyradius.io.client.timeout;

import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.client.PendingRequestCtx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedTimeoutHandlerTest {

    @AutoClose("stop")
    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;

    @Mock
    private Runnable mockRetry;

    @Test
    void retryFailIfMaxAttempts() {
        var promise = eventExecutor.<RadiusResponse>newPromise();
        var ctx = new PendingRequestCtx(null, null, promise);

        final FixedTimeoutHandler retryStrategy = new FixedTimeoutHandler(timer, 2, 0);

        // totalAttempts < maxAttempts
        retryStrategy.scheduleTimeout(mockRetry, 1, ctx);
        assertEquals(1, timer.pendingTimeouts());

        verify(mockRetry, timeout(500)).run();
        assertEquals(0, timer.pendingTimeouts());

        // totalAttempts >= maxAttempts
        retryStrategy.scheduleTimeout(mockRetry, 2, ctx);
        assertEquals(1, timer.pendingTimeouts());

        // still one invocation after 500ms
        verify(mockRetry, after(500)).run();
        assertEquals(0, timer.pendingTimeouts());

        assertFalse(promise.isSuccess());
        assertTrue(promise.cause().getMessage().toLowerCase().contains("max attempts reached"));
    }

    @Test
    void retryRunOk() {
        var promise = eventExecutor.<RadiusResponse>newPromise();
        var ctx = new PendingRequestCtx(null, null, promise);

        final FixedTimeoutHandler retryStrategy = new FixedTimeoutHandler(timer, 3, 100);

        // first retry runs
        retryStrategy.scheduleTimeout(mockRetry, 1, ctx);
        assertEquals(1, timer.pendingTimeouts());

        verify(mockRetry, timeout(500)).run();
        assertEquals(0, timer.pendingTimeouts());
    }

    @Test
    void noRetryIfPromiseDone() {
        final FixedTimeoutHandler retryStrategy = new FixedTimeoutHandler(timer, 3, 0);

        var promise = eventExecutor.<RadiusResponse>newPromise();
        var ctx = new PendingRequestCtx(null, null, promise);
        promise.trySuccess(null);
        assertTrue(promise.isDone());

        retryStrategy.scheduleTimeout(mockRetry, 2, ctx);
        assertEquals(1, timer.pendingTimeouts());

        verify(mockRetry, after(500).never()).run();
        assertEquals(0, timer.pendingTimeouts());
    }
}
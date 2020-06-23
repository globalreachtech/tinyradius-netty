package org.tinyradius.client.timeout;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.packet.response.RadiusResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicTimeoutHandlerTest {

    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);

    @Mock
    private Runnable mockRetry;

    @Test
    void retryFailIfMaxAttempts() {
        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();

        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 2, 0);

        // totalAttempts < maxAttempts
        retryStrategy.onTimeout(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());

        verify(mockRetry, timeout(500)).run();
        assertEquals(0, timer.pendingTimeouts());

        // totalAttempts >= maxAttempts
        retryStrategy.onTimeout(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());

        // still one invocation after 500ms
        verify(mockRetry, after(500)).run();
        assertEquals(0, timer.pendingTimeouts());

        assertFalse(promise.isSuccess());
        assertTrue(promise.cause().getMessage().toLowerCase().contains("max attempts reached"));
    }

    @Test
    void retryRunOk() {
        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();

        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 3, 100);

        // first retry runs
        retryStrategy.onTimeout(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());

        verify(mockRetry, timeout(500)).run();
        assertEquals(0, timer.pendingTimeouts());
    }

    @Test
    void noRetryIfPromiseDone() {
        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 3, 0);

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();
        promise.trySuccess(null);
        assertTrue(promise.isDone());

        retryStrategy.onTimeout(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());

        verify(mockRetry, after(500).never()).run();
        assertEquals(0, timer.pendingTimeouts());
    }
}
package org.tinyradius.client.timeout;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.packet.GenericRadiusPacket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicTimeoutHandlerTest {

    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);

    @Mock
    private Runnable mockRetry;

    private void waitTimer() {
        while (timer.pendingTimeouts() != 0) {
            try {
                Thread.sleep(110);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    void retryFailIfMaxAttempts() {
        final Promise<GenericRadiusPacket> promise = eventLoopGroup.next().newPromise();

        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 2, 0);

        // totalAttempts < maxAttempts
        retryStrategy.onTimeout(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        verify(mockRetry, times(1)).run();

        // totalAttempts >= maxAttempts
        retryStrategy.onTimeout(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        verify(mockRetry, times(1)).run(); // unchanged
        assertFalse(promise.isSuccess());
        assertTrue(promise.cause().getMessage().toLowerCase().contains("max attempts reached"));
    }

    @Test
    void retryRunOk() {
        final Promise<GenericRadiusPacket> promise = eventLoopGroup.next().newPromise();

        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 3, 100);

        // first retry runs
        retryStrategy.onTimeout(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        verify(mockRetry, times(1)).run();
    }

    @Test
    void noRetryIfPromiseDone() {
        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 3, 0);

        final Promise<GenericRadiusPacket> promise = eventLoopGroup.next().newPromise();
        promise.trySuccess(null);
        assertTrue(promise.isDone());

        retryStrategy.onTimeout(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());

        waitTimer();

        verify(mockRetry, never()).run();
    }
}
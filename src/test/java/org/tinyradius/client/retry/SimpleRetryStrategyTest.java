package org.tinyradius.client.retry;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.packet.RadiusPacket;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRetryStrategyTest {

    private static final HashedWheelTimer timer = new HashedWheelTimer();
    private static final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @AfterAll
    static void afterAll() {
        timer.stop();
        eventLoopGroup.shutdownGracefully().syncUninterruptibly();
    }

    private static void waitTimer() {
        while (timer.pendingTimeouts() != 0) {
            try {
                Thread.sleep(110);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    void retryFailIfMaxAttempts() {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        final MockRetry mockRetry = new MockRetry();

        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 2, 0);

        // totalAttempts < maxAttempts
        retryStrategy.scheduleRetry(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        assertEquals(1, mockRetry.count.get());

        // totalAttempts >= maxAttempts
        retryStrategy.scheduleRetry(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        assertEquals(1, mockRetry.count.get()); // unchanged
        assertFalse(promise.isSuccess());
        assertTrue(promise.cause().getMessage().toLowerCase().contains("max retries reached"));
    }

    @Test
    void retryRunOk() {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        final MockRetry mockRetry = new MockRetry();

        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 3, 100);

        // first retry runs
        retryStrategy.scheduleRetry(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        assertEquals(1, mockRetry.count.get());
    }

    @Test
    void noRetryIfPromiseDone() {
        final MockRetry mockRetry = new MockRetry();

        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 3, 0);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        promise.trySuccess(null);
        assertTrue(promise.isDone());

        retryStrategy.scheduleRetry(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());

        waitTimer();

        assertEquals(0, mockRetry.count.get()); // verify retry did not run
    }

    private static class MockRetry implements Runnable {

        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public void run() {
            count.incrementAndGet();
        }
    }
}
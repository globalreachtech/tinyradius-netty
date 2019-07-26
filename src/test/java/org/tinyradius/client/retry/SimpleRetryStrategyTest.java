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
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    void retryFailIfMaxAttempts() {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        final MockRetry mockRetry = new MockRetry();

        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 2, 0);

        // attempt == maxAttempt
        retryStrategy.scheduleRetry(mockRetry, 2, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        assertEquals(1, mockRetry.getCount());

        // attempt > maxAttempt
        retryStrategy.scheduleRetry(mockRetry, 3, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        assertEquals(1, mockRetry.getCount()); // unchanged
        assertFalse(promise.isSuccess());
        assertTrue(promise.cause().getMessage().toLowerCase().contains("max retries reached"));
    }

    @Test
    void retryRunOk() {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        final MockRetry mockRetry = new MockRetry();

        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 3, 0);

        // first retry runs
        retryStrategy.scheduleRetry(mockRetry, 1, promise);
        assertEquals(1, timer.pendingTimeouts());
        waitTimer();

        assertEquals(1, mockRetry.getCount());
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

        assertEquals(0, mockRetry.getCount()); // verify retry did not run

    }

    private static void waitTimer() {
        while (timer.pendingTimeouts() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class MockRetry implements Runnable {

        private AtomicInteger count = new AtomicInteger(0);

        @Override
        public void run() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.intValue();
        }
    }
}
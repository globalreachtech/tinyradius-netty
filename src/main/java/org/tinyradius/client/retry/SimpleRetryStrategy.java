package org.tinyradius.client.retry;

import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SimpleRetryStrategy implements RetryStrategy {

    private final Timer timer;
    private final int maxAttempts;
    private final int retryWait;

    /**
     *
     * @param timer
     * @param maxAttempts
     * @param retryWait time to wait before next retry, in milliseconds
     */
    public SimpleRetryStrategy(Timer timer, int maxAttempts, int retryWait) {
        this.timer = timer;
        this.maxAttempts = maxAttempts;
        this.retryWait = retryWait;
    }

    @Override
    public void scheduleRetry(Runnable retry, int totalAttempts, Promise<RadiusPacket> promise) {
        timer.newTimeout(t -> {
            if (promise.isDone())
                return;

            if (totalAttempts >= maxAttempts)
                promise.tryFailure(new RadiusException("Client send failed, max retries reached: " + maxAttempts));
            else
                retry.run();
        }, retryWait, MILLISECONDS);
    }
}

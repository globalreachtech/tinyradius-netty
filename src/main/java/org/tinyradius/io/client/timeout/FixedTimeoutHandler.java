package org.tinyradius.io.client.timeout;

import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.tinyradius.core.packet.response.RadiusResponse;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * TimeoutHandler that waits a fixed period for every timeout,
 * up to a predefined max attempt count.
 */
public class FixedTimeoutHandler implements TimeoutHandler {

    private final Timer timer;
    private final int maxAttempts;
    private final int timeoutMs;

    public FixedTimeoutHandler(Timer timer) {
        this(timer, 1, 1000);
    }

    /**
     * @param timer       netty timer for timing out requests
     * @param maxAttempts max number of attempts to try before returning failure
     * @param timeoutMs   time to wait before timeout or next retry, in milliseconds
     */
    public FixedTimeoutHandler(Timer timer, int maxAttempts, int timeoutMs) {
        this.timer = timer;
        this.maxAttempts = maxAttempts;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void onTimeout(Runnable retry, int totalAttempts, Promise<RadiusResponse> promise) {
        timer.newTimeout(t -> {
            if (promise.isDone())
                return;

            if (totalAttempts >= maxAttempts)
                promise.tryFailure(new TimeoutException("Client send timeout - max attempts reached: " + maxAttempts));
            else
                retry.run();
        }, timeoutMs, MILLISECONDS);
    }
}

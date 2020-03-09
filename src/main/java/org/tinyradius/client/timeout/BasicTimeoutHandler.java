package org.tinyradius.client.timeout;

import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusResponse;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * TimeoutHandler that waits a fixed period for every timeout,
 * up to a predefined max attempt count.
 */
public class BasicTimeoutHandler implements TimeoutHandler {

    private final Timer timer;
    private final int maxAttempts;
    private final int timeoutMs;

    public BasicTimeoutHandler(Timer timer) {
        this(timer, 1, 1000);
    }

    /**
     * @param timer       netty timer for timing out requests
     * @param maxAttempts max number of attempts to try before returning failure
     * @param timeoutMs   time to wait before timeout or next retry, in milliseconds
     */
    public BasicTimeoutHandler(Timer timer, int maxAttempts, int timeoutMs) {
        this.timer = timer;
        this.maxAttempts = maxAttempts;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void onTimeout(Runnable callback, int totalAttempts, Promise<RadiusResponse> promise) {
        timer.newTimeout(t -> {
            if (promise.isDone())
                return;

            if (totalAttempts >= maxAttempts)
                promise.tryFailure(new IOException("Client send timeout, max attempts reached: " + maxAttempts));
            else
                callback.run();
        }, timeoutMs, MILLISECONDS);
    }
}

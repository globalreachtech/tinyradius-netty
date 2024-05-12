package org.tinyradius.io.client.timeout;

import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import lombok.RequiredArgsConstructor;
import org.tinyradius.core.packet.response.RadiusResponse;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * TimeoutHandler that waits a fixed period for every timeout,
 * up to a predefined max attempt count.
 */
@RequiredArgsConstructor
public class FixedTimeoutHandler implements TimeoutHandler {

    /**
     * netty timer for timing out requests
     */
    private final Timer timer;

    /**
     * max number of attempts to try before returning failure
     */
    private final int maxAttempts;

    /**
     * time to wait before timeout or next retry, in milliseconds
     */
    private final int timeoutMs;

    public FixedTimeoutHandler(Timer timer) {
        this(timer, 1, 1000);
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

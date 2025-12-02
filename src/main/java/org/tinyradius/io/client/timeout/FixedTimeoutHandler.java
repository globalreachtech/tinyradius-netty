package org.tinyradius.io.client.timeout;

import io.netty.util.Timer;
import org.tinyradius.io.client.ClientEventListener;
import org.tinyradius.io.client.PendingRequestCtx;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.tinyradius.io.client.ClientEventListener.EventType.ATTEMPT_TIMEOUT;

/**
 * TimeoutHandler that waits a fixed period for every timeout,
 * up to a predefined max attempt count.
 *
 * @param timer       netty timer for timing out requests
 * @param maxAttempts max number of attempts to try before returning failure
 * @param timeoutMs   time to wait before timeout or next retry, in milliseconds
 */
public record FixedTimeoutHandler(Timer timer, int maxAttempts, int timeoutMs) implements TimeoutHandler {

    public FixedTimeoutHandler(Timer timer) {
        this(timer, 1, 1000);
    }

    @Override
    public void scheduleTimeout(Runnable retry, int totalAttempts, PendingRequestCtx ctx, ClientEventListener eventListener) {
        timer.newTimeout(t -> {
            if (ctx.getResponse().isDone())
                return;

            eventListener.onEvent(ATTEMPT_TIMEOUT, ctx);

            if (totalAttempts >= maxAttempts)
                ctx.getResponse().tryFailure(new TimeoutException("Client send timeout - max attempts reached: " + maxAttempts));
            else
                retry.run();
        }, timeoutMs, MILLISECONDS);
    }
}

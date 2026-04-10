package org.tinyradius.io.client.timeout;

import io.netty.util.Timer;
import org.jspecify.annotations.NonNull;
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
public record FixedTimeoutHandler(@NonNull Timer timer, int maxAttempts, int timeoutMs) implements TimeoutHandler {

    /**
     * Creates a new FixedTimeoutHandler with default values (1 attempt, 1000ms timeout).
     *
     * @param timer netty timer for timing out requests
     */
    public FixedTimeoutHandler(@NonNull Timer timer) {
        this(timer, 1, 1000);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleTimeout(@NonNull Runnable retry, int totalAttempts, @NonNull PendingRequestCtx ctx, @NonNull ClientEventListener eventListener) {
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

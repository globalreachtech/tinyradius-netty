package org.tinyradius.io.client.timeout;

import org.tinyradius.io.client.ClientEventListener;
import org.tinyradius.io.client.PendingRequestCtx;

import static org.tinyradius.io.client.ClientEventListener.NO_OP_LISTENER;

/**
 * Schedules a retry in the future and handles timeouts.
 * <p>
 * Invoked after every request is sent to schedule a retry
 * or timeout check in the future.
 */
public interface TimeoutHandler {

    /**
     * Schedule a timeout handler in the future. Invoked immediately after a request is sent
     * to schedule next retry.
     * <p>
     * When retry is due to run, implementation should also check if promise isDone() before running.
     * <p>
     * Implemented here instead of RadiusClient so custom scheduling / retry backoff
     * can be used depending on implementation, and actual retry can be deferred. Scheduling
     * and logic should be implemented here, while RadiusClient only deals with IO.
     *
     * @param retry         runnable to invoke to retry
     * @param totalAttempts current attempt count
     * @param ctx           request context with promise that resolves when a response is received
     * @param eventListener event listener
     */
    void scheduleTimeout(Runnable retry, int totalAttempts, PendingRequestCtx ctx, ClientEventListener eventListener);

    /**
     * Schedule a timeout handler in the future. Invoked immediately after a request is sent
     * to schedule next retry.
     * <p>
     * When retry is due to run, implementation should also check if promise isDone() before running.
     * <p>
     * Implemented here instead of RadiusClient so custom scheduling / retry backoff
     * can be used depending on implementation, and actual retry can be deferred. Scheduling
     * and logic should be implemented here, while RadiusClient only deals with IO.
     *
     * @param retry         runnable to invoke to retry
     * @param totalAttempts current attempt count
     * @param ctx           request context with promise that resolves when a response is received
     */
    default void scheduleTimeout(Runnable retry, int totalAttempts, PendingRequestCtx ctx) {
        scheduleTimeout(retry, totalAttempts, ctx, NO_OP_LISTENER);
    }
}

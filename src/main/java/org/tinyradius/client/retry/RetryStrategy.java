package org.tinyradius.client.retry;

import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusPacket;

public interface RetryStrategy {

    /**
     * Schedule a retry in the future.
     * <p>
     * When retry is due to run, should also check if promise isDone() before running.
     * <p>
     * Implemented here instead of RadiusClient so custom scheduling / retry backoff
     * can be used depending on implementation, and actual retry can be deferred. Scheduling
     * and logic should be implemented here, while RadiusClient only deals with IO.
     *
     * @param retry   runnable to invoke to retry
     * @param attempt current attempt count
     * @param promise request promise that resolves when a reponse is received
     */
    void scheduleRetry(Runnable retry, int attempt, Promise<RadiusPacket> promise);
}

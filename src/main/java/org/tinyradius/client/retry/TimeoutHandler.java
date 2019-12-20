package org.tinyradius.client.retry;

import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusPacket;

public interface TimeoutHandler {

    /**
     * Schedule a retry in the future. Invoked immediately after a request is sent
     * to schedule next retry.
     * <p>
     * When retry is due to run, should also check if promise isDone() before running.
     * <p>
     * Implemented here instead of RadiusClient so custom scheduling / retry backoff
     * can be used depending on implementation, and actual retry can be deferred. Scheduling
     * and logic should be implemented here, while RadiusClient only deals with IO.
     *
     * @param retry   runnable to invoke to retry
     * @param totalAttempts current attempt count
     * @param promise request promise that resolves when a response is received
     */
    void onTimeout(Runnable retry, int totalAttempts, Promise<RadiusPacket> promise);
}

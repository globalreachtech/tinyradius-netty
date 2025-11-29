package org.tinyradius.io.client;

/**
 * Instrumentation event hooks for RadiusClient, with event types for various stages in a request/response lifecycle
 */
@FunctionalInterface
public interface ClientEventListener {

    /**
     * Invoked when an event occurs.
     *
     * @param eventType         event type
     * @param pendingRequestCtx pending request context containing response promise
     */
    void onEvent(EventType eventType, PendingRequestCtx pendingRequestCtx);

    enum EventType {
        /**
         * Triggered just before a request is sent and channel flushed
         */
        PRE_SEND,
        /**
         * Request attempt timed out. Attempts may be retried, causing multiple instances of this event
         */
        ATTEMPT_TIMEOUT,
        /**
         * Response packet received
         */
        POST_RECEIVE
    }


    ClientEventListener NO_OP_LISTENER = (EventType eventType, PendingRequestCtx pendingRequestCtx) -> {
    };

}

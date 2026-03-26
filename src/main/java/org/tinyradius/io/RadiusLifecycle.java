package org.tinyradius.io;

import io.netty.util.concurrent.Future;

/**
 * A lifecycle interface for RADIUS clients and servers.
 */
public interface RadiusLifecycle extends AutoCloseable {

    /**
     * Returns a future that is notified when this lifecycle component is ready.
     *
     * @return a future that is notified when this lifecycle component is ready
     */
    Future<Void> isReady();

    /**
     * Closes this resource asynchronously.
     * @return a future that is notified when the close operation is complete
     */
    Future<Void> closeAsync();

    /**
     * Closes this resource, relinquishing any underlying resources. Note that this will block until close is complete.
     * <p>
     * Prefer {@link #closeAsync()} to avoid blocking the calling thread and to allow graceful handling of InterruptedExceptions.
     */
    @Override
    default void close() {
        try {
            closeAsync().sync();
        } catch (InterruptedException e) {
            // AutoCloseable.close() should not throw InterruptedException
            Thread.currentThread().interrupt();
        }
    }

}

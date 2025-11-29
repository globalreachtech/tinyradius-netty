package org.tinyradius.io;

import io.netty.util.concurrent.Future;

public interface RadiusLifecycle extends AutoCloseable {

    Future<Void> isReady();

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

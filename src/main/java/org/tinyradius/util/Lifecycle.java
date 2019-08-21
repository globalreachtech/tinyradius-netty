package org.tinyradius.util;

import io.netty.util.concurrent.Future;

public interface Lifecycle {

    /**
     * Initialize dependencies.
     *
     * @return future completes when required resources have been set up.
     */
    Future<Void> start();

    /**
     * Shutdown and close resources.
     *
     * @return future completes when resources shutdown
     */
    Future<Void> stop();
}

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
     * Shutdown and close resources. Does not guarantee when shutdown is complete.
     */
    Future<Void> stop();

}

package org.tinyradius.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.SucceededFuture;

public interface Lifecycle {

    /**
     * Initialize dependencies.
     *
     * @return future completes when required resources have been set up.
     */
    default Future<Void> start(){
        return new SucceededFuture<>(ImmediateEventExecutor.INSTANCE, null);
    }

    /**
     * Shutdown and close resources.
     *
     * @return future completes when resources shutdown
     */
    default Future<Void> stop(){
        return new SucceededFuture<>(ImmediateEventExecutor.INSTANCE, null);
    }

}

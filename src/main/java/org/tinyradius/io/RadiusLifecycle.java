package org.tinyradius.io;

import io.netty.util.concurrent.Future;

import java.io.Closeable;

public interface RadiusLifecycle extends Closeable {

    Future<Void> isReady();

    Future<Void> closeAsync();

    @Override
    default void close() {
        closeAsync();
    }

}

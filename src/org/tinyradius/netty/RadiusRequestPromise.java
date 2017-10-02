package org.tinyradius.netty;

import io.netty.util.concurrent.Promise;

/**
 * RadiusRequestFuture interface
 */
public interface RadiusRequestPromise
        extends RadiusRequestFuture, Promise<Void> {
}
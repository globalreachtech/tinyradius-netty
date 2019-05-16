package com.globalreachtech.tinyradius.netty;

import io.netty.util.concurrent.Future;

/**
 * RadiusRequestFuture interface
 */
public interface RadiusRequestFuture extends Future<Void> {

    RadiusRequestContext context();
}

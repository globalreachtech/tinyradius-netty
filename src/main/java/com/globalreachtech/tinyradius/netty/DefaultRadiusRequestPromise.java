package com.globalreachtech.tinyradius.netty;


import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

import static java.util.Objects.requireNonNull;

/**
 * DefaultRadiusRequestFuture class
 */
public class DefaultRadiusRequestPromise extends DefaultPromise<Void>
        implements RadiusRequestPromise {

    private RadiusRequestContext context;

    public DefaultRadiusRequestPromise(RadiusRequestContext context, EventExecutor executor) {
        super(executor);
        this.context = requireNonNull(context, "context cannot be null");
    }

    public RadiusRequestContext context() {
        return context;
    }

    @Override
    public int compareTo(RadiusRequestPromise o) {
        return 0;
    }
}

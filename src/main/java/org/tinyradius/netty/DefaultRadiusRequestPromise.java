package org.tinyradius.netty;


import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

/**
 * DefaultRadiusRequestFuture class
 */
public class DefaultRadiusRequestPromise extends DefaultPromise<Void>
        implements RadiusRequestPromise {

    private RadiusRequestContext context;

    /**
     *
     */
    public DefaultRadiusRequestPromise(RadiusRequestContext context, EventExecutor executor) {
        super(executor);
        if (context == null)
            throw new NullPointerException("context cannot be null");
        this.context = context;
    }

    /**
     *
     * @return
     */
    public RadiusRequestContext context() {
        return context;
    }
}

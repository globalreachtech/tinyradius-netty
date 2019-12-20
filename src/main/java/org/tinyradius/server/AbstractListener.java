package org.tinyradius.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public abstract class AbstractListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Bootstrap bootstrap;

//    bootstrap = new Bootstrap()
//                .group(eventLoopGroup)
//                .channel(channelClass);

    protected AbstractListener(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * @param listenAddress  the address to bind to
     * @param channelHandler handler to attach to end of channel pipeline
     * @return channel that resolves after it is bound to address and registered with eventLoopGroup
     */
    protected ChannelFuture listen(SocketAddress listenAddress, ChannelHandler channelHandler) {
        return bootstrap.clone()
                .handler(channelHandler)
                .bind(listenAddress);
    }

    /**
     * Initialize dependencies.
     *
     * @return future completes when required resources have been set up.
     */
    public abstract Future<Void> start();

    /**
     * Shutdown and close resources.
     *
     * @return future completes when resources shutdown
     */
    public abstract Future<Void> stop();

}

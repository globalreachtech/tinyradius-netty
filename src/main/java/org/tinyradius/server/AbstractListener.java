package org.tinyradius.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class AbstractListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final EventLoopGroup eventLoopGroup;
    private final Timer timer;

    protected AbstractListener(EventLoopGroup eventLoopGroup, Timer timer) {
        this.eventLoopGroup = eventLoopGroup;
        this.timer = timer;
    }

    /**
     * @param channel        to listen on
     * @param listenAddress  the address to bind to
     * @param channelHandler handler to attach to end of channel pipeline
     * @return channel that resolves after it is bound to address and registered with eventLoopGroup
     */
    protected ChannelFuture listen(DatagramChannel channel, SocketAddress listenAddress, ChannelHandler channelHandler) {
        channel.pipeline().addLast(channelHandler);

        final ChannelPromise promise = channel.newPromise();

        // fail-safe timeout
        Timeout timeout = timer.newTimeout(t -> promise.tryFailure(
                new TimeoutException("Timeout while binding channel " + channel + " to " + listenAddress)), 20, SECONDS);

        promise.addListener(f -> {
            timeout.cancel();

            if (f.isSuccess())
                logger.info("Now listening on {}", listenAddress);
            else
                logger.warn("Unable to listen on {}: {}", listenAddress, f.cause());
        });

        eventLoopGroup.register(channel).addListener(f -> {
            if (f.isSuccess())
                channel.bind(listenAddress).addListener(g -> {
                    if (g.isSuccess())
                        promise.trySuccess();
                    else
                        promise.tryFailure(g.cause());
                });
            else
                promise.tryFailure(f.cause());
        });

        return promise;
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

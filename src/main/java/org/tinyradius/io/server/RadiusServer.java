package org.tinyradius.io.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.io.RadiusLifecycle;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer implements RadiusLifecycle {

    private static final Logger log = LogManager.getLogger(RadiusServer.class);
    private final EventLoopGroup eventLoopGroup;

    private final List<ChannelFuture> channelFutures;

    private final Promise<Void> isReady;

    /**
     * Creates a new RADIUS server that listens on two sockets.
     *
     * @param bootstrap bootstrap with channel class and eventLoopGroup set up
     * @param handler1  ChannelHandler to handle requests received on socket1
     * @param handler2  ChannelHandler to handle requests received on socket2
     * @param socket1   socket to listen on
     * @param socket2   socket to listen on
     */
    public RadiusServer(@NonNull Bootstrap bootstrap,
                        @NonNull ChannelHandler handler1,
                        @NonNull ChannelHandler handler2,
                        @NonNull InetSocketAddress socket1,
                        @NonNull InetSocketAddress socket2) {
        this(bootstrap, List.of(handler1, handler2), List.of(socket1, socket2));
    }

    /**
     * Creates a new RADIUS server that listens on multiple sockets.
     *
     * @param bootstrap       bootstrap with channel class and eventLoopGroup set up
     * @param channelHandlers list of channelHandlers to handle requests
     * @param socketAddresses list of socketAddresses to bind channelHandlers to, must be same length
     */
    public RadiusServer(@NonNull Bootstrap bootstrap, @NonNull List<ChannelHandler> channelHandlers, @NonNull List<InetSocketAddress> socketAddresses) {
        if (channelHandlers.size() != socketAddresses.size())
            throw new IllegalArgumentException(String.format("ChannelHandlers size (%s) and SocketAddresses size (%s) don't match",
                    channelHandlers.size(), socketAddresses.size()));
        eventLoopGroup = bootstrap.config().group();

        var eventLoop = eventLoopGroup.next();
        isReady = eventLoop.newPromise();
        var combiner = new PromiseCombiner(eventLoop);
        channelFutures = IntStream.range(0, channelHandlers.size())
                .mapToObj(i -> bootstrap.clone().handler(channelHandlers.get(i)).bind(socketAddresses.get(i)))
                .toList();

        eventLoop.execute(() -> {
            combiner.addAll(channelFutures.toArray(ChannelFuture[]::new));
            combiner.finish(isReady);
        });

        isReady.addListener(f ->
                log.info("Server start success: {} for address {}", f.isSuccess(), socketAddresses));
    }

    /**
     * Returns the channels the server is listening on.
     *
     * @return the channels the server is listening on
     */
    @NonNull
    public List<Channel> getChannels() {
        return channelFutures.stream().map(ChannelFuture::channel).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Future<Void> isReady() {
        return isReady;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Future<Void> closeAsync() {
        log.info("Closing server on {}", channelFutures.stream()
                .map(ChannelFuture::channel)
                .map(Channel::localAddress)
                .toList());

        var futures = channelFutures.stream()
                .map(ChannelFuture::channel)
                .map(ChannelOutboundInvoker::close)
                .toList();

        var eventLoop = eventLoopGroup.next();
        var isClosed = eventLoop.<Void>newPromise();
        var combiner = new PromiseCombiner(eventLoop);
        eventLoop.execute(() -> {
            combiner.addAll(futures.toArray(ChannelFuture[]::new));
            combiner.finish(isClosed);
        });

        return isClosed;
    }
}
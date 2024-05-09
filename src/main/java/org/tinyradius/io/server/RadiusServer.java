package org.tinyradius.io.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import lombok.extern.log4j.Log4j2;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements a simple Radius server.
 */
@Log4j2
public class RadiusServer implements Closeable {

    private final List<ChannelFuture> channelFutures;
    private final Promise<Void> isReady;

    /**
     * @param bootstrap bootstrap with channel class and eventLoopGroup set up
     * @param handler1  ChannelHandler to handle requests received on socket1
     * @param handler2  ChannelHandler to handle requests received on socket2
     * @param socket1   socket to listen on
     * @param socket2   socket to listen on
     */
    public RadiusServer(Bootstrap bootstrap,
                        ChannelHandler handler1,
                        ChannelHandler handler2,
                        InetSocketAddress socket1,
                        InetSocketAddress socket2) {
        this(bootstrap, List.of(handler1, handler2), List.of(socket1, socket2));
    }

    /**
     * @param bootstrap       bootstrap with channel class and eventLoopGroup set up
     * @param channelHandlers list of channelHandlers to handle requests
     * @param socketAddresses list of socketAddresses to bind channelHandlers to, must be same length
     */
    public RadiusServer(Bootstrap bootstrap, List<ChannelHandler> channelHandlers, List<InetSocketAddress> socketAddresses) {
        if (channelHandlers.size() != socketAddresses.size())
            throw new IllegalArgumentException(String.format("ChannelHandlers size (%s) and SocketAddresses size (%s) don't match",
                    channelHandlers.size(), socketAddresses.size()));

        isReady = bootstrap.config().group().next().newPromise();
        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);

        channelFutures = IntStream.range(0, channelHandlers.size())
                .mapToObj(i -> bootstrap.clone().handler(channelHandlers.get(i)).bind(socketAddresses.get(i)))
                .collect(Collectors.toList());

        combiner.addAll(channelFutures.toArray(ChannelFuture[]::new));
        combiner.finish(isReady);
        isReady.addListener(f -> {
            final List<SocketAddress> addresses = channelFutures.stream()
                    .map(ChannelFuture::channel)
                    .map(Channel::localAddress).collect(Collectors.toList());
            log.info("Server start success: {} for address {}", f.isSuccess(), addresses);
        });
    }

    public Future<Void> isReady() {
        return isReady;
    }

    public List<Channel> getChannels() {
        return channelFutures.stream().map(ChannelFuture::channel).collect(Collectors.toList());
    }

    @Override
    public void close() {
        log.info("Closing server on {}", channelFutures.stream()
                .map(ChannelFuture::channel)
                .map(Channel::localAddress)
                .collect(Collectors.toList()));
        channelFutures.stream()
                .map(ChannelFuture::channel)
                .forEach(ChannelOutboundInvoker::close);
    }
}
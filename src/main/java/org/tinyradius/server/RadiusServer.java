package org.tinyradius.server;

import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.util.Lifecycle;

import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(RadiusServer.class);

    protected final EventLoopGroup eventLoopGroup;
    private final HandlerAdapter authHandler;
    private final HandlerAdapter acctHandler;
    private final InetSocketAddress authSocket;
    private final InetSocketAddress acctSocket;

    private final DatagramChannel authChannel;
    private final DatagramChannel acctChannel;

    private Future<Void> serverStatus = null;

    /**
     * @param eventLoopGroup for both channel IO and processing
     * @param factory        to create new Channel
     * @param authHandler    ChannelHandler to handle requests received on authSocket
     * @param acctHandler    ChannelHandler to handle requests received on acctSocket
     * @param authSocket     socket to listen on for auth requests
     * @param acctSocket     socket to listen on for accounting requests
     */
    public RadiusServer(EventLoopGroup eventLoopGroup,
                        ChannelFactory<? extends DatagramChannel> factory,
                        HandlerAdapter authHandler,
                        HandlerAdapter acctHandler,
                        InetSocketAddress authSocket,
                        InetSocketAddress acctSocket) {
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.authHandler = authHandler;
        this.acctHandler = acctHandler;
        this.authSocket = authSocket;
        this.acctSocket = acctSocket;
        this.authChannel = factory.newChannel();
        this.acctChannel = factory.newChannel();
    }

    @Override
    public Future<Void> start() {
        if (this.serverStatus != null)
            return this.serverStatus;

        Promise<Void> status = eventLoopGroup.next().newPromise();

        // todo error handling/timeout?
        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(listenAuth(), listenAcct());
        combiner.finish(status);

        this.serverStatus = status;
        return status;
    }

    @Override
    public Future<Void> stop() {
        logger.info("stopping Radius server");

        final Promise<Void> promise = eventLoopGroup.next().newPromise();

        final PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        promiseCombiner.addAll(authChannel.close(), acctChannel.close());

        promiseCombiner.finish(promise);
        return promise;
    }

    private ChannelFuture listenAuth() {
        logger.info("starting RadiusAuthListener on port " + authSocket.getPort());
        authChannel.pipeline().addLast(authHandler);
        return listen(authChannel, authSocket);
    }

    private ChannelFuture listenAcct() {
        logger.info("starting RadiusAcctListener on port " + acctSocket.getPort());
        acctChannel.pipeline().addLast(acctHandler);
        return listen(acctChannel, acctSocket);
    }

    /**
     * @param channel       to listen on
     * @param listenAddress the address to bind to
     * @return channelFuture of started channel socket
     */
    protected ChannelFuture listen(final DatagramChannel channel, final InetSocketAddress listenAddress) {
        requireNonNull(channel, "channel cannot be null");
        requireNonNull(listenAddress, "listenAddress cannot be null");

        final ChannelPromise promise = channel.newPromise();

        eventLoopGroup.register(channel)
                .addListener(f -> channel.bind(listenAddress)
                        .addListener(g -> promise.trySuccess()));

        return promise;
    }

    public DatagramChannel getAuthChannel() {
        return authChannel;
    }

    public DatagramChannel getAcctChannel() {
        return acctChannel;
    }
}
package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer extends AbstractListener {

    private static final Logger logger = LoggerFactory.getLogger(RadiusServer.class);

    private final EventLoopGroup eventLoopGroup;

    private final ChannelHandler authHandler;
    private final ChannelHandler acctHandler;
    private final InetSocketAddress authSocket;
    private final InetSocketAddress acctSocket;

    private ChannelFuture authFuture;
    private ChannelFuture acctFuture;

    private Future<Void> serverStatus = null;

    /**
     * @param eventLoopGroup for both channel IO and processing
     * @param authHandler    ChannelHandler to handle requests received on authSocket
     * @param acctHandler    ChannelHandler to handle requests received on acctSocket
     * @param authSocket     socket to listen on for auth requests
     * @param acctSocket     socket to listen on for accounting requests
     */
    public RadiusServer(EventLoopGroup eventLoopGroup,
                        Class<? extends DatagramChannel> datagramClass,
                        ChannelHandler authHandler,
                        ChannelHandler acctHandler,
                        InetSocketAddress authSocket,
                        InetSocketAddress acctSocket) {
        super(eventLoopGroup, datagramClass);
        this.eventLoopGroup = eventLoopGroup;
        this.authHandler = authHandler;
        this.acctHandler = acctHandler;
        this.authSocket = authSocket;
        this.acctSocket = acctSocket;
    }

    @Override
    public Future<Void> start() {
        if (this.serverStatus != null)
            return this.serverStatus;

        authFuture = listen(authSocket, authHandler);
        acctFuture = listen(acctSocket, acctHandler);

        Promise<Void> status = eventLoopGroup.next().newPromise();

        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(authFuture, acctFuture);
        combiner.finish(status);

        this.serverStatus = status;
        return status;
    }

    @Override
    public Future<Void> stop() {
        logger.info("stopping Radius server");

        final Promise<Void> promise = eventLoopGroup.next().newPromise();

        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        if (authFuture != null)
            combiner.add(authFuture.channel().close());
        if (acctFuture != null)
            combiner.add(acctFuture.channel().close());

        combiner.finish(promise);
        return promise;
    }

    public Channel getAuthChannel() {
        return authFuture.channel();
    }

    public Channel getAcctChannel() {
        return acctFuture.channel();
    }
}
package org.tinyradius.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(RadiusServer.class);

    private final ChannelFuture accessFuture;
    private final ChannelFuture accountingFuture;
    private final Promise<Void> serverStatus;

    /**
     * @param accessHandler     ChannelHandler to handle requests received on authSocket
     * @param accountingHandler ChannelHandler to handle requests received on acctSocket
     * @param accessSocket      socket to listen on for auth requests
     * @param accountingSocket  socket to listen on for accounting requests
     */
    public RadiusServer(Bootstrap bootstrap,
                        ChannelHandler accessHandler,
                        ChannelHandler accountingHandler,
                        InetSocketAddress accessSocket,
                        InetSocketAddress accountingSocket) {
        accessFuture = bootstrap.clone().handler(accessHandler).bind(accessSocket);
        accountingFuture = bootstrap.clone().handler(accountingHandler).bind(accountingSocket);

        serverStatus = bootstrap.config().group().next().newPromise();
        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(accessFuture, accountingFuture);
        combiner.finish(serverStatus);
    }

    public Future<Void> start() {
        return serverStatus;
    }

    public Channel getAuthChannel() {
        return accessFuture.channel();
    }

    public Channel getAcctChannel() {
        return accountingFuture.channel();
    }

    @Override
    public void close() {
        accessFuture.channel().close();
        accountingFuture.channel().close();
    }
}
package org.tinyradius.io.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    private final ChannelFuture accessFuture;
    private final ChannelFuture accountingFuture;
    private final Promise<Void> isReady;

    /**
     * @param bootstrap         bootstrap with channel class and eventLoopGroup set up
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

        isReady = bootstrap.config().group().next().newPromise();
        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(accessFuture, accountingFuture);
        combiner.finish(isReady);
        isReady.addListener(f -> {
            if (f.isSuccess())
                logger.info("Server started on {} and {}", accessFuture.channel().localAddress(), accountingFuture.channel().localAddress());
            else
                logger.info("Could not start server on {} and {}", accessFuture.channel().localAddress(), accountingFuture.channel().localAddress());
        });
    }

    public Future<Void> isReady() {
        return isReady;
    }

    public Channel getAuthChannel() {
        return accessFuture.channel();
    }

    public Channel getAcctChannel() {
        return accountingFuture.channel();
    }

    @Override
    public void close() {
        logger.info("Closing server on {} and {}", accessFuture.channel().localAddress(), accountingFuture.channel().localAddress());
        accessFuture.channel().close();
        accountingFuture.channel().close();
    }
}
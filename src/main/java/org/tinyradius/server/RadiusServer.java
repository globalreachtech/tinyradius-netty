package org.tinyradius.server;

import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.util.Lifecycle;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer<T extends DatagramChannel> implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(RadiusServer.class);

    protected final ChannelFactory<T> factory;
    protected final EventLoopGroup eventLoopGroup;
    private final ChannelHandler authHandler;
    private final ChannelHandler acctHandler;
    protected final int authPort;
    protected final int acctPort;

    protected final InetAddress listenAddress;
    private T authChannel = null;
    private T acctChannel = null;

    private Future<Void> serverStatus = null;

    /**
     * @param eventLoopGroup for both channel IO and processing
     * @param factory        to create new Channel
     * @param listenAddress  local address to bind to, will be wildcard address if null
     * @param authHandler    ChannelHandler to handle requests received on authPort
     * @param acctHandler    ChannelHandler to handle requests received on acctPort
     * @param authPort       port to bind to, or set to 0 to let system choose
     * @param acctPort       port to bind to, or set to 0 to let system choose
     */
    public RadiusServer(EventLoopGroup eventLoopGroup,
                        ChannelFactory<T> factory,
                        InetAddress listenAddress,
                        ChannelHandler authHandler,
                        ChannelHandler acctHandler,
                        int authPort, int acctPort) {
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.authHandler = authHandler;
        this.acctHandler = acctHandler;
        this.authPort = validPort(authPort);
        this.acctPort = validPort(acctPort);
        this.listenAddress = listenAddress;
    }

    @Override
    public Future<Void> start() {
        if (this.serverStatus != null)
            return this.serverStatus;

        final Promise<Void> status = eventLoopGroup.next().newPromise();

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(listenAuth(), listenAcct());
        promiseCombiner.finish(status);

        this.serverStatus = status;
        return status;
    }

    @Override
    public void stop() {
        logger.info("stopping Radius server");
        if (authChannel != null)
            authChannel.close();
        if (acctChannel != null)
            acctChannel.close();
    }

    protected int validPort(int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("bad port number");
        return port;
    }

    protected ChannelFuture listenAuth() {
        logger.info("starting RadiusAuthListener on port " + authPort);
        getAcctChannel().pipeline().addLast(authHandler);
        return listen(getAuthChannel(), new InetSocketAddress(listenAddress, authPort));
    }

    protected ChannelFuture listenAcct() {
        logger.info("starting RadiusAcctListener on port " + acctPort);
        getAcctChannel().pipeline().addLast(acctHandler);
        return listen(getAcctChannel(), new InetSocketAddress(listenAddress, acctPort));
    }

    /**
     * @param channel       to listen on
     * @param listenAddress the address to bind to
     * @return channelFuture of started channel socket
     */
    protected ChannelFuture listen(final T channel, final InetSocketAddress listenAddress) {
        requireNonNull(channel, "channel cannot be null");
        requireNonNull(listenAddress, "listenAddress cannot be null");

        final ChannelPromise promise = channel.newPromise();

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(eventLoopGroup.register(channel), channel.bind(listenAddress));
        promiseCombiner.finish(promise);

        return promise;
    }

    protected T getAuthChannel() {
        if (authChannel == null)
            authChannel = factory.newChannel();
        return authChannel;
    }

    protected T getAcctChannel() {
        if (acctChannel == null)
            acctChannel = factory.newChannel();
        return acctChannel;
    }

}
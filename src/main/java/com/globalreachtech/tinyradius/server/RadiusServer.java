package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

/**
 * Implements a simple Radius server. This class must be subclassed to
 * provide an implementation for getSharedSecret() and getUserPassword().
 * If the server supports accounting, it must override
 * accountingRequestReceived().
 */
public abstract class RadiusServer<T extends DatagramChannel> {

    private static Log logger = LogFactory.getLog(RadiusServer.class);

    protected final ChannelFactory<T> factory;
    protected final EventLoopGroup eventLoopGroup;
    private final AuthHandler authHandler;
    private final AcctHandler acctHandler;
    protected final int authPort;
    protected final int acctPort;

    protected InetAddress listenAddress;
    private T authChannel = null;
    private T acctChannel = null;

    private Future<Void> serverStatus = null;

    /**
     * @param listenAddress null address will assign wildcard address
     */
    public RadiusServer(EventLoopGroup eventLoopGroup,
                        ChannelFactory<T> factory,
                        InetAddress listenAddress,
                        AuthHandler authHandler,
                        AcctHandler acctHandler,
                        int authPort, int acctPort) {
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.authHandler = authHandler;
        this.acctHandler = acctHandler;
        this.authPort = validPort(authPort);
        this.acctPort = validPort(acctPort);
        this.listenAddress = listenAddress;
    }

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

    /**
     * Stops the server and closes the sockets.
     */
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

    /**
     * Returns the IP address the server listens on.
     * Returns null if listening on the wildcard address.
     *
     * @return listen address or null
     */
    public InetAddress getListenAddress() {
        return listenAddress;
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
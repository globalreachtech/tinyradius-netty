package org.tinyradius.server;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

/**
 * Implements a simple Radius server.
 */
public class RadiusServer extends AbstractListener {

    private static final Logger logger = LoggerFactory.getLogger(RadiusServer.class);

    private final EventLoopGroup eventLoopGroup;

    private final HandlerAdapter<? extends RadiusPacket, ? extends SecretProvider> authHandler;
    private final HandlerAdapter<? extends RadiusPacket, ? extends SecretProvider> acctHandler;
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
                        Timer timer,
                        ChannelFactory<? extends DatagramChannel> factory,
                        HandlerAdapter<? extends RadiusPacket, ? extends SecretProvider> authHandler,
                        HandlerAdapter<? extends RadiusPacket, ? extends SecretProvider> acctHandler,
                        InetSocketAddress authSocket,
                        InetSocketAddress acctSocket) {
        super(eventLoopGroup, timer);
        this.eventLoopGroup = eventLoopGroup;
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

        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(
                listen(authChannel, authSocket, authHandler),
                listen(acctChannel, acctSocket, acctHandler));
        combiner.finish(status);

        this.serverStatus = status;
        return status;
    }

    @Override
    public Future<Void> stop() {
        logger.info("stopping Radius server");

        final Promise<Void> promise = eventLoopGroup.next().newPromise();

        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        if (authChannel.isRegistered())
            combiner.add(authChannel.close());
        if (acctChannel.isRegistered())
            combiner.add(acctChannel.close());

        combiner.finish(promise);
        return promise;
    }

    public DatagramChannel getAuthChannel() {
        return authChannel;
    }

    public DatagramChannel getAcctChannel() {
        return acctChannel;
    }
}
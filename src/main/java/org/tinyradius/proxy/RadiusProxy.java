package org.tinyradius.proxy;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.server.RadiusServer;
import org.tinyradius.util.Lifecycle;

import java.net.InetSocketAddress;

/**
 * This class implements a basic Radius proxy that receives Radius packets
 * and forwards them to a Radius server.
 * <p>
 * You have to provide a handler that handles incoming requests.
 */
public class RadiusProxy extends RadiusServer implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(RadiusProxy.class);

    private final ProxyHandlerAdapter handlerAdapter;

    /**
     * @param eventLoopGroup for both channel IO and processing
     * @param factory        to create new Channel
     * @param handlerAdapter ProxyChannelInboundHandler to handle requests received on both authPort
     *                       and acctPort. Should also implement {@link Lifecycle} as the handler is
     *                       expected to manage the socket for proxying.
     * @param authSocket     port to bind to, or set to 0 to let system choose
     * @param acctSocket     port to bind to, or set to 0 to let system choose
     */
    public RadiusProxy(EventLoopGroup eventLoopGroup,
                       ChannelFactory<? extends DatagramChannel> factory,
                       ProxyHandlerAdapter handlerAdapter,
                       InetSocketAddress authSocket,
                       InetSocketAddress acctSocket) {
        super(eventLoopGroup, factory, handlerAdapter, handlerAdapter, authSocket, acctSocket);
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    public Future<Void> start() {
        Promise<Void> promise = eventLoopGroup.next().newPromise();

        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(super.start(), handlerAdapter.start());
        combiner.finish(promise);

        return promise;
    }

    @Override
    public Future<Void> stop() {
        logger.info("stopping Radius proxy");

        final Promise<Void> promise = eventLoopGroup.next().newPromise();

        final PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        promiseCombiner.addAll(handlerAdapter.stop(), super.stop());
        promiseCombiner.finish(promise);

        return promise;
    }
}

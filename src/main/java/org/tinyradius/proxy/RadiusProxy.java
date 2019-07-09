package org.tinyradius.proxy;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.server.RadiusServer;
import org.tinyradius.util.Lifecycle;

import java.net.InetAddress;

/**
 * This class implements a basic Radius proxy that receives Radius packets
 * and forwards them to a Radius server.
 * <p>
 * You have to provide a handler that handles incoming requests.
 */
public class RadiusProxy<T extends DatagramChannel> extends RadiusServer<T> implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(RadiusProxy.class);

    private final Lifecycle channelInboundHandler;

    /**
     * @param eventLoopGroup        for both channel IO and processing
     * @param factory               to create new Channel
     * @param listenAddress         local address to bind to, will be wildcard address if null
     * @param channelInboundHandler ProxyChannelInboundHandler to handle requests received on both authPort
     *                              and acctPort. Should also implement {@link Lifecycle} as the handler is
     *                              expected to manage the socket for proxying.
     * @param authPort              port to bind to, or set to 0 to let system choose
     * @param acctPort              port to bind to, or set to 0 to let system choose
     */
    public RadiusProxy(EventLoopGroup eventLoopGroup,
                       ChannelFactory<T> factory,
                       InetAddress listenAddress,
                       ProxyChannelInboundHandler channelInboundHandler,
                       int authPort, int acctPort) {
        super(eventLoopGroup, factory, listenAddress, channelInboundHandler, channelInboundHandler, authPort, acctPort);
        this.channelInboundHandler = channelInboundHandler;
    }

    @Override
    public Future<Void> start() {
        Promise<Void> promise = eventLoopGroup.next().newPromise();

        eventLoopGroup.submit(() -> {
            final PromiseCombiner combiner = new PromiseCombiner(eventLoopGroup.next());
            combiner.addAll(super.start(), channelInboundHandler.start());
            combiner.finish(promise);
        });

        return promise;
    }

    @Override
    public void stop() {
        logger.info("stopping Radius proxy");
        channelInboundHandler.stop();
        super.stop();
    }

}

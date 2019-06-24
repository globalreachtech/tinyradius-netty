package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.server.RadiusServer;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * This class implements a basic Radius proxy that receives Radius packets
 * and forwards them to a Radius server.
 * <p>
 * You have to provide a handler that handles incoming requests.
 */
public class RadiusProxy<T extends DatagramChannel> extends RadiusServer<T> {

    private static final Logger logger = LoggerFactory.getLogger(RadiusProxy.class);
    private final ProxyHandler proxyHandler;

    public RadiusProxy(EventLoopGroup eventLoopGroup,
                       ChannelFactory<T> factory,
                       InetAddress listenAddress,
                       ProxyHandler proxyHandler,
                       int authPort, int acctPort) {
        super(eventLoopGroup, factory, listenAddress, proxyHandler, proxyHandler, authPort, acctPort);
        this.proxyHandler = proxyHandler;
    }

    public Future<Void> start() {
        Promise<Void> promise = eventLoopGroup.next().newPromise();

        final PromiseCombiner combiner = new PromiseCombiner(eventLoopGroup.next());
        combiner.addAll(super.start(), proxyHandler.start());
        combiner.finish(promise);
        return promise;
    }

    @Override
    public void stop() {
        logger.info("stopping Radius proxy");
        proxyHandler.stop();
        super.stop();
    }

}

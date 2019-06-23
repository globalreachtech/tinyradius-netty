package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.server.RadiusServer;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;

/**
 * This class implements a Radius proxy that receives Radius packets
 * and forwards them to a Radius server.
 * <p>
 * You have to provide a packet manager that manages the proxy connection
 * a packet belongs to.
 */
public class RadiusProxy<T extends DatagramChannel> extends RadiusServer<T> {

    private static Log logger = LogFactory.getLog(RadiusProxy.class);
    private final ProxyHandler proxyHandler;

    public RadiusProxy(EventLoopGroup eventLoopGroup,
                       ChannelFactory<T> factory,
                       InetAddress listenAddress,
                       ProxyHandler proxyHandler,
                       int authPort, int acctPort) {
        super(eventLoopGroup, factory, listenAddress, proxyHandler, proxyHandler, authPort, acctPort);
        this.proxyHandler = proxyHandler;
    }

    @Override
    public void stop() {
        logger.info("stopping Radius proxy");
        proxyHandler.close();
        super.stop();
    }

}

package org.tinyradius.e2e.handler;

import io.netty.channel.ChannelHandler;
import org.tinyradius.core.packet.request.AccountingRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.RadiusClient;
import org.tinyradius.io.server.handler.ProxyHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

@ChannelHandler.Sharable
public class SimpleProxyHandler extends ProxyHandler {

    private final int originAcctPort;
    private final int originAuthPort;

    private final String originSecret;

    public SimpleProxyHandler(RadiusClient radiusClient, int originAccessPort, int originAcctPort, String originSecret) {
        super(radiusClient);
        this.originAuthPort = originAccessPort;
        this.originAcctPort = originAcctPort;
        this.originSecret = originSecret;
    }

    @Override
    public Optional<RadiusEndpoint> getOriginServer(RadiusRequest request, RadiusEndpoint client) {
        InetAddress address = InetAddress.getLoopbackAddress();
        int port = request instanceof AccountingRequest ? originAcctPort : originAuthPort;
        return Optional.of(new RadiusEndpoint(new InetSocketAddress(address, port), originSecret));
    }
}

package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.RadiusClient;

public class ProxyHandler extends SimpleChannelInboundHandler<RequestContext> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private final RadiusClient radiusClient;

    public ProxyHandler(RadiusClient radiusClient) {
        this.radiusClient = radiusClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestContext msg) throws Exception {

    }
}

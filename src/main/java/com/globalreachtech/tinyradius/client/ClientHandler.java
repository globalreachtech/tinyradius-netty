package com.globalreachtech.tinyradius.client;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

public abstract class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    /**
     * Save state info about outgoing requests so handler has context when handling replies
     * @return promise that will complete when corresponding reply is received
     */
    public abstract Promise<RadiusPacket> logOutbound(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor);
}

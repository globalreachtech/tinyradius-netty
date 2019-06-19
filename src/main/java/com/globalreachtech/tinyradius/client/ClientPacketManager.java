package com.globalreachtech.tinyradius.client;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

public interface ClientPacketManager {

    Promise<RadiusPacket> logOutbound(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor);

    void logInbound(DatagramPacket packet);
}

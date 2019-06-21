package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.client.RadiusClient;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.atomic.AtomicInteger;

public class ProxyPacketManager implements RadiusProxy.ConnectionManager, RadiusClient.PacketManager {

    /**
     * Index for Proxy-State attribute.
     */
    private AtomicInteger proxyIndex = new AtomicInteger(1);

    @Override
    public String nextProxyIndex() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    public Promise<RadiusPacket> handleOutbound(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {
        return null;
    }

    @Override
    public void handleInbound(DatagramPacket packet) {

    }
}

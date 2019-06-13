package com.globalreachtech.tinyradius.grt;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.netty.ServerPacketManager;
import com.globalreachtech.tinyradius.netty.RadiusProxy;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;

public class GrtRadiusServer extends RadiusProxy<NioDatagramChannel> {

    public GrtRadiusServer(Dictionary dictionary, EventLoopGroup eventLoopGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory factory, ServerPacketManager deduplicator, int authPort, int acctPort, int proxyPort) {
        super(dictionary, eventLoopGroup, eventExecutorGroup, factory, deduplicator, authPort, acctPort, proxyPort);
    }

    @Override
    public String getSharedSecret(InetSocketAddress client) {
        return null;
    }

    @Override
    public String getUserPassword(String userName) {
        return null;
    }

    @Override
    public RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
        return null;
    }
}

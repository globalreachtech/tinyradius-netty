package com.globalreachtech.pas;

import com.globalreachtech.tinyradius.client.ClientPacketManager;
import com.globalreachtech.tinyradius.proxy.ProxyPacketManager;
import com.globalreachtech.tinyradius.proxy.RadiusProxy;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class GrtRadiusProxy extends RadiusProxy<NioDatagramChannel> {

    public GrtRadiusProxy(Dictionary dictionary,
                          EventLoopGroup eventLoopGroup,
                          ChannelFactory<NioDatagramChannel> factory,
                          ProxyPacketManager proxyPacketManager,
                          ClientPacketManager clientPacketManager,
                          InetAddress listenAddress,
                          int authPort, int acctPort, int proxyPort) {
        super(eventLoopGroup, factory, proxyPacketManager, clientPacketManager, listenAddress, authPort, acctPort, proxyPort);
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

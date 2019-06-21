package com.globalreachtech.pas;

import com.globalreachtech.tinyradius.client.RadiusClient;
import com.globalreachtech.tinyradius.proxy.RadiusProxy;
import com.globalreachtech.tinyradius.server.AcctHandler;
import com.globalreachtech.tinyradius.server.AuthHandler;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetAddress;

public class GrtRadiusProxy extends RadiusProxy<NioDatagramChannel> {

    /**
     * @param eventLoopGroup
     * @param factory
     * @param packetManager
     * @param listenAddress       null address to assign wildcard address
     * @param authHandler
     * @param acctHandler
     * @param authPort
     * @param acctPort
     * @param proxyPort
     */
    public GrtRadiusProxy(EventLoopGroup eventLoopGroup, ChannelFactory<NioDatagramChannel> factory, RadiusClient.PacketManager packetManager, InetAddress listenAddress, AuthHandler authHandler, AcctHandler acctHandler, int authPort, int acctPort, int proxyPort) {
        super(eventLoopGroup, factory, listenAddress, authHandler, acctHandler, authPort, acctPort);
    }
}

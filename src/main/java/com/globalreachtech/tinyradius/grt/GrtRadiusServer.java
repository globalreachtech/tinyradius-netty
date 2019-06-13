package com.globalreachtech.tinyradius.grt;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.netty.DefaultDeduplicator;
import com.globalreachtech.tinyradius.netty.RadiusServer;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;

public class GrtRadiusServer extends RadiusServer {

    public GrtRadiusServer(Dictionary dictionary, EventLoopGroup eventLoopGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory factory, DefaultDeduplicator packetDeduplicator, int authPort, int acctPort) {
        super(dictionary, eventLoopGroup, eventExecutorGroup, factory, packetDeduplicator, authPort, acctPort);
    }

    @Override
    public String getSharedSecret(InetSocketAddress client) {
        return null;
    }

    @Override
    public String getUserPassword(String userName) {
        return null;
    }
}

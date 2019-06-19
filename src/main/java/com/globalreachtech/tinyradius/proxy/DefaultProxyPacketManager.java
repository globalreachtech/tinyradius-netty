package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.server.DefaultServerPacketManager;
import com.globalreachtech.tinyradius.util.RadiusProxyConnection;
import io.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultProxyPacketManager extends DefaultServerPacketManager implements ProxyPacketManager {

    /**
     * Cache for Radius proxy connections belonging to sent packets
     * without a received clientResponse.
     * Key: Proxy Index (String), Value: RadiusProxyConnection
     */
    private Map<String, RadiusProxyConnection> proxyConnections = new ConcurrentHashMap<>();

    public DefaultProxyPacketManager(Timer timer, long ttlMs) {
        super(timer, ttlMs);
    }

    @Override
    public boolean isClientPacketDuplicate(RadiusPacket packet, InetSocketAddress address) {
        return super.isClientPacketDuplicate(packet, address);
    }


    @Override
    public void putProxyConnection(String proxyIndex, RadiusProxyConnection proxyConnection) {
        proxyConnections.put(proxyIndex, proxyConnection);
    }

    @Override
    public RadiusProxyConnection removeProxyConnection(String proxyIndex) {
        return proxyConnections.remove(proxyIndex);
    }
}

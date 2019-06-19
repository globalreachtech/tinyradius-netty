package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.RadiusProxy;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusProxyConnection;
import io.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyPacketManager extends ServerPacketManager implements RadiusProxy.IProxyPacketManager {

    /**
     * Cache for Radius proxy connections belonging to sent packets
     * without a received clientResponse.
     * Key: Proxy Index (String), Value: RadiusProxyConnection
     */
    private Map<String, RadiusProxyConnection> proxyConnections = new ConcurrentHashMap<>();

    public ProxyPacketManager(Timer timer, long ttlMs) {
        super(timer, ttlMs);
    }

    @Override
    public boolean isPacketDuplicate(RadiusPacket packet, InetSocketAddress address) {
        return false;
    }


    @Override
    public RadiusProxyConnection put(String proxyIndex, RadiusProxyConnection proxyConnection) {
        return proxyConnections.put(proxyIndex, proxyConnection);
    }

    @Override
    public RadiusProxyConnection remove(String proxyIndex) {
        return proxyConnections.remove(proxyIndex);
    }
}

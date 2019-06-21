package com.globalreachtech.tinyradius.proxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultConnectionManager implements RadiusProxy.ConnectionManager {

    /**
     * Index for Proxy-State attribute.
     */
    private AtomicInteger proxyIndex = new AtomicInteger(1);

    /**
     * Cache for Radius proxy connections belonging to sent packets
     * without a received clientResponse.
     * Key: Proxy Index (String), Value: RadiusProxyConnection
     */
    private Map<String, RadiusProxyConnection> proxyConnections = new ConcurrentHashMap<>();

    @Override
    public String nextProxyIndex() {
        return Integer.toString(proxyIndex.getAndIncrement());
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

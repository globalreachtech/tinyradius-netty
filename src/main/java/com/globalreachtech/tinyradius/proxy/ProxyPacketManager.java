package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.server.ServerPacketManager;
import com.globalreachtech.tinyradius.util.RadiusProxyConnection;

public interface ProxyPacketManager extends ServerPacketManager {
    void putProxyConnection(String proxyIndex, RadiusProxyConnection proxyConnection);

    RadiusProxyConnection removeProxyConnection(String proxyIndex);
}

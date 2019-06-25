package com.tinyradius.util;

import java.net.InetSocketAddress;

public interface SecretProvider {

    /**
     * Returns the shared secret used to communicate with the client/host with the
     * passed IP address or null if the client is not allowed at this server.
     *
     * @param address IP address and port number of remote host/client
     * @return shared secret or null
     */
    String getSharedSecret(InetSocketAddress address);

}

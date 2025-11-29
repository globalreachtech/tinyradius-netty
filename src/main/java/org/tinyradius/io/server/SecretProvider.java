package org.tinyradius.io.server;

import org.tinyradius.core.packet.request.RadiusRequest;

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

    /**
     * An alternative method of returning shared secret but with the
     * current radius request so that alternative secrets can be returned based on
     * some sort of context.
     * <p>
     * By default, this method calls the original getSharedSecret.
     *
     * @param address IP address and port number of remote host/client
     * @param request the RadiusRequest relating to this request
     * @return shared secret or null
     */
    @SuppressWarnings("unused")
    default String getSharedSecret(InetSocketAddress address, RadiusRequest request) {
        return getSharedSecret(address);
    }

}

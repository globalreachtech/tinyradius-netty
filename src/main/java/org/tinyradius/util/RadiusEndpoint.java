package org.tinyradius.util;

import java.net.InetSocketAddress;

/**
 * Wrapper class for a remote endpoint address and the shared secret
 * used for securing the communication.
 */
public class RadiusEndpoint {

    private final InetSocketAddress endpointAddress;
    private final String sharedSecret;

    public RadiusEndpoint(InetSocketAddress remoteAddress, String sharedSecret) {
        this.endpointAddress = remoteAddress;
        this.sharedSecret = sharedSecret;
    }

    public InetSocketAddress getEndpointAddress() {
        return endpointAddress;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }
}

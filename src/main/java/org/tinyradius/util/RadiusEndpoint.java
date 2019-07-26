package org.tinyradius.util;

import java.net.InetSocketAddress;

/**
 * Wrapper class for a remote endpoint address and the shared secret
 * used for securing the communication.
 */
public class RadiusEndpoint {

    private final InetSocketAddress address;
    private final String sharedSecret;

    public RadiusEndpoint(InetSocketAddress remoteAddress, String sharedSecret) {
        this.address = remoteAddress;
        this.sharedSecret = sharedSecret;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }
}

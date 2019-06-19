package com.globalreachtech.tinyradius.util;

import java.net.InetSocketAddress;

/**
 * This class stores information about a Radius endpoint.
 * This includes the address of the remote endpoint and the shared secret
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadiusEndpoint that = (RadiusEndpoint) o;
        return endpointAddress.equals(that.endpointAddress) &&
                sharedSecret.equals(that.sharedSecret);
    }

}

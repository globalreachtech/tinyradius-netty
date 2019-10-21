package org.tinyradius.util;

import java.net.InetSocketAddress;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Wrapper class for a remote endpoint address and the shared secret
 * used for securing the communication.
 */
public class RadiusEndpoint {

    private final InetSocketAddress address;
    private final String sharedSecret;

    public RadiusEndpoint(InetSocketAddress remoteAddress, String sharedSecret) {
        this.address = requireNonNull(remoteAddress);
        this.sharedSecret = requireNonNull(sharedSecret);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadiusEndpoint endpoint = (RadiusEndpoint) o;
        return address.equals(endpoint.address) &&
                sharedSecret.equals(endpoint.sharedSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, sharedSecret);
    }
}

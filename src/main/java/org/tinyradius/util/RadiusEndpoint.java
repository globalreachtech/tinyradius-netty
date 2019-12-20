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
    private final String secret;

    public RadiusEndpoint(InetSocketAddress remoteAddress, String secret) {
        this.address = requireNonNull(remoteAddress);
        this.secret = requireNonNull(secret);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadiusEndpoint endpoint = (RadiusEndpoint) o;
        return address.equals(endpoint.address) &&
                secret.equals(endpoint.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, secret);
    }
}

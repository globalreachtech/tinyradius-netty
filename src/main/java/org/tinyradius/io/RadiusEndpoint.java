package org.tinyradius.io;


import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;

/**
 * Wrapper class for a remote endpoint address and the shared secret
 * used for securing the communication.
 */
public record RadiusEndpoint(InetSocketAddress address, String secret) {

    @NonNull
    @Override
    public String toString() {
        return "RadiusEndpoint{" +
                "address=" + address +
                '}';
    }
}

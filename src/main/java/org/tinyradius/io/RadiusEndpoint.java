package org.tinyradius.io;

import lombok.Data;
import lombok.Setter;
import lombok.ToString;

import java.net.InetSocketAddress;

import static lombok.AccessLevel.NONE;

/**
 * Wrapper class for a remote endpoint address and the shared secret
 * used for securing the communication.
 */
@Setter(NONE)
@Data
public class RadiusEndpoint {

    private final InetSocketAddress address;

    @ToString.Exclude
    private final String secret;
}

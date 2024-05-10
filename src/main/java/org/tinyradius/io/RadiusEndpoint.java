package org.tinyradius.io;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetSocketAddress;

/**
 * Wrapper class for a remote endpoint address and the shared secret
 * used for securing the communication.
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class RadiusEndpoint {

    private final InetSocketAddress address;
    private final String secret;
}

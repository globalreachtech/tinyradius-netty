package org.tinyradius.io;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RadiusEndpointTest {

    @Test
    void testRadiusEndpointToString() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1812);
        RadiusEndpoint endpoint = new RadiusEndpoint(address, "secret");
        assertEquals("RadiusEndpoint{address=" + address + "}", endpoint.toString());
    }
}

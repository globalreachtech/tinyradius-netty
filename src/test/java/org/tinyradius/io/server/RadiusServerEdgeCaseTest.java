package org.tinyradius.io.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RadiusServerEdgeCaseTest {

    @Test
    void testConstructorMismatchedSizes() {
        Bootstrap bootstrap = mock(Bootstrap.class);
        List<ChannelHandler> handlers = List.of(mock(ChannelHandler.class));
        List<InetSocketAddress> addresses = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () ->
                new RadiusServer(bootstrap, handlers, addresses)
        );
    }
}

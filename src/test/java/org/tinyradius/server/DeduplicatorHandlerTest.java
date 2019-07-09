package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicatorHandlerTest {

    @Test
    void handlePacket() {

        final DeduplicatorHandler<RadiusPacket> deduplicatorHandler = new DeduplicatorHandler<>(
                (channel, packet, remoteAddress, sharedSecret) -> null, new HashedWheelTimer(), 10000);
        // todo
    }
}
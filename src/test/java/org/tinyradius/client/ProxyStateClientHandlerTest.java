package org.tinyradius.client;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProxyStateClientHandlerTest {

    private static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

    @Test()
    void processRequestCreateProxyStateAttribute() {
        int id = new SecureRandom().nextInt(256);
        HashedWheelTimer timer = new HashedWheelTimer();
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, timer, address -> "secret", 3, 1000);

        final RadiusPacket originalPacket = new AccessRequest(dictionary, id, null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusPacket packet = handler.prepareRequest(originalPacket, endpoint, eventLoopGroup.next().newPromise());

        List<RadiusAttribute> attributes = packet.getAttributes();
        assertEquals(1, attributes.size());
        assertNotNull(packet.getAttribute("Proxy-State"));
    }
}

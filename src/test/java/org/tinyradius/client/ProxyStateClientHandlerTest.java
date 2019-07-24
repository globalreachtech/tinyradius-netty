package org.tinyradius.client;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.attribute.Attributes.createAttribute;

class ProxyStateClientHandlerTest {

    private final DefaultDictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @Test
    void processRequestCreateProxyStateAttribute() {
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> "secret");
        int id = random.nextInt(256);

        final RadiusPacket originalPacket = new AccessRequest(dictionary, id, null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusPacket processedPacket1 = handler.prepareRequest(originalPacket, endpoint, eventLoopGroup.next().newPromise());

        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();
        assertEquals(1, attributes1.size());
        final byte[] proxyState1 = processedPacket1.getAttribute("Proxy-State").getValue();

        assertEquals("1", new String(proxyState1, UTF_8));

        final RadiusPacket processedPacket2 = handler.prepareRequest(processedPacket1, endpoint, eventLoopGroup.next().newPromise());

        final List<RadiusAttribute> attributes2 = processedPacket2.getAttributes();
        assertEquals(1, attributes1.size());
        assertEquals(2, attributes2.size());

        final List<RadiusAttribute> attributes = processedPacket2.getAttributes(33);
        assertEquals("1", new String(attributes.get(0).getValue(), UTF_8));
        assertEquals("2", new String(attributes.get(1).getValue(), UTF_8));
    }

    @Test
    void channelReadIsStateful() throws RadiusException {
        final String secret = "mySecret";
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> secret);

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, 33, "")));

        // sanity check, should log ignore and throw no error
        handler.channelRead0(null, RadiusPacketEncoder.toDatagram(response.encodeRequest(secret), new InetSocketAddress(0)));

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        // process packet out
        final RadiusPacket request = new RadiusPacket(dictionary, 1, 1);
        final RadiusPacket preparedRequest = handler.prepareRequest(request, new RadiusEndpoint(new InetSocketAddress(0), secret), promise);

        // capture proxyState of outbound packet
        final String proxyState = new String(preparedRequest.getAttribute("Proxy-State").getValue(), UTF_8);

        System.out.println(proxyState);

        // channel read different proxyState returns nothing
//        handler.channelRead0(null, responseDatagram);

        // channel read correct proxyState returns packet
        // channel read correct proxyState again returns nothing

        // check proxyState is removed after reading
    }

}

package org.tinyradius.client.handler;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

class SimpleClientHandlerTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @AfterAll
    static void afterAll() {
        eventLoopGroup.shutdownGracefully().syncUninterruptibly();
    }

    @Test
    void noContextFound() throws RadiusException {
        final SimpleClientHandler handler = new SimpleClientHandler(dictionary);
        final RadiusPacket radiusPacket = new RadiusPacket(dictionary, 2, 1).encodeResponse("foo", new byte[16]);
        final DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(
                radiusPacket, new InetSocketAddress(0), new InetSocketAddress(1));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(datagramPacket));

        assertTrue(exception.getMessage().toLowerCase().contains("context not found"));
    }

    @Test
    void handleResponse() throws RadiusException {
        final String secret = "mySecret";
        final String attributeString = "myResponseAttribute";
        final int id = new SecureRandom().nextInt(256);
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        final SimpleClientHandler handler = new SimpleClientHandler(dictionary);
        final AccessRequest request = new AccessRequest(dictionary, id, null, "myUser", "myPassword");

        final RadiusPacket encodedRequest = handler.prepareRequest(request, new RadiusEndpoint(new InetSocketAddress(12345), secret), promise);
        assertFalse(promise.isDone());

        final RadiusPacket response = new RadiusPacket(dictionary, 2, id, Collections.singletonList(
                createAttribute(dictionary, -1, 1, attributeString)))
                .encodeResponse(secret, encodedRequest.getAuthenticator());

        // ignore packet from wrong port
        final DatagramPacket badDatagram = RadiusPacketEncoder.toDatagram(response, new InetSocketAddress(0), new InetSocketAddress(9999));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(badDatagram));

        assertTrue(exception.getMessage().toLowerCase().contains("context not found"));
        assertFalse(promise.isDone());

        // correct port
        final DatagramPacket goodDatagram = RadiusPacketEncoder.toDatagram(response, new InetSocketAddress(0), new InetSocketAddress(12345));
        handler.handleResponse(goodDatagram);

        assertTrue(promise.isDone());
        assertEquals(attributeString, promise.getNow().getAttribute("User-Name").getValueString());
    }

}
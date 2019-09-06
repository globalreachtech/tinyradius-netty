package org.tinyradius.client.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

class ProxyStateClientHandlerTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);
    private final SecureRandom random = new SecureRandom();
    private static final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private final int PROXY_STATE = 33;

    @AfterAll
    static void afterAll() {
        eventLoopGroup.shutdownGracefully().syncUninterruptibly();
    }

    @Test
    void outboundAppendNewProxyState() throws RadiusException {
        final String secret = "test";
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(packetEncoder);
        int id = random.nextInt(256);

        final RadiusPacket originalRequest = new AccessRequest(dictionary, id, null).encodeRequest(secret);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);

        final DatagramPacket datagram1 = handler.prepareDatagram(originalRequest, endpoint, null, eventLoopGroup.next().newPromise());

        final RadiusPacket processedPacket1 = packetEncoder.fromDatagram(datagram1);
        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();
        assertEquals(1, attributes1.size());
        final byte[] proxyState1 = processedPacket1.getAttribute("Proxy-State").getValue();

        assertEquals("1", new String(proxyState1, UTF_8));

        final DatagramPacket datagram2 = handler.prepareDatagram(processedPacket1, endpoint, null, eventLoopGroup.next().newPromise());

        final RadiusPacket processedPacket2 = packetEncoder.fromDatagram(datagram2);
        final List<RadiusAttribute> attributes2 = processedPacket2.getAttributes();
        assertEquals(1, attributes1.size());
        assertEquals(2, attributes2.size());

        final List<RadiusAttribute> attributes = processedPacket2.getAttributes(PROXY_STATE);
        assertEquals("1", new String(attributes.get(0).getValue(), UTF_8));
        assertEquals("2", new String(attributes.get(1).getValue(), UTF_8));
    }

    @Test
    void responseNoProxyState() {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(packetEncoder);
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket noAttributeResponse = new RadiusPacket(dictionary, 2, 1);
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        noAttributeResponse.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("no proxy-state attribute"));
    }

    @Test
    void responseProxyStateNotFound() {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(packetEncoder);
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket invalidProxyStateResponse = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, "123abc")));
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        invalidProxyStateResponse.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("request context not found"));
    }

    @Test
    void responseIdentifierMismatch() throws RadiusException {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(packetEncoder);
        final byte[] requestAuth = random.generateSeed(16);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), endpoint, null, promise);
        final RadiusPacket preparedRequest = packetEncoder.fromDatagram(datagram);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket badId = new RadiusPacket(dictionary, 2, 99,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        badId.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("identifier mismatch"));
    }

    @Test
    void responseAuthVerifyFail() throws RadiusException {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(packetEncoder);
        final byte[] requestAuth = random.generateSeed(16);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), endpoint, null, promise);
        final RadiusPacket preparedRequest = packetEncoder.fromDatagram(datagram);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket badId = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        badId.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void responseUnknownSender() {
        final ProxyStateClientHandler proxyStateClientHandler = new ProxyStateClientHandler(packetEncoder);

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> proxyStateClientHandler.handleResponse(
                        new DatagramPacket(Unpooled.buffer(), new InetSocketAddress(0))));
        assertTrue(exception.getMessage().toLowerCase().contains("unknown sender"));
    }

    @Test
    void channelReadIsStateful() throws RadiusException {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(packetEncoder);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        // process packet out
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), endpoint, null, promise);
        final RadiusPacket preparedRequest = packetEncoder.fromDatagram(datagram);

        // capture request details
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();
        final byte[] requestAuthenticator = preparedRequest.getAuthenticator();

        assertFalse(promise.isDone());

        // channel read correct proxyState returns packet
        final RadiusPacket goodResponse = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)))
                .encodeResponse(secret, requestAuthenticator);

        handler.handleResponse(packetEncoder.toDatagram(
                goodResponse, endpoint.getAddress()));

        final RadiusPacket decodedResponse = promise.getNow();
        assertTrue(promise.isDone());
        assertEquals(goodResponse.getIdentifier(), decodedResponse.getIdentifier());
        assertEquals(goodResponse.getType(), decodedResponse.getType());
        assertArrayEquals(goodResponse.getAuthenticator(), decodedResponse.getAuthenticator());

        // check proxyState is removed after reading
        assertEquals(0, decodedResponse.getAttributes().size());
        assertEquals(1, goodResponse.getAttributes().size());
        assertArrayEquals(requestProxyState, goodResponse.getAttribute(PROXY_STATE).getValue());

        // channel read again lookup fails
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        goodResponse, endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("request context not found"));

        // check promise hasn't changed
        assertTrue(promise.isDone());
        assertSame(decodedResponse, promise.getNow());
    }
}

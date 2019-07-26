package org.tinyradius.client.handler;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

class ProxyStateClientHandlerTest {

    private final DefaultDictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();
    private static final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private final int PROXY_STATE = 33;

    @AfterAll
    static void afterAll() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    void processRequestCreateProxyStateAttribute() {
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> "secret");
        int id = random.nextInt(256);

        final RadiusPacket originalRequest = new AccessRequest(dictionary, id, null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusPacket processedPacket1 = handler.prepareRequest(originalRequest, endpoint, eventLoopGroup.next().newPromise());

        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();
        assertEquals(1, attributes1.size());
        final byte[] proxyState1 = processedPacket1.getAttribute("Proxy-State").getValue();

        assertEquals("1", new String(proxyState1, UTF_8));

        final RadiusPacket processedPacket2 = handler.prepareRequest(processedPacket1, endpoint, eventLoopGroup.next().newPromise());

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
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> secret);
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket noAttributeResponse = new RadiusPacket(dictionary, 2, 1);
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(RadiusPacketEncoder.toDatagram(
                        noAttributeResponse.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("no proxy-state attribute"));
    }

    @Test
    void responseProxyStateNotFound() {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> secret);
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket invalidProxyStateResponse = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, "unknownProxyState")));
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(RadiusPacketEncoder.toDatagram(
                        invalidProxyStateResponse.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("request context not found"));
    }

    @Test
    void responseIdentifierMismatch() {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> secret);
        final byte[] requestAuth = random.generateSeed(16);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        final RadiusPacket preparedRequest = handler.prepareRequest(
                new RadiusPacket(dictionary, 1, 1), endpoint, promise);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket badId = new RadiusPacket(dictionary, 2, 99,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(RadiusPacketEncoder.toDatagram(
                        badId.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("identifier mismatch"));
    }

    @Test
    void responseAuthVerifyFail() {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> secret);
        final byte[] requestAuth = random.generateSeed(16);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        final RadiusPacket preparedRequest = handler.prepareRequest(
                new RadiusPacket(dictionary, 1, 1), endpoint, promise);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket badId = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(RadiusPacketEncoder.toDatagram(
                        badId.encodeResponse(secret, requestAuth), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void channelReadIsStateful() throws RadiusException {
        final String secret = "mySecret";
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);
        final ProxyStateClientHandler handler = new ProxyStateClientHandler(dictionary, address -> secret);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        // process packet out
        final RadiusPacket preparedRequest = handler.prepareRequest(
                new RadiusPacket(dictionary, 1, 1), endpoint, promise);

        // capture request details
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();
        final byte[] requestAuthenticator = preparedRequest.getAuthenticator();

        assertFalse(promise.isDone());

        // channel read correct proxyState returns packet
        final RadiusPacket goodResponse = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)))
                .encodeResponse(secret, requestAuthenticator);

        handler.handleResponse(RadiusPacketEncoder.toDatagram(
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
                () -> handler.handleResponse(RadiusPacketEncoder.toDatagram(
                        goodResponse.encodeResponse(secret, requestAuthenticator), endpoint.getAddress())));

        assertTrue(exception.getMessage().toLowerCase().contains("request context not found"));

        // check promise hasn't changed
        assertTrue(promise.isDone());
        assertSame(decodedResponse, promise.getNow());
    }
}

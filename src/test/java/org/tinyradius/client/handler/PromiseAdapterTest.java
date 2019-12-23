package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.client.ClientResponseCtx;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

@ExtendWith(MockitoExtension.class)
class PromiseAdapterTest {

    private static final int PROXY_STATE = 33;

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);
    private final SecureRandom random = new SecureRandom();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void outboundAppendNewProxyState() throws RadiusException {
        final String secret = "test";
        final PromiseAdapter handler = new PromiseAdapter();
        int id = random.nextInt(256);

        final RadiusPacket originalRequest = new AccessRequest(dictionary, id, null).encodeRequest(secret);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);

        // process once
        final List<Object> out1 = new ArrayList<>();
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        handler.encode(ctx, new ClientResponseCtx(originalRequest, endpoint, promise), out1);

        final RadiusPacket processedPacket1 = packetEncoder.fromDatagram(out1.get(0));
        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();

        // check proxy-state added
        assertEquals(1, attributes1.size());
        final byte[] proxyState1 = processedPacket1.getAttribute("Proxy-State").getValue();
        assertEquals("1", new String(proxyState1, UTF_8));

        // process again
        final Promise<Object> objectPromise = eventLoopGroup.next().newPromise();
        final DatagramPacket datagram2 = handler.encode(ctx, processedPacket1, endpoint, );
        final RadiusPacket processedPacket2 = packetEncoder.fromDatagram(datagram2);

        // check another proxy-state added
        final List<RadiusAttribute> attributes2 = processedPacket2.getAttributes();
        assertEquals(1, attributes1.size());
        assertEquals(2, attributes2.size());

        final List<RadiusAttribute> attributes = processedPacket2.getAttributes(PROXY_STATE);
        assertEquals("1", new String(attributes.get(0).getValue(), UTF_8));
        assertEquals("2", new String(attributes.get(1).getValue(), UTF_8));
    }

    @Test
    void prepareDatagramEncodesAccessRequest() throws RadiusException {
        final String secret1 = UUID.randomUUID().toString();
        final String secret2 = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        final PromiseAdapter handler = new PromiseAdapter();
        int id = random.nextInt(256);

        final RadiusPacket accessRequest = new AccessRequest(dictionary, id, null, username, password)
                .encodeRequest(secret1);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret2);

        // process
        final DatagramPacket accessPacketDatagram = handler.prepareDatagram(accessRequest, endpoint, eventLoopGroup.next().newPromise());

        final RadiusPacket sentAccessPacket = packetEncoder.fromDatagram(accessPacketDatagram, secret2);
        assertTrue(sentAccessPacket instanceof AccessRequest); // sanity check - we are not testing decoder here

        // check user details correctly encoded
        assertEquals(username, ((AccessRequest) sentAccessPacket).getUserName());
        assertEquals(password, ((AccessRequest) sentAccessPacket).getUserPassword());
    }

    @Test
    void responseSenderNull() {
        final PromiseAdapter handler = new PromiseAdapter(packetEncoder);
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1);
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        response.encodeResponse("mySecret", requestAuth), new InetSocketAddress(0))));

        assertTrue(exception.getMessage().toLowerCase().contains("sender is null"));
    }

    @Test
    void responseNoProxyState() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final PromiseAdapter handler = new PromiseAdapter();
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1);
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        response.encodeResponse(secret, requestAuth), new InetSocketAddress(0), remoteAddress)));

        assertTrue(exception.getMessage().toLowerCase().contains("no proxy-state attribute"));
    }

    @Test
    void responseProxyStateNotFound() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final PromiseAdapter handler = new PromiseAdapter();
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, "123abc")));
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        response.encodeResponse(secret, requestAuth), new InetSocketAddress(0), remoteAddress)));

        assertTrue(exception.getMessage().toLowerCase().contains("request context not found"));
    }

    @Test
    void responseIdentifierMismatch() throws RadiusException {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final PromiseAdapter handler = new PromiseAdapter();
        final byte[] requestAuth = random.generateSeed(16);

        // add remoteAddress-secret and identifier mapping to handler
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), new RadiusEndpoint(remoteAddress, secret), eventLoopGroup.next().newPromise());
        final RadiusPacket preparedRequest = packetEncoder.fromDatagram(datagram);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 99,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        response.encodeResponse(secret, requestAuth), new InetSocketAddress(0), remoteAddress)));

        assertTrue(exception.getMessage().toLowerCase().contains("identifier mismatch"));
    }

    @Test
    void responseSenderAddressMismatch() throws RadiusException {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final PromiseAdapter handler = new PromiseAdapter();
        final byte[] requestAuth = random.generateSeed(16);

        // add remoteAddress-secret and identifier mapping to handler
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), new RadiusEndpoint(remoteAddress, secret), eventLoopGroup.next().newPromise());
        final RadiusPacket preparedRequest = packetEncoder.fromDatagram(datagram);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        response.encodeResponse(secret, requestAuth), new InetSocketAddress(0), new InetSocketAddress(456))));

        assertTrue(exception.getMessage().toLowerCase().contains("not match recipient address"));
    }

    @Test
    void responseAuthVerifyFail() throws RadiusException {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final PromiseAdapter handler = new PromiseAdapter();
        final byte[] requestAuth = random.generateSeed(16);

        // add remoteAddress-secret and identifier mapping to handler
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), new RadiusEndpoint(remoteAddress, secret), eventLoopGroup.next().newPromise());
        final RadiusPacket preparedRequest = packetEncoder.fromDatagram(datagram);
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        response.encodeResponse(secret, requestAuth), new InetSocketAddress(0), remoteAddress)));

        assertTrue(exception.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void channelReadIsStateful() throws RadiusException, InterruptedException {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final PromiseAdapter handler = new PromiseAdapter();

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        // process packet out
        final DatagramPacket datagram = handler.prepareDatagram(
                new RadiusPacket(dictionary, 1, 1), new RadiusEndpoint(remoteAddress, secret), promise);
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
                goodResponse, new InetSocketAddress(1), remoteAddress));

        final RadiusPacket decodedResponse = promise.getNow();
        assertTrue(promise.isDone());
        assertEquals(goodResponse.getIdentifier(), decodedResponse.getIdentifier());
        assertEquals(goodResponse.getType(), decodedResponse.getType());
        assertArrayEquals(goodResponse.getAuthenticator(), decodedResponse.getAuthenticator());

        // check proxyState is removed after reading
        assertEquals(0, decodedResponse.getAttributes().size());
        assertEquals(1, goodResponse.getAttributes().size());
        assertArrayEquals(requestProxyState, goodResponse.getAttribute(PROXY_STATE).getValue());

        // pause to avoid race condition
        Thread.sleep(100);

        // channel read again lookup fails
        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handler.handleResponse(packetEncoder.toDatagram(
                        goodResponse, new InetSocketAddress(0), remoteAddress)));

        assertTrue(exception.getMessage().toLowerCase().contains("request context not found"));

        // check promise hasn't changed
        assertTrue(promise.isDone());
        assertSame(decodedResponse, promise.getNow());
    }
}

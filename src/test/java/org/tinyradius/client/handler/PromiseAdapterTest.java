package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

@ExtendWith(MockitoExtension.class)
class PromiseAdapterTest {

    private static final int PROXY_STATE = 33;

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Promise<RadiusPacket> promise;

    private final PromiseAdapter handler = new PromiseAdapter();

    @Test
    void encodeAppendProxyState() {
        final String secret = "test";
        int id = random.nextInt(256);

        final RadiusPacket originalRequest = new AccessRequest(dictionary, id, null).encodeRequest(secret);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);

        // process once
        final List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(originalRequest, endpoint, promise), out1);

        assertEquals(1, out1.size());
        final RadiusPacket processedPacket1 = ((PendingRequestCtx) out1.get(0)).getRequest();
        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();

        // check proxy-state added
        assertEquals(1, attributes1.size());
        final byte[] proxyState1 = processedPacket1.getAttribute("Proxy-State").getValue();
        assertEquals("1", new String(proxyState1, UTF_8));

        // process again
        final List<Object> out2 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(processedPacket1, endpoint, promise), out2);

        assertEquals(1, out1.size());
        final RadiusPacket processedPacket2 = ((PendingRequestCtx) out2.get(0)).getRequest();

        // check another proxy-state added
        final List<RadiusAttribute> attributes2 = processedPacket2.getAttributes();
        assertEquals(1, attributes1.size());
        assertEquals(2, attributes2.size());

        final List<RadiusAttribute> attributes = processedPacket2.getAttributes(PROXY_STATE);
        assertEquals("1", new String(attributes.get(0).getValue(), UTF_8));
        assertEquals("2", new String(attributes.get(1).getValue(), UTF_8));
    }


    @Test
    void decodeNoProxyState() {
        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1);

        final List<Object> out1 = new ArrayList<>();
        handler.decode(ctx, response, out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void decodeProxyStateNotFound() {
        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, "123abc")));

        final List<Object> out1 = new ArrayList<>();
        handler.decode(ctx, response, out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void encodeDecodeIdMismatch() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket request = new RadiusPacket(dictionary, 1, 1);
        final RadiusEndpoint requestEndpoint = new RadiusEndpoint(remoteAddress, secret);

        // add remoteAddress-secret and identifier mapping to handler
        final List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, eventLoopGroup.next().newPromise()), out1);

        assertEquals(1, out1.size());

        final RadiusPacket preparedRequest = ((PendingRequestCtx) out1.get(0)).getRequest();
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 99,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final List<Object> out2 = new ArrayList<>();
        handler.decode(ctx, response.encodeResponse(secret, requestAuth), out2);

        assertEquals(0, out2.size());
    }

    @Test
    void decodeAuthCheckFail() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final byte[] requestAuth = random.generateSeed(16);


        final RadiusPacket request = new RadiusPacket(dictionary, 1, 1);
        final RadiusEndpoint requestEndpoint = new RadiusEndpoint(remoteAddress, secret);

        // add remoteAddress-secret and identifier mapping to handler
        final List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out1);

        assertEquals(1, out1.size());
        final RadiusPacket preparedRequest = ((RequestCtx) out1.get(0)).getRequest();
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)));

        final List<Object> out2 = new ArrayList<>();
        handler.decode(ctx, response.encodeResponse(secret, requestAuth), out2);
    }

    @Test
    void encodeDecodeSuccess() throws InterruptedException {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);

        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        final RadiusPacket request = new RadiusPacket(dictionary, 1, 1);
        final RadiusEndpoint requestEndpoint = new RadiusEndpoint(remoteAddress, secret);

        // process packet out
        final List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out1);

        assertEquals(1, out1.size());

        final RadiusPacket preparedRequest = ((RequestCtx) out1.get(0)).getRequest();

        // capture request details
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).getValue();
        final byte[] requestAuthenticator = preparedRequest.getAuthenticator();

        assertFalse(promise.isDone());

        // channel read correct proxyState returns packet
        final RadiusPacket goodResponse = new RadiusPacket(dictionary, 2, 1,
                Collections.singletonList(createAttribute(dictionary, -1, PROXY_STATE, requestProxyState)))
                .encodeResponse(secret, requestAuthenticator);

        final List<Object> out2 = new ArrayList<>();
        handler.decode(ctx, goodResponse, out2);

        assertTrue(out2.isEmpty());

        final RadiusPacket decodedResponse = promise.getNow();
        assertTrue(promise.isDone());
        assertEquals(goodResponse.getIdentifier(), decodedResponse.getIdentifier());
        assertEquals(goodResponse.getType(), decodedResponse.getType());
        assertArrayEquals(goodResponse.getAuthenticator(), decodedResponse.getAuthenticator());

        // check proxyState is removed after reading
        assertEquals(0, decodedResponse.getAttributes().size());

        // pause to avoid race condition
        Thread.sleep(100);

        // channel read again lookup fails
        final List<Object> out3 = new ArrayList<>();
        handler.decode(ctx, goodResponse, out3);

        assertTrue(out3.isEmpty());

        // check promise hasn't changed
        assertTrue(promise.isDone());
        assertSame(decodedResponse, promise.getNow());
    }
}

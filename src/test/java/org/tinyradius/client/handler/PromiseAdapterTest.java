package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccessRequestPap;
import org.tinyradius.packet.request.AccountingRequest;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.AccessResponse;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.util.Attributes.create;
import static org.tinyradius.packet.util.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.packet.util.PacketType.ACCOUNTING_RESPONSE;

@ExtendWith(MockitoExtension.class)
class PromiseAdapterTest {

    private static final byte PROXY_STATE = 33;

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Promise<RadiusResponse> mockPromise;

    private final PromiseAdapter handler = new PromiseAdapter();

    @Test
    void encodeAppendProxyState() {
        final String secret = "test";
        byte id = (byte) random.nextInt(256);

        final RadiusRequest originalRequest = new AccountingRequest(dictionary, id, null, Collections.emptyList()).encodeRequest(secret);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);

        // process once
        final List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(originalRequest, endpoint, mockPromise), out1);

        assertEquals(1, out1.size());
        final RadiusRequest processedPacket1 = ((PendingRequestCtx) out1.get(0)).getRequest();
        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();

        // check proxy-state added
        assertEquals(1, attributes1.size());
        final byte[] proxyState1 = processedPacket1.getAttribute("Proxy-State").get().getValue();
        assertEquals("1", new String(proxyState1, UTF_8));

        // process again
        final List<Object> out2 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(processedPacket1, endpoint, mockPromise), out2);

        assertEquals(1, out1.size());
        final RadiusRequest processedPacket2 = ((PendingRequestCtx) out2.get(0)).getRequest();

        // check another proxy-state added
        final List<RadiusAttribute> attributes2 = processedPacket2.getAttributes();
        assertEquals(1, attributes1.size());
        assertEquals(2, attributes2.size());

        final List<RadiusAttribute> attributes = processedPacket2.filterAttributes(PROXY_STATE);
        assertEquals("1", new String(attributes.get(0).getValue(), UTF_8));
        assertEquals("2", new String(attributes.get(1).getValue(), UTF_8));
    }


    @Test
    void decodeNoProxyState() {
        final RadiusResponse response = new AccessResponse(dictionary, ACCESS_ACCEPT, (byte) 1, null, Collections.emptyList());

        final List<Object> in = new ArrayList<>();
        handler.decode(ctx, response, in);

        assertTrue(in.isEmpty());
    }

    @Test
    void decodeProxyStateNotFound() {
        final RadiusResponse response = new AccessResponse(dictionary, ACCESS_ACCEPT, (byte) 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, PROXY_STATE, "123abc")));

        final List<Object> in = new ArrayList<>();
        handler.decode(ctx, response, in);

        assertTrue(in.isEmpty());
    }

    @Test
    void encodeDecodeIdMismatch() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final byte[] requestAuth = random.generateSeed(16);

        //using id 1
        final RadiusRequest request = new AccessRequestPap(dictionary, (byte) 1, requestAuth, Collections.emptyList(), "myPw");
        final RadiusEndpoint requestEndpoint = new RadiusEndpoint(remoteAddress, secret);

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();

        // add remoteAddress-secret and identifier mapping to handler
        final List<Object> out = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out);

        assertEquals(1, out.size());

        final RadiusRequest preparedRequest = ((PendingRequestCtx) out.get(0)).getRequest();
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).get().getValue();

        // using id 99
        final RadiusResponse response = RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 99, null,
                Collections.singletonList(create(dictionary, -1, PROXY_STATE, requestProxyState)));

        final List<Object> in = new ArrayList<>();
        handler.decode(ctx, response.encodeResponse(secret, requestAuth), in);

        assertEquals(0, in.size());
        assertFalse(promise.isDone());
    }

    @Test
    void decodeAuthCheckFail() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);

        final RadiusRequest request = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList(), "myPw");
        final RadiusEndpoint requestEndpoint = new RadiusEndpoint(remoteAddress, secret);

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();

        // add remoteAddress-secret and identifier mapping to handler
        final List<Object> out = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out);

        assertEquals(1, out.size());
        final RadiusRequest preparedRequest = ((RequestCtx) out.get(0)).getRequest();
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).get().getValue();

        final RadiusResponse response = RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 1, null,
                Collections.singletonList(create(dictionary, -1, PROXY_STATE, requestProxyState)));

        // response uses different auth
        final byte[] randomAuth = random.generateSeed(16);
        final List<Object> in = new ArrayList<>();
        handler.decode(ctx, response.encodeResponse(secret, randomAuth), in);

        assertEquals(0, in.size());
        assertFalse(promise.isDone());
    }

    @Test
    void encodeDecodeSuccess() {
        final String secret = "mySecret";
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();

        final RadiusRequest request = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        final RadiusEndpoint requestEndpoint = new RadiusEndpoint(remoteAddress, secret);

        // process packet out
        final List<Object> out = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out);

        assertEquals(1, out.size());

        final RadiusRequest preparedRequest = ((RequestCtx) out.get(0)).getRequest();

        // capture request details
        final byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).get().getValue();
        final byte[] requestAuthenticator = preparedRequest.getAuthenticator();

        assertFalse(promise.isDone());

        // channel read correct proxyState returns packet
        final RadiusResponse goodResponse = RadiusResponse.create(dictionary, ACCOUNTING_RESPONSE, (byte) 1, null,
                Collections.singletonList(create(dictionary, -1, PROXY_STATE, requestProxyState)))
                .encodeResponse(secret, requestAuthenticator);

        // decode response
        final List<Object> in1 = new ArrayList<>();
        handler.decode(ctx, goodResponse, in1);
        assertTrue(in1.isEmpty());

        // check promise is done
        final RadiusResponse decodedResponse = promise.getNow();
        assertTrue(promise.isDone());
        assertEquals(goodResponse.getId(), decodedResponse.getId());
        assertEquals(goodResponse.getType(), decodedResponse.getType());
        assertArrayEquals(goodResponse.getAuthenticator(), decodedResponse.getAuthenticator());

        // check proxyState is removed after reading
        assertEquals(0, decodedResponse.getAttributes().size());
    }
}

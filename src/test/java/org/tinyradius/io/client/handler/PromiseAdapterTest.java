package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.*;
import org.tinyradius.core.packet.response.AccessResponse;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.PendingRequestCtx;
import org.tinyradius.io.server.RequestCtx;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.PROXY_STATE;
import static org.tinyradius.core.packet.PacketType.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(MockitoExtension.class)
class PromiseAdapterTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();
    private final EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;
    private final Promise<RadiusResponse> promise = eventExecutor.newPromise();

    private final InetSocketAddress address = new InetSocketAddress(0);

    @Mock
    private ChannelHandlerContext ctx;

    private final PromiseAdapter handler = new PromiseAdapter();

    @Test
    void encodeAppendProxyState() throws RadiusPacketException {
        String secret = "test";
        byte id = (byte) random.nextInt(256);

        AccountingRequest originalRequest = (AccountingRequest)
                RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, id, null, Collections.emptyList()).encodeRequest(secret);
        RadiusEndpoint endpoint = new RadiusEndpoint(address, secret);

        // process once
        List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(originalRequest, endpoint, promise), out1);

        assertEquals(1, out1.size());
        RadiusRequest processedPacket1 = ((PendingRequestCtx) out1.get(0)).getRequest();
        List<RadiusAttribute> attributes1 = processedPacket1.getAttributes();

        // check proxy-state added
        assertEquals(1, attributes1.size());
        byte[] proxyState1 = processedPacket1.getAttribute(PROXY_STATE).get().getValue();
        UUID requestId1 = UUID.fromString(new String(proxyState1, UTF_8));

        // process again
        List<Object> out2 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(processedPacket1, endpoint, promise), out2);

        assertEquals(1, out1.size());
        RadiusRequest processedPacket2 = ((PendingRequestCtx) out2.get(0)).getRequest();

        // check another proxy-state added
        List<RadiusAttribute> attributes2 = processedPacket2.getAttributes();
        assertEquals(1, attributes1.size());
        assertEquals(2, attributes2.size());

        List<RadiusAttribute> attributes = processedPacket2.getAttributes(PROXY_STATE);
        assertEquals(requestId1, UUID.fromString(new String(attributes.get(0).getValue(), UTF_8)));
        assertNotEquals(requestId1, UUID.fromString(new String(attributes.get(1).getValue(), UTF_8)));
    }


    @Test
    void decodeNoProxyState() throws RadiusPacketException {
        AccessResponse.Accept response = (AccessResponse.Accept)
                RadiusResponse.create(dictionary, (byte) 2, (byte) 1, null, Collections.emptyList());

        List<Object> in = new ArrayList<>();
        handler.decode(ctx, response, in);

        assertTrue(in.isEmpty());
    }

    @Test
    void decodeProxyStateNotFound() throws RadiusPacketException {
        AccessResponse.Accept response = (AccessResponse.Accept)
                RadiusResponse.create(dictionary, (byte) 2, (byte) 1, null,
                        Collections.singletonList(dictionary.createAttribute(-1, PROXY_STATE, "123abc")));

        List<Object> in = new ArrayList<>();
        handler.decode(ctx, response, in);

        assertTrue(in.isEmpty());
    }

    @Test
    void encodeDecodeIdMismatch() throws RadiusPacketException {
        String secret = "mySecret";
        byte[] requestAuth = random.generateSeed(16);

        // using id 1
        AccessRequestPap request = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, requestAuth, Collections.emptyList()))
                        .withPapPassword("myPw");
        RadiusEndpoint requestEndpoint = new RadiusEndpoint(address, secret);

        Promise<RadiusResponse> promise = eventExecutor.newPromise();

        // add address-secret and id mapping to handler
        List<Object> out = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out);

        assertEquals(1, out.size());

        RadiusRequest preparedRequest = ((PendingRequestCtx) out.get(0)).getRequest();
        byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).get().getValue();

        // using id 99
        RadiusResponse response = RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 99, null,
                Collections.singletonList(dictionary.createAttribute(-1, PROXY_STATE, requestProxyState)));

        List<Object> in = new ArrayList<>();
        handler.decode(ctx, response.encodeResponse(secret, requestAuth), in);

        assertEquals(0, in.size());
        assertFalse(promise.isDone());
    }

    @Test
    void decodeAuthCheckFail() throws RadiusPacketException {
        String secret = "mySecret";

        AccessRequestPap request = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withPapPassword("myPw");
        RadiusEndpoint requestEndpoint = new RadiusEndpoint(address, secret);

        Promise<RadiusResponse> promise = eventExecutor.newPromise();

        // add address-secret and id mapping to handler
        List<Object> out = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out);

        assertEquals(1, out.size());
        RadiusRequest preparedRequest = ((RequestCtx) out.get(0)).getRequest();
        byte[] requestProxyState = preparedRequest.getAttribute(PROXY_STATE).get().getValue();

        RadiusResponse response = RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, PROXY_STATE, requestProxyState)));

        // response uses different auth
        byte[] randomAuth = random.generateSeed(16);
        List<Object> in = new ArrayList<>();
        handler.decode(ctx, response.encodeResponse(secret, randomAuth), in);

        assertEquals(0, in.size());
        assertFalse(promise.isDone());
    }

    @Test
    void encodeDecodeSuccess() throws RadiusPacketException {
        String secret = "mySecret";
        String pw = "myPw";

        Promise<RadiusResponse> promise = eventExecutor.newPromise();

        AccessRequestNoAuth request = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList())
                .addAttribute("Tunnel-Password", pw);
        RadiusEndpoint requestEndpoint = new RadiusEndpoint(address, secret);
        assertEquals(pw, new String(request.getAttribute("Tunnel-Password").get().getValue(), UTF_8));

        // process packet out
        List<Object> out = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(request, requestEndpoint, promise), out);

        assertEquals(1, out.size());

        RadiusRequest encodedRequest = ((RequestCtx) out.get(0)).getRequest();
        assertNotEquals(pw, new String(encodedRequest.getAttribute("Tunnel-Password").get().getValue(), UTF_8));

        // capture request details
        byte[] requestProxyState = encodedRequest.getAttribute(PROXY_STATE).get().getValue();
        byte[] requestAuthenticator = encodedRequest.getAuthenticator();
        assert requestAuthenticator != null;

        assertFalse(promise.isDone());

        // channel read correct proxyState returns packet
        RadiusResponse encodedResponse = RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, PROXY_STATE, requestProxyState))
                .addAttribute(dictionary.createAttribute(-1, 69, pw.getBytes(UTF_8)))
                .encodeResponse(secret, requestAuthenticator);

        assertFalse(promise.isDone());
        assertNotEquals(pw, new String(encodedResponse.getAttribute("Tunnel-Password").get().getValue(), UTF_8));

        // decode response
        List<Object> in1 = new ArrayList<>();
        handler.decode(ctx, encodedResponse, in1);
        assertTrue(in1.isEmpty());

        // check promise is done
        RadiusResponse decodedResponse = promise.getNow();
        assertTrue(promise.isDone());
        assertEquals(encodedResponse.getId(), decodedResponse.getId());
        assertEquals(encodedResponse.getType(), decodedResponse.getType());
        assertArrayEquals(encodedResponse.getAuthenticator(), decodedResponse.getAuthenticator());
        assertEquals(pw, new String(decodedResponse.getAttribute("Tunnel-Password").get().getValue(), UTF_8));

        // check proxyState is removed after reading
        assertEquals(2, decodedResponse.getAttributes().size());
    }


    @Test
    void encodeRadiusException() throws RadiusPacketException {
        String secret = UUID.randomUUID().toString();
        String username = "myUsername";
        String password = "myPassword";
        int id = random.nextInt(256);

        RadiusRequest packet = ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) id, null, Collections.emptyList()))
                .withPapPassword(password)
                .addAttribute(1, username);
        assertInstanceOf(AccessRequestPap.class, packet);
        RadiusEndpoint endpoint = new RadiusEndpoint(address, secret);

        // make packet too long to force encoder error
        for (int i = 0; i < 337; i++) {
            packet = packet.addAttribute(dictionary.createAttribute(-1, 1, username));
        }

        // process
        List<Object> out1 = new ArrayList<>();
        handler.encode(ctx, new PendingRequestCtx(packet, endpoint, promise), out1);

        // check
        assertTrue(promise.isDone());
        assertFalse(promise.isSuccess());
        assertEquals(RadiusPacketException.class, promise.cause().getClass());
        assertTrue(promise.cause().getMessage().contains("Packet too long"));
        assertEquals(0, out1.size());
    }
}

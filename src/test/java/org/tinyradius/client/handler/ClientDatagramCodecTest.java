package org.tinyradius.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccessRequestPap;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tinyradius.packet.request.RadiusRequest.fromDatagram;

@ExtendWith(MockitoExtension.class)
class ClientDatagramCodecTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();
    private final EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;

    private final ClientDatagramCodec codec = new ClientDatagramCodec(dictionary);
    private final InetSocketAddress address = new InetSocketAddress(0);
    private final Promise<RadiusResponse> promise = eventExecutor.newPromise();

    private static final byte USER_NAME = 1;

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void decodeSuccess() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);
        final String password = "myPw";

        final RadiusResponse encodeResponse = RadiusResponse.create(dictionary, (byte) 5, (byte) 1, null, Collections.emptyList())
                .addAttribute("User-Password", password)
                .encodeResponse("mySecret", requestAuth);

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, encodeResponse.toDatagram(address, address), out1);

        assertEquals(1, out1.size());
        final RadiusResponse actual = (RadiusResponse) out1.get(0);
        assertEquals(encodeResponse.toString(), actual.toString()); // should still be encoded
    }

    @Test
    void decodeRadiusException() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = RadiusResponse.create(dictionary, (byte) 5, (byte) 1, null, Collections.emptyList());

        final DatagramPacket datagram = response.encodeResponse("mySecret", requestAuth).toDatagram(
                address, address);

        datagram.content().array()[3] = 7; // corrupt bytes to trigger error

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, datagram, out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void decodeRemoteAddressNull() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = RadiusResponse.create(dictionary, (byte) 2, (byte) 1, null, Collections.emptyList());

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, response.encodeResponse("mySecret", requestAuth).toDatagram(
                address), out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void encodeAccessRequest() throws RadiusPacketException {
        final String secret = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        final byte id = (byte) random.nextInt(256);

        final RadiusRequest accessRequest = new AccessRequestPap(dictionary, id, null, Collections.emptyList())
                .withPassword(password)
                .addAttribute(USER_NAME, username);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);

        when(ctx.channel()).thenReturn(mock(Channel.class));

        // process
        final List<Object> out1 = new ArrayList<>();
        codec.encode(ctx, new PendingRequestCtx(accessRequest, endpoint, promise), out1);

        assertEquals(1, out1.size());
        final AccessRequestPap sentAccessPacket = (AccessRequestPap) fromDatagram(dictionary, (DatagramPacket) out1.get(0))
                .decodeRequest(secret);

        // check user details correctly encoded
        assertEquals(id, sentAccessPacket.getId());
        assertEquals(username, sentAccessPacket.getAttribute(USER_NAME).get().getValueString());
        assertEquals(password, sentAccessPacket.getPassword().get());
    }

    @Test
    void encodeRadiusException() {
        final String secret = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        int id = random.nextInt(256);

        RadiusRequest packet = new AccessRequestPap(dictionary, (byte) id, null, Collections.emptyList())
                .withPassword(password)
                .addAttribute(USER_NAME, username);
        final RadiusEndpoint endpoint = new RadiusEndpoint(address, secret);

        when(ctx.channel()).thenReturn(mock(Channel.class));

        // make packet too long to force encoder error
        for (int i = 0; i < 4000; i++) {
            packet = packet.addAttribute(dictionary.createAttribute(-1, USER_NAME, username));
        }

        // process
        final List<Object> out1 = new ArrayList<>();
        codec.encode(ctx, new PendingRequestCtx(packet, endpoint, promise), out1);

        // check
        assertTrue(promise.isDone());
        assertFalse(promise.isSuccess());
        assertEquals(RadiusPacketException.class, promise.cause().getClass());
        assertEquals(0, out1.size());
    }
}
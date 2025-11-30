package org.tinyradius.io.client.handler;

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
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.PendingRequestCtx;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.request.RadiusRequest.fromDatagram;

@SuppressWarnings("OptionalGetWithoutIsPresent")
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
        codec.decode(ctx, new DatagramPacket(encodeResponse.toByteBuf(), address, address), out1);

        assertEquals(1, out1.size());
        final RadiusResponse actual = (RadiusResponse) out1.get(0);
        assertEquals(encodeResponse.toString(), actual.toString()); // should still be encoded
    }

    @Test
    void decodeRadiusException() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = RadiusResponse.create(dictionary, (byte) 5, (byte) 1, null, Collections.emptyList());

        final DatagramPacket datagram = new DatagramPacket(
                response.encodeResponse("mySecret", requestAuth).toByteBuf(), address, address);

        datagram.content().setByte(3, 7); // corrupt bytes to trigger error

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, datagram, out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void decodeRemoteAddressNull() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = RadiusResponse.create(dictionary, (byte) 2, (byte) 1, null, Collections.emptyList());

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, new DatagramPacket(response.encodeResponse("mySecret", requestAuth).toByteBuf(), address), out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void encodeAccessRequest() throws RadiusPacketException {
        final String secret = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        final byte id = (byte) random.nextInt(256);

        final AccessRequestPap accessRequest = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, id, null, Collections.emptyList()))
                        .withPapPassword(password)
                        .addAttribute(USER_NAME, username)
                        .encodeRequest(secret);
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
}
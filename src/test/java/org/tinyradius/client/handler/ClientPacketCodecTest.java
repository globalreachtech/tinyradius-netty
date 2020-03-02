package org.tinyradius.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessPap;
import org.tinyradius.packet.GenericRadiusPacket;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.packet.auth.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static net.jradius.packet.attribute.AttributeDictionary.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.tinyradius.packet.PacketCodec.fromDatagram;
import static org.tinyradius.packet.PacketCodec.toDatagram;

@ExtendWith(MockitoExtension.class)
class ClientPacketCodecTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();

    private final ClientPacketCodec codec = new ClientPacketCodec(dictionary);
    private final InetSocketAddress address = new InetSocketAddress(0);

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Promise<RadiusResponse> promise;

    @Test
    void decodeSuccess() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = new RadiusResponse(dictionary, 2, 1, null, Collections.emptyList());

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, toDatagram(
                response.encodeResponse("mySecret", requestAuth), address, address), out1);

        assertEquals(1, out1.size());
        GenericRadiusPacket actual = (GenericRadiusPacket) out1.get(0);
        assertEquals(response.toString(), actual.toString());
    }

    @Test
    void decodeRadiusException() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = new RadiusResponse(dictionary, 2, 1, null, Collections.emptyList());

        final DatagramPacket datagram = toDatagram(
                response.encodeResponse("mySecret", requestAuth), address, address);

        datagram.content().array()[3] = 7; // corrupt bytes to trigger error

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, datagram, out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void decodeRemoteAddressNull() throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = new RadiusResponse(dictionary, 2, 1, null, Collections.emptyList());

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, toDatagram(
                response.encodeResponse("mySecret", requestAuth), address), out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void encodeAccessRequest() throws RadiusPacketException {
        final String secret = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        int id = random.nextInt(256);

        final AccessPap accessRequest = new AccessPap(dictionary, id, null, Collections.emptyList());
        accessRequest.setAttributeString(USER_NAME, username);
        accessRequest.setPlaintextPassword(password);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret);

        when(ctx.channel()).thenReturn(mock(Channel.class));

        // process
        final List<Object> out1 = new ArrayList<>();
        codec.encode(ctx, new PendingRequestCtx(accessRequest, endpoint, promise), out1);

        assertEquals(1, out1.size());
        final AccessPap sentAccessPacket = (AccessPap) fromDatagram(
                dictionary, (DatagramPacket) out1.get(0), secret);

        // check user details correctly encoded
        assertEquals(id, sentAccessPacket.getIdentifier());
        assertEquals(username, sentAccessPacket.getAttributeString(USER_NAME));
        assertEquals(password, sentAccessPacket.getPlaintextPassword());
    }

    @Test
    void encodeRadiusException() {
        final String secret = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        int id = random.nextInt(256);

        final AccessPap packet = new AccessPap(dictionary, id, null, Collections.emptyList());
        packet.setAttributeString(USER_NAME, username);
        packet.setPlaintextPassword(password);
        final RadiusEndpoint endpoint = new RadiusEndpoint(address, secret);

        when(ctx.channel()).thenReturn(mock(Channel.class));

        // make packet too long to force encoder error
        for (int i = 0; i < 4000; i++) {
            packet.addAttribute(Attributes.create(dictionary, -1, USER_NAME, username));
        }

        // process
        final List<Object> out1 = new ArrayList<>();
        codec.encode(ctx, new PendingRequestCtx(packet, endpoint, promise), out1);

        // check
        ArgumentCaptor<Exception> e = ArgumentCaptor.forClass(Exception.class);
        verify(promise).tryFailure(e.capture());
        assertEquals(RadiusPacketException.class, e.getValue().getClass());
        assertEquals(0, out1.size());
    }
}
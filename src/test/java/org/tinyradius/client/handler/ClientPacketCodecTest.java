package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.RequestCtxWrapper;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ClientPacketCodecTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);
    private final SecureRandom random = new SecureRandom();

    private ClientPacketCodec codec = new ClientPacketCodec(packetEncoder);

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Promise<RadiusPacket> promise;

    @Test
    void decodeSuccess() {
        throw new RuntimeException();
    }

    @Test
    void decodeRadiusException(){
        throw new RuntimeException();
    }

    @Test
    void decodeRemoteAddressNull() throws RadiusException {
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusPacket response = new RadiusPacket(dictionary, 2, 1);

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, packetEncoder.toDatagram(
                response.encodeResponse("mySecret", requestAuth), new InetSocketAddress(0)), out1);

        assertTrue(out1.isEmpty());
    }

    @Test
    void encodeAccessRequest() throws RadiusException {
        final String secret1 = UUID.randomUUID().toString();
        final String secret2 = UUID.randomUUID().toString();
        final String username = "myUsername";
        final String password = "myPassword";
        int id = random.nextInt(256);

        final RadiusPacket accessRequest = new AccessRequest(dictionary, id, null, username, password)
                .encodeRequest(secret1);
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), secret2);

        // process
        final List<Object> out1 = new ArrayList<>();
        codec.encode(ctx, new RequestCtxWrapper(accessRequest, endpoint, promise), out1);

        assertEquals(1, out1.size());
        final DatagramPacket accessPacketDatagram = (DatagramPacket) out1.get(0);
        final RadiusPacket sentAccessPacket = packetEncoder.fromDatagram(accessPacketDatagram, secret2);
        assertTrue(sentAccessPacket instanceof AccessRequest); // sanity check - we are not testing decoder here

        // check user details correctly encoded
        assertEquals(username, ((AccessRequest) sentAccessPacket).getUserName());
        assertEquals(password, ((AccessRequest) sentAccessPacket).getUserPassword());
    }

    @Test
    void encodeRadiusException() {
        throw new RuntimeException();
        // check promise
        // check nothing sent

    }
}
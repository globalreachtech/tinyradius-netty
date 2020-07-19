package org.tinyradius.core.packet.response;

import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.PacketType;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RadiusResponseTest {

    private static final byte USER_NAME = 1;

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final InetSocketAddress remoteAddress = new InetSocketAddress(0);

    @Test
    void createResponse() throws RadiusPacketException {
        final AccessResponse accessAccept = (AccessResponse) RadiusResponse.create(dictionary, PacketType.ACCESS_ACCEPT, (byte) 1, null, Collections.emptyList());
        assertEquals(PacketType.ACCESS_ACCEPT, accessAccept.getType());

        final GenericResponse accountingResponse = (GenericResponse) RadiusResponse.create(dictionary, PacketType.ACCOUNTING_RESPONSE, (byte) 2, null, Collections.emptyList());
        assertEquals(PacketType.ACCOUNTING_RESPONSE, accountingResponse.getType());
    }

    @Test
    void fromResponseDatagram() throws RadiusPacketException {
        final String user = "user2";
        final String plaintextPw = "myPassword2";
        final String sharedSecret = "sharedSecret2";

        final byte id = (byte) random.nextInt(256);

        final AccessRequestPap request = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, (byte) 1, id, null, Collections.emptyList()))
                        .withPapPassword(plaintextPw)
                        .addAttribute(USER_NAME, user);
        final RadiusRequest encodedRequest = request.encodeRequest(sharedSecret);

        final AccessResponse.Accept response = (AccessResponse.Accept) RadiusResponse.create(dictionary, (byte) 2, id, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, 33, "state3333".getBytes(UTF_8)));
        final RadiusResponse encodedResponse = response.encodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        final DatagramPacket datagramPacket = new DatagramPacket(encodedResponse.toByteBuf(), remoteAddress);
        final RadiusResponse packet = RadiusResponse.fromDatagram(dictionary, datagramPacket)
                .decodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        assertEquals(encodedResponse.getId(), packet.getId());
        assertEquals("state3333", new String(packet.getAttribute(33).get().getValue()));
        assertArrayEquals(encodedResponse.getAuthenticator(), packet.getAuthenticator());
    }
}
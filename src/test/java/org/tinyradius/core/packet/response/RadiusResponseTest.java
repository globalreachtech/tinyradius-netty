package org.tinyradius.core.packet.response;

import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.PacketType;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.RadiusPacketException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class RadiusResponseTest {

    private static final byte USER_NAME = 1;

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final InetSocketAddress remoteAddress = new InetSocketAddress(0);

    @Test
    void createResponse() {
        RadiusResponse accessAccept = RadiusResponse.create(dictionary, PacketType.ACCESS_ACCEPT, (byte) 1, null, Collections.emptyList());
        assertEquals(PacketType.ACCESS_ACCEPT, accessAccept.getType());
        assertTrue(accessAccept instanceof AccessResponse); // don't care about subclass

        RadiusResponse accountingResponse = RadiusResponse.create(dictionary, PacketType.ACCOUNTING_RESPONSE, (byte) 2, null, Collections.emptyList());
        assertEquals(PacketType.ACCOUNTING_RESPONSE, accountingResponse.getType());
        assertEquals(GenericResponse.class, accountingResponse.getClass());
    }

    @Test
    void fromResponseDatagram() throws RadiusPacketException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";

        final byte id = (byte) random.nextInt(256);

        final RadiusRequest request = new AccessRequestPap(dictionary, id, null, Collections.emptyList())
                .withPassword(plaintextPw)
                .addAttribute(USER_NAME, user);
        final RadiusRequest encodedRequest = request.encodeRequest(sharedSecret);

        final RadiusResponse response = new AccessResponse.Accept(dictionary, id, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, (byte) 33, "state3333".getBytes(UTF_8)));
        final RadiusResponse encodedResponse = response.encodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        DatagramPacket datagramPacket = encodedResponse.toDatagram(remoteAddress);
        RadiusResponse packet = RadiusResponse.fromDatagram(dictionary, datagramPacket);
        packet.decodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        assertEquals(encodedResponse.getId(), packet.getId());
        assertEquals("state3333", new String(packet.getAttribute((byte) 33).get().getValue()));
        assertArrayEquals(encodedResponse.getAuthenticator(), packet.getAuthenticator());
    }
}
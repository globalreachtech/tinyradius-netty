package org.tinyradius.packet;

import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.AccessRequest.AUTH_PAP;
import static org.tinyradius.packet.PacketType.*;
import static org.tinyradius.packet.RadiusPacketEncoder.createRadiusPacket;

class RadiusPacketEncoderTest {

    private static final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final InetSocketAddress remoteAddress = new InetSocketAddress(0);

    @Test
    void nextPacketId() {
        for (int i = 0; i < 1000; i++) {
            final int next = RadiusPacketEncoder.nextPacketId();
            assertTrue(next < 256);
            assertTrue(next >= 0);
        }
    }

    @Test
    void toDatagram() throws RadiusException {
        final InetSocketAddress address = new InetSocketAddress(random.nextInt(65535));
        RadiusPacket request = new AccountingRequest(dictionary, 1, null);
        request.addAttribute(new RadiusAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)));

        final RadiusPacket encoded = request.encodeRequest("mySecret");

        DatagramPacket datagram = RadiusPacketEncoder.toDatagram(encoded, address);

        assertEquals(address, datagram.recipient());

        // packet
        final byte[] packet = datagram.content().array();
        assertEquals(28, packet.length);

        assertEquals(encoded.getPacketType(), packet[0]);
        assertEquals(encoded.getPacketIdentifier(), packet[1]);
        assertEquals(packet.length, packet[2] << 8 | packet[3]);
        assertArrayEquals(encoded.getAuthenticator(), Arrays.copyOfRange(packet, 4, 20));

        // attribute
        final byte[] attributes = Arrays.copyOfRange(packet, 20, packet.length);
        assertEquals(8, attributes.length);

        assertEquals(33, attributes[0]);
        assertEquals(attributes.length, attributes[1]);
        assertEquals("state1", new String(Arrays.copyOfRange(attributes, 2, attributes.length)));
    }

    @Test
    void getRadiusPacketFromDatagram() throws RadiusException {
        String user = "user1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequest original = new AccessRequest(dictionary, 1, null, user, plaintextPw)
                .encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(original, remoteAddress);
        RadiusPacket packet = RadiusPacketEncoder.fromRequestDatagram(dictionary, datagramPacket, sharedSecret);

        assertEquals(ACCESS_REQUEST, packet.getPacketType());
        assertTrue(packet instanceof AccessRequest);
        assertEquals(plaintextPw, ((AccessRequest) packet).getUserPassword());
        assertArrayEquals(original.getAttribute("User-Password").getData(), packet.getAttribute("User-Password").getData());
        assertEquals(original.getUserName(), packet.getAttribute("User-Name").getDataString());
    }

    @Test
    void getRadiusPacketFromResponseDatagram() throws RadiusException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";

        AccessRequest request = new AccessRequest(dictionary, 2, null, user, plaintextPw);
        request.setAuthProtocol(AUTH_PAP);
        AccessRequest encodedRequest = request.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(encodedRequest, remoteAddress);
        RadiusPacket radiusPacket = RadiusPacketEncoder.fromResponseDatagram(dictionary, datagramPacket, sharedSecret, encodedRequest);
        String expectedPlaintextPw = ((AccessRequest) radiusPacket).getUserPassword();

        assertArrayEquals(encodedRequest.getAttribute("User-Password").getData(), radiusPacket.getAttribute("User-Password").getData());
        assertEquals(plaintextPw, expectedPlaintextPw);
        assertEquals(encodedRequest.getAttribute("User-Name").getDataString(), radiusPacket.getAttribute("User-Name").getDataString());
    }

    @Test
    void createRequestRadiusPacket() {
        final byte[] authenticator = new byte[16];
        random.nextBytes(authenticator);

        RadiusPacket accessRequest = createRadiusPacket(dictionary, ACCESS_REQUEST, 1, authenticator, Collections.emptyList());
        RadiusPacket coaRequest = createRadiusPacket(dictionary, COA_REQUEST, 2, authenticator, Collections.emptyList());
        RadiusPacket disconnectRequest = createRadiusPacket(dictionary, DISCONNECT_REQUEST, 3, authenticator, Collections.emptyList());
        RadiusPacket accountingRequest = createRadiusPacket(dictionary, ACCOUNTING_REQUEST, 4, authenticator, Collections.emptyList());

        assertEquals(ACCESS_REQUEST, accessRequest.getPacketType());
        assertEquals(AccessRequest.class, accessRequest.getClass());

        assertEquals(COA_REQUEST, coaRequest.getPacketType());
        assertEquals(RadiusPacket.class, coaRequest.getClass());

        assertEquals(DISCONNECT_REQUEST, disconnectRequest.getPacketType());
        assertEquals(RadiusPacket.class, disconnectRequest.getClass());

        assertEquals(ACCOUNTING_REQUEST, accountingRequest.getPacketType());
        assertEquals(AccountingRequest.class, accountingRequest.getClass());
    }
}
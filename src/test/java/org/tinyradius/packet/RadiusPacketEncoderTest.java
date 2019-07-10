package org.tinyradius.packet;

import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.AccessRequest.AUTH_PAP;
import static org.tinyradius.packet.PacketType.*;

class RadiusPacketEncoderTest {

    private static final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final InetSocketAddress remoteAddress = new InetSocketAddress(0);

    private byte[] authenticator = new byte[16];

    @BeforeEach
    void setup() {
        random.nextBytes(authenticator);
    }

    @Test
    void getNextPacketIdentifier() {
        for (int i = 0; i < 1000; i++) {
            final int next = RadiusPacketEncoder.nextPacketId();
            assertTrue(next < 256);
            assertTrue(next >= 0);
        }
    }


    @Test
    void encodeRadiusPacket() throws IOException {
        AccessRequest request = new AccessRequest(dictionary, 1, authenticator);
        DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(request, remoteAddress);

        assertTrue(datagramPacket.content().isReadable());
        assertEquals(remoteAddress, datagramPacket.recipient());
    }

    @Test
    void getRadiusPacketFromDatagram() throws IOException, RadiusException {
        String user = "user1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequest request = new AccessRequest(dictionary, 1, null, user, plaintextPw);
        request.setAuthProtocol(AUTH_PAP);
        AccessRequest encodedRequest = request.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(encodedRequest, remoteAddress);
        RadiusPacket radiusPacket = RadiusPacketEncoder.fromRequestDatagram(dictionary, datagramPacket, sharedSecret);
        String expectedPlaintextPw = ((AccessRequest) radiusPacket).getUserPassword();

        assertArrayEquals(encodedRequest.getAttribute("User-Password").getData(), radiusPacket.getAttribute("User-Password").getData());
        assertEquals(plaintextPw, expectedPlaintextPw);
        assertEquals(encodedRequest.getAttribute("User-Name").getDataString(), radiusPacket.getAttribute("User-Name").getDataString());
    }

    @Test
    void getRadiusPacketFromResponseDatagram() throws IOException, RadiusException {
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
        List<Integer> packetTypes = new ArrayList<>();
        packetTypes.add(ACCESS_REQUEST);
        packetTypes.add(COA_REQUEST);
        packetTypes.add(DISCONNECT_REQUEST);
        packetTypes.add(ACCOUNTING_REQUEST);

        RadiusPacket accessRequest = RadiusPacketEncoder.createRadiusPacket(dictionary, ACCESS_REQUEST, 1, authenticator, Collections.emptyList());
        RadiusPacket coaRequest = RadiusPacketEncoder.createRadiusPacket(dictionary, COA_REQUEST, 2, authenticator, Collections.emptyList());
        RadiusPacket disconnectRequest = RadiusPacketEncoder.createRadiusPacket(dictionary, DISCONNECT_REQUEST, 3, authenticator, Collections.emptyList());
        RadiusPacket accountingRequest = RadiusPacketEncoder.createRadiusPacket(dictionary, ACCOUNTING_REQUEST, 4, authenticator, Collections.emptyList());
        RadiusPacket radiusPacket = RadiusPacketEncoder.createRadiusPacket(dictionary, STATUS_REQUEST, 5, authenticator, Collections.emptyList());

        assertEquals(ACCESS_REQUEST, accessRequest.getPacketType());
        assertEquals(COA_REQUEST, coaRequest.getPacketType());
        assertEquals(DISCONNECT_REQUEST, disconnectRequest.getPacketType());
        assertEquals(ACCOUNTING_REQUEST, accountingRequest.getPacketType());
        assertFalse(packetTypes.contains(radiusPacket.getPacketType()));
        assertEquals(STATUS_REQUEST, radiusPacket.getPacketType());
    }

}
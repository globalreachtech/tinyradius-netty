package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;
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
    void toDatagramMaxPacketSize() throws RadiusException {
        // test max length 4096
        RadiusPacket maxSizeRequest = new RadiusPacket(dictionary, 200, 250);
        for (int i = 0; i < 20; i++) {
            // add 200 octets per iteration (198 + 2-byte header)
            maxSizeRequest.addAttribute(createAttribute(dictionary, -1, 33, random.generateSeed(198)));
        }
        maxSizeRequest.addAttribute(createAttribute(dictionary, -1, 33, random.generateSeed(74)));

        final ByteBuf byteBuf = RadiusPacketEncoder
                .toDatagram(maxSizeRequest.encodeRequest("mySecret"), new InetSocketAddress(0))
                .content();

        // 20-byte header + (20 * (198+2)) + (74+2)
        assertEquals(4096, byteBuf.readableBytes());
        final byte[] array = byteBuf.copy().array();
        assertEquals(4096, toUnsignedInt(array[2]) << 8 | toUnsignedInt(array[3]));

        // test length 4097
        maxSizeRequest.removeLastAttribute(33);
        maxSizeRequest.addAttribute(createAttribute(dictionary, -1, 33, random.generateSeed(75)));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> RadiusPacketEncoder.toDatagram(maxSizeRequest.encodeRequest("mySecret"), new InetSocketAddress(0)));

        assertTrue(exception.getMessage().contains("packet too long"));
    }

    @Test
    void toDatagram() throws RadiusException {
        final InetSocketAddress address = new InetSocketAddress(random.nextInt(65535));
        RadiusPacket request = new RadiusPacket(dictionary, 200, 250);

        final byte[] proxyState = random.generateSeed(198);
        request.addAttribute(createAttribute(dictionary, -1, 33, proxyState));
        request.addAttribute(createAttribute(dictionary, -1, 33, random.generateSeed(198)));

        final RadiusPacket encoded = request.encodeRequest("mySecret");

        DatagramPacket datagram = RadiusPacketEncoder.toDatagram(encoded, address);

        assertEquals(address, datagram.recipient());

        // packet
        final byte[] packet = datagram.content().array();
        assertEquals(420, packet.length);

        assertEquals(encoded.getPacketType(), toUnsignedInt(packet[0]));
        assertEquals(encoded.getPacketIdentifier(), toUnsignedInt(packet[1]));
        assertEquals(packet.length, toUnsignedInt(packet[2]) << 8 | toUnsignedInt(packet[3]));
        assertArrayEquals(encoded.getAuthenticator(), Arrays.copyOfRange(packet, 4, 20));

        // attribute
        final byte[] attributes = Arrays.copyOfRange(packet, 20, packet.length);
        assertEquals(400, attributes.length); // 2x 2-octet header + 2x 198

        assertEquals(33, attributes[0]);
        assertArrayEquals(proxyState, Arrays.copyOfRange(attributes, 2, toUnsignedInt(attributes[1])));
    }

    @Test
    void fromBigRequestDatagram() throws RadiusException {
        String sharedSecret = "sharedSecret1";

        // test max length 4096
        AccountingRequest rawRequest = new AccountingRequest(dictionary, 250, null);
        for (int i = 0; i < 20; i++) {
            // add 200 octets per iteration (198 + 2-byte header)
            rawRequest.addAttribute(createAttribute(dictionary, -1, 33, random.generateSeed(198)));
        }
        rawRequest.addAttribute(createAttribute(dictionary, -1, 33, random.generateSeed(74)));

        final RadiusPacket maxSizeRequest = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagram = RadiusPacketEncoder
                .toDatagram(maxSizeRequest, new InetSocketAddress(0));
        assertEquals(4096, datagram.content().readableBytes());

        RadiusPacket result = RadiusPacketEncoder.fromRequestDatagram(dictionary, datagram, sharedSecret);

        assertEquals(maxSizeRequest.getPacketType(), result.getPacketType());
        assertEquals(maxSizeRequest.getPacketIdentifier(), result.getPacketIdentifier());
        assertArrayEquals(maxSizeRequest.getAuthenticator(), result.getAuthenticator());
        assertArrayEquals(maxSizeRequest.getAttributeBytes(), result.getAttributeBytes());

        assertEquals(maxSizeRequest.getAttributes(33).size(), result.getAttributes(33).size());

        assertArrayEquals(datagram.content().array(), RadiusPacketEncoder.toDatagram(result, new InetSocketAddress(0)).content().array());

        // todo test parse 4097 length packet
    }

    @Test
    void fromRequestDatagram() throws RadiusException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        // todo test other types
        AccountingRequest rawRequest = new AccountingRequest(dictionary, 250, null);
        rawRequest.setUserName(user);
        final RadiusPacket request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(request, remoteAddress);
        RadiusPacket packet = RadiusPacketEncoder.fromRequestDatagram(dictionary, datagramPacket, sharedSecret);

        assertEquals(ACCOUNTING_REQUEST, packet.getPacketType());
        assertTrue(packet instanceof AccountingRequest);
        assertEquals(rawRequest.getPacketIdentifier(), packet.getPacketIdentifier());
        assertEquals(rawRequest.getUserName(), packet.getAttribute("User-Name").getDataString());

        // todo test with bad authenticator (will not work with AccessRequest as auth is random)
    }

    @Test
    void fromResponseDatagram() throws RadiusException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";

        final int id = random.nextInt(256);

        final AccessRequest request = new AccessRequest(dictionary, id, null, user, plaintextPw);
        final AccessRequest encodedRequest = request.encodeRequest(sharedSecret);

        final RadiusPacket response = new RadiusPacket(dictionary, 2, id);
        response.addAttribute(createAttribute(dictionary, -1, 33, "state3333".getBytes(UTF_8)));
        final RadiusPacket encodedResponse = response.encodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(encodedResponse, remoteAddress);
        RadiusPacket packet = RadiusPacketEncoder.fromResponseDatagram(dictionary, datagramPacket, sharedSecret, encodedRequest);

        assertEquals(encodedResponse.getPacketIdentifier(), packet.getPacketIdentifier());
        assertEquals("state3333", new String(packet.getAttribute(33).getData()));
        assertArrayEquals(encodedResponse.getAuthenticator(), packet.getAuthenticator());

        // todo test with different request packetId/auth
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
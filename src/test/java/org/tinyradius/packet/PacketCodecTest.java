package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.auth.RadiusRequest;
import org.tinyradius.packet.auth.RadiusResponse;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.create;
import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.PacketCodec.fromDatagram;
import static org.tinyradius.packet.PacketCodec.toDatagram;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;
import static org.tinyradius.packet.RadiusPacket.HEADER_LENGTH;

class PacketCodecTest {

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final InetSocketAddress remoteAddress = new InetSocketAddress(0);

    @Test
    void nextPacketId() {
        for (int i = 0; i < 1000; i++) {
            final int next = RadiusPackets.nextPacketId();
            assertTrue(next < 256);
            assertTrue(next >= 0);
        }
    }

    private void addBytesToPacket(RadiusPacket packet, int targetSize) {
        int dataSize = targetSize - HEADER_LENGTH;
        for (int i = 0; i < Math.floor((double) dataSize / 200); i++) {
            // add 200 octets per iteration (198 + 2-byte header)
            packet.addAttribute(create(dictionary, -1, 33, random.generateSeed(198)));
        }
        packet.addAttribute(create(dictionary, -1, 33, random.generateSeed((dataSize % 200) - 2)));
    }

    @Test
    void toDatagramMaxPacketSize() throws RadiusPacketException {
        // test max length 4096
        RadiusRequest maxSizeRequest = new RadiusRequest(dictionary, 200, 250, null, Collections.emptyList());
        addBytesToPacket(maxSizeRequest, 4096);

        final ByteBuf byteBuf = toDatagram(maxSizeRequest.encodeRequest("mySecret"), new InetSocketAddress(0))
                .content();

        assertEquals(4096, byteBuf.readableBytes());
        assertEquals(4096, byteBuf.getShort(2));

        // test length 4097
        RadiusRequest oversizeRequest = new RadiusRequest(dictionary, 200, 250, null, Collections.emptyList());
        addBytesToPacket(oversizeRequest, 4097);

        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> toDatagram(oversizeRequest.encodeRequest("mySecret"), new InetSocketAddress(0)));

        assertTrue(exception.getMessage().toLowerCase().contains("packet too long"));

        byteBuf.release();
    }

    @Test
    void testToDatagram() throws RadiusPacketException {
        final InetSocketAddress address = new InetSocketAddress(random.nextInt(65535));
        RadiusRequest request = new RadiusRequest(dictionary, 200, 250, null, Collections.emptyList());

        final byte[] proxyState = random.generateSeed(198);
        request.addAttribute(create(dictionary, -1, 33, proxyState));
        request.addAttribute(create(dictionary, -1, 33, random.generateSeed(198)));

        final RadiusRequest encoded = request.encodeRequest("mySecret");

        DatagramPacket datagram = toDatagram(encoded, address);

        assertEquals(address, datagram.recipient());

        // packet
        final byte[] packet = datagram.content().array();
        assertEquals(420, packet.length);

        assertEquals(encoded.getType(), toUnsignedInt(packet[0]));
        assertEquals(encoded.getIdentifier(), toUnsignedInt(packet[1]));
        assertEquals(packet.length, ByteBuffer.wrap(packet).getShort(2));
        assertArrayEquals(encoded.getAuthenticator(), Arrays.copyOfRange(packet, 4, 20));

        // attribute
        final byte[] attributes = Arrays.copyOfRange(packet, 20, packet.length);
        assertEquals(400, attributes.length); // 2x 2-octet header + 2x 198

        assertEquals(33, attributes[0]);
        assertArrayEquals(proxyState, Arrays.copyOfRange(attributes, 2, toUnsignedInt(attributes[1])));

        datagram.release();
    }

    @Test
    void fromMaxSizeRequestDatagram() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        // test max length 4096
        AccountingRequest rawRequest = new AccountingRequest(dictionary, 250, null);
        addBytesToPacket(rawRequest, 4096);
        final RadiusRequest maxSizeRequest = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagram = toDatagram(maxSizeRequest, new InetSocketAddress(0));
        assertEquals(4096, datagram.content().readableBytes());

        RadiusRequest result = fromDatagram(dictionary, datagram, sharedSecret);

        assertEquals(maxSizeRequest.getType(), result.getType());
        assertEquals(maxSizeRequest.getIdentifier(), result.getIdentifier());
        assertArrayEquals(maxSizeRequest.getAuthenticator(), result.getAuthenticator());
        assertArrayEquals(maxSizeRequest.getAttributeBytes(), result.getAttributeBytes());

        assertEquals(maxSizeRequest.getAttributes(33).size(), result.getAttributes(33).size());

        // reconvert to check if bytes match
        assertArrayEquals(datagram.content().array(), toDatagram(result, new InetSocketAddress(0)).content().array());
    }

    @Test
    void fromOverSizeRequestDatagram() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        // make 4090 octet packet
        AccountingRequest packet = new AccountingRequest(dictionary, 250, null);
        addBytesToPacket(packet, 4090);

        final byte[] validBytes = toDatagram(packet.encodeRequest(sharedSecret), new InetSocketAddress(0))
                .content().copy().array();
        assertEquals(4090, validBytes.length);

        // create 7 octet attribute
        final byte[] attribute = create(dictionary, -1, 33, random.generateSeed(5)).toByteArray();
        assertEquals(7, attribute.length);

        // append attribute
        final ByteBuf buffer = Unpooled.buffer(4097, 4097)
                .writeBytes(validBytes)
                .writeBytes(attribute)
                .setShort(2, 4097);

        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> fromDatagram(dictionary, new DatagramPacket(buffer, new InetSocketAddress(0)), sharedSecret));

        assertTrue(exception.getMessage().contains("packet too long"));
    }

    @Test
    void accountingRequestFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        AccountingRequest rawRequest = new AccountingRequest(dictionary, 250, null);
        rawRequest.setAttributeString(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = toDatagram(request, remoteAddress);
        RadiusRequest packet = fromDatagram(dictionary, datagramPacket, sharedSecret);

        assertEquals(ACCOUNTING_REQUEST, packet.getType());
        assertTrue(packet instanceof AccountingRequest);
        assertEquals(rawRequest.getIdentifier(), packet.getIdentifier());
        assertEquals(rawRequest.getAttributeString(USER_NAME), packet.getAttributeString(USER_NAME));
    }

    @Test
    void accountingRequestBadAuthFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        AccountingRequest rawRequest = new AccountingRequest(dictionary, 250, null);
        rawRequest.setAttributeString(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket originalDatagram = toDatagram(request, remoteAddress);

        final byte[] array = originalDatagram.content().array();
        array[4] = 0; // corrupt authenticator

        final DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(array), originalDatagram.recipient());

        final RadiusPacketException radiusPacketException = assertThrows(RadiusPacketException.class,
                () -> fromDatagram(dictionary, datagramPacket, sharedSecret));

        assertTrue(radiusPacketException.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void accessRequestFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String password = "myPassword";
        String sharedSecret = "sharedSecret1";

        AccessPap rawRequest = new AccessPap(dictionary, 250, null, Collections.emptyList());
        rawRequest.setAttributeString(USER_NAME, user);
        rawRequest.setPlaintextPassword(password);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = toDatagram(request, remoteAddress);
        RadiusRequest radiusPacket = fromDatagram(dictionary, datagramPacket, sharedSecret);

        assertEquals(ACCESS_REQUEST, radiusPacket.getType());
        assertTrue(radiusPacket instanceof AccessRequest);

        AccessPap packet = (AccessPap) radiusPacket;
        assertEquals(rawRequest.getIdentifier(), packet.getIdentifier());
        assertEquals(rawRequest.getAttributeString(USER_NAME), packet.getAttributeString(USER_NAME));
        assertEquals(rawRequest.getPlaintextPassword(), packet.getPlaintextPassword());
    }

    @Test
    void fromResponseDatagram() throws RadiusPacketException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";

        final int id = random.nextInt(256);

        final AccessPap request = new AccessPap(dictionary, id, null, Collections.emptyList());
        request.setAttributeString(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);
        final AccessRequest encodedRequest = request.encodeRequest(sharedSecret);

        final RadiusResponse response = new AccessResponse(dictionary, 2, id, null, Collections.emptyList());
        response.addAttribute(create(dictionary, -1, 33, "state3333".getBytes(UTF_8)));
        final RadiusResponse encodedResponse = response.encodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        DatagramPacket datagramPacket = toDatagram(encodedResponse, remoteAddress);
        RadiusResponse packet = fromDatagram(dictionary, datagramPacket, sharedSecret, encodedRequest);

        assertEquals(encodedResponse.getIdentifier(), packet.getIdentifier());
        assertEquals("state3333", new String(packet.getAttribute(33).getValue()));
        assertArrayEquals(encodedResponse.getAuthenticator(), packet.getAuthenticator());
    }
}
package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccessRequest;
import org.tinyradius.packet.request.AccessRequestPap;
import org.tinyradius.packet.request.AccountingRequest;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.AccessResponse;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.util.Attributes.create;
import static org.tinyradius.packet.request.AccessRequest.USER_NAME;
import static org.tinyradius.packet.util.PacketCodec.*;
import static org.tinyradius.packet.util.PacketType.ACCESS_REQUEST;
import static org.tinyradius.packet.util.PacketType.ACCOUNTING_REQUEST;

class PacketCodecTest {

    private static final int HEADER_LENGTH = 20;

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final InetSocketAddress remoteAddress = new InetSocketAddress(0);

    private void addBytesToPacket(BaseRadiusPacket packet, int targetSize) {
        int dataSize = targetSize - HEADER_LENGTH;
        for (int i = 0; i < Math.floor((double) dataSize / 200); i++) {
            // add 200 octets per iteration (198 + 2-byte header)
            packet.addAttribute(create(dictionary, -1, (byte) 33, random.generateSeed(198)));
        }
        packet.addAttribute(create(dictionary, -1, (byte) 33, random.generateSeed((dataSize % 200) - 2)));
    }

    @Test
    void toDatagramMaxPacketSize() throws RadiusPacketException {
        // test max length 4096
        RadiusRequest maxSizeRequest = RadiusRequest.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList());
        addBytesToPacket(maxSizeRequest, 4096);

        final ByteBuf byteBuf = toDatagram(maxSizeRequest.encodeRequest("mySecret"), new InetSocketAddress(0))
                .content();

        assertEquals(4096, byteBuf.readableBytes());
        assertEquals(4096, byteBuf.getShort(2));

        // test length 4097
        RadiusRequest oversizeRequest = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        addBytesToPacket(oversizeRequest, 4097);

        // encode on separate line - encodeRequest() and toDatagram() both throw RadiusPacketException
        final RadiusRequest encodedRequest = oversizeRequest.encodeRequest("mySecret");
        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> toDatagram(encodedRequest, new InetSocketAddress(0)));

        assertTrue(exception.getMessage().toLowerCase().contains("packet too long"));

        byteBuf.release();
    }

    @Test
    void testToDatagram() throws RadiusPacketException {
        final InetSocketAddress address = new InetSocketAddress(random.nextInt(65535));
        RadiusRequest request = RadiusRequest.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList());

        final byte[] proxyState = random.generateSeed(198);
        request.addAttribute(create(dictionary, -1, (byte) 33, proxyState));
        request.addAttribute(create(dictionary, -1, (byte) 33, random.generateSeed(198)));

        final RadiusRequest encoded = request.encodeRequest("mySecret");

        DatagramPacket datagram = toDatagram(encoded, address);

        assertEquals(address, datagram.recipient());

        // packet
        final byte[] packet = datagram.content().array();
        assertEquals(420, packet.length);

        assertEquals(encoded.getType(), toUnsignedInt(packet[0]));
        assertEquals(encoded.getId(), toUnsignedInt(packet[1]));
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
        AccountingRequest rawRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        addBytesToPacket(rawRequest, 4096);
        final RadiusRequest maxSizeRequest = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagram = toDatagram(maxSizeRequest, new InetSocketAddress(0));
        assertEquals(4096, datagram.content().readableBytes());

        RadiusRequest result = fromDatagramRequest(dictionary, datagram);
        result.verifyRequest(sharedSecret);

        assertEquals(maxSizeRequest.getType(), result.getType());
        assertEquals(maxSizeRequest.getId(), result.getId());
        assertArrayEquals(maxSizeRequest.getAuthenticator(), result.getAuthenticator());
        assertArrayEquals(maxSizeRequest.getAttributeBytes(), result.getAttributeBytes());

        assertEquals(maxSizeRequest.getAttributes((byte) 33).size(), result.getAttributes((byte) 33).size());

        // reconvert to check if bytes match
        assertArrayEquals(datagram.content().array(), toDatagram(result, new InetSocketAddress(0)).content().array());
    }

    @Test
    void fromOverSizeRequestDatagram() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        // make 4090 octet packet
        AccountingRequest packet = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        addBytesToPacket(packet, 4090);

        final byte[] validBytes = toDatagram(packet.encodeRequest(sharedSecret), new InetSocketAddress(0))
                .content().copy().array();
        assertEquals(4090, validBytes.length);

        // create 7 octet attribute
        final byte[] attribute = create(dictionary, -1, (byte) 33, random.generateSeed(5)).toByteArray();
        assertEquals(7, attribute.length);

        // append attribute
        final ByteBuf buffer = Unpooled.buffer(4097, 4097)
                .writeBytes(validBytes)
                .writeBytes(attribute)
                .setShort(2, 4097);

        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> fromDatagramRequest(dictionary, new DatagramPacket(buffer, new InetSocketAddress(0))));

        assertTrue(exception.getMessage().contains("packet too long"));
    }

    @Test
    void accountingRequestFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        AccountingRequest rawRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        rawRequest.addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = toDatagram(request, remoteAddress);
        RadiusRequest packet = fromDatagramRequest(dictionary, datagramPacket);
        packet.verifyRequest(sharedSecret);

        assertEquals(ACCOUNTING_REQUEST, packet.getType());
        assertTrue(packet instanceof AccountingRequest);
        assertEquals(rawRequest.getId(), packet.getId());
        assertEquals(rawRequest.getAttributeString(USER_NAME), packet.getAttributeString(USER_NAME));
    }

    @Test
    void accountingRequestBadAuthFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        AccountingRequest rawRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        rawRequest.addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket originalDatagram = toDatagram(request, remoteAddress);

        final byte[] array = originalDatagram.content().array();
        array[4] = 0; // corrupt authenticator

        final DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(array), originalDatagram.recipient());

        final RadiusRequest newRequest = fromDatagramRequest(dictionary, datagramPacket);
        final RadiusPacketException radiusPacketException = assertThrows(RadiusPacketException.class,
                () -> newRequest.verifyRequest(sharedSecret));

        assertTrue(radiusPacketException.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void accessRequestFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String password = "myPassword";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap rawRequest = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList());
        rawRequest.addAttribute(USER_NAME, user);
        rawRequest.setPlaintextPassword(password);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = toDatagram(request, remoteAddress);
        RadiusRequest radiusPacket = fromDatagramRequest(dictionary, datagramPacket);
        radiusPacket.verifyRequest(sharedSecret);

        assertEquals(ACCESS_REQUEST, radiusPacket.getType());
        assertTrue(radiusPacket instanceof AccessRequest);

        AccessRequestPap packet = (AccessRequestPap) radiusPacket;
        assertEquals(rawRequest.getId(), packet.getId());
        assertEquals(rawRequest.getAttributeString(USER_NAME), packet.getAttributeString(USER_NAME));
        assertEquals(rawRequest.getPlaintextPassword(), packet.getPlaintextPassword());
    }

    @Test
    void fromResponseDatagram() throws RadiusPacketException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";

        final byte id = (byte) random.nextInt(256);

        final AccessRequestPap request = new AccessRequestPap(dictionary, id, null, Collections.emptyList());
        request.addAttribute(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);
        final AccessRequest encodedRequest = request.encodeRequest(sharedSecret);

        final RadiusResponse response = new AccessResponse(dictionary, (byte) 2, id, null, Collections.emptyList());
        response.addAttribute(create(dictionary, -1, (byte) 33, "state3333".getBytes(UTF_8)));
        final RadiusResponse encodedResponse = response.encodeResponse(sharedSecret, encodedRequest.getAuthenticator());

        DatagramPacket datagramPacket = toDatagram(encodedResponse, remoteAddress);
        RadiusResponse packet = fromDatagramResponse(dictionary, datagramPacket);
        packet.verifyResponse(sharedSecret, encodedRequest.getAuthenticator());

        assertEquals(encodedResponse.getId(), packet.getId());
        assertEquals("state3333", new String(packet.getAttribute((byte) 33).getValue()));
        assertArrayEquals(encodedResponse.getAuthenticator(), packet.getAuthenticator());
    }
}
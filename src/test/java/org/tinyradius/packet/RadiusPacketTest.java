package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.Byte.toUnsignedInt;
import static org.junit.jupiter.api.Assertions.*;

class RadiusPacketTest {

    private static final int HEADER_LENGTH = 20;

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();

    private RadiusRequest addBytesToPacket(RadiusRequest packet, int targetSize) {
        int dataSize = targetSize - HEADER_LENGTH;
        for (int i = 0; i < Math.floor((double) dataSize / 200); i++) {
            // add 200 octets per iteration (198 + 2-byte header)
            packet = packet.addAttribute(dictionary.createAttribute(-1, (byte) 33, random.generateSeed(198)));
        }
        packet = packet.addAttribute(dictionary.createAttribute(-1, (byte) 33, random.generateSeed((dataSize % 200) - 2)));

        return packet;
    }

    @Test
    void toDatagramMaxPacketSize() throws RadiusPacketException {
        // test max length 4096
        RadiusRequest maxSizeRequest = RadiusRequest.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList());
        maxSizeRequest = addBytesToPacket(maxSizeRequest, 4096);

        final ByteBuf byteBuf = maxSizeRequest.encodeRequest("mySecret").toDatagram(new InetSocketAddress(0))
                .content();

        assertEquals(4096, byteBuf.readableBytes());
        assertEquals(4096, byteBuf.getShort(2));

        // test length 4097
        RadiusRequest oversizeRequest = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        oversizeRequest = addBytesToPacket(oversizeRequest, 4097);

        // encode on separate line - encodeRequest() and toDatagram() both throw RadiusPacketException
        final RadiusRequest encodedRequest = oversizeRequest.encodeRequest("mySecret");
        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> encodedRequest.toDatagram(new InetSocketAddress(0)));

        assertTrue(exception.getMessage().toLowerCase().contains("packet too long"));

        byteBuf.release();
    }

    @Test
    void testToDatagram() throws RadiusPacketException {
        final InetSocketAddress address = new InetSocketAddress(random.nextInt(65535));
        final byte[] proxyState = random.generateSeed(198);

        RadiusRequest request = RadiusRequest.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, (byte) 33, proxyState))
                .addAttribute(dictionary.createAttribute(-1, (byte) 33, random.generateSeed(198)));

        final RadiusRequest encoded = request.encodeRequest("mySecret");

        DatagramPacket datagram = encoded.toDatagram(address);

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

}
package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.RadiusRequest;

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

    private RadiusRequest addBytesToPacket(RadiusRequest packet, int targetSize) throws RadiusPacketException {
        int dataSize = targetSize - HEADER_LENGTH;
        for (int i = 0; i < Math.floor((double) dataSize / 200); i++) {
            // add 200 octets per iteration (198 + 2-byte header)
            packet = packet.addAttribute(dictionary.createAttribute(-1, 33, random.generateSeed(198)));
        }
        packet = packet.addAttribute(dictionary.createAttribute(-1, 33, random.generateSeed((dataSize % 200) - 2)));

        return packet;
    }

    @Test
    void toDatagramMaxPacketSize() throws RadiusPacketException {
        // test max length 4096
        RadiusRequest maxSizeRequest = RadiusRequest.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList());
        maxSizeRequest = addBytesToPacket(maxSizeRequest, 4096);

        final ByteBuf byteBuf = new DatagramPacket(maxSizeRequest.encodeRequest("mySecret").toByteBuf(), new InetSocketAddress(0))
                .content();

        assertEquals(4096, byteBuf.readableBytes());
        assertEquals(4096, byteBuf.getShort(2));

        // test length 4097
        final RadiusRequest oversizeRequest = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> addBytesToPacket(oversizeRequest, 4097));

        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    void testToDatagram() throws RadiusPacketException {
        final InetSocketAddress address = new InetSocketAddress(random.nextInt(65535));
        final byte[] proxyState = random.generateSeed(198);

        final RadiusRequest request = RadiusRequest.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, 33, proxyState))
                .addAttribute(dictionary.createAttribute(-1, 33, random.generateSeed(198)));

        final RadiusRequest encoded = request.encodeRequest("mySecret");

        final DatagramPacket datagram = new DatagramPacket(encoded.toByteBuf(), address);

        assertEquals(address, datagram.recipient());

        // packet
        final byte[] packet = datagram.content().copy().array();
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
    }

}
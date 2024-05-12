package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.RfcAttributeTypes.USER_NAME;
import static org.tinyradius.core.packet.PacketType.*;

class RadiusRequestTest {

    private static final int HEADER_LENGTH = 20;

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final InetSocketAddress remoteAddress = new InetSocketAddress(0);

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
    void createRequest() throws RadiusPacketException {
        final AccessRequest accessRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());
        assertEquals(ACCESS_REQUEST, accessRequest.getType());

        final RadiusRequest coaRequest = RadiusRequest.create(dictionary, COA_REQUEST, (byte) 2, null, Collections.emptyList());
        assertEquals(COA_REQUEST, coaRequest.getType());
        assertEquals(GenericRequest.class, coaRequest.getClass());

        final RadiusRequest accountingRequest = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 3, null, Collections.emptyList());
        assertEquals(ACCOUNTING_REQUEST, accountingRequest.getType());
        assertEquals(AccountingRequest.class, accountingRequest.getClass());
    }

    @Test
    void accountingRequestFromDatagram() throws RadiusPacketException {
        final String user = "user1";
        final String sharedSecret = "sharedSecret1";

        final AccountingRequest rawRequest = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList())
                .addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagramPacket = new DatagramPacket(request.toByteBuf(), remoteAddress);
        final AccountingRequest packet = (AccountingRequest) RadiusRequest.fromDatagram(dictionary, datagramPacket)
                .decodeRequest(sharedSecret);

        assertEquals(ACCOUNTING_REQUEST, packet.getType());
        assertEquals(rawRequest.getId(), packet.getId());
        assertEquals(rawRequest.getAttribute(USER_NAME), packet.getAttribute(USER_NAME));
    }

    @Test
    void accountingRequestBadAuthFromDatagram() throws RadiusPacketException {
        final String user = "user1";
        final String sharedSecret = "sharedSecret1";

        final AccountingRequest rawRequest = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList())
                .addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket originalDatagram = new DatagramPacket(request.toByteBuf(), remoteAddress);

        final byte[] array = originalDatagram.content().copy().array();
        array[4] = 0; // corrupt authenticator

        final DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(array), originalDatagram.recipient());

        final RadiusRequest newRequest = RadiusRequest.fromDatagram(dictionary, datagramPacket);
        final RadiusPacketException radiusPacketException = assertThrows(RadiusPacketException.class,
                () -> newRequest.decodeRequest(sharedSecret));

        assertTrue(radiusPacketException.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void accessRequestFromDatagram() throws RadiusPacketException {
        final String user = "user1";
        final String password = "myPassword";
        final String sharedSecret = "sharedSecret1";

        final AccessRequestPap rawRequest = (AccessRequestPap)
                ((AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withPapPassword(password)
                        .addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagramPacket = new DatagramPacket(request.toByteBuf(), remoteAddress);
        final AccessRequest radiusPacket = (AccessRequest) RadiusRequest.fromDatagram(dictionary, datagramPacket)
                .decodeRequest(sharedSecret);

        assertEquals(ACCESS_REQUEST, radiusPacket.getType());

        final AccessRequestPap packet = (AccessRequestPap) radiusPacket;
        assertEquals(rawRequest.getId(), packet.getId());
        assertEquals(rawRequest.getAttribute(USER_NAME), packet.getAttribute(USER_NAME));
        assertEquals(rawRequest.getPassword(), packet.getPassword());
    }


    @Test
    void fromMaxSizeRequestDatagram() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        // test max length 4096
        AccountingRequest rawRequest = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList());
        rawRequest = (AccountingRequest) addBytesToPacket(rawRequest, 4096);
        final RadiusRequest maxSizeRequest = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagram = new DatagramPacket(maxSizeRequest.toByteBuf(), new InetSocketAddress(0));
        assertEquals(4096, datagram.content().readableBytes());

        final RadiusRequest result = RadiusRequest.fromDatagram(dictionary, datagram)
                .decodeRequest(sharedSecret);

        assertEquals(maxSizeRequest.getType(), result.getType());
        assertEquals(maxSizeRequest.getId(), result.getId());
        assertArrayEquals(maxSizeRequest.toBytes(), result.toBytes());

        assertEquals(maxSizeRequest.getAttributes(33).size(), result.getAttributes(33).size());

        // reconvert to check if bytes match
        assertArrayEquals(datagram.content().copy().array(), new DatagramPacket(result.toByteBuf(), new InetSocketAddress(0)).content().copy().array());
    }

    @Test
    void fromOverSizeRequestDatagram() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";

        // make 4090 octet packet
        AccountingRequest packet = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList());
        packet = (AccountingRequest) addBytesToPacket(packet, 4090);

        final byte[] validBytes = new DatagramPacket(packet.encodeRequest(sharedSecret).toByteBuf(), new InetSocketAddress(0))
                .content().copy().array();
        assertEquals(4090, validBytes.length);

        // create 7 octet attribute
        final byte[] attribute = dictionary.createAttribute(-1, 33, random.generateSeed(5)).toByteArray();
        assertEquals(7, attribute.length);

        // append attribute
        final ByteBuf buffer = Unpooled.buffer(4097, 4097)
                .writeBytes(validBytes)
                .writeBytes(attribute)
                .setShort(2, 4097);

        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> RadiusRequest.fromDatagram(dictionary, new DatagramPacket(buffer, new InetSocketAddress(0))));
        assertTrue(exception.getMessage().contains("too long"));
    }
}
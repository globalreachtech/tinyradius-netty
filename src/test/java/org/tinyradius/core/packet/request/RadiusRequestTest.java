package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.RadiusPacketException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.packet.PacketType.*;

class RadiusRequestTest {

    private static final int HEADER_LENGTH = 20;
    private static final byte USER_NAME = 1;

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final InetSocketAddress remoteAddress = new InetSocketAddress(0);

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
    void createRequest() {
        RadiusRequest accessRequest = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());
        assertEquals(ACCESS_REQUEST, accessRequest.getType());
        assertTrue(accessRequest instanceof AccessRequest); // don't care about subclass

        RadiusRequest coaRequest = RadiusRequest.create(dictionary, COA_REQUEST, (byte) 2, null, Collections.emptyList());
        assertEquals(COA_REQUEST, coaRequest.getType());
        assertEquals(GenericRequest.class, coaRequest.getClass());

        RadiusRequest accountingRequest = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 3, null, Collections.emptyList());
        assertEquals(ACCOUNTING_REQUEST, accountingRequest.getType());
        assertEquals(AccountingRequest.class, accountingRequest.getClass());
    }

    @Test
    void accountingRequestFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        AccountingRequest rawRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        rawRequest.addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = request.toDatagram(remoteAddress);
        RadiusRequest packet = RadiusRequest.fromDatagram(dictionary, datagramPacket);
        packet.decodeRequest(sharedSecret);

        assertEquals(ACCOUNTING_REQUEST, packet.getType());
        assertTrue(packet instanceof AccountingRequest);
        assertEquals(rawRequest.getId(), packet.getId());
        assertEquals(rawRequest.getAttribute(USER_NAME), packet.getAttribute(USER_NAME));
    }

    @Test
    void accountingRequestBadAuthFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String sharedSecret = "sharedSecret1";

        AccountingRequest rawRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        rawRequest.addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket originalDatagram = request.toDatagram(remoteAddress);

        final byte[] array = originalDatagram.content().array();
        array[4] = 0; // corrupt authenticator

        final DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(array), originalDatagram.recipient());

        final RadiusRequest newRequest = RadiusRequest.fromDatagram(dictionary, datagramPacket);
        final RadiusPacketException radiusPacketException = assertThrows(RadiusPacketException.class,
                () -> newRequest.decodeRequest(sharedSecret));

        assertTrue(radiusPacketException.getMessage().toLowerCase().contains("authenticator check failed"));
    }

    @Test
    void accessRequestFromDatagram() throws RadiusPacketException {
        String user = "user1";
        String password = "myPassword";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap rawRequest = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList())
                .withPassword(password);
        rawRequest.addAttribute(USER_NAME, user);
        final RadiusRequest request = rawRequest.encodeRequest(sharedSecret);

        DatagramPacket datagramPacket = request.toDatagram(remoteAddress);
        RadiusRequest radiusPacket = RadiusRequest.fromDatagram(dictionary, datagramPacket)
                .decodeRequest(sharedSecret);

        assertEquals(ACCESS_REQUEST, radiusPacket.getType());
        assertTrue(radiusPacket instanceof AccessRequest);

        AccessRequestPap packet = (AccessRequestPap) radiusPacket;
        assertEquals(rawRequest.getId(), packet.getId());
        assertEquals(rawRequest.getAttribute(USER_NAME), packet.getAttribute(USER_NAME));
        assertEquals(rawRequest.getPassword(), packet.getPassword());
    }


    @Test
    void fromMaxSizeRequestDatagram() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        // test max length 4096
        RadiusRequest rawRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        rawRequest = addBytesToPacket(rawRequest, 4096);
        final RadiusRequest maxSizeRequest = rawRequest.encodeRequest(sharedSecret);

        final DatagramPacket datagram = maxSizeRequest.toDatagram(new InetSocketAddress(0));
        assertEquals(4096, datagram.content().readableBytes());

        RadiusRequest result = RadiusRequest.fromDatagram(dictionary, datagram);
        result.decodeRequest(sharedSecret);

        assertEquals(maxSizeRequest.getType(), result.getType());
        assertEquals(maxSizeRequest.getId(), result.getId());
        assertArrayEquals(maxSizeRequest.getAuthenticator(), result.getAuthenticator());
        assertArrayEquals(maxSizeRequest.getAttributeBytes(), result.getAttributeBytes());

        assertEquals(maxSizeRequest.filterAttributes((byte) 33).size(), result.filterAttributes((byte) 33).size());

        // reconvert to check if bytes match
        assertArrayEquals(datagram.content().array(), result.toDatagram(new InetSocketAddress(0)).content().array());
    }

    @Test
    void fromOverSizeRequestDatagram() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        // make 4090 octet packet
        RadiusRequest packet = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        packet = addBytesToPacket(packet, 4090);

        final byte[] validBytes = packet.encodeRequest(sharedSecret).toDatagram(new InetSocketAddress(0))
                .content().copy().array();
        assertEquals(4090, validBytes.length);

        // create 7 octet attribute
        final byte[] attribute = dictionary.createAttribute(-1, (byte) 33, random.generateSeed(5)).toByteArray();
        assertEquals(7, attribute.length);

        // append attribute
        final ByteBuf buffer = Unpooled.buffer(4097, 4097)
                .writeBytes(validBytes)
                .writeBytes(attribute)
                .setShort(2, 4097);

        final RadiusPacketException exception = assertThrows(RadiusPacketException.class,
                () -> RadiusRequest.fromDatagram(dictionary, new DatagramPacket(buffer, new InetSocketAddress(0))));

        assertTrue(exception.getMessage().contains("packet too long"));
    }

}
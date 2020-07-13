package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.NestedAttributeHolder;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.RadiusRequest;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public interface RadiusPacket<T extends RadiusPacket<T>> extends NestedAttributeHolder<T> {

    Logger packetLogger = LogManager.getLogger();
    int HEADER_LENGTH = 20;
    int MAX_PACKET_LENGTH = 4096;

    static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e); // never happens
        }
    }

    static ByteBuf toByteBuf(RadiusPacket<?> packet) throws RadiusPacketException {
        byte[] attributes = packet.getAttributeBytes();
        int length = HEADER_LENGTH + attributes.length;
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Packet too long");
        if (packet.getAuthenticator() == null)
            throw new RadiusPacketException("Missing authenticator");
        if (packet.getAuthenticator().length != 16)
            throw new RadiusPacketException("Authenticator must be length 16");

        return Unpooled.buffer(length, length)
                .writeByte(packet.getType())
                .writeByte(packet.getId())
                .writeShort(length)
                .writeBytes(packet.getAuthenticator())
                .writeBytes(attributes);
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param byteBuf    DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    static RadiusRequest fromByteBuf(Dictionary dictionary, ByteBuf byteBuf) throws RadiusPacketException {

        final ByteBuffer content = byteBuf.nioBuffer();
        if (content.remaining() < HEADER_LENGTH) {
            throw new RadiusPacketException("Readable bytes is less than header length");
        }

        final byte type = content.get();
        final byte packetId = content.get();
        final int length = content.getShort();

        if (length < HEADER_LENGTH)
            throw new RadiusPacketException("Bad packet: packet too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Bad packet: packet too long (" + length + " bytes)");

        byte[] authenticator = new byte[16];
        content.get(authenticator);

        if (content.remaining() != length - HEADER_LENGTH)
            throw new RadiusPacketException("Bad packet: packet length mismatch");

        return RadiusRequest.create(dictionary, type, packetId, authenticator,
                AttributeHolder.extractAttributes(dictionary, -1, content));
    }

    /**
     * @param recipient destination socket
     * @param sender    source socket, nullable
     * @return converted DatagramPacket
     * @throws RadiusPacketException if packet could not be encoded/serialized to datagram
     */
    default DatagramPacket toDatagram(InetSocketAddress recipient, InetSocketAddress sender) throws RadiusPacketException {
        return new DatagramPacket(toByteBuf(this), recipient, sender);
    }

    /**
     * @param recipient destination socket
     * @return converted DatagramPacket
     * @throws RadiusPacketException if packet could not be encoded/serialized to datagram
     */
    default DatagramPacket toDatagram(InetSocketAddress recipient) throws RadiusPacketException {
        return new DatagramPacket(toByteBuf(this), recipient);
    }

    /**
     * @return Radius packet type
     */
    byte getType();

    /**
     * @return Radius packet identifier
     */
    byte getId();

    /**
     * Returns the authenticator for this Radius packet.
     * <p>
     * For a Radius packet read from a stream, this will return the
     * authenticator sent by the server.
     * <p>
     * For a new Radius packet to be sent, this will return the authenticator created,
     * or null if no authenticator has been created yet.
     *
     * @return authenticator, 16 bytes
     */
    byte[] getAuthenticator();

    /**
     * @return list of RadiusAttributes in packet
     */
    @Override
    List<RadiusAttribute> getAttributes();

    /**
     * @return the dictionary this Radius packet uses.
     */
    @Override
    Dictionary getDictionary();

    /**
     * @param sharedSecret shared secret
     * @param requestAuth  request authenticator if verifying response,
     *                     otherwise set to 16 zero octets
     * @throws RadiusPacketException if authenticator check fails
     */
    default void verifyPacketAuth(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final byte[] expectedAuth = genHashedAuth(sharedSecret, requestAuth);
        final byte[] auth = getAuthenticator();
        if (auth == null)
            throw new RadiusPacketException("Packet Authenticator check failed - authenticator missing");

        if (auth.length != 16)
            throw new RadiusPacketException("Packet Authenticator check failed - must be 16 octets, actual " + auth.length);

        if (!Arrays.equals(expectedAuth, auth)) {
            // find attributes that should be encoded but aren't
            final boolean decodedAlready = getAttributes().stream()
                    .filter(a -> a.getAttributeTemplate()
                            .map(AttributeTemplate::encryptEnabled)
                            .orElse(false))
                    .anyMatch(a -> !a.isEncoded());

            if (decodedAlready)
                packetLogger.info("Skipping Packet Authenticator check - attributes have been decrypted already");
            else
                throw new RadiusPacketException("Packet Authenticator check failed - bad authenticator or shared secret");
        }
    }

    /**
     * Generates an authenticator for a Radius packet.
     * <p>
     * Note: 'this' packet authenticator is ignored, only requestAuth param is used.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  request authenticator if hashing for response,
     *                     otherwise set to 16 zero octets
     * @return new 16 byte response authenticator
     */
    default byte[] genHashedAuth(String sharedSecret, byte[] requestAuth) {
        requireNonNull(requestAuth, "Authenticator cannot be null");
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        final byte[] attributeBytes = getAttributeBytes();
        final int length = HEADER_LENGTH + attributeBytes.length;

        final MessageDigest md5 = getMd5Digest();
        md5.update(getType());
        md5.update(getId());
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuth);
        md5.update(attributeBytes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }
}

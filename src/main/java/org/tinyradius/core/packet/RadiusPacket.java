package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.NestedAttributeHolder;
import org.tinyradius.core.attribute.type.RadiusAttribute;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public interface RadiusPacket<T extends RadiusPacket<T>> extends NestedAttributeHolder<T> {

    int HEADER_LENGTH = 20;
    int MAX_PACKET_LENGTH = 4096;

    static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e); // never happens
        }
    }

    /**
     * Returns header of this buffer's starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@link #HEADER_LENGTH}).
     */
    static ByteBuf readHeader(ByteBuf data) throws RadiusPacketException {
        final int length = data.readableBytes();
        if (length < HEADER_LENGTH)
            throw new RadiusPacketException("Bad packet: parsable bytes too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Bad packet: parsable bytes too long (" + length + " bytes)");

        final ByteBuf header = data.readSlice(20);
        final short declaredLength = header.getShort(2);

        if (length != declaredLength)
            throw new RadiusPacketException("Bad packet: packet length mismatch, " +
                    "parsable bytes (" + length + ")  does not match declared length (" + declaredLength + ")");

        return header;
    }

    /**
     * @param type       packet type
     * @param id         packet id
     * @param auth       nullable 16-byte array
     * @param attributes packet attributes, used to calculate packet length for header
     * @return ByteBuf with 20 readable bytes
     * @throws RadiusPacketException packet validation exceptions
     */
    static ByteBuf buildHeader(byte type, byte id, byte[] auth, List<RadiusAttribute> attributes) throws RadiusPacketException {
        if (auth != null && auth.length != 16) // length check only if not null
            throw new RadiusPacketException("Packet Authenticator must be 16 octets, actual: " + auth.length);

        final int attributeLen = attributes.stream()
                .map(RadiusAttribute::getData)
                .mapToInt(ByteBuf::readableBytes)
                .sum();

        return Unpooled.buffer(HEADER_LENGTH, HEADER_LENGTH)
                .writeByte(type)
                .writeByte(id)
                .writeShort(attributeLen + HEADER_LENGTH)
                .writeBytes(auth == null ? new byte[16] : auth);
    }

    ByteBuf getHeader();

    /**
     * @return Radius packet type
     */
    byte getType();

    /**
     * @return Radius packet id
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

    default ByteBuf toByteBuf() {
        return Unpooled.wrappedBuffer(getHeader(), getAttributeByteBuf());
    }

    default byte[] toBytes() {
        return toByteBuf().copy().array();
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

        final byte[] attributeBytes = getAttributeByteBuf().copy().array();
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

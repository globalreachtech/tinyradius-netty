package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.NestedAttributeHolder;
import org.tinyradius.core.attribute.type.RadiusAttribute;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A RADIUS packet.
 *
 * @param <T> The type of the packet
 */
public interface RadiusPacket<T extends RadiusPacket<T>> extends NestedAttributeHolder<T> {

    /**
     * The length of the RADIUS packet header.
     */
    int HEADER_LENGTH = 20;
    /**
     * The maximum length of a RADIUS packet.
     */
    int MAX_PACKET_LENGTH = 4096;

    /**
     * Returns a new MD5 message digest.
     *
     * @return a new MD5 message digest
     */
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
     *
     * @param data byte array to parse
     * @return ByteBuf with 20 readable bytes
     * @throws RadiusPacketException if data is incorrect size or the length
     *                               field does not match the packet size.
     */
    @NonNull
    static ByteBuf readHeader(@NonNull ByteBuf data) throws RadiusPacketException {
        int length = data.readableBytes();
        if (length < HEADER_LENGTH)
            throw new RadiusPacketException("Bad packet: parsable bytes too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Bad packet: parsable bytes too long (" + length + " bytes)");

        var header = data.readSlice(20);
        int declaredLength = header.getShort(2);

        if (length != declaredLength)
            throw new RadiusPacketException("Bad packet: packet length mismatch, " +
                    "parsable bytes (" + length + ")  does not match declared length (" + declaredLength + ")");

        return header;
    }

    /**
     * Builds a RADIUS packet header.
     *
     * @param type       packet type
     * @param id         packet id
     * @param auth       16-byte array, defaults to empty byte[16] if null
     * @param attributes packet attributes, used to calculate packet length for header
     * @return ByteBuf with 20 readable bytes
     * @throws RadiusPacketException packet validation exceptions
     */
    @NonNull
    static ByteBuf buildHeader(byte type, byte id, byte @Nullable [] auth, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        if (auth != null && auth.length != 16) // length check only if not null
            throw new RadiusPacketException("Packet Authenticator must be 16 octets, actual: " + auth.length);

        int attributeLen = attributes.stream()
                .map(RadiusAttribute::getData)
                .mapToInt(ByteBuf::readableBytes)
                .sum();

        return Unpooled.buffer(HEADER_LENGTH, HEADER_LENGTH)
                .writeByte(type)
                .writeByte(id)
                .writeShort(attributeLen + HEADER_LENGTH)
                .writeBytes(auth == null ? new byte[16] : auth);
    }

    /**
     * Returns the header of the packet.
     *
     * @return the header of the packet
     */
    @NonNull
    ByteBuf getHeader();

    /**
     * Returns the Radius packet type.
     *
     * @return Radius packet type
     */
    default byte getType() {
        return getHeader().getByte(0);
    }

    /**
     * Returns the Radius packet id.
     *
     * @return Radius packet id
     */
    default byte getId() {
        return getHeader().getByte(1);
    }

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
    @Nullable
    default byte[] getAuthenticator() {
        var array = getHeader().slice(4, 16).copy().array();
        return Arrays.equals(array, new byte[array.length]) ?
                null : array;
    }

    /**
     * Returns the length of the packet.
     *
     * @return the length of the packet
     */
    default int getLength() {
        return getHeader().readableBytes() + getAttributeByteBuf().readableBytes();
    }

    /**
     * Returns the packet as a ByteBuf.
     *
     * @return the packet as a ByteBuf
     */
    @NonNull
    default ByteBuf toByteBuf() {
        return Unpooled.unreleasableBuffer(
                Unpooled.wrappedBuffer(getHeader(), getAttributeByteBuf()));
    }

    /**
     * Returns the packet as a ByteBuffer.
     *
     * @return the packet as a ByteBuffer
     */
    @NonNull
    default ByteBuffer toByteBuffer() {
        return toByteBuf().nioBuffer();
    }

    /**
     * Returns the packet as a byte array.
     *
     * @return the packet as a byte array
     */
    @NonNull
    default byte[] toBytes() {
        return toByteBuf().copy().array();
    }

    /**
     * Generates an authenticator for a Radius packet.
     * <p>
     * Note: 'this' packet authenticator is ignored, only requestAuth param is used.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  request authenticator if hashing for response, defaults to empty byte[16] if null
     * @return new 16-byte response authenticator
     */
    @NonNull
    default byte[] genHashedAuth(@NonNull String sharedSecret, byte @Nullable [] requestAuth) {
        if (sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        byte[] attributeBytes = getAttributeByteBuf().copy().array();
        int length = HEADER_LENGTH + attributeBytes.length;

        var md5 = getMd5Digest();
        md5.update(getType());
        md5.update(getId());
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuth == null ? new byte[16] : requestAuth);
        md5.update(attributeBytes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }
}

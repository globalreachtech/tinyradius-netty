package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.*;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
public abstract class BaseRadiusPacket<T extends RadiusPacket<T>> implements RadiusPacket<T> {

    private static final int HEADER_LENGTH = 20;
    private static final int CHILD_VENDOR_ID = -1;

    private final ByteBuf header;
    private final List<RadiusAttribute> attributes;

    private final Dictionary dictionary;

    public BaseRadiusPacket(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        this.dictionary = Objects.requireNonNull(dictionary, "Dictionary is null");
        this.header = Objects.requireNonNull(header);
        this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));

        if (header.readableBytes() != HEADER_LENGTH)
            throw new IllegalArgumentException("Packet header must be length " + HEADER_LENGTH + ", actual: " + header.readableBytes());

        final int length = getHeader().readableBytes() + getAttributeByteBuf().readableBytes();
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Packet too long - length max " + MAX_PACKET_LENGTH + ", actual: " + length);

        final short declaredLength = header.getShort(2);
        if (length != declaredLength)
            throw new RadiusPacketException("Packet length mismatch, " +
                    "actual length (" + length + ")  does not match declared length (" + declaredLength + ")");
    }

    @Override
    public int getChildVendorId() {
        return CHILD_VENDOR_ID;
    }

    @Override
    public ByteBuf getHeader() {
        return header;
    }

    @Override
    public byte getId() {
        return header.getByte(1);
    }

    @Override
    public byte getType() {
        return header.getByte(0);
    }

    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public byte[] getAuthenticator() {
        final byte[] array = header.slice(4, 16).copy().array();
        return Arrays.equals(array, new byte[array.length]) ?
                null : array;
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public T withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf newHeader = RadiusPacket.buildHeader(getType(), getId(), getAuthenticator(), attributes);
        return with(newHeader, attributes);
    }

    public T withAuthAttributes(byte[] auth, List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf newHeader = RadiusPacket.buildHeader(getType(), getId(), auth, attributes);
        return with(newHeader, attributes);
    }

    /**
     * Naive with(), does not recalculate packet lengths in header.
     */
    protected abstract T with(ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException;

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append(PacketType.getPacketTypeName(getType()));
        s.append(", ID ");
        s.append(getId());
        for (RadiusAttribute attr : getAttributes()) {
            s.append("\n");
            s.append(attr.toString());
        }
        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseRadiusPacket)) return false;
        final BaseRadiusPacket<?> that = (BaseRadiusPacket<?>) o;
        return header.equals(that.header) &&
                attributes.equals(that.attributes) &&
                dictionary.equals(that.dictionary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, attributes, dictionary);
    }
}

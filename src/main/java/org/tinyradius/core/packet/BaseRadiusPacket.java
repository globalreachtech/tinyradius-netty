package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
            throw new RadiusPacketException("Packet length max " + MAX_PACKET_LENGTH + ", actual: " + length);

        final short declaredLength = header.getShort(2);
        if (length != declaredLength)
            throw new RadiusPacketException("Packet length mismatch, " +
                    "actual length (" + length + ")  does not match declared length (" + declaredLength + ")");
    }

    protected ByteBuf headerWithAuth(byte[] auth) {
        return getHeader().copy().setBytes(4, auth);
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
        final byte[] auth = new byte[16];
        header.getBytes(4, auth);
        return auth;
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

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

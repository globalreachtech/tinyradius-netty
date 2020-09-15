package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import javax.xml.bind.DatatypeConverter;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
public class OctetsAttribute implements RadiusAttribute {

    private final Dictionary dictionary;

    private final ByteBuf data;
    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    public OctetsAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        this.dictionary = requireNonNull(dictionary, "Dictionary not set");
        this.vendorId = vendorId;
        this.data = requireNonNull(data, "Attribute data not set");

        final int actualLength = data.readableBytes();
        if (actualLength > 255)
            throw new IllegalArgumentException("Attribute too long, max 255 octets, actual: " + actualLength);

        final Optional<Vendor> vendor = dictionary.getVendor(vendorId);
        final int typeSize = vendor.map(Vendor::getTypeSize).orElse(1);
        final int lengthSize = vendor.map(Vendor::getLengthSize).orElse(1);

        final int length = extractLength(typeSize, lengthSize);
        if (length != actualLength)
            throw new IllegalArgumentException("Attribute declared length is " + length + ", actual length: " + actualLength);
    }

    private int extractLength(int typeSize, int lengthSize) {
        switch (lengthSize) {
            case 0:
                return data.readableBytes();
            case 2:
                return data.getShort(typeSize);
            case 1:
            default:
                return Byte.toUnsignedInt(data.getByte(typeSize)); // max 255
        }
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    /**
     * @return RFC2868 Tag
     */
    @Override
    public Optional<Byte> getTag() {
        return isTagged() ?
                Optional.of(data.getByte(getHeaderSize())) :
                Optional.empty();
    }

    @Override
    public byte[] getValue() {
        final int offset = getHeaderSize() + getTagSize();
        return data.slice(offset, data.readableBytes() - offset)
                .copy().array();
    }

    @Override
    public String getValueString() {
        return "0x" + DatatypeConverter.printHexBinary(getValue());
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public ByteBuf getData() {
        return data;
    }

    @Override
    public String toString() {
        final String tag = getTag()
                .map(t -> ":" + t)
                .orElse("");
        return getAttributeName() + tag + " = " + getValueString();
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, requestAuth, secret) :
                this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OctetsAttribute)) return false;
        OctetsAttribute that = (OctetsAttribute) o;
        return getVendorId() == that.getVendorId() &&
                data.equals(that.data);
        /*
         * https://netty.io/4.1/api/io/netty/buffer/ByteBuf.html#equals-java.lang.Object-
         * Determines if the content of the specified buffer is identical to the content of this array. 'Identical' here means:
         * the size of the contents of the two buffers are same and
         * every single byte of the content of the two buffers are same.
         */
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, getVendorId());
    }

    public static byte[] stringHexParser(String value) {
        return DatatypeConverter.parseHexBinary(value);
    }
}

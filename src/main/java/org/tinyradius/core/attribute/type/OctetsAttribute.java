package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import jakarta.xml.bind.DatatypeConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
@Getter
@EqualsAndHashCode
public class OctetsAttribute implements RadiusAttribute {

    public static final RadiusAttributeFactory<OctetsAttribute> FACTORY = new Factory();

    @EqualsAndHashCode.Exclude
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
    public String toString() {
        final String tag = getTag()
                .map(t -> ":" + t)
                .orElse("");
        return getAttributeName() + tag + "=" + getValueString();
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, requestAuth, secret) :
                this;
    }

    public static byte[] stringHexParser(String value) {
        return DatatypeConverter.parseHexBinary(value);
    }

    private static class Factory implements RadiusAttributeFactory<OctetsAttribute> {

        @Override
        public OctetsAttribute newInstance(Dictionary dictionary, int vendorId, ByteBuf value) {
            return new OctetsAttribute(dictionary, vendorId, value);
        }

        @Override
        public byte[] parse(Dictionary dictionary, int vendorId, int type, String value) {
            return stringHexParser(value);
        }
    }

}

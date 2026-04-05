package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.Optional;

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

    public OctetsAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        this.dictionary = dictionary;
        this.vendorId = vendorId;
        this.data = data;

        int actualLength = data.readableBytes();
        if (actualLength > 255)
            throw new IllegalArgumentException("Attribute too long, max 255 octets, actual: " + actualLength);

        var vendor = dictionary.getVendor(vendorId);
        int typeSize = vendor.map(Vendor::typeSize).orElse(1);
        int lengthSize = vendor.map(Vendor::lengthSize).orElse(1);

        int length = extractLength(typeSize, lengthSize);
        if (length != actualLength)
            throw new IllegalArgumentException("Attribute declared length is " + length + ", actual length: " + actualLength);
    }

    private int extractLength(int typeSize, int lengthSize) {
        return switch (lengthSize) {
            case 0 -> data.readableBytes();
            case 2 -> data.getShort(typeSize);
            default -> Byte.toUnsignedInt(data.getByte(typeSize)); // max 255
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Optional<Byte> getTag() {
        return isTagged() ?
                Optional.of(data.getByte(getHeaderSize())) :
                Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public byte[] getValue() {
        int offset = getHeaderSize() + getTagSize();
        return data.slice(offset, data.readableBytes() - offset)
                .copy().array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getValueString() {
        return "0x" + HEX_FORMAT.formatHex(getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toString() {
        var tag = getTag()
                .map(t -> ":" + t)
                .orElse("");
        return getAttributeName() + tag + "=" + getValueString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public RadiusAttribute encode(@NonNull byte[] requestAuth, @NonNull String secret) throws RadiusPacketException {
        var template = getAttributeTemplate();
        if (template.isPresent()) {
            return template.get().encode(this, requestAuth, secret);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public RadiusAttribute decode(@NonNull byte[] requestAuth, @NonNull String secret) throws RadiusPacketException {
        var template = getAttributeTemplate();
        if (template.isPresent()) {
            return template.get().decode(this, requestAuth, secret);
        }
        return this;
    }

    /**
     * Parses a hex string into a byte array.
     *
     * @param value hex string
     * @return byte array
     */
    @NonNull
    public static byte[] stringHexParser(@NonNull String value) {
        return HEX_FORMAT.parseHex(value);
    }

    private static class Factory implements RadiusAttributeFactory<OctetsAttribute> {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public OctetsAttribute newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
            return new OctetsAttribute(dictionary, vendorId, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public byte[] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
            return stringHexParser(value);
        }
    }

}

package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.Objects;
import java.util.Optional;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
public class OctetsAttribute implements RadiusAttribute {

    /**
     * Default factory for creating {@link OctetsAttribute} instances.
     */
    public static final RadiusAttributeFactory<OctetsAttribute> FACTORY = new Factory();

    private final Dictionary dictionary;

    private final ByteBuf data;
    private final int vendorId; // for (Vendor-Specific) sub-attributes, otherwise -1

    /**
     * Creates a new OctetsAttribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param data       the attribute data
     */
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
    public int getVendorId() {
        return vendorId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ByteBuf getData() {
        return data;
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
    public byte @NonNull [] getValue() {
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
     * Parses a hex string into a byte array.
     *
     * @param value hex string
     * @return byte array
     */
    public static byte @NonNull [] stringHexParser(@NonNull String value) {
        return HEX_FORMAT.parseHex(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OctetsAttribute that)) return false;
        return vendorId == that.vendorId && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, vendorId);
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
        public byte @NonNull [] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
            return stringHexParser(value);
        }
    }

}

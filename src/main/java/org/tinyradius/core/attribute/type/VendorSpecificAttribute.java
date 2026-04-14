package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.ArrayList;
import java.util.List;

import static org.tinyradius.core.attribute.AttributeTypes.VENDOR_SPECIFIC;


/**
 * Vendor-Specific attribute. Both an attribute itself and an attribute container for sub-attributes.
 */
public class VendorSpecificAttribute extends OctetsAttribute implements AttributeHolder<VendorSpecificAttribute> {

    /**
     * Default factory for creating {@link VendorSpecificAttribute} instances.
     */
    public static final RadiusAttributeFactory<VendorSpecificAttribute> FACTORY = new Factory();

    // derived from byteBuf
    private final List<RadiusAttribute> attributes;

    /**
     * Constructs a new Vendor-Specific attribute.
     *
     * @param dictionary    dictionary to use for (sub)attributes
     * @param childVendorId vendor ID of the sub-attributes
     * @param attributes    sub-attributes held
     */
    public VendorSpecificAttribute(@NonNull Dictionary dictionary, int childVendorId, @NonNull List<RadiusAttribute> attributes) {
        this(dictionary, attributes, toByteBuf(childVendorId, attributes));
        if (attributes.stream().anyMatch(a -> a.getVendorId() != childVendorId))
            throw new IllegalArgumentException("Vendor-Specific attribute sub-attributes must have same vendorId as VSA childVendorId: " + childVendorId);
    }

    /**
     * Creates a new Vendor-Specific attribute.
     *
     * @param dictionary dictionary to use for (sub)attributes
     * @param vendorId   must be -1 (VSA is always a top level attribute)
     * @param data       data to parse for childVendorId and sub-attributes
     */
    public VendorSpecificAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        this(dictionary,
                AttributeHolder.readAttributes(dictionary, validate(data).getInt(2), data.slice(6, data.readableBytes() - 6)),
                data);
        if (vendorId != -1)
            throw new IllegalArgumentException("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: " + vendorId);
    }

    /**
     * Internal constructor for VendorSpecificAttribute.
     *
     * @param dictionary dictionary to use for (sub)attributes
     * @param attributes sub-attributes held
     * @param data       equivalent of childVendorId + subattribute data in byte array form
     */
    private VendorSpecificAttribute(@NonNull Dictionary dictionary, @NonNull List<RadiusAttribute> attributes, @NonNull ByteBuf data) {
        super(dictionary, -1, data);
        this.attributes = List.copyOf(attributes);
        if (data.getByte(0) != VENDOR_SPECIFIC)
            throw new IllegalArgumentException("Vendor-Specific attribute attributeId should always be 26, " +
                    "actual: " + data.getByte(0));
        validate(data);
    }

    /**
     * Returns the vendor ID of the sub-attributes.
     *
     * @return the child vendor ID
     */
    @Override
    public int getChildVendorId() {
        return getData().getInt(2);
    }

    /**
     * Returns the list of sub-attributes held by this VSA.
     *
     * @return the attributes
     */
    @Override
    @NonNull
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    @NonNull
    private static ByteBuf validate(@NonNull ByteBuf data) {
        int len = data.readableBytes();
        if (len < 7) // VSA headers are 6 bytes
            throw new IllegalArgumentException("Vendor-Specific attribute should be greater than 6 octets, actual: " + len);
        return data;
    }

    /**
     * Converts vendor ID and attributes to a ByteBuf.
     *
     * @param childVendorId vendor ID
     * @param attributes    list of attributes
     * @return the ByteBuf
     */
    @NonNull
    private static ByteBuf toByteBuf(int childVendorId, @NonNull List<RadiusAttribute> attributes) {
        var attributesBytes = AttributeHolder.attributesToBytes(attributes);
        var header = Unpooled.buffer(6, 6)
                .writeByte(VENDOR_SPECIFIC)
                .writeByte(attributesBytes.readableBytes() + 6)
                .writeInt(childVendorId);
        return Unpooled.wrappedBuffer(header, attributesBytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public VendorSpecificAttribute withAttributes(@NonNull List<RadiusAttribute> attributes) {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<RadiusAttribute> flatten() {
        return new ArrayList<>(getAttributes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public VendorSpecificAttribute encode(byte @NonNull [] requestAuth, @NonNull String secret) throws RadiusPacketException {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), encodeAttributes(requestAuth, secret));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public VendorSpecificAttribute decode(byte @NonNull [] requestAuth, @NonNull String secret) throws RadiusPacketException {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), decodeAttributes(requestAuth, secret));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toString() {
        var sb = new StringBuilder()
                .append("Vendor-Specific: Vendor ID ").append(getChildVendorId());
        getDictionary()
                .getVendor(getChildVendorId())
                .map(Vendor::name)
                .ifPresent(s -> sb.append(" (").append(s).append(")"));
        for (var sa : getAttributes()) {
            sb.append("\n  ").append(sa.toString());
        }
        return sb.toString();
    }

    private static class Factory implements RadiusAttributeFactory<VendorSpecificAttribute> {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public VendorSpecificAttribute newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
            return new VendorSpecificAttribute(dictionary, vendorId, value);
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

package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
@Getter
@EqualsAndHashCode(callSuper = true) // fields `attributes`/`childVendorId` are derived from byteBuf
public class VendorSpecificAttribute extends OctetsAttribute implements AttributeHolder<VendorSpecificAttribute> {

    public static final RadiusAttributeFactory<VendorSpecificAttribute> FACTORY = new Factory();

    @EqualsAndHashCode.Exclude
    private final int childVendorId;

    @EqualsAndHashCode.Exclude
    private final List<RadiusAttribute> attributes;

    /**
     * @param dictionary dictionary to use for (sub)attributes
     * @param vendorId   ignored, VSAs should always be -1 (top level attribute)
     * @param data       data to parse for childVendorId and sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        this(dictionary, validate(data).getInt(2),
                AttributeHolder.readAttributes(dictionary, data.getInt(2), data.slice(6, data.readableBytes() - 6)),
                data);
        if (vendorId != -1)
            throw new IllegalArgumentException("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: " + vendorId);
    }

    // get around `Call to 'this()' must be first statement in constructor body`
    private static ByteBuf validate(ByteBuf data) {
        final int len = data.readableBytes();
        if (len < 7) // VSA headers are 6 bytes
            throw new IllegalArgumentException("Vendor-Specific attribute should be greater than 6 octets, actual: " + len);

        return data;
    }

    /**
     * Constructs a new Vendor-Specific attribute
     *
     * @param dictionary    dictionary to use for (sub)attributes
     * @param childVendorId vendor ID of the sub-attributes
     * @param attributes    sub-attributes held
     */
    public VendorSpecificAttribute(Dictionary dictionary, int childVendorId, List<RadiusAttribute> attributes) {
        this(dictionary, childVendorId, attributes, toByteBuf(childVendorId, attributes));
        final boolean mismatchVendorId = attributes.stream()
                .map(RadiusAttribute::getVendorId)
                .anyMatch(id -> id != childVendorId);
        if (mismatchVendorId)
            throw new IllegalArgumentException("Vendor-Specific attribute sub-attributes must have same vendorId as VSA childVendorId: " + childVendorId);
    }

    private static ByteBuf toByteBuf(int childVendorId, List<RadiusAttribute> attributes) {
        final ByteBuf attributesBytes = AttributeHolder.attributesToBytes(attributes);
        final ByteBuf header = Unpooled.buffer(6, 6)
                .writeByte(VENDOR_SPECIFIC)
                .writeByte(attributesBytes.readableBytes() + 6)
                .writeInt(childVendorId);
        return Unpooled.wrappedBuffer(header, attributesBytes);
    }

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param childVendorId vendor ID of the sub-attributes
     * @param attributes    sub-attributes held
     * @param data          equivalent of childVendorId + subattribute data in byte array form
     */
    private VendorSpecificAttribute(Dictionary dictionary, int childVendorId, List<RadiusAttribute> attributes, ByteBuf data) {
        super(dictionary, -1, data);
        this.childVendorId = childVendorId;
        this.attributes = List.copyOf(attributes);

        if (data.getByte(0) != VENDOR_SPECIFIC)
            throw new IllegalArgumentException("Vendor-Specific attribute attributeId should always be 26, " +
                    "actual: " + data.getByte(0));

        validate(data);
    }

    @Override
    public VendorSpecificAttribute withAttributes(List<RadiusAttribute> attributes) {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), attributes);
    }

    @Override
    public List<RadiusAttribute> flatten() {
        return new ArrayList<>(getAttributes());
    }

    @Override
    public VendorSpecificAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), encodeAttributes(requestAuth, secret));
    }

    @Override
    public VendorSpecificAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), decodeAttributes(requestAuth, secret));
    }

    @Override
    public String toString() {
        var sb = new StringBuilder()
                .append("Vendor-Specific: Vendor ID ").append(getChildVendorId());
        getDictionary()
                .getVendor(getChildVendorId())
                .map(Vendor::name)
                .ifPresent(s -> sb.append(" (").append(s).append(")"));
        for (RadiusAttribute sa : getAttributes()) {
            sb.append("\n  ").append(sa.toString());
        }
        return sb.toString();
    }

    private static class Factory implements RadiusAttributeFactory<VendorSpecificAttribute> {

        @Override
        public VendorSpecificAttribute newInstance(Dictionary dictionary, int vendorId, ByteBuf value) {
            return new VendorSpecificAttribute(dictionary, vendorId, value);
        }

        @Override
        public byte[] parse(Dictionary dictionary, int vendorId, int type, String value) {
            return stringHexParser(value);
        }
    }
}

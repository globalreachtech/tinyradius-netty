package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Vendor-Specific attribute. Both an attribute itself and an attribute container for sub-attributes.
 */
public class VendorSpecificAttribute extends OctetsAttribute implements AttributeHolder<VendorSpecificAttribute> {

    public static final byte VENDOR_SPECIFIC = 26;

    private final int childVendorId;
    private final List<RadiusAttribute> attributes;

    /**
     * @param dictionary dictionary to use for (sub)attributes
     * @param vendorId   ignored, VSAs should always be -1 (top level attribute)
     * @param data       data to parse for childVendorId and sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        this(dictionary, data.getInt(0),
                AttributeHolder.readAttributes(dictionary, data.getInt(0), data.slice(4, data.readableBytes() - 4)),
                data);
        if (vendorId != -1)
            throw new IllegalArgumentException("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: " + vendorId);
    }

    /**
     * Constructs a new Vendor-Specific attribute
     *
     * @param dictionary    dictionary to use for (sub)attributes
     * @param childVendorId vendor ID of the sub-attributes
     * @param attributes    sub-attributes held
     */
    public VendorSpecificAttribute(Dictionary dictionary, int childVendorId, List<RadiusAttribute> attributes) {
        this(dictionary, childVendorId, attributes, Unpooled.wrappedBuffer(
                Unpooled.buffer(4, 4).writeInt(childVendorId),
                AttributeHolder.attributesToBytes(attributes)));
        final boolean mismatchVendorId = attributes.stream()
                .map(RadiusAttribute::getVendorId)
                .anyMatch(id -> id != childVendorId);
        if (mismatchVendorId)
            throw new IllegalArgumentException("Vendor-Specific attribute sub-attributes must have same vendorId as VSA childVendorId");
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
        this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));

        if (data.getByte(0) != VENDOR_SPECIFIC)
            throw new IllegalArgumentException("Vendor-Specific attribute attributeId should always be 26, " +
                    "actual: " + data.getByte(0));

        final int len = data.readableBytes();
        if (len < 7) // VSA headers are 6 bytes
            throw new IllegalArgumentException("Vendor-Specific attribute should be greater than 6 octets, actual: " + len);
    }

    @Override
    public int getChildVendorId() {
        return childVendorId;
    }

    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
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
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), encodeAttributes(requestAuth, secret));
    }

    @Override
    public RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return new VendorSpecificAttribute(getDictionary(), getChildVendorId(), decodeAttributes(requestAuth, secret));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vendor-Specific: Vendor ID ").append(getChildVendorId());
        getDictionary()
                .getVendor(getChildVendorId())
                .map(Vendor::getName)
                .ifPresent(s -> sb.append(" (").append(s).append(")"));
        for (RadiusAttribute sa : getAttributes()) {
            sb.append("\n  ").append(sa.toString());
        }
        return sb.toString();
    }

}

package org.tinyradius.attribute;

import io.netty.buffer.Unpooled;
import org.tinyradius.dictionary.Dictionary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tinyradius.attribute.Attributes.extractAttributes;

/**
 * This class represents a "Vendor-Specific" attribute.
 */
public class VendorSpecificAttribute extends RadiusAttribute implements AttributeHolder {

    public static final int VENDOR_SPECIFIC = 26;

    private final List<RadiusAttribute> attributes;

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param vendorId      ignored, parsed from data directly
     * @param attributeType ignored, should always be Vendor-Specific (26)
     * @param data          data to parse for vendorId and sub-attributes
     */
    VendorSpecificAttribute(Dictionary dictionary, int vendorId, int attributeType, byte[] data) {
        this(dictionary, extractVendorId(data), extractAttributes(dictionary, extractVendorId(data), data, 4));
    }

    /**
     * @param data byte array, length minimum 4
     * @return vendorId
     */
    private static int extractVendorId(byte[] data) {
        return ByteBuffer.wrap(data).getInt();
    }

    VendorSpecificAttribute(Dictionary dictionary, int vendorId, int ignored, String ignored2) {
        this(dictionary, vendorId, new ArrayList<>());
    }

    VendorSpecificAttribute(Dictionary dictionary, int vendorId, List<RadiusAttribute> subAttributes) {
        super(dictionary, vendorId, VENDOR_SPECIFIC, new byte[0]);
        this.attributes = subAttributes;
    }

    /**
     * Constructs a new Vendor-Specific attribute to be sent.
     *
     * @param dictionary dictionary to use for (sub)attributes
     * @param vendorId   vendor ID of the sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId) {
        this(dictionary, vendorId, new ArrayList<>());
    }

    /**
     * Adds a sub-attribute to this attribute.
     *
     * @param attribute sub-attribute to add
     */
    @Override
    public void addAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() != getVendorId())
            throw new IllegalArgumentException("Attribute vendor ID doesn't match");

        attributes.add(attribute);
    }

    /**
     * Removes the specified sub-attribute from this attribute.
     *
     * @param attribute RadiusAttribute to remove
     */
    @Override
    public void removeAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() != getVendorId())
            throw new IllegalArgumentException("Attribute vendor ID doesn't match");

        attributes.remove(attribute);
    }

    /**
     * Returns the list of sub-attributes.
     *
     * @return List of RadiusAttributes
     */
    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Renders this attribute as a byte array.
     */
    @Override
    public byte[] toByteArray() {
        final byte[] attributeBytes = getAttributeBytes();
        final int len = attributeBytes.length + 6;

        if (len < 7)
            throw new RuntimeException("Vendor-Specific attribute should be greater than 6 octets, actual: " + len);

        if (len > 255)
            throw new RuntimeException("Vendor-Specific attribute should be less than 256 octets, actual: " + len);

        return Unpooled.buffer(len, len)
                .writeByte(VENDOR_SPECIFIC)
                .writeByte(len)
                .writeInt(getVendorId())
                .writeBytes(attributeBytes)
                .array();
    }

    /**
     * Returns a string representation for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vendor-Specific: ");
        String vendorName = getDictionary().getVendorName(getVendorId());
        if (vendorName != null) {
            sb.append(vendorName)
                    .append(" (").append(getVendorId()).append(")");
        } else {
            sb.append("vendor ID ").append(getVendorId());
        }
        for (RadiusAttribute sa : getAttributes()) {
            sb.append("\n  ").append(sa.toString());
        }
        this.getAttributeMap();
        return sb.toString();
    }

    @Override
    public Map<String, String> getAttributeMap() {
        // VSAs don't hold any actual data, we only care about the sub-attributes
        return AttributeHolder.super.getAttributeMap();
    }
}

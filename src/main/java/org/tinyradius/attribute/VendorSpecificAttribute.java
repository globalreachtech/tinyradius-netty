package org.tinyradius.attribute;

import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.util.AttributeHolder;
import org.tinyradius.dictionary.Dictionary;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.tinyradius.attribute.util.Attributes.extractAttributes;

/**
 * This class represents a "Vendor-Specific" attribute.
 */
public class VendorSpecificAttribute extends RadiusAttribute implements AttributeHolder.Writable {

    public static final byte VENDOR_SPECIFIC = 26;

    private final int requiredVendorId;
    private final List<RadiusAttribute> attributes;

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param vendorId      ignored, VSAs should always be -1 (top level attribute)
     * @param attributeType ignored, should always be Vendor-Specific (26)
     * @param data          data as hex to parse for vendorId and sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId, int attributeType, String data) {
        this(dictionary, vendorId, attributeType, DatatypeConverter.parseHexBinary(data));
    }

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param vendorId      ignored, VSAs should always be -1 (top level attribute)
     * @param attributeType ignored, should always be Vendor-Specific (26)
     * @param data          data to parse for vendorId and sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId, int attributeType, byte[] data) {
        this(dictionary, extractAttributes(dictionary, extractVendorId(data), data, 4), extractVendorId(data));
    }

    /**
     * @param data byte array, length minimum 4
     * @return vendorId
     */
    private static int extractVendorId(byte[] data) {
        return ByteBuffer.wrap(data).getInt();
    }

    /**
     * Constructs a new Vendor-Specific attribute to be sent.
     *
     * @param dictionary       dictionary to use for (sub)attributes
     * @param subAttributes    sub-attributes held
     * @param requiredVendorId vendor ID of the sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, List<RadiusAttribute> subAttributes, int requiredVendorId) {
        super(dictionary, -1, VENDOR_SPECIFIC, new byte[0]);
        this.requiredVendorId = requiredVendorId;
        this.attributes = subAttributes;
    }

    @Override
    public int getChildVendorId() {
        return requiredVendorId;
    }

    /**
     * Adds a sub-attribute to this attribute.
     *
     * @param attribute sub-attribute to add
     */
    @Override
    public void addAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() != getChildVendorId())
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
        if (attribute.getVendorId() != getChildVendorId())
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
            throw new IllegalStateException("Vendor-Specific attribute should be greater than 6 octets, actual: " + len);

        if (len > 255)
            throw new IllegalStateException("Vendor-Specific attribute should be less than 256 octets, actual: " + len);

        return Unpooled.buffer(len, len)
                .writeByte(VENDOR_SPECIFIC)
                .writeByte(len)
                .writeInt(getChildVendorId())
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
        String vendorName = getDictionary().getVendorName(getChildVendorId());
        if (vendorName != null) {
            sb.append(vendorName)
                    .append(" (").append(getChildVendorId()).append(")");
        } else {
            sb.append("Vendor ID ").append(getChildVendorId());
        }
        for (RadiusAttribute sa : getAttributes()) {
            sb.append("\n  ").append(sa.toString());
        }
        return sb.toString();
    }

    @Override
    public List<RadiusAttribute> flatten() {
        // VSAs don't hold any actual data, we only care about the sub-attributes
        return new ArrayList<>(getAttributes());
    }
}

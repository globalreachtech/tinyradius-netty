package org.tinyradius.attribute;

import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.util.AttributeHolder;
import org.tinyradius.dictionary.Dictionary;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.tinyradius.attribute.util.Attributes.extractAttributes;

/**
 * This class represents a "Vendor-Specific" attribute. Both an attribute itself and an attribute container.
 * <p>
 * Immutable. To mutate, see {@link Builder}
 */
public class VendorSpecificAttribute extends RadiusAttribute implements AttributeHolder {

    public static final byte VENDOR_SPECIFIC = 26;

    private final int childVendorId;
    private final List<RadiusAttribute> attributes;

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param vendorId      ignored, VSAs should always be -1 (top level attribute)
     * @param attributeType ignored, should always be Vendor-Specific (26)
     * @param data          data as hex to parse for childVendorId and sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId, int attributeType, String data) {
        this(dictionary, vendorId, attributeType, DatatypeConverter.parseHexBinary(data));
    }

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param vendorId      ignored, VSAs should always be -1 (top level attribute)
     * @param attributeType ignored, should always be Vendor-Specific (26)
     * @param data          data to parse for childVendorId and sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, int vendorId, int attributeType, byte[] data) {
        this(dictionary, extractAttributes(dictionary, vendorId(data), data, 4), vendorId(data), data);
        if (vendorId != -1)
            throw new IllegalStateException("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: " + vendorId);
        if (attributeType != 26)
            throw new IllegalStateException("Vendor-Specific attribute attributeType should always be 26, actual: " + attributeType);
    }

    /**
     * @param data byte array, length minimum 4
     * @return vendorId
     */
    private static int vendorId(byte[] data) {
        return ByteBuffer.wrap(data).getInt();
    }

    /**
     * Constructs a new Vendor-Specific attribute
     *
     * @param dictionary    dictionary to use for (sub)attributes
     * @param attributes    sub-attributes held
     * @param childVendorId vendor ID of the sub-attributes
     */
    public VendorSpecificAttribute(Dictionary dictionary, List<RadiusAttribute> attributes, int childVendorId) {
        this(dictionary, attributes, childVendorId, Unpooled.buffer()
                .writeInt(childVendorId)
                .writeBytes(AttributeHolder.attributesToBytes(attributes))
                .copy().array());
    }

    /**
     * @param dictionary    dictionary to use for (sub)attributes
     * @param attributes    sub-attributes held
     * @param childVendorId vendor ID of the sub-attributes
     * @param data          equivalent of childVendorId + subattribute data in byte array form
     */
    private VendorSpecificAttribute(Dictionary dictionary, List<RadiusAttribute> attributes, int childVendorId, byte[] data) {
        super(dictionary, -1, VENDOR_SPECIFIC, data);
        this.childVendorId = childVendorId;
        this.attributes = Collections.unmodifiableList(attributes);

        final int len = data.length + 2;
        if (len < 7) // VSA headers are 6 bytes
            throw new IllegalStateException("Vendor-Specific attribute should be greater than 6 octets, actual: " + len);
    }

    @Override
    public int getChildVendorId() {
        return childVendorId;
    }

    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
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
        return new ArrayList<>(getAttributes());
    }

    public Builder toBuilder() {
        return new Builder()
                .setDictionary(getDictionary())
                .setChildVendorId(getChildVendorId())
                .setAttributes(getAttributes());
    }

    /**
     * Builder for attribute manipulation before building immutable attribute
     */
    public static class Builder implements AttributeHolder.Writable {

        private Dictionary dictionary;
        private List<RadiusAttribute> attributes = new ArrayList<>();
        private int childVendorId;

        public VendorSpecificAttribute build() {
            return new VendorSpecificAttribute(dictionary, attributes, childVendorId);
        }

        @Override
        public int getChildVendorId() {
            return childVendorId;
        }

        @Override
        public Dictionary getDictionary() {
            return dictionary;
        }

        @Override
        public List<RadiusAttribute> getAttributes() {
            return attributes;
        }

        public Builder setDictionary(Dictionary dictionary) {
            this.dictionary = dictionary;
            return this;
        }

        public Builder setAttributes(List<RadiusAttribute> attributes) {
            this.attributes = new ArrayList<>(attributes);
            return this;
        }

        public Builder setChildVendorId(int childVendorId) {
            this.childVendorId = childVendorId;
            return this;
        }
    }
}

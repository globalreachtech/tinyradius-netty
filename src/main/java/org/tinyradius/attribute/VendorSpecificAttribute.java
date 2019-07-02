package org.tinyradius.attribute;

import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a "Vendor-Specific" attribute.
 */
public class VendorSpecificAttribute extends RadiusAttribute {

    /**
     * Radius attribute type code for Vendor-Specific
     */
    public static final int VENDOR_SPECIFIC = 26;

    /**
     * Sub attributes. Only set if isRawData == false.
     */
    private List<RadiusAttribute> subAttributes = new ArrayList<>();

    /**
     * Vendor ID of sub-attributes.
     */
    private int childVendorId;

    /**
     * Constructs an empty Vendor-Specific attribute that can be read from a
     * Radius packet.
     */
    public VendorSpecificAttribute() {
    }

    /**
     * Constructs a new Vendor-Specific attribute to be sent.
     *
     * @param vendorId vendor ID of the sub-attributes
     */
    public VendorSpecificAttribute(int vendorId) {
        setAttributeType(VENDOR_SPECIFIC);
        setChildVendorId(vendorId);
    }

    /**
     * Sets the vendor ID of the child attributes.
     *
     * @param childVendorId vendor ID of sub-attributes
     */
    public void setChildVendorId(int childVendorId) {
        this.childVendorId = childVendorId;
    }

    /**
     * Returns the vendor ID of the sub-attributes.
     *
     * @return vendor ID of sub attributes
     */
    public int getChildVendorId() {
        return childVendorId;
    }

    /**
     * Also copies the new dictionary to sub-attributes.
     *
     * @param dictionary dictionary to set
     * @see RadiusAttribute#setDictionary(Dictionary)
     */
    public void setDictionary(Dictionary dictionary) {
        super.setDictionary(dictionary);
        for (RadiusAttribute attr : subAttributes) {
            attr.setDictionary(dictionary);
        }
    }

    /**
     * Adds a sub-attribute to this attribute.
     *
     * @param attribute sub-attribute to add
     */
    public void addSubAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() != getChildVendorId())
            throw new IllegalArgumentException(
                    "sub attribute has incorrect vendor ID");

        subAttributes.add(attribute);
    }

    /**
     * Adds a sub-attribute with the specified name to this attribute.
     *
     * @param name  name of the sub-attribute
     * @param value value of the sub-attribute
     * @throws IllegalArgumentException invalid sub-attribute name or value
     */
    public void addSubAttribute(String name, String value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("type name is empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("value is empty");

        AttributeType type = getDictionary().getAttributeTypeByName(name);
        if (type == null)
            throw new IllegalArgumentException("unknown attribute type '" + name + "'");
        if (type.getVendorId() == -1)
            throw new IllegalArgumentException("attribute type '" + name + "' is not a Vendor-Specific sub-attribute");
        if (type.getVendorId() != getChildVendorId())
            throw new IllegalArgumentException("attribute type '" + name + "' does not belong to vendor ID " + getChildVendorId());

        RadiusAttribute attribute = createRadiusAttribute(getDictionary(), getChildVendorId(), type.getTypeCode());
        attribute.setAttributeValue(value);
        addSubAttribute(attribute);
    }

    /**
     * Removes the specified sub-attribute from this attribute.
     *
     * @param attribute RadiusAttribute to remove
     */
    public void removeSubAttribute(RadiusAttribute attribute) {
        subAttributes.remove(attribute);
    }

    /**
     * Returns the list of sub-attributes.
     *
     * @return List of RadiusAttribute objects
     */
    public List<RadiusAttribute> getSubAttributes() {
        return subAttributes;
    }

    /**
     * Returns all sub-attributes of this attribut which have the given type.
     *
     * @param attributeType type of sub-attributes to get
     * @return list of RadiusAttribute objects, does not return null
     */
    public List<RadiusAttribute> getSubAttributes(int attributeType) {
        if (attributeType < 1 || attributeType > 255)
            throw new IllegalArgumentException("sub-attribute type out of bounds");

        List<RadiusAttribute> result = new LinkedList<>();
        for (RadiusAttribute a : subAttributes) {
            if (attributeType == a.getAttributeType())
                result.add(a);
        }
        return result;
    }

    /**
     * Returns a sub-attribute of the given type which may only occur once in
     * this attribute.
     *
     * @param type sub-attribute type
     * @return RadiusAttribute object or null if there is no such sub-attribute
     * @throws RuntimeException if there are multiple occurences of the
     *                          requested sub-attribute type
     */
    public RadiusAttribute getSubAttribute(int type) {
        List<RadiusAttribute> attrs = getSubAttributes(type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple sub-attributes of requested type " + type);

        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Returns a single sub-attribute of the given type name.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if the attribute occurs multiple times
     */
    public RadiusAttribute getSubAttribute(String type) {
        if (type == null || type.isEmpty())
            throw new IllegalArgumentException("type name is empty");

        AttributeType t = getDictionary().getAttributeTypeByName(type);
        if (t == null)
            throw new IllegalArgumentException("unknown attribute type name '" + type + "'");
        if (t.getVendorId() != getChildVendorId())
            throw new IllegalArgumentException("vendor ID mismatch");

        return getSubAttribute(t.getTypeCode());
    }

    /**
     * Returns the value of the Radius attribute of the given type or null if
     * there is no such attribute.
     *
     * @param type attribute type name
     * @return value of the attribute as a string or null if there is no such
     * attribute
     * @throws IllegalArgumentException if the type name is unknown
     * @throws RuntimeException         attribute occurs multiple times
     */
    public String getSubAttributeValue(String type) {
        RadiusAttribute attr = getSubAttribute(type);
        return attr == null ?
                null : attr.getAttributeValue();
    }

    /**
     * Renders this attribute as a byte array.
     *
     * @see RadiusAttribute#writeAttribute()
     */
    public byte[] writeAttribute() {
        // write vendor ID
        ByteArrayOutputStream bos = new ByteArrayOutputStream(255);
        bos.write(getChildVendorId() >> 24 & 0x0ff);
        bos.write(getChildVendorId() >> 16 & 0x0ff);
        bos.write(getChildVendorId() >> 8 & 0x0ff);
        bos.write(getChildVendorId() & 0x0ff);

        // write sub-attributes
        try {
            for (RadiusAttribute subAttribute : subAttributes) {
                bos.write(subAttribute.writeAttribute());
            }
        } catch (IOException ioe) {
            // occurs never
            throw new RuntimeException("error writing data", ioe);
        }

        // check data length
        byte[] attrData = bos.toByteArray();
        int len = attrData.length;
        if (len > 253)
            throw new RuntimeException("Vendor-Specific attribute too long: " + bos.size());

        // compose attribute
        byte[] attr = new byte[len + 2];
        attr[0] = VENDOR_SPECIFIC; // code
        attr[1] = (byte) (len + 2); // length
        System.arraycopy(attrData, 0, attr, 2, len);
        return attr;
    }

    /**
     * Reads a Vendor-Specific attribute and decodes the internal sub-attribute
     * structure.
     *
     * @see RadiusAttribute#readAttribute(byte[], int)
     */
    @Override
    public void readAttribute(byte[] data, int offset) throws RadiusException {
        int vsaCode = data[offset];
        int vsaLen = ((int) data[offset + 1] & 0x0ff) - 6;

        if (vsaLen < 6)
            throw new RadiusException("Vendor-Specific attribute too short: " + vsaLen);

        if (vsaCode != VENDOR_SPECIFIC)
            throw new RadiusException("not a Vendor-Specific attribute");

        // read vendor ID and vendor data
        int vendorId = (unsignedByteToInt(data[offset + 2]) << 24
                | unsignedByteToInt(data[offset + 3]) << 16
                | unsignedByteToInt(data[offset + 4]) << 8 | unsignedByteToInt(data[offset + 5]));
        setChildVendorId(vendorId);

        // validate sub-attribute structure
        int pos = 0;
        int count = 0;
        while (pos < vsaLen) {
            if (pos + 1 >= vsaLen)
                throw new RadiusException("Vendor-Specific attribute malformed");
            // int vsaSubType = data[(offset + 6) + pos] & 0x0ff;
            int vsaSubLen = data[(offset + 6) + pos + 1] & 0x0ff;
            pos += vsaSubLen;
            count++;
        }
        if (pos != vsaLen)
            throw new RadiusException("Vendor-Specific attribute malformed");

        subAttributes = new ArrayList<>(count);
        pos = 0;
        while (pos < vsaLen) {
            int subtype = data[(offset + 6) + pos] & 0x0ff;
            RadiusAttribute a = createRadiusAttribute(getDictionary(), vendorId, subtype);
            a.readAttribute(data, (offset + 6) + pos);
            subAttributes.add(a);
            int sublength = data[(offset + 6) + pos + 1] & 0x0ff;
            pos += sublength;
        }
    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    /**
     * Returns a string representation for debugging.
     *
     * @see RadiusAttribute#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vendor-Specific: ");
        int vendorId = getChildVendorId();
        String vendorName = getDictionary().getVendorName(vendorId);
        if (vendorName != null) {
            sb.append(vendorName);
            sb.append(" (");
            sb.append(vendorId);
            sb.append(")");
        } else {
            sb.append("vendor ID ");
            sb.append(vendorId);
        }
        for (RadiusAttribute sa : getSubAttributes()) {
            sb.append("\n");
            sb.append(sa.toString());
        }
        return sb.toString();
    }
}

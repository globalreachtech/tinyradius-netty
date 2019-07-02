package org.tinyradius.attribute;

import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import static java.util.Objects.requireNonNull;

/**
 * This class represents a generic Radius attribute. Subclasses implement
 * methods to access the fields of special attributes.
 */
public class RadiusAttribute {

    //todo implement equals/hashcode

    private Dictionary dictionary = DefaultDictionary.INSTANCE;

    private int attributeType = -1;

    private int vendorId = -1; //only for sub-attributes of Vendor-Specific attributes.

    private byte[] attributeData = null;

    public RadiusAttribute() {
    }

    /**
     * Constructs a Radius attribute with the specified type and data.
     *
     * @param type attribute type, see AttributeTypes.*
     * @param data attribute data
     */
    public RadiusAttribute(int type, byte[] data) {
        setAttributeType(type);
        setAttributeData(data);
    }

    /**
     * Returns the data for this attribute.
     *
     * @return attribute data
     */
    public byte[] getAttributeData() {
        return attributeData;
    }

    /**
     * Sets the data for this attribute.
     *
     * @param attributeData attribute data
     */
    public void setAttributeData(byte[] attributeData) {
        this.attributeData = requireNonNull(attributeData, "attribute data is null");
    }

    /**
     * Returns the type of this Radius attribute.
     *
     * @return type code, 0-255
     */
    public int getAttributeType() {
        return attributeType;
    }

    /**
     * Sets the type of this Radius attribute.
     *
     * @param attributeType type code, 0-255
     */
    public void setAttributeType(int attributeType) {
        if (attributeType < 0 || attributeType > 255)
            throw new IllegalArgumentException("attribute type invalid: " + attributeType);
        this.attributeType = attributeType;
    }

    /**
     * Sets the value of the attribute using a string.
     *
     * @param value value as a string
     */
    public void setAttributeValue(String value) {
        throw new RuntimeException("cannot set the value of attribute " + attributeType + " as a string");
    }

    /**
     * @return the value of this attribute as a string.
     */
    public String getAttributeValue() {
        return getHexString(getAttributeData());
    }

    /**
     * Gets the Vendor-Id of the Vendor-Specific attribute this
     * attribute belongs to. Returns -1 if this attribute is not
     * a sub attribute of a Vendor-Specific attribute.
     *
     * @return vendor ID
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * Sets the Vendor-Id of the Vendor-Specific attribute this
     * attribute belongs to. The default value of -1 means this attribute
     * is not a sub attribute of a Vendor-Specific attribute.
     *
     * @param vendorId vendor ID
     */
    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    /**
     * Returns the dictionary this Radius attribute uses.
     *
     * @return Dictionary INSTANCE
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * Sets a custom dictionary to use. If no dictionary is set,
     * the default dictionary is used.
     *
     * @param dictionary Dictionary class to use
     * @see DefaultDictionary
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Returns this attribute encoded as a byte array.
     *
     * @return attribute
     */
    public byte[] writeAttribute() {
        if (getAttributeType() == -1)
            throw new IllegalArgumentException("attribute type not set");
        requireNonNull(attributeData, "attribute data not set");

        byte[] attr = new byte[2 + attributeData.length];
        attr[0] = (byte) getAttributeType();
        attr[1] = (byte) (2 + attributeData.length);
        System.arraycopy(attributeData, 0, attr, 2, attributeData.length);
        return attr;
    }

    /**
     * Reads in this attribute from the passed byte array.
     *
     * @param data   input data
     * @param offset byte to start reading from
     * @throws RadiusException malformed packet
     */
    public void readAttribute(byte[] data, int offset) throws RadiusException {
        int length = data[offset + 1] & 0x0ff;
        if (length < 2)
            throw new RadiusException("attribute length too small: " + length);
        int attrType = data[offset] & 0x0ff;
        byte[] attrData = new byte[length - 2];
        System.arraycopy(data, offset + 2, attrData, 0, length - 2);
        setAttributeType(attrType);
        setAttributeData(attrData);
    }

    public String toString() {
        String name;

        // determine attribute name
        AttributeType at = getAttributeTypeObject();
        if (at != null)
            name = at.getName();
        else if (getVendorId() != -1)
            name = "Unknown-Sub-Attribute-" + getAttributeType();
        else
            name = "Unknown-Attribute-" + getAttributeType();

        // indent sub attributes
        if (getVendorId() != -1)
            name = "  " + name;

        return name + ": " + getAttributeValue();
    }

    /**
     * Retrieves an AttributeType object for this attribute.
     *
     * @return AttributeType object for (sub-)attribute or null
     */
    public AttributeType getAttributeTypeObject() {
        if (getVendorId() != -1)
            return dictionary.getAttributeTypeByCode(getVendorId(), getAttributeType());

        return dictionary.getAttributeTypeByCode(getAttributeType());
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type.
     *
     * @param dictionary    Dictionary to use
     * @param vendorId      vendor ID or -1
     * @param attributeType attribute type
     * @return RadiusAttribute object
     */
    public static RadiusAttribute createRadiusAttribute(Dictionary dictionary, int vendorId, int attributeType) {
        RadiusAttribute attribute = new RadiusAttribute();

        AttributeType<?> at = dictionary.getAttributeTypeByCode(vendorId, attributeType);
        if (at != null && at.getAttributeClass() != null) {
            try {
                attribute = at.getAttributeClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // error instantiating class - should not occur
                e.printStackTrace();
            }
        }

        attribute.setAttributeType(attributeType);
        attribute.setDictionary(dictionary);
        attribute.setVendorId(vendorId);
        return attribute;
    }

    /**
     * Creates a Radius attribute, including vendor-specific
     * attributes. The default dictionary is used.
     *
     * @param vendorId      vendor ID or -1
     * @param attributeType attribute type
     * @return RadiusAttribute INSTANCE
     */
    public static RadiusAttribute createRadiusAttribute(int vendorId, int attributeType) {
        Dictionary dictionary = DefaultDictionary.INSTANCE;
        return createRadiusAttribute(dictionary, vendorId, attributeType);
    }

    /**
     * Creates a Radius attribute. The default dictionary is
     * used.
     *
     * @param attributeType attribute type
     * @return RadiusAttribute INSTANCE
     */
    public static RadiusAttribute createRadiusAttribute(int attributeType) {
        Dictionary dictionary = DefaultDictionary.INSTANCE;
        return createRadiusAttribute(dictionary, -1, attributeType);
    }

    /**
     * Returns the byte array as a hex string in the format
     * "0x1234".
     *
     * @param data byte array
     * @return hex string
     */
    private static String getHexString(byte[] data) {
        StringBuilder hex = new StringBuilder("0x");
        if (data != null)
            for (byte datum : data) {
                String digit = Integer.toString(datum & 0x0ff, 16);
                if (digit.length() < 2)
                    hex.append('0');
                hex.append(digit);
            }
        return hex.toString();
    }
}

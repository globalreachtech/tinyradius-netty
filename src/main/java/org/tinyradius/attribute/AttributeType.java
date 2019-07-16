package org.tinyradius.attribute;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * Represents a Radius attribute type.
 */
public class AttributeType {

    private final int vendorId;
    private final int typeCode;
    private final String name;
    private final Attributes.ByteArrayConstructor byteArrayConstructor;
    private final Attributes.StringConstructor stringConstructor;
    private final Map<Integer, String> enumeration = new HashMap<>();

    /**
     * Create a new attribute type.
     *
     * @param attributeType Radius attribute type code
     * @param name          Attribute type name
     * @param typeStr       string|octets|integer|date|ipaddr|ipv6addr|ipv6prefix
     */
    public AttributeType(int attributeType, String name, String typeStr) {
        this(-1, attributeType, name, typeStr);
    }

    /**
     * Constructs a Vendor-Specific sub-attribute type.
     *
     * @param vendorId      vendor ID
     * @param attributeType sub-attribute type code
     * @param name          sub-attribute name
     * @param rawDataType   string|octets|integer|date|ipaddr|ipv6addr|ipv6prefix
     */
    public AttributeType(int vendorId, int attributeType, String name, String rawDataType) {
        if (attributeType < 1 || attributeType > 255)
            throw new IllegalArgumentException("attribute type code out of bounds");
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is empty");
        requireNonNull(rawDataType, "data type is null");
        this.vendorId = vendorId;
        this.typeCode = attributeType;
        this.name = name;

        final String dataType = rawDataType.toLowerCase();

        if (dataType.equals("vsa") || attributeType == VENDOR_SPECIFIC) {
            byteArrayConstructor = VendorSpecificAttribute::new;
            stringConstructor = VendorSpecificAttribute::new;
            return;
        }

        switch (dataType) {
            case "string":
                byteArrayConstructor = StringAttribute::new;
                stringConstructor = StringAttribute::new;
                break;
            case "integer":
            case "date":
                byteArrayConstructor = IntegerAttribute::new;
                stringConstructor = IntegerAttribute::new;
                break;
            case "ipaddr":
                byteArrayConstructor = IpAttribute.V4::new;
                stringConstructor = IpAttribute.V4::new;
                break;
            case "ipv6addr":
                byteArrayConstructor = IpAttribute.V6::new;
                stringConstructor = IpAttribute.V6::new;
                break;
            case "ipv6prefix":
                byteArrayConstructor = Ipv6PrefixAttribute::new;
                stringConstructor = Ipv6PrefixAttribute::new;
                break;
            case "octets":
            default:
                byteArrayConstructor = RadiusAttribute::new;
                stringConstructor = RadiusAttribute::new;
        }
    }

    /**
     * @return Radius type code for this attribute e.g. '1' (for User-Name)
     */
    public int getTypeCode() {
        return typeCode;
    }

    /**
     * @return name of type e.g. 'User-Name'
     */
    public String getName() {
        return name;
    }

    Attributes.ByteArrayConstructor getByteArrayConstructor() {
        return byteArrayConstructor;
    }

    Attributes.StringConstructor getStringConstructor() {
        return stringConstructor;
    }

    /**
     * @return vendor ID or -1 if not applicable
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * @param value int value
     * @return the name of the given integer value if this attribute
     * is an enumeration, or null if it is not or if the integer value
     * is unknown.
     */
    public String getEnumeration(int value) {
        return enumeration.get(value);
    }

    /**
     * @param value string value
     * @return the number of the given string value if this attribute is
     * an enumeration, or null if it is not or if the string value is unknown.
     */
    public Integer getEnumeration(String value) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("value is empty");
        for (Map.Entry<Integer, String> e : enumeration.entrySet()) {
            if (e.getValue().equals(value))
                return e.getKey();
        }
        return null;
    }

    /**
     * Adds a name for an integer value of this attribute.
     *
     * @param num  number that shall get a name
     * @param name the name for this number
     */
    public void addEnumerationValue(int num, String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is empty");
        enumeration.put(num, name);
    }

    public String toString() {
        String s = getTypeCode() + "/" + getName() + ": " + byteArrayConstructor.getClass();
        if (getVendorId() != -1)
            s += " (vendor " + getVendorId() + ")";
        return s;
    }
}

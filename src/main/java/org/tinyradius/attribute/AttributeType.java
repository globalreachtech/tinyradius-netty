package org.tinyradius.attribute;

import org.tinyradius.util.RadiusException;

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
    private final Attributes.PacketParser packetParser;
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
     * @param rawDataType      string|octets|integer|date|ipaddr|ipv6addr|ipv6prefix
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
            packetParser = VendorSpecificAttribute::parse;
            byteArrayConstructor = VendorSpecificAttribute::new;
            stringConstructor = VendorSpecificAttribute::new;
            return;
        }

        switch (dataType) {
            case "string":
                packetParser = StringAttribute::parse;
                byteArrayConstructor = StringAttribute::new;
                stringConstructor = StringAttribute::new;
                break;
            case "integer":
            case "date":
                packetParser = IntegerAttribute::parse;
                byteArrayConstructor = IntegerAttribute::new;
                stringConstructor = IntegerAttribute::new;
                break;
            case "ipaddr":
                packetParser = IpAttribute.V4::parse;
                byteArrayConstructor = IpAttribute.V4::new;
                stringConstructor = IpAttribute.V4::new;
                break;
            case "ipv6addr":
                packetParser = IpAttribute.V6::parse;
                byteArrayConstructor = IpAttribute.V6::new;
                stringConstructor = IpAttribute.V6::new;
                break;
            case "ipv6prefix":
                packetParser = Ipv6PrefixAttribute::parse;
                byteArrayConstructor = Ipv6PrefixAttribute::new;
                stringConstructor = Ipv6PrefixAttribute::new;
                break;
            case "octets":
            default:
                packetParser = RadiusAttribute::parse;
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

    /**
     * Retrieves the RadiusAttribute descendant class which represents
     * attributes of this type.
     *
     * @return class
     */
    public Attributes.PacketParser getPacketParser() {
        return packetParser;
    }

    public Attributes.ByteArrayConstructor getByteArrayConstructor() {
        return byteArrayConstructor;
    }

    public Attributes.StringConstructor getStringConstructor() {
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

    /**
     * @return string for debugging
     */
    public String toString() {
        String s = getTypeCode() + "/" + getName() + ": " + packetParser.getClass();
        if (getVendorId() != -1)
            s += " (vendor " + getVendorId() + ")";
        return s;
    }
}

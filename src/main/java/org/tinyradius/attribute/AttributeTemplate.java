package org.tinyradius.attribute;

import org.tinyradius.attribute.type.*;
import org.tinyradius.dictionary.Dictionary;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.type.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * Represents a Radius attribute type.
 */
public class AttributeTemplate {

    private final int vendorId;
    private final byte type;
    private final String name;
    private final short encrypt = 0;

    private final String dataType;
    private final ByteArrayConstructor byteArrayConstructor;
    private final StringConstructor stringConstructor;

    private final Map<Integer, String> int2str = new HashMap<>();
    private final Map<String, Integer> str2int = new HashMap<>();

    /**
     * Create a new attribute type.
     *
     * @param vendorId    vendor ID or -1 if N/A
     * @param type        sub-attribute type code
     * @param name        sub-attribute name
     * @param rawDataType string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     */
    public AttributeTemplate(int vendorId, int type, String name, String rawDataType) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("Attribute type code out of bounds");
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        requireNonNull(rawDataType, "Data type is null");
        this.vendorId = vendorId;
        this.type = (byte) type;
        this.name = name;

        dataType = rawDataType.toLowerCase();

        if (dataType.equals("vsa") || (vendorId == -1 && type == VENDOR_SPECIFIC)) {
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

    public RadiusAttribute create(Dictionary dictionary, byte[] data) {
        return byteArrayConstructor.newInstance(dictionary, vendorId, type, data);
    }

    public RadiusAttribute create(Dictionary dictionary, String data) {
        return stringConstructor.newInstance(dictionary, vendorId, type, data);
    }

    /**
     * @return Radius type code for this attribute e.g. '1' (for User-Name)
     */
    public byte getType() {
        return type;
    }

    /**
     * @return name of type e.g. 'User-Name'
     */
    public String getName() {
        return name;
    }

    /**
     * @return vendor ID or -1 if not applicable
     */
    public int getVendorId() {
        return vendorId;
    }

    public String getDataType() {
        return dataType;
    }

    /**
     * @param value int value
     * @return the name of the given integer value if this attribute
     * is an enumeration, or null if it is not or if the integer value
     * is unknown.
     */
    public String getEnumeration(int value) {
        return int2str.get(value);
    }

    /**
     * @param value string value
     * @return the number of the given string value if this attribute is
     * an enumeration, or null if it is not or if the string value is unknown.
     */
    public Integer getEnumeration(String value) {
        return str2int.get(value);
    }

    /**
     * Adds a name for an integer value of this attribute.
     *
     * @param num  number that shall get a name
     * @param name the name for this number
     */
    public void addEnumerationValue(int num, String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        int2str.put(num, name);
        str2int.put(name, num);
    }

    public String toString() {
        String s = toUnsignedInt(getType()) + "/" + getName() + ": " + getDataType();
        if (getVendorId() != -1)
            s += " (Vendor " + getVendorId() + ")";
        return s;
    }

    interface ByteArrayConstructor {
        RadiusAttribute newInstance(Dictionary dictionary, int vendorId, byte type, byte[] data);
    }

    interface StringConstructor {
        RadiusAttribute newInstance(Dictionary dictionary, int vendorId, byte type, String data);
    }
}

package org.tinyradius.attribute;

import org.tinyradius.attribute.encrypt.EncryptMethod;
import org.tinyradius.attribute.type.AttributeType;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.encrypt.EncryptMethod.*;
import static org.tinyradius.attribute.type.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * Represents a Radius attribute type.
 */
public class AttributeTemplate {

    private final int vendorId;
    private final byte type;
    private final String name;
    private final EncryptMethod encrypt;

    private final String dataType;
    private final AttributeType rawType;
    private final AttributeType encodedType;

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
        this(vendorId, type, name, rawDataType, (byte) 0);
    }

    /**
     * Create a new attribute type.
     *
     * @param vendorId    vendor ID or -1 if N/A
     * @param type        sub-attribute type code
     * @param name        sub-attribute name
     * @param rawDataType string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     * @param encrypt     encrypt flag as per FreeRadius dictionary format, can be 1/2/3, or 0 for no encryption
     */
    public AttributeTemplate(int vendorId, int type, String name, String rawDataType, byte encrypt) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("Attribute type code out of bounds");
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        requireNonNull(rawDataType, "Data type is null");
        this.vendorId = vendorId;
        this.type = (byte) type;
        this.name = name;
        this.dataType = rawDataType.toLowerCase();

        rawType = vendorId == -1 && type == VENDOR_SPECIFIC ?
                AttributeType.VSA :
                AttributeType.fromDataType(this.dataType);

        if (vendorId == -1 && type == 2) // User-Password
            this.encrypt = RFC2865_USER_PASSWORD;
        else if (vendorId == -1 && type == 1) // Tunnel-Password
            this.encrypt = RFC2868_TUNNEL_PASSWORD;
        else if (vendorId == 529 && type == 214) // Ascend-Send-Secret
            this.encrypt = ASCENT_SEND_SECRET;
        else
            this.encrypt = fromId(encrypt);

        encodedType = this.encrypt == NO_ENCRYPT ?
                rawType : AttributeType.OCTETS;
    }

    public RadiusAttribute create(Dictionary dictionary, byte[] data) {
        return rawType.create(dictionary, vendorId, type, data);
    }

    public RadiusAttribute create(Dictionary dictionary, String data) {
        return rawType.create(dictionary, vendorId, type, data);
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
}

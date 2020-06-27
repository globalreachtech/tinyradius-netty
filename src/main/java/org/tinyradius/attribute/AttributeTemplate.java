package org.tinyradius.attribute;

import org.tinyradius.attribute.encrypt.AttributeCodecType;
import org.tinyradius.attribute.type.AttributeType;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.encrypt.AttributeCodecType.*;
import static org.tinyradius.attribute.type.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * Represents a Radius attribute type.
 */
public class AttributeTemplate {

    private final int vendorId;
    private final byte type;
    private final String name;
    private final boolean hasTag = false; // todo
    private final AttributeCodecType codecType;

    private final String dataType;
    private final AttributeType decodedType;
    private final AttributeType encodedType;

    private final Map<Integer, String> int2str = new HashMap<>();
    private final Map<String, Integer> str2int = new HashMap<>();

    /**
     * Create a new attribute type.
     *
     * @param vendorId    vendor ID or -1 if N/A
     * @param type        sub-attribute type code, as unsigned byte
     * @param name        sub-attribute name
     * @param rawDataType string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     */
    public AttributeTemplate(int vendorId, byte type, String name, String rawDataType) {
        this(vendorId, type, name, rawDataType, (byte) 0);
    }

    /**
     * Create a new attribute type.
     *
     * @param vendorId    vendor ID or -1 if N/A
     * @param type        sub-attribute type code, as unsigned byte
     * @param name        sub-attribute name
     * @param rawDataType string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     * @param encryptFlag encrypt flag as per FreeRadius dictionary format, can be 1/2/3, or 0 for no encryption
     */
    public AttributeTemplate(int vendorId, byte type, String name, String rawDataType, byte encryptFlag) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        requireNonNull(rawDataType, "Data type is null");
        this.vendorId = vendorId;
        this.type = type;
        this.name = name;
        this.dataType = rawDataType.toLowerCase();

        decodedType = vendorId == -1 && type == VENDOR_SPECIFIC ?
                AttributeType.VSA :
                AttributeType.fromDataType(this.dataType);

        this.codecType = detectAttributeCodec(vendorId, type, encryptFlag);

        this.encodedType = this.codecType == NO_ENCRYPT ?
                decodedType : AttributeType.OCTETS;
    }

    public RadiusAttribute create(Dictionary dictionary, byte[] value) {
        return decodedType.create(dictionary, vendorId, type, value);
    }

    public RadiusAttribute create(Dictionary dictionary, String value) {
        return decodedType.create(dictionary, vendorId, type, value);
    }

    /**
     * Create RadiusAttribute with encoded data.
     * <p>
     * If attribute type has encryption enabled, this parses as OctetsAttribute
     * so contents aren't validated at construction and can be decoded separately
     * later. Otherwise will create same attribute type as {@link #create(Dictionary, byte[])}.
     *
     * @param dictionary  dictionary to use
     * @param encodedData encoded data, attribute data excl. type/length/tag
     * @return new RadiusAttribute
     */
    public RadiusAttribute createEncoded(Dictionary dictionary, byte[] encodedData) {
        return encodedType.create(dictionary, vendorId, type, encodedData);
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

    /**
     * @param attribute   attribute to encode
     * @param secret      shared secret to encode with
     * @param requestAuth (corresponding) request packet authenticator
     * @return attribute with encoded data
     */
    public RadiusAttribute encode(RadiusAttribute attribute, String secret, byte[] requestAuth) throws RadiusPacketException {
        try {
            return createEncoded(attribute.getDictionary(),
                    codecType.getCodec().encode(attribute.getValue(), secret, requestAuth));
        } catch (Exception e) {
            throw new RadiusPacketException("Error encoding attribute " + attribute.toString(), e);
        }
    }

    /**
     * @param attribute   attribute to decode
     * @param secret      shared secret to decode with
     * @param requestAuth (corresponding) request packet authenticator
     * @return attribute with decoded data
     */
    public RadiusAttribute decode(RadiusAttribute attribute, String secret, byte[] requestAuth) throws RadiusPacketException {
        try {
            return create(attribute.getDictionary(),
                    codecType.getCodec().decode(attribute.getValue(), secret, requestAuth));
        } catch (Exception e) {
            throw new RadiusPacketException("Error decoding attribute " + attribute.toString(), e);
        }
    }

    public String toString() {
        String s = toUnsignedInt(getType()) + "/" + getName() + ": " + getDataType();
        if (getVendorId() != -1)
            s += " (Vendor " + getVendorId() + ")";
        return s;
    }

    protected AttributeCodecType detectAttributeCodec(int vendorId, int type, byte encryptFlag) {
        if (vendorId == -1 && type == 2) // User-Password
            return RFC2865_USER_PASSWORD;

        if (vendorId == -1 && type == 69) // Tunnel-Password
            return RFC2868_TUNNEL_PASSWORD;

        if (vendorId == 529 && type == 214) // Ascend-Send-Secret
            return ASCENT_SEND_SECRET;

        return fromId(encryptFlag);
    }
}

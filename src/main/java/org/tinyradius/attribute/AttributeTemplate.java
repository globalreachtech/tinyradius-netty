package org.tinyradius.attribute;

import org.tinyradius.attribute.codec.AttributeCodecType;
import org.tinyradius.attribute.type.AttributeType;
import org.tinyradius.attribute.type.OctetsAttribute;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.attribute.type.decorator.EncodedDecorator;
import org.tinyradius.attribute.type.decorator.TaggedDecorator;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.codec.AttributeCodecType.*;
import static org.tinyradius.attribute.type.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * Represents a Radius attribute type.
 */
public class AttributeTemplate {

    private final int vendorId;
    private final byte type;
    private final String name;
    private final String dataType;

    private final boolean hasTag;
    private final AttributeCodecType codecType;

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
        this(vendorId, type, name, rawDataType, (byte) 0, false);
    }

    /**
     * Create a new attribute type.
     *
     * @param vendorId    vendor ID or -1 if N/A
     * @param type        sub-attribute type code, as unsigned byte
     * @param name        sub-attribute name
     * @param rawDataType string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     * @param encryptFlag encrypt flag as per FreeRadius dictionary format, can be 1/2/3, or 0 for no encryption
     * @param hasTag      whether attribute supports tags, as defined in RFC2868
     */
    public AttributeTemplate(int vendorId, byte type, String name, String rawDataType, byte encryptFlag, boolean hasTag) {
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

        this.hasTag = detectHasTag(vendorId, type, hasTag);
        this.codecType = detectAttributeCodec(vendorId, type, encryptFlag);
        this.encodedType = this.codecType == NO_ENCRYPT ?
                decodedType : AttributeType.OCTETS;
    }

    /**
     * Create RadiusAttribute.
     *
     * @param dictionary dictionary to use
     * @param tag        tag as per RFC2868
     * @param value      value to set attribute
     * @return new RadiusAttribute
     */
    public RadiusAttribute create(Dictionary dictionary, byte tag, byte[] value) {
        return create(decodedType, dictionary, tag, value);
    }

    /**
     * Create RadiusAttribute.
     *
     * @param dictionary dictionary to use
     * @param tag        tag as per RFC2868
     * @param value      value to set attribute
     * @return new RadiusAttribute
     */
    public RadiusAttribute create(Dictionary dictionary, byte tag, String value) {
        return autoWrapTag(tag, decodedType.create(dictionary, vendorId, type, value));
    }

    /**
     * Parse RadiusAttribute from raw byte data.
     * <p>
     * If attribute type has encryption, this will create an OctetsAttribute wrapped in EncoderDecorator,
     * otherwise uses the type specified in dictionary.
     *
     * @param dictionary dictionary to use
     * @param rawData    attribute data to parse excl. type/length
     * @return new RadiusAttribute
     */
    public RadiusAttribute parse(Dictionary dictionary, byte[] rawData) {
        if (hasTag()) {
            if (rawData.length == 0)
                throw new IllegalArgumentException("Attribute data (excl. type, length fields) cannot be empty if attribute requires tag.");

            return autoWrapEncode(
                    autoWrapTag(rawData[0],
                            encodedType.create(dictionary, vendorId, type, Arrays.copyOfRange(rawData, 1, rawData.length))));
        }

        final OctetsAttribute attribute = encodedType.create(dictionary, vendorId, type, rawData);
        return autoWrapEncode(attribute);
    }

    /**
     * Create RadiusAttribute with encoded data.
     * <p>
     * If attribute type supports encryption, this will
     * return an use OctetsAttribute as
     * underlying implementation so contents aren't validated at construction.
     *
     * @param dictionary   dictionary to use
     * @param tag          tag as per RFC2868
     * @param encodedValue encoded data, attribute data excl. type/length/tag
     * @return new RadiusAttribute
     */
    public RadiusAttribute createEncoded(Dictionary dictionary, byte tag, byte[] encodedValue) {
        return autoWrapEncode(create(encodedType, dictionary, tag, encodedValue));
    }

    private RadiusAttribute create(AttributeType attributeType, Dictionary dictionary, byte tag, byte[] value) {
        return autoWrapTag(tag, attributeType.create(dictionary, vendorId, type, value));
    }

    private RadiusAttribute autoWrapTag(byte tag, OctetsAttribute attribute) {
        return hasTag() ?
                new TaggedDecorator(tag, attribute) : attribute;
    }

    private RadiusAttribute autoWrapEncode(RadiusAttribute attribute) {
        return encryptEnabled() ?
                new EncodedDecorator(attribute) : attribute;
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

    /**
     * @return string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @return whether attribute supports Tag field as per RFC2868
     */
    public boolean hasTag() {
        return hasTag;
    }

    public boolean encryptEnabled() {
        return codecType != NO_ENCRYPT;
    }

    /**
     * @return one of AttributeCodecType enum, defaults to NO_ENCRYPT for none
     */
    public AttributeCodecType getCodecType() {
        return codecType;
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
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to encode with
     * @return attribute with encoded data
     * @throws RadiusPacketException errors encoding attribute
     */
    public RadiusAttribute encode(RadiusAttribute attribute, byte[] requestAuth, String secret) throws RadiusPacketException {
        if (!encryptEnabled() || attribute.isEncoded())
            return attribute;

        try {
            return createEncoded(attribute.getDictionary(), attribute.getTag(),
                    codecType.getCodec().encode(attribute.getValue(), requestAuth, secret));
        } catch (Exception e) {
            throw new RadiusPacketException("Error encoding attribute " + attribute.toString(), e);
        }
    }

    /**
     * @param attribute   attribute to decode
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to decode with
     * @return attribute with decoded data
     * @throws RadiusPacketException errors decoding attribute
     */
    public RadiusAttribute decode(RadiusAttribute attribute, byte[] requestAuth, String secret) throws RadiusPacketException {
        if (!attribute.isEncoded())
            return attribute;

        try {
            return create(attribute.getDictionary(), attribute.getTag(),
                    codecType.getCodec().decode(attribute.getValue(), requestAuth, secret));
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

    private static boolean detectHasTag(int vendorId, int type, boolean hasTag) {
        // Tunnel-Password
        return (vendorId == -1 && type == 69) || hasTag;
    }

    private static AttributeCodecType detectAttributeCodec(int vendorId, int type, byte encryptFlag) {
        if (vendorId == -1 && type == 2) // User-Password
            return RFC2865_USER_PASSWORD;

        if (vendorId == -1 && type == 69) // Tunnel-Password
            return RFC2868_TUNNEL_PASSWORD;

        if (vendorId == 529 && type == 214) // Ascend-Send-Secret
            return ASCENT_SEND_SECRET;

        return fromId(encryptFlag);
    }
}

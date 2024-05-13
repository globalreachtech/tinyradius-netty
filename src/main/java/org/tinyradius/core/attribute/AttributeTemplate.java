package org.tinyradius.core.attribute;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.attribute.type.EncodedAttribute;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.RadiusAttributeFactory;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.NONE;
import static org.tinyradius.core.attribute.AttributeTypes.TUNNEL_PASSWORD;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

/**
 * Represents a Radius attribute type.
 */
@Getter
@EqualsAndHashCode
public class AttributeTemplate {

    /**
     * vendor ID or -1 if not applicable
     */
    private final int vendorId;
    /**
     * Radius type code for this attribute e.g. '1' (for User-Name)
     */
    private final int type;
    /**
     * name of type e.g. 'User-Name'
     */
    private final String name;
    /**
     * string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     */
    private final String dataType;

    /**
     * whether attribute supports Tag field as per RFC2868
     */
    private final boolean tagged;
    /**
     * one of AttributeCodecType enum, defaults to NO_ENCRYPT for none
     */
    private final AttributeCodecType codecType;

    @EqualsAndHashCode.Exclude
    private final RadiusAttributeFactory<? extends RadiusAttribute> factory;

    @Getter(NONE)
    @EqualsAndHashCode.Exclude
    private final Map<Integer, String> int2str = new HashMap<>();
    @Getter(NONE)
    @EqualsAndHashCode.Exclude
    private final Map<String, Integer> str2int = new HashMap<>();

    /**
     * Create a new attribute type.
     *
     * @param vendorId    vendor ID or -1 if N/A
     * @param type        sub-attribute type code, as unsigned byte
     * @param name        sub-attribute name
     * @param dataType    string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     * @param encryptFlag encrypt flag as per FreeRadius dictionary format, can be 1/2/3, or default 0 for none
     * @param hasTag      whether attribute supports tags, as defined in RFC2868, default false
     */
    public AttributeTemplate(int vendorId, int type, @NonNull String name, @NonNull String dataType, RadiusAttributeFactory<? extends RadiusAttribute> factory, byte encryptFlag, boolean hasTag) {
        if (name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        this.vendorId = vendorId;
        this.type = type;
        this.name = name;
        this.dataType = dataType.toLowerCase();
        this.factory = factory;
        this.tagged = detectHasTag(vendorId, type, hasTag);
        this.codecType = detectAttributeCodec(vendorId, type, encryptFlag);
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
        return factory.create(dictionary, vendorId, type, tag, value);
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
        return factory.create(dictionary, vendorId, type, tag, value);
    }

    /**
     * Parse RadiusAttribute from raw byte data.
     * <p>
     * If attribute type has encryption, this will create an OctetsAttribute wrapped in EncodedDecorator,
     * otherwise uses the type specified in dictionary.
     *
     * @param dictionary dictionary to use
     * @param data       attribute data to parse incl. type/length
     * @return new RadiusAttribute
     */
    public RadiusAttribute parse(Dictionary dictionary, ByteBuf data) {
        return isEncrypt() ?
                new EncodedAttribute(OctetsAttribute.FACTORY.create(dictionary, vendorId, data)) :
                factory.create(dictionary, vendorId, data);
    }

    /**
     * Create RadiusAttribute with encoded data.
     * <p>
     * If attribute type supports encryption, this will
     * return an OctetsAttribute as underlying implementation
     * so contents aren't validated at construction.
     *
     * @param dictionary   dictionary to use
     * @param tag          tag as per RFC2868
     * @param encodedValue encoded data, attribute data excl. type/length/tag
     * @return new RadiusAttribute
     */
    public RadiusAttribute createEncoded(Dictionary dictionary, byte tag, byte[] encodedValue) {
        return isEncrypt() ?
                new EncodedAttribute(OctetsAttribute.FACTORY.create(dictionary, vendorId, type, tag, encodedValue)) :
                factory.create(dictionary, vendorId, type, tag, encodedValue);
    }

    public boolean isEncrypt() {
        return codecType != NO_ENCRYPT;
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
        // don't wrap in EncodedDecorator if not supported
        if (!isEncrypt() || attribute.isEncoded())
            return attribute;

        try {
            return createEncoded(attribute.getDictionary(), attribute.getTag().orElse((byte) 0),
                    codecType.getCodec().encode(attribute.getValue(), requestAuth, secret));
        } catch (Exception e) {
            throw new RadiusPacketException("Error encoding attribute " + attribute, e);
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
            return create(attribute.getDictionary(), attribute.getTag().orElse((byte) 0),
                    codecType.getCodec().decode(attribute.getValue(), requestAuth, secret));
        } catch (Exception e) {
            throw new RadiusPacketException("Error decoding attribute " + attribute, e);
        }
    }

    public String toString() {
        String s = Integer.toUnsignedString(getType()) + "/" + getName() + ": " + getDataType();
        if (getVendorId() != -1)
            s += " (Vendor " + getVendorId() + ")";
        return s;
    }

    private static boolean detectHasTag(int vendorId, int type, boolean hasTag) {
        return (vendorId == -1 && type == TUNNEL_PASSWORD) || hasTag;
    }

    private static AttributeCodecType detectAttributeCodec(int vendorId, int type, byte encryptFlag) {
        if (vendorId == -1 && type == USER_PASSWORD)
            return RFC2865_USER_PASSWORD;

        if (vendorId == -1 && type == TUNNEL_PASSWORD)
            return RFC2868_TUNNEL_PASSWORD;

        if (vendorId == 529 && type == 214) // Ascend-Send-Secret
            return ASCEND_SEND_SECRET;

        return fromEncryptFlagId(encryptFlag);
    }
}

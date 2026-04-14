package org.tinyradius.core.attribute;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.attribute.type.EncodedAttribute;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.RadiusAttributeFactory;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.tinyradius.core.attribute.AttributeTypes.TUNNEL_PASSWORD;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

/**
 * Represents a Radius attribute type.
 * <p>
 * Each attribute type is represented by an instance of this object.
 * This class stores the type code, the type name, and the vendor ID
 * for each attribute type.
 */
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
     * Whether attribute supports Tag field as per RFC2868
     */
    private final boolean tagged;
    /**
     * One of {@link AttributeCodecType} enum, defaults to NO_ENCRYPT for none
     */
    private final AttributeCodecType codecType;

    private final RadiusAttributeFactory<? extends RadiusAttribute> factory;

    private final Map<Integer, String> int2str = new HashMap<>();

    private final Map<String, Integer> str2int = new HashMap<>();

    /**
     * Create a new attribute type.
     *
     * @param vendorId            vendor ID or -1 if N/A
     * @param type                sub-attribute type code, as unsigned byte
     * @param name                sub-attribute name
     * @param dataType            string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
     * @param factory             the factory to create the attribute
     * @param dictionaryCodecType codec type
     * @param hasTag              whether attribute supports tags, as defined in RFC2868, default false
     */
    public AttributeTemplate(int vendorId,
                             int type,
                             @NonNull String name,
                             @NonNull String dataType,
                             @NonNull RadiusAttributeFactory<? extends RadiusAttribute> factory,
                             AttributeCodecType dictionaryCodecType,
                             boolean hasTag) {
        if (name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        this.vendorId = vendorId;
        this.type = type;
        this.name = name;
        this.dataType = dataType.toLowerCase();
        this.factory = factory;
        this.tagged = detectHasTag(vendorId, type, hasTag);
        this.codecType = confirmAttributeCodec(vendorId, type, dictionaryCodecType);
    }

    /**
     * Returns the vendor ID of this attribute type.
     *
     * @return vendor ID or -1 if not applicable
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * Returns the Radius type code for this attribute.
     *
     * @return Radius type code
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the name of this attribute type.
     *
     * @return attribute type name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the data type of this attribute.
     *
     * @return data type (e.g., "string", "octets", "integer")
     */
    @NonNull
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns whether this attribute supports the Tag field as per RFC 2868.
     *
     * @return true if tagged, false otherwise
     */
    public boolean isTagged() {
        return tagged;
    }

    /**
     * Returns the codec type used for this attribute.
     *
     * @return the attribute codec type
     */
    @NonNull
    public AttributeCodecType getCodecType() {
        return codecType;
    }

    /**
     * Returns the factory used to create instances of this attribute.
     *
     * @return the Radius attribute factory
     */
    @NonNull
    public RadiusAttributeFactory<? extends RadiusAttribute> getFactory() {
        return factory;
    }

    /**
     * Create RadiusAttribute.
     *
     * @param dictionary dictionary to use
     * @param tag        tag as per RFC2868
     * @param value      value to set attribute
     * @return new RadiusAttribute
     */
    @NonNull
    public RadiusAttribute create(@NonNull Dictionary dictionary, byte tag, byte @NonNull [] value) {
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
    @NonNull
    public RadiusAttribute create(@NonNull Dictionary dictionary, byte tag, @NonNull String value) {
        return factory.create(dictionary, vendorId, type, tag, value);
    }

    /**
     * Parse RadiusAttribute from raw byte data.
     * <p>
     * If the attribute type has encryption, this will create an OctetsAttribute wrapped in EncodedDecorator,
     * otherwise uses the type specified in the dictionary.
     *
     * @param dictionary dictionary to use
     * @param data       attribute data to parse incl. type/length
     * @return new RadiusAttribute
     */
    @NonNull
    public RadiusAttribute parse(@NonNull Dictionary dictionary, @NonNull ByteBuf data) {
        return isEncrypt() ?
                new EncodedAttribute(OctetsAttribute.FACTORY.create(dictionary, vendorId, data)) :
                factory.create(dictionary, vendorId, data);
    }

    /**
     * Create RadiusAttribute with encoded data.
     * <p>
     * If the attribute type supports encryption, this will
     * return an OctetsAttribute as an underlying implementation
     * so contents aren't validated at construction.
     *
     * @param dictionary   dictionary to use
     * @param tag          tag as per RFC2868
     * @param encodedValue encoded data, attribute data excl. type/length/tag
     * @return new RadiusAttribute
     */
    @NonNull
    public RadiusAttribute createEncoded(@NonNull Dictionary dictionary, byte tag, byte @NonNull [] encodedValue) {
        return isEncrypt() ?
                new EncodedAttribute(OctetsAttribute.FACTORY.create(dictionary, vendorId, type, tag, encodedValue)) :
                factory.create(dictionary, vendorId, type, tag, encodedValue);
    }

    /**
     * Returns true if attribute supports encryption.
     *
     * @return true if attribute supports encryption
     */
    public boolean isEncrypt() {
        return codecType != NO_ENCRYPT;
    }

    /**
     * Returns the enumeration name for the given integer value.
     *
     * @param value int value
     * @return the name of the given integer value if this attribute
     * is an enumeration, or null if it is not or if the integer value
     * is unknown.
     */
    @Nullable
    public String getEnumeration(int value) {
        return int2str.get(value);
    }

    /**
     * Returns the integer value for the given enumeration name.
     *
     * @param value string value
     * @return the number of the given string value if this attribute is
     * an enumeration, or null if it is not or if the string value is unknown.
     */
    @Nullable
    public Integer getEnumeration(@NonNull String value) {
        return str2int.get(value);
    }

    /**
     * Adds a name for an integer value of this attribute.
     *
     * @param num  number that shall get a name
     * @param name the name for this number
     */
    public void addEnumerationValue(int num, @NonNull String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
        int2str.put(num, name);
        str2int.put(name, num);
    }

    /**
     * Encodes the attribute.
     *
     * @param attribute   attribute to encode
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to encode with
     * @return attribute with encoded data
     * @throws RadiusPacketException errors encoding attribute
     */
    @NonNull
    public RadiusAttribute encode(@NonNull RadiusAttribute attribute, byte @NonNull [] requestAuth, @NonNull String secret) throws RadiusPacketException {
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
     * Decodes the attribute.
     *
     * @param attribute   attribute to decode
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to decode with
     * @return attribute with decoded data
     * @throws RadiusPacketException errors decoding attribute
     */
    @NonNull
    public RadiusAttribute decode(@NonNull RadiusAttribute attribute, byte @NonNull [] requestAuth, @NonNull String secret) throws RadiusPacketException {
        if (attribute.isDecoded())
            return attribute;

        try {
            return create(attribute.getDictionary(), attribute.getTag().orElse((byte) 0),
                    codecType.getCodec().decode(attribute.getValue(), requestAuth, secret));
        } catch (Exception e) {
            throw new RadiusPacketException("Error decoding attribute " + attribute, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toString() {
        var s = Integer.toUnsignedString(getType()) + "/" + getName() + ": " + getDataType();
        if (getVendorId() != -1)
            s += " (Vendor " + getVendorId() + ")";
        return s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeTemplate that = (AttributeTemplate) o;
        return vendorId == that.vendorId
                && type == that.type
                && tagged == that.tagged
                && Objects.equals(name, that.name)
                && Objects.equals(dataType, that.dataType)
                && codecType == that.codecType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(vendorId, type, name, dataType, tagged, codecType);
    }

    /**
     * Detects if the attribute supports tags.
     *
     * @param vendorId the vendor ID
     * @param type     the attribute type
     * @param hasTag   the initial value
     * @return true if it has a tag
     */
    private static boolean detectHasTag(int vendorId, int type, boolean hasTag) {
        return (vendorId == -1 && type == TUNNEL_PASSWORD) || hasTag;
    }

    /**
     * Override attribute codec if RFC specifies
     *
     * @param vendorId  the vendor ID
     * @param type      the attribute type
     * @param codecType the initial encrypt flag
     * @return the codec type
     */
    @NonNull
    private static AttributeCodecType confirmAttributeCodec(int vendorId, int type, AttributeCodecType codecType) {
        if (vendorId == -1 && type == USER_PASSWORD)
            return RFC2865_USER_PASSWORD;

        if (vendorId == -1 && type == TUNNEL_PASSWORD)
            return RFC2868_TUNNEL_PASSWORD;

        return codecType;
    }
}

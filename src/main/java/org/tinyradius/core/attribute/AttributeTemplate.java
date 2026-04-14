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

import static org.tinyradius.core.attribute.AttributeTypes.TUNNEL_PASSWORD;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

/**
 * Represents a Radius attribute type.
 * <p>
 * Each attribute type is represented by an instance of this object.
 * This class stores the type code, the type name, and the vendor ID
 * for each attribute type.
 *
 * @param vendorId  vendor ID or -1 if not applicable
 * @param type      Radius type code for this attribute e.g. '1' (for User-Name)
 * @param name      name of type e.g. 'User-Name'
 * @param dataType  string | octets | integer | date | ipaddr | ipv6addr | ipv6prefix
 * @param tagged    Whether attribute supports Tag field as per RFC2868
 * @param codecType One of {@link AttributeCodecType} enum, defaults to NO_ENCRYPT for none
 * @param factory   the factory used to create instances of this attribute
 * @param int2str   Internal map for integer-to-string enumeration mapping
 * @param str2int   Internal map for string-to-integer enumeration mapping
 */
public record AttributeTemplate(
        int vendorId,
        int type,
        @NonNull String name,
        @NonNull String dataType,
        boolean tagged,
        @NonNull AttributeCodecType codecType,
        @NonNull RadiusAttributeFactory<? extends RadiusAttribute> factory,
        @NonNull Map<Integer, String> int2str,
        @NonNull Map<String, Integer> str2int
) {

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
        this(vendorId, type, name, dataType.toLowerCase(),
                detectHasTag(vendorId, type, hasTag),
                confirmAttributeCodec(vendorId, type, dictionaryCodecType),
                factory, new HashMap<>(), new HashMap<>());
        if (name.isEmpty())
            throw new IllegalArgumentException("Name is empty");
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
        var s = Integer.toUnsignedString(type()) + "/" + name() + ": " + dataType();
        if (vendorId() != -1)
            s += " (Vendor " + vendorId() + ")";
        return s;
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

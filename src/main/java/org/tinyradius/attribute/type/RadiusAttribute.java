package org.tinyradius.attribute.type;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;
import java.util.Optional;

public interface RadiusAttribute {

    /**
     * @return attribute data as raw bytes
     */
    byte[] getValue();

    /**
     * @return attribute type code, 0-255
     */
    byte getType();

    /**
     * @return value of this attribute as a hex string.
     */
    String getValueString();

    /**
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    int getVendorId();

    /**
     * @return dictionary that attribute uses
     */
    Dictionary getDictionary();

    /**
     * @return entire attribute (including headers) as byte array
     */
    byte[] toByteArray();

    String getAttributeName();

    /**
     * Returns set of all nested attributes if contains sub-attributes,
     * otherwise singleton set of current attribute.
     *
     * @return List of RadiusAttributes
     */
    List<RadiusAttribute> flatten();

    /**
     * @return AttributeTemplate used to define this attribute
     */
    Optional<AttributeTemplate> getAttributeTemplate();

    // todo use flag or subtypes to prevent encoding/decoding multiple times
    default boolean isEncoded() {
        return false;
    }

    /**
     * @param secret shared secret to encode with
     * @param requestAuth (corresponding) request packet authenticator
     * @return attribute with encoded data
     */
    default RadiusAttribute encode(String secret, byte[] requestAuth) throws RadiusPacketException {
//        if (isEncoded())
//            return this;

        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, secret, requestAuth) :
                this;
    }

    /**
     * @param secret shared secret to encode with
     * @param requestAuth (corresponding) request packet authenticator
     * @return attribute with encoded data
     */
    default RadiusAttribute decode(String secret, byte[] requestAuth) throws RadiusPacketException {
//        if (!isEncoded())
//            return this;

        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, secret, requestAuth) : this;
    }
}

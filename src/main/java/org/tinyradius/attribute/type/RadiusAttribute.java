package org.tinyradius.attribute.type;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;
import java.util.Optional;

public interface RadiusAttribute {

    /**
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    int getVendorId();

    /**
     * @return attribute type code, 0-255
     */
    byte getType();

    /**
     * @return Tag if available and specified for attribute type (RFC2868)
     */
    byte getTag();

    /**
     * @return attribute data as raw bytes
     */
    byte[] getValue();

    /**
     * @return value of this attribute as a hex string.
     */
    String getValueString();

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

    /**
     * Encodes attribute. Must be idempotent.
     *
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to encode with
     * @return attribute with encoded data
     * @throws RadiusPacketException errors encoding attribute
     */
    RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException;

    /**
     * Decodes attribute. Must be idempotent.
     *
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to encode with
     * @return attribute with encoded data
     * @throws RadiusPacketException errors decoding attribute
     */
    RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException;

    default boolean isEncoded() {
        return false;
    }
}

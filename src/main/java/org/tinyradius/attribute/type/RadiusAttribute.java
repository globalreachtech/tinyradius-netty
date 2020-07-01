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
     * @param secret      shared secret to encode with
     * @param requestAuth (corresponding) request packet authenticator
     * @return attribute with encoded data
     * @throws RadiusPacketException errors encoding attribute
     */
    RadiusAttribute encode(String secret, byte[] requestAuth) throws RadiusPacketException;

    /**
     * Decodes attribute. Must be idempotent.
     *
     * @param secret      shared secret to encode with
     * @param requestAuth (corresponding) request packet authenticator
     * @return attribute with encoded data
     * @throws RadiusPacketException errors decoding attribute
     */
    RadiusAttribute decode(String secret, byte[] requestAuth) throws RadiusPacketException;
}

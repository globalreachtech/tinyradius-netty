package org.tinyradius.core.attribute.type;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface RadiusAttribute {

    /**
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    int getVendorId();

    /**
     * @return attribute type code, typically 0-255
     */
    int getType();

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
    default byte[] toByteArray() {
        final int len = getValue().length + 2 + getTagBytes().length;
        return ByteBuffer.allocate(len)
                .put(getTypeBytes())
                .put((byte) len)
                .put(getTagBytes())
                .put(getValue())
                .array();
    }

    default byte[] getTypeBytes() {
        return new byte[]{(byte) getType()};
    }

    /**
     * @return byte array of length 1 containing {@link #getTag()},
     * or empty byte array of length 0 if attribute does not support tags
     */
    default byte[] getTagBytes() {
        final boolean tagged = getAttributeTemplate()
                .map(AttributeTemplate::isTagged)
                .orElse(false);
        return tagged ? new byte[]{getTag()} : new byte[0];
    }

    default String getAttributeName() {
        return getAttributeTemplate()
                .map(AttributeTemplate::getName)
                .orElse(getVendorId() != -1 ?
                        "Unknown-Sub-Attribute-" + getType() :
                        "Unknown-Attribute-" + getType());
    }

    /**
     * Returns set of all nested attributes if contains sub-attributes,
     * otherwise singleton set of current attribute.
     *
     * @return List of RadiusAttributes
     */
    default List<RadiusAttribute> flatten() {
        return Collections.singletonList(this);
    }

    /**
     * @return AttributeTemplate used to define this attribute
     */
    default Optional<AttributeTemplate> getAttributeTemplate() {
        return getDictionary().getAttributeTemplate(getVendorId(), getType());
    }

    /**
     * Encodes attribute. Must be idempotent.
     *
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to encode with
     * @return attribute with encoded data
     * @throws RadiusPacketException errors encoding attribute
     */
    default RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return this;
    }

    /**
     * Decodes attribute. Must be idempotent.
     *
     * @param requestAuth (corresponding) request packet authenticator
     * @param secret      shared secret to encode with
     * @return attribute with encoded data
     * @throws RadiusPacketException errors decoding attribute
     */
    default RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return this;
    }

    default boolean isEncoded() {
        return false;
    }
}

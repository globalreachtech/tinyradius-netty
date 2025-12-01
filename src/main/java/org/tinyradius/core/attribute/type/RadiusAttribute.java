package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

public interface RadiusAttribute {
    HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    /**
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    int getVendorId();

    /**
     * @return attribute type code, typically 0-255
     */
    default int getType() {
        return switch (getTypeSize()) {
            case 2 -> getData().getShort(0);
            case 4 -> getData().getInt(0);
            default -> Byte.toUnsignedInt(getData().getByte(0));
        };
    }

    /**
     * @return number of octets used by attribute - uses declared length where possible, otherwise uses readableBytes
     */
    default int getLength() {
        return switch (getLengthSize()) {
            case 0 -> getData().readableBytes();
            case 2 -> getData().getShort(getTypeSize());
            default -> Byte.toUnsignedInt(getData().getByte(getTypeSize())); // max 255
        };
    }

    /**
     * @return number of octets used for header, typically 2 except for VSAs with custom type/length sizes
     */
    default int getHeaderSize() {
        return getTypeSize() + getLengthSize();
    }

    /**
     * @return Tag if available and specified for attribute type (RFC2868)
     */
    Optional<Byte> getTag();

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
     * {@link #toByteBuf()} is preferred if caller exposes a reference to the ByteBuf elsewhere to avoid mutating netty ref counts
     *
     * @return underlying ByteBuf for attribute, includes attribute header, (optional) tag, and value
     */
    ByteBuf getData();

    /**
     * @return underlying ByteBuf for attribute, includes attribute header, (optional) tag, and value
     */
    default ByteBuf toByteBuf() {
        return Unpooled.unreleasableBuffer(getData());
    }

    /**
     * @return entire attribute (including headers) as byte array
     */
    default byte[] toByteArray() {
        return getData().copy().array();
    }

    /**
     * @return number of octets used for type, typically 1 except certain VSAs
     */
    default int getTypeSize() {
        return getVendor()
                .map(Vendor::typeSize)
                .orElse(1);
    }

    /**
     * @return number of octets used for length, typically 1 except certain VSAs
     */
    default int getLengthSize() {
        return getVendor()
                .map(Vendor::lengthSize)
                .orElse(1);
    }

    /**
     * @return 1 if attribute supports a tag, otherwise 0
     */
    default int getTagSize() {
        return getDictionary().getAttributeTemplate(getVendorId(), getType())
                .map(AttributeTemplate::isTagged)
                .orElse(false) ? 1 : 0;
    }

    default Optional<Vendor> getVendor() {
        return getDictionary().getVendor(getVendorId());
    }

    default boolean isTagged() {
        return getAttributeTemplate()
                .map(AttributeTemplate::isTagged)
                .orElse(false);
    }

    default AttributeCodecType codecType() {
        return getAttributeTemplate()
                .map(AttributeTemplate::getCodecType)
                .orElse(NO_ENCRYPT);
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

    default boolean isDecoded() {
        return !isEncoded();
    }
}

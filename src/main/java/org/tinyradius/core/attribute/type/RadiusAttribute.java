package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;
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

/**
 * Base interface for all RADIUS attributes.
 */
public interface RadiusAttribute {
    /**
     * Hex formatter for attribute values.
     */
    HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    /**
     * Returns the vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1.
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    int getVendorId();

    /**
     * Returns the attribute type code, typically 0-255.
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
     * Returns the number of octets used by attribute - uses declared length where possible, otherwise uses readableBytes.
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
     * Returns the number of octets used for header, typically 2 except for VSAs with custom type/length sizes.
     * @return number of octets used for header, typically 2 except for VSAs with custom type/length sizes
     */
    default int getHeaderSize() {
        return getTypeSize() + getLengthSize();
    }

    /**
     * Returns the tag if available and specified for attribute type (RFC2868).
     * @return Tag if available and specified for attribute type (RFC2868)
     */
    @NonNull
    Optional<Byte> getTag();

    /**
     * Returns the attribute data as raw bytes.
     * @return attribute data as raw bytes
     */
    @NonNull
    byte[] getValue();

    /**
     * Returns the value of this attribute as a hex string.
     * @return value of this attribute as a hex string.
     */
    @NonNull
    String getValueString();

    /**
     * Returns the dictionary that attribute uses.
     * @return dictionary that attribute uses
     */
    @NonNull
    Dictionary getDictionary();

    /**
     * {@link #toByteBuf()} is preferred if caller exposes a reference to the ByteBuf elsewhere to avoid mutating netty ref counts
     *
     * @return underlying ByteBuf for attribute, includes attribute header, (optional) tag, and value
     */
    @NonNull
    ByteBuf getData();

    /**
     * Returns the underlying ByteBuf for attribute, includes attribute header, (optional) tag, and value.
     * @return underlying ByteBuf for attribute, includes attribute header, (optional) tag, and value
     */
    @NonNull
    default ByteBuf toByteBuf() {
        return Unpooled.unreleasableBuffer(getData());
    }

    /**
     * Returns the entire attribute (including headers) as byte array.
     * @return entire attribute (including headers) as byte array
     */
    @NonNull
    default byte[] toByteArray() {
        return getData().copy().array();
    }

    /**
     * Returns the number of octets used for type, typically 1 except certain VSAs.
     * @return number of octets used for type, typically 1 except certain VSAs
     */
    default int getTypeSize() {
        return getVendor()
                .map(Vendor::typeSize)
                .orElse(1);
    }

    /**
     * Returns the number of octets used for length, typically 1 except certain VSAs.
     * @return number of octets used for length, typically 1 except certain VSAs
     */
    default int getLengthSize() {
        return getVendor()
                .map(Vendor::lengthSize)
                .orElse(1);
    }

    /**
     * Returns 1 if attribute supports a tag, otherwise 0.
     * @return 1 if attribute supports a tag, otherwise 0
     */
    default int getTagSize() {
        return getDictionary().getAttributeTemplate(getVendorId(), getType())
                .map(AttributeTemplate::isTagged)
                .orElse(false) ? 1 : 0;
    }

    /**
     * Returns the vendor for this attribute, if any.
     *
     * @return the vendor for this attribute, if any
     */
    @NonNull
    default Optional<Vendor> getVendor() {
        return getDictionary().getVendor(getVendorId());
    }

    /**
     * Returns true if this attribute is tagged.
     * @return true if this attribute is tagged
     */
    default boolean isTagged() {
        return getAttributeTemplate()
                .map(AttributeTemplate::isTagged)
                .orElse(false);
    }

    /**
     * Returns the codec type for this attribute.
     * @return the codec type for this attribute
     */
    @NonNull
    default AttributeCodecType codecType() {
        return getAttributeTemplate()
                .map(AttributeTemplate::getCodecType)
                .orElse(NO_ENCRYPT);
    }

    /**
     * Returns the name of the attribute.
     * @return the name of the attribute
     */
    @NonNull
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
    @NonNull
    default List<RadiusAttribute> flatten() {
        return Collections.singletonList(this);
    }

    /**
     * Returns the AttributeTemplate used to define this attribute.
     * @return AttributeTemplate used to define this attribute
     */
    @NonNull
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
    @NonNull
    default RadiusAttribute encode(@NonNull byte[] requestAuth, @NonNull String secret) throws RadiusPacketException {
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
    @NonNull
    default RadiusAttribute decode(@NonNull byte[] requestAuth, @NonNull String secret) throws RadiusPacketException {
        return this;
    }

    /**
     * Returns true if this attribute is encoded.
     * @return true if this attribute is encoded
     */
    default boolean isEncoded() {
        return false;
    }

    /**
     * Returns true if this attribute is decoded.
     * @return true if this attribute is decoded
     */
    default boolean isDecoded() {
        return !isEncoded();
    }
}

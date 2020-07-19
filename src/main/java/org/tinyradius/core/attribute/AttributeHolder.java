package org.tinyradius.core.attribute;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.AnonSubAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Basic attribute holder, for VendorSpecificAttribute (to hold sub-attributes) or RadiusPackets
 * <p>
 * Should only hold single layer of attributes
 */
public interface AttributeHolder<T extends AttributeHolder<T>> {

    static ByteBuf attributesToBytes(List<RadiusAttribute> attributes) {
        return Unpooled.wrappedBuffer(attributes.stream()
                .map(RadiusAttribute::getData)
                .toArray(ByteBuf[]::new));
    }

    /**
     * Reads attributes and increments readerIndex.
     *
     * @param dictionary dictionary to parse attribute
     * @param vendorId   vendor Id to set attributes
     * @param data       byte array to parse
     * @return list of RadiusAttributes
     */
    static List<RadiusAttribute> readAttributes(Dictionary dictionary, int vendorId, ByteBuf data) {
        final Optional<Vendor> vendor = dictionary.getVendor(vendorId);

        // if reading sub-attribute for undefined VSA, treat entire body of vsa (ex vendorId) as undistinguished bytes
        if (vendorId != -1 && !vendor.isPresent())
            return Collections.singletonList(new AnonSubAttribute(dictionary, vendorId, data));

        final ArrayList<RadiusAttribute> attributes = new ArrayList<>();

        // at least 2 octets left (minimum size header)
        while (data.isReadable(2)) {
            attributes.add(readAttribute(dictionary, vendorId, data));
        }

        if (data.isReadable())
            throw new IllegalArgumentException("Attribute malformed, " + data.readableBytes() + " bytes remaining to parse");

        return attributes;
    }

    /**
     * Parses attribute and increases readerIndex by size of attribute.
     */
    static RadiusAttribute readAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        final Optional<Vendor> vendor = dictionary.getVendor(vendorId);

        final int typeSize = vendor
                .map(Vendor::getTypeSize)
                .orElse(1);

        int type;
        switch (typeSize) {
            case 2:
                type = data.getShort(data.readerIndex());
                break;
            case 4:
                type = data.getInt(data.readerIndex());
                break;
            case 1:
            default:
                type = Byte.toUnsignedInt(data.getByte(data.readerIndex()));
        }

        final int lengthSize = vendor
                .map(Vendor::getLengthSize)
                .orElse(1);

        int length;
        switch (lengthSize) {
            case 0:
                length = data.readableBytes();
                break;
            case 2:
                length = data.getShort(data.readerIndex() + typeSize);
                break;
            case 1:
            default:
                length = Byte.toUnsignedInt(data.getByte(data.readerIndex() + typeSize)); // max 255
        }

        if (length < typeSize + lengthSize)
            throw new IllegalArgumentException("Invalid attribute length " + length + ", must be >= typeSize + lengthSize, " +
                    "but typeSize=" + typeSize + ", lengthSize=" + lengthSize);

        if (length > data.readableBytes())
            throw new IllegalArgumentException("Invalid attribute length " + length + ", parsable bytes " + data.readableBytes());

        return dictionary.createAttribute(vendorId, type, data.readSlice(length));
    }

    /**
     * @return VendorId to restrict (sub)attributes, or -1 for top level
     */
    int getChildVendorId();

    Dictionary getDictionary();

    /**
     * @return list of RadiusAttributes
     */
    List<RadiusAttribute> getAttributes();

    /**
     * Convenience method to get single attribute.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default Optional<RadiusAttribute> getAttribute(String type) {
        return filterAttributes(type).stream().findFirst();
    }

    /**
     * Convenience method to get single attribute.
     *
     * @param type attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default Optional<RadiusAttribute> getAttribute(int type) {
        return filterAttributes(type).stream().findFirst();
    }

    /**
     * Returns all attributes of the given type, regardless of vendorId
     *
     * @param type type of attributes to get
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> filterAttributes(int type) {
        return filterAttributes(a -> a.getType() == type);
    }

    /**
     * Returns attributes of the given type name.
     * Also searches sub-attributes if appropriate.
     *
     * @param name attribute type name
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> filterAttributes(String name) {
        final Optional<AttributeTemplate> type = getDictionary().getAttributeTemplate(name);
        if (type.isPresent())
            return filterAttributes(type.get());

        throw new IllegalArgumentException("Unknown attribute type name'" + name + "'");
    }

    /**
     * Returns attributes of the given type, filtered by given predicate
     *
     * @param filter RadiusAttribute filter predicate
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> filterAttributes(Predicate<RadiusAttribute> filter) {
        return getAttributes().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Returns attributes of the given attribute type.
     * Also searches sub-attributes if appropriate.
     *
     * @param type attribute type name
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> filterAttributes(AttributeTemplate type) {
        if (type.getVendorId() == getChildVendorId())
            return filterAttributes(type.getType());

        return Collections.emptyList();
    }

    /**
     * Encodes the attributes of this Radius packet to a byte array.
     *
     * @return byte array with encoded attributes
     */
    default ByteBuf getAttributeByteBuf() {
        return attributesToBytes(getAttributes());
    }

    T withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException;

    /**
     * Adds a attribute to this attribute container.
     *
     * @param attribute attribute to add
     * @return object of same type with appended attribute
     */
    default T addAttribute(RadiusAttribute attribute) throws RadiusPacketException {
        if (attribute.getVendorId() != getChildVendorId())
            throw new IllegalArgumentException("Attribute vendor ID doesn't match: " +
                    "required " + getChildVendorId() + ", actual " + attribute.getVendorId());

        final ArrayList<RadiusAttribute> attributes = new ArrayList<>(getAttributes());
        attributes.add(attribute);
        return withAttributes(attributes);
    }

    default T addAttribute(String name, String value) throws RadiusPacketException {
        return addAttribute(
                getDictionary().createAttribute(name, value));
    }

    /**
     * Adds a Radius attribute.
     *
     * @param type  attribute type code
     * @param value string value to set
     * @return object of same type with appended attribute
     */
    default T addAttribute(int type, String value) throws RadiusPacketException {
        return addAttribute(
                getDictionary().createAttribute(getChildVendorId(), type, (byte) 0, value));
    }

    /**
     * Removes all instances of the specified attribute from this attribute container.
     *
     * @param attribute attributes to remove
     * @return object of same type with removed attribute
     */
    default T removeAttribute(RadiusAttribute attribute) throws RadiusPacketException {
        return withAttributes(
                filterAttributes(a -> !a.equals(attribute)));
    }

    /**
     * Removes all attributes from this packet which have got the specified type.
     *
     * @param type attribute type to remove
     * @return object of same type with removed attributes
     */
    default T removeAttributes(int type) throws RadiusPacketException {
        return withAttributes(
                filterAttributes(a -> a.getType() != type));
    }

    /**
     * Removes the last occurrence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     * @return object of same type with removed attribute
     */
    default T removeLastAttribute(int type) throws RadiusPacketException {
        List<RadiusAttribute> attributes = filterAttributes(type);
        if (attributes.isEmpty())
            return withAttributes(getAttributes());

        return removeAttribute(attributes.get(attributes.size() - 1));
    }

    /**
     * @param requestAuth  request authenticator to encode attributes
     * @param sharedSecret shared secret with server/client to encode attributes
     * @return encoded version of attributes
     * @throws RadiusPacketException errors encoding attributes
     */
    default List<RadiusAttribute> encodeAttributes(byte[] requestAuth, String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> encoded = new ArrayList<>();
        for (RadiusAttribute a : getAttributes()) {
            RadiusAttribute encode = a.encode(requestAuth, sharedSecret);
            encoded.add(encode);
        }
        return encoded;
    }

    /**
     * @param requestAuth  request authenticator to decode attributes
     * @param sharedSecret shared secret with server/client to decode attributes
     * @return decoded/original version of attributes
     * @throws RadiusPacketException errors decoding attributes
     */
    default List<RadiusAttribute> decodeAttributes(byte[] requestAuth, String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> decoded = new ArrayList<>();
        for (RadiusAttribute a : getAttributes()) {
            RadiusAttribute decode = a.decode(requestAuth, sharedSecret);
            decoded.add(decode);
        }
        return decoded;
    }
}

package org.tinyradius.core.attribute;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Byte.toUnsignedInt;

/**
 * Basic attribute holder, for VendorSpecificAttribute (to hold sub-attributes) or RadiusPackets
 * <p>
 * Should only hold single layer of attributes
 */
public interface AttributeHolder<T extends AttributeHolder<T>> {

    static byte[] attributesToBytes(List<RadiusAttribute> attributes) {
        final ByteBuf buffer = Unpooled.buffer();

        for (RadiusAttribute attribute : attributes) {
            buffer.writeBytes(attribute.toByteArray());
        }

        return buffer.copy().array();
    }

    /**
     * @param dictionary dictionary to parse attribute
     * @param vendorId   vendor Id to set attributes
     * @param data       byte array to parse
     * @param pos        position in byte array at which to parse
     * @return list of RadiusAttributes
     */
    static List<RadiusAttribute> extractAttributes(Dictionary dictionary, int vendorId, byte[] data, int pos) {
        final ArrayList<RadiusAttribute> attributes = new ArrayList<>();

        // at least 2 octets left
        while (data.length - pos >= 2) {
            final byte type = data[pos];
            final int length = toUnsignedInt(data[pos + 1]); // max 255
            final int expectedLen = length - 2;
            if (expectedLen < 0)
                throw new IllegalArgumentException("Invalid attribute length " + length + ", must be >=2");
            if (expectedLen > data.length - pos)
                throw new IllegalArgumentException("Invalid attribute length " + length + ", remaining bytes " + (data.length - pos));
            attributes.add(dictionary.parseAttribute(vendorId, type, Arrays.copyOfRange(data, pos + 2, pos + length)));
            pos += length;
        }

        if (pos != data.length)
            throw new IllegalArgumentException("Attribute malformed, lengths do not match, " +
                    "parse position " + pos + ", bytes length " + data.length);
        return attributes;
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
    default Optional<RadiusAttribute> getAttribute(byte type) {
        return filterAttributes(type).stream().findFirst();
    }

    /**
     * Returns all attributes of the given type, regardless of vendorId
     *
     * @param type type of attributes to get
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> filterAttributes(byte type) {
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
    default byte[] getAttributeBytes() {
        return attributesToBytes(getAttributes());
    }

    T withAttributes(List<RadiusAttribute> attributes);

    /**
     * Adds a attribute to this attribute container.
     *
     * @param attribute attribute to add
     * @return object of same type with appended attribute
     */
    default T addAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() != getChildVendorId())
            throw new IllegalArgumentException("Attribute vendor ID doesn't match: " +
                    "required " + getChildVendorId() + ", actual " + attribute.getVendorId());

        final ArrayList<RadiusAttribute> attributes = new ArrayList<>(getAttributes());
        attributes.add(attribute);
        return withAttributes(attributes);
    }

    default T addAttribute(String name, String value) {
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
    default T addAttribute(byte type, String value) {
        return addAttribute(
                getDictionary().createAttribute(getChildVendorId(), type, value));
    }

    /**
     * Removes all instances of the specified attribute from this attribute container.
     *
     * @param attribute attributes to remove
     * @return object of same type with removed attribute
     */
    default T removeAttribute(RadiusAttribute attribute) {
        return withAttributes(
                filterAttributes(a -> !a.equals(attribute)));
    }

    /**
     * Removes all attributes from this packet which have got the specified type.
     *
     * @param type attribute type to remove
     * @return object of same type with removed attributes
     */
    default T removeAttributes(byte type) {
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
    default T removeLastAttribute(byte type) {
        List<RadiusAttribute> attributes = filterAttributes(type);
        if (attributes.isEmpty())
            return withAttributes(getAttributes());

        return removeAttribute(attributes.get(attributes.size() - 1));
    }

}

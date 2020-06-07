package org.tinyradius.attribute.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic attribute holder, for VendorSpecificAttribute (to hold sub-attributes) or RadiusPackets
 * <p>
 * Should only hold single layer of attributes
 */
public interface AttributeHolder {

    /**
     * @return VendorId to restrict (sub)attributes, or -1 for top level
     */
    int getChildVendorId();

    Dictionary getDictionary();

    /**
     * @return list of RadiusAttributes
     */
    List<RadiusAttribute> getAttributes();

    default AttributeType lookupAttributeType(String name) {
        final AttributeType type = getDictionary().getAttributeTypeByName(name);
        if (type == null)
            throw new IllegalArgumentException("Unknown attribute type name'" + name + "'");
        return type;
    }

    /**
     * Convenience method to get single attribute as string.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default String getAttributeString(byte type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        return attrs.isEmpty() ? null : attrs.get(0).getValueString();
    }

    /**
     * Convenience method to get single attribute.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default RadiusAttribute getAttribute(String type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Convenience method to get single attribute.
     *
     * @param type attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default RadiusAttribute getAttribute(byte type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Returns all attributes of the given type, regardless of vendorId
     *
     * @param type type of attributes to get
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> getAttributes(byte type) {
        return getAttributes().stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Returns attributes of the given type name.
     * Also searches sub-attributes if appropriate.
     *
     * @param type attribute type name
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> getAttributes(String type) {
        return getAttributes(lookupAttributeType(type));
    }

    /**
     * Returns attributes of the given attribute type.
     * Also searches sub-attributes if appropriate.
     *
     * @param type attribute type name
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> getAttributes(AttributeType type) {
        if (type.getVendorId() == getChildVendorId())
            return getAttributes(type.getType());
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

    static byte[] attributesToBytes(List<RadiusAttribute> attributes) {
        final ByteBuf buffer = Unpooled.buffer();

        for (RadiusAttribute attribute : attributes) {
            buffer.writeBytes(attribute.toByteArray());
        }

        return buffer.copy().array();
    }

    interface Writable<T extends Writable<T>> extends AttributeHolder {

        T withAttributes(List<RadiusAttribute> attributes);

        /**
         * Adds a attribute to this attribute container.
         *
         * @param attribute attribute to add
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
                    Attributes.create(getDictionary(), name, value));
        }

        /**
         * Adds a Radius attribute.
         *
         * @param type  attribute type code
         * @param value string value to set
         */
        default T addAttribute(byte type, String value) {
            return addAttribute(
                    Attributes.create(getDictionary(), getChildVendorId(), type, value));
        }

        /**
         * Removes all instances of the specified attribute from this attribute container.
         *
         * @param attribute attributes to remove
         */
        default T removeAttribute(RadiusAttribute attribute) {
            return withAttributes(getAttributes().stream()
                    .filter(a -> !a.equals(attribute))
                    .collect(Collectors.toList()));
        }

        /**
         * Removes all attributes from this packet which have got the specified type.
         *
         * @param type attribute type to remove
         */
        default T removeAttributes(byte type) {
            return withAttributes(getAttributes().stream()
                    .filter(a -> a.getType() != type)
                    .collect(Collectors.toList()));
        }

        /**
         * Removes the last occurrence of the attribute of the given
         * type from the packet.
         *
         * @param type attribute type code
         */
        default T removeLastAttribute(byte type) {
            List<RadiusAttribute> attributes = getAttributes(type);
            if (attributes.isEmpty())
                return withAttributes(Collections.emptyList());

            return removeAttribute(attributes.get(attributes.size() - 1));
        }
    }
}

package org.tinyradius.attribute.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic attribute holder, for VendorSpecificAttribute (to hold sub-attributes) or RadiusPackets
 */
public interface AttributeHolder {

    /**
     * @return VendorId to restrict (sub)attributes, or -1 for top level
     */
    int getChildVendorId();

    Dictionary getDictionary();

    List<RadiusAttribute> getAttributes();

    void addAttribute(RadiusAttribute attribute);

    void removeAttribute(RadiusAttribute attribute);

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
     * Adds a Radius attribute.
     * Uses AttributeTypes to lookup the type code and converts the value.
     *
     * @param name  name of the attribute, for example "NAS-IP-Address", should NOT be 'Vendor-Specific'
     * @param value value of the attribute, for example "127.0.0.1"
     * @throws IllegalArgumentException if type name or value is invalid
     */
    default void addAttribute(String name, String value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Type name is null/empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Value is null/empty");

        addAttribute(
                lookupAttributeType(name).create(getDictionary(), value));
    }

    /**
     * Adds a Radius attribute.
     *
     * @param type  attribute type code
     * @param value string value to set
     */
    default void addAttribute(byte type, String value) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Value is null/empty");

        addAttribute(
                Attributes.create(getDictionary(), getChildVendorId(), type, value));
    }

    /**
     * Removes all attributes from this packet which have got the specified type.
     *
     * @param type attribute type to remove
     */
    default void removeAttributes(byte type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        attrs.forEach(this::removeAttribute);
    }

    /**
     * Removes the last occurrence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     */
    default void removeLastAttribute(byte type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs.isEmpty())
            return;

        removeAttribute(attrs.get(attrs.size() - 1));
    }

    /**
     * @return Map of attribute key-value
     */
    default Map<String, String> getAttributeMap() {
        final HashMap<String, String> map = new HashMap<>();
        getAttributes().forEach(a -> map.putAll(a.getAttributeMap()));
        return map;
    }

    /**
     * Encodes the attributes of this Radius packet to a byte array.
     *
     * @return byte array with encoded attributes
     */
    default byte[] getAttributeBytes() {
        final ByteBuf buffer = Unpooled.buffer();

        for (RadiusAttribute attribute : getAttributes()) {
            buffer.writeBytes(attribute.toByteArray());
        }

        return buffer.copy().array();
    }
}

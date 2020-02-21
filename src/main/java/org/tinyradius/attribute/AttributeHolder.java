package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.tinyradius.attribute.VendorSpecificAttribute.VENDOR_SPECIFIC;

public interface AttributeHolder {

    Dictionary getDictionary();

    /**
     * @return VendorId, or -1 if not appropriate
     */
    int getVendorId();

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
     * Returns all (sub)attributes of the given type.
     * Returns an empty list if there are no such attributes.
     *
     * @param type type of attributes to get
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> getAttributes(int type) {
        return getAttributes().stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single Radius attribute of the given type name.
     * Also searches sub-attributes if appropriate.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if the attribute occurs multiple times
     */
    default List<RadiusAttribute> getAttributes(String type) {
        final AttributeType attributeType = lookupAttributeType(type);
        return getAttributes(attributeType.getVendorId(), attributeType.getTypeCode());
    }

    /**
     * Returns all attributes of this packet that have got the
     * given type and belong to the given vendor ID.
     * Returns an empty list if there are no such attributes.
     *
     * @param vendorId      vendor ID, or -1
     * @param attributeType attribute type code
     * @return list of RadiusAttribute objects, never null
     */
    default List<RadiusAttribute> getAttributes(int vendorId, int attributeType) {
        if (vendorId == getVendorId())
            return getAttributes(attributeType);

        return getVendorSpecificAttributes(vendorId).stream()
                .map(VendorSpecificAttribute::getAttributes)
                .flatMap(Collection::stream)
                .filter(sa -> sa.getType() == attributeType && sa.getVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    /**
     * Returns the Vendor-Specific attribute(s) for the given vendor ID.
     *
     * @param vendorId vendor ID of the attribute(s)
     * @return List with VendorSpecificAttribute objects, never null
     */
    default List<VendorSpecificAttribute> getVendorSpecificAttributes(int vendorId) {
        return getAttributes(VENDOR_SPECIFIC).stream()
                .filter(VendorSpecificAttribute.class::isInstance)
                .map(VendorSpecificAttribute.class::cast)
                .filter(a -> a.getVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    /**
     * Adds a Radius attribute to this packet.
     * Uses AttributeTypes to lookup the type code and converts the value.
     * Can also be used to add sub-attributes.
     *
     * @param name  name of the attribute, for example "NAS-IP-Address", should NOT be 'Vendor-Specific'
     * @param value value of the attribute, for example "127.0.0.1"
     * @throws IllegalArgumentException if type name or value is invalid
     */
    default void addAttribute(String name, String value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Type name is empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Value is empty");

        RadiusAttribute attribute = lookupAttributeType(name).create(getDictionary(), value);
        addAttribute(attribute);
    }

    /**
     * Removes all sub-attributes of the given vendor and
     * type.
     *
     * @param vendorId vendor ID, or -1
     * @param typeCode attribute type code
     */
    default void removeAttributes(int vendorId, int typeCode) {
        if (vendorId == getVendorId()) {
            removeAttributes(typeCode);
            return;
        }

        List<VendorSpecificAttribute> vsas = getVendorSpecificAttributes(vendorId);
        for (VendorSpecificAttribute vsa : vsas) {
            List<RadiusAttribute> sas = vsa.getAttributes();
            sas.removeIf(attr -> attr.getType() == typeCode && attr.getVendorId() == vendorId);
            if (sas.isEmpty())
                // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                removeAttribute(vsa);
        }
    }

    /**
     * Removes all attributes from this packet which have got the specified type.
     *
     * @param type attribute type to remove
     */
    default void removeAttributes(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        attrs.forEach(this::removeAttribute);
    }

    /**
     * Removes the last occurrence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     */
    default void removeLastAttribute(int type) {
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
    default RadiusAttribute getAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Convenience method to get single attribute.
     *
     * @param vendorId vendor ID
     * @param type     attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default RadiusAttribute getAttribute(int vendorId, int type) {
        List<RadiusAttribute> attrs = getAttributes(vendorId, type);
        return attrs.isEmpty() ? null : attrs.get(0);
    }
}

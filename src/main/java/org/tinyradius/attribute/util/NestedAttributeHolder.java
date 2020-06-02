package org.tinyradius.attribute.util;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AttributeHolder that supports sub-attributes (Vendor-Specific Attributes) and filtering by vendorId
 */
public interface NestedAttributeHolder extends AttributeHolder {

    /**
     * Returns all attributes of this packet that match the
     * given type and vendorId.
     * <p>
     * If vendorId doesn't match childVendorId, will search sub-attributes.
     *
     * @param vendorId vendor ID, or -1
     * @param type     attribute type code
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> getAttributes(int vendorId, byte type) {
        if (vendorId == getChildVendorId())
            return getAttributes(type);

        return getVendorSpecificAttributes(vendorId).stream()
                .map(VendorSpecificAttribute::getAttributes)
                .flatMap(Collection::stream)
                .filter(sa -> sa.getType() == type && sa.getVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    @Override
    default List<RadiusAttribute> getAttributes(AttributeType type) {
        return getAttributes(type.getVendorId(), type.getType());
    }

    /**
     * Convenience method to get single attribute.
     * <p>
     * If vendorId doesn't match childVendorId, will search sub-attributes.
     *
     * @param vendorId vendor ID, or -1
     * @param type     attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default RadiusAttribute getAttribute(int vendorId, byte type) {
        List<RadiusAttribute> attrs = getAttributes(vendorId, type);
        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Adds a Radius attribute to this packet. Can also be used
     * to add Vendor-Specific sub-attributes. If a attribute with
     * a vendor code != -1 is passed in, a VendorSpecificAttribute
     * is created for the sub-attribute.
     *
     * @param attribute RadiusAttribute object
     */
    @Override
    default void addAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == getChildVendorId()) {
            getAttributes().add(attribute);
        } else {
            VendorSpecificAttribute vsa = new VendorSpecificAttribute(getDictionary(), new ArrayList<>(), attribute.getVendorId());
            vsa.addAttribute(attribute);
            getAttributes().add(vsa);
        }
    }

    /**
     * Removes the specified attribute from this packet.
     *
     * @param attribute RadiusAttribute to remove
     */
    @Override
    default void removeAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == getChildVendorId()) {
            getAttributes().remove(attribute);
        } else {
            removeSubAttribute(attribute);
        }
    }

    default void removeSubAttribute(RadiusAttribute attribute) {
        for (VendorSpecificAttribute vsa : getVendorSpecificAttributes(attribute.getVendorId())) {
            vsa.removeAttribute(attribute);
            if (vsa.getAttributes().isEmpty())
                // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                getAttributes().remove(vsa);
        }
    }

    /**
     * Removes all (sub)attributes of the given vendor and
     * type.
     * <p>
     * If vendorId doesn't match childVendorId, will search sub-attributes.
     *
     * @param vendorId vendor ID, or -1
     * @param typeCode attribute type code
     */
    default void removeAttributes(int vendorId, byte typeCode) {
        if (vendorId == getChildVendorId()) {
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
     * Returns the Vendor-Specific attribute(s) for the given vendor ID.
     *
     * @param vendorId vendor ID to filter by
     * @return List with VendorSpecificAttribute objects, or empty list
     */
    default List<VendorSpecificAttribute> getVendorSpecificAttributes(int vendorId) {
        return getAttributes().stream()
                .filter(VendorSpecificAttribute.class::isInstance)
                .map(VendorSpecificAttribute.class::cast)
                .filter(a -> a.getChildVendorId() == vendorId)
                .collect(Collectors.toList());
    }
}

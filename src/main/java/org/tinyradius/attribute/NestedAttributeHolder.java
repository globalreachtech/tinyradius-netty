package org.tinyradius.attribute;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.tinyradius.attribute.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * AttributeHolder that supports sub-attributes and filtering by vendorId
 */
public interface NestedAttributeHolder extends AttributeHolder {

    /**
     * @return -1, ie no restrictions on vendorIds of attributes
     */
    default int getVendorId() {
        return -1;
    }

    /**
     * Returns all attributes of this packet that match the
     * given type and vendorId.
     * <p>
     * If vendorId is -1, will only search for top level attributes.
     *
     * @param vendorId vendor ID, or -1
     * @param type     attribute type code
     * @return list of RadiusAttribute objects, or empty list
     */
    default List<RadiusAttribute> getAttributes(int vendorId, byte type) {
        if (vendorId == getVendorId())
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
     *
     * @param vendorId vendor ID
     * @param type     attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     */
    default RadiusAttribute getAttribute(int vendorId, byte type) {
        List<RadiusAttribute> attrs = getAttributes(vendorId, type);
        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Removes all (sub)attributes of the given vendor and
     * type.
     * <p>
     * If vendorId is -1, will only search for top level attributes.
     *
     * @param vendorId vendor ID, or -1
     * @param typeCode attribute type code
     */
    default void removeAttributes(int vendorId, byte typeCode) {
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
     * Returns the Vendor-Specific attribute(s) for the given vendor ID.
     *
     * @param vendorId vendor ID to filter by
     * @return List with VendorSpecificAttribute objects, or empty list
     */
    default List<VendorSpecificAttribute> getVendorSpecificAttributes(int vendorId) {
        return getAttributes(VENDOR_SPECIFIC).stream()
                .filter(VendorSpecificAttribute.class::isInstance)
                .map(VendorSpecificAttribute.class::cast)
                .filter(a -> a.getVendorId() == vendorId)
                .collect(Collectors.toList());
    }


}

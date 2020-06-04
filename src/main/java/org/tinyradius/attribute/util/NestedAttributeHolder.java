package org.tinyradius.attribute.util;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AttributeHolder that supports sub-attributes (wrapped by Vendor-Specific Attributes)
 * and filtering by vendorId.
 * <p>
 * An abstraction of all attribute management methods used by Radius packets.
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

        return getVendorAttributes(vendorId).stream()
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
     * Returns the Vendor-Specific attribute(s) for the given vendor ID.
     *
     * @param vendorId vendor ID to filter by
     * @return List with VendorSpecificAttribute objects, or empty list
     */
    default List<VendorSpecificAttribute> getVendorAttributes(int vendorId) {
        return getAttributes().stream()
                .filter(VendorSpecificAttribute.class::isInstance)
                .map(VendorSpecificAttribute.class::cast)
                .filter(a -> a.getChildVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    /**
     * @return List of attributes, flattening VSAs and unwrapping nested attributes if found
     */
    default List<RadiusAttribute> getFlattenedAttributes() {
        return getAttributes().stream()
                .map(RadiusAttribute::flatten)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    interface Writable extends NestedAttributeHolder, AttributeHolder.Writable {

        /**
         * Adds a Radius attribute to this packet. Can also be used
         * to add Vendor-Specific sub-attributes. If a attribute with
         * a vendor code != -1 is passed in, a VendorSpecificAttribute
         * is automatically created for the sub-attribute.
         *
         * @param attribute RadiusAttribute object
         */
        @Override
        default void addAttribute(RadiusAttribute attribute) {
            if (attribute.getVendorId() == getChildVendorId()) {
                AttributeHolder.Writable.super.addAttribute(attribute);
            } else {
                VendorSpecificAttribute vsa = new VendorSpecificAttribute(getDictionary(), Collections.singletonList(attribute), attribute.getVendorId());
                AttributeHolder.Writable.super.addAttribute(vsa);
            }
        }

        /**
         * Removes all instances of the specified attribute from this packet.
         *
         * @param attribute RadiusAttribute to remove
         */
        @Override
        default void removeAttribute(RadiusAttribute attribute) {
            if (attribute.getVendorId() == getChildVendorId()) {
                AttributeHolder.Writable.super.removeAttribute(attribute);
                return;
            }

            getAttributes().replaceAll(a -> {
                if (a instanceof VendorSpecificAttribute) {
                    final VendorSpecificAttribute.Builder builder = ((VendorSpecificAttribute) a).toBuilder();
                    builder.removeAttribute(attribute);

                    return builder.getAttributes().isEmpty() ? null : builder.build();
                }

                return a;
            });

            // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
            getAttributes().removeIf(Objects::isNull);
        }

        /**
         * Removes all (sub)attributes of the given vendor and type.
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

            getAttributes().replaceAll(a -> {
                if (a instanceof VendorSpecificAttribute) {
                    final VendorSpecificAttribute.Builder builder = ((VendorSpecificAttribute) a).toBuilder();
                    builder.getAttributes().removeIf(attr ->
                            attr.getType() == typeCode && attr.getVendorId() == vendorId);

                    return builder.getAttributes().isEmpty() ? null : builder.build();
                }

                return a;
            });

            // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
            getAttributes().removeIf(Objects::isNull);
        }
    }
}

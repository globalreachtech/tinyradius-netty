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

    interface Writable<T extends Writable<T>> extends NestedAttributeHolder, AttributeHolder.Writable<T> {

        /**
         * Adds a Radius attribute to this packet. Can also be used
         * to add Vendor-Specific sub-attributes. If a attribute with
         * a vendor code != -1 is passed in, a VendorSpecificAttribute
         * is automatically created for the sub-attribute.
         *
         * @param attribute RadiusAttribute object
         */
        @Override
        default T addAttribute(RadiusAttribute attribute) {
            final RadiusAttribute toAdd = attribute.getVendorId() == getChildVendorId() ?
                    attribute :
                    new VendorSpecificAttribute(getDictionary(), attribute.getVendorId(), Collections.singletonList(attribute));

            return AttributeHolder.Writable.super.addAttribute(toAdd);
        }

        /**
         * Removes all instances of the specified attribute from this packet.
         *
         * @param attribute RadiusAttribute to remove
         */
        @Override
        default T removeAttribute(RadiusAttribute attribute) {
            if (attribute.getVendorId() == getChildVendorId())
                return AttributeHolder.Writable.super.removeAttribute(attribute);

            final List<RadiusAttribute> attributes = getAttributes().stream().map(a -> {
                if (a instanceof VendorSpecificAttribute) {
                    final VendorSpecificAttribute vsa = (VendorSpecificAttribute) a;

                    if (vsa.getAttributes().contains(attribute)) {
                        final List<RadiusAttribute> vsaAttributes = vsa.getAttributes().stream()
                                .filter(sa -> !sa.equals(attribute))
                                .collect(Collectors.toList());

                        return vsaAttributes.isEmpty() ? null : vsa.withAttributes(vsaAttributes);
                    }
                }

                return a;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            return withAttributes(attributes);
        }

        /**
         * Removes all (sub)attributes of the given vendor and type.
         * <p>
         * If vendorId doesn't match childVendorId, will search sub-attributes.
         *
         * @param vendorId vendor ID, or -1
         * @param typeCode attribute type code
         */
        default T removeAttributes(int vendorId, byte typeCode) {
            if (vendorId == getChildVendorId())
                return removeAttributes(typeCode);

            final List<RadiusAttribute> attributes = getAttributes().stream().map(a -> {
                if (a instanceof VendorSpecificAttribute) {
                    final VendorSpecificAttribute vsa = (VendorSpecificAttribute) a;

                    final List<RadiusAttribute> vsaAttributes = vsa.getAttributes().stream()
                            .filter(sa -> sa.getType() != typeCode || sa.getVendorId() != vendorId)
                            .collect(Collectors.toList());

                    return vsaAttributes.isEmpty() ? null : vsa.withAttributes(vsaAttributes);
                }

                return a;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            return withAttributes(attributes);
        }
    }
}

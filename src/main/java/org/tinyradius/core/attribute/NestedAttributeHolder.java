package org.tinyradius.core.attribute;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.VendorSpecificAttribute;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * AttributeHolder that supports sub-attributes (wrapped by Vendor-Specific Attributes)
 * and filtering by vendorId.
 * <p>
 * An abstraction of all attribute management methods used by Radius packets.
 */
public interface NestedAttributeHolder<T extends NestedAttributeHolder<T>> extends AttributeHolder<T> {

    int CHILD_VENDOR_ID = -1;

    static RadiusAttribute vsaAutowrap(RadiusAttribute attribute) {
        return attribute.getVendorId() == CHILD_VENDOR_ID ?
                attribute :
                new VendorSpecificAttribute(attribute.getDictionary(), attribute.getVendorId(), List.of(attribute));
    }

    @Override
    default int getChildVendorId() {
        return CHILD_VENDOR_ID;
    }

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
    default List<RadiusAttribute> getAttributes(int vendorId, int type) {
        if (vendorId == getChildVendorId())
            return getAttributes(type);

        return getVendorAttributes(vendorId).stream()
                .map(VendorSpecificAttribute::getAttributes)
                .flatMap(Collection::stream)
                .filter(sa -> sa.getType() == type && sa.getVendorId() == vendorId)
                .collect(toList());
    }

    @Override
    default List<RadiusAttribute> getAttributes(AttributeTemplate type) {
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
    default Optional<RadiusAttribute> getAttribute(int vendorId, int type) {
        return getAttributes(vendorId, type).stream().findFirst();
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
                .collect(toList());
    }

    /**
     * @return List of attributes, flattening VSAs and unwrapping nested attributes if found
     */
    default List<RadiusAttribute> getFlattenedAttributes() {
        return getAttributes().stream()
                .map(RadiusAttribute::flatten)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    /**
     * Adds a Radius attribute to this packet. Can also be used
     * to add Vendor-Specific sub-attributes. If an attribute with
     * a vendor code != -1 is passed in, a VendorSpecificAttribute
     * is automatically created for the sub-attribute.
     *
     * @param attribute RadiusAttribute object
     */
    @Override
    default T addAttribute(RadiusAttribute attribute) throws RadiusPacketException {
        return AttributeHolder.super.addAttribute(vsaAutowrap(attribute));
    }

    /**
     * Removes all instances of the specified attribute from this packet.
     *
     * @param attribute RadiusAttribute to remove
     */
    @Override
    default T removeAttribute(RadiusAttribute attribute) throws RadiusPacketException {
        if (attribute.getVendorId() == getChildVendorId())
            return AttributeHolder.super.removeAttribute(attribute);

        final List<RadiusAttribute> attributes = getAttributes().stream()
                .map(a -> {
                    if (!(a instanceof VendorSpecificAttribute vsa))
                        return a;

                    final List<RadiusAttribute> subAttributes = vsa.getAttributes(sa -> !sa.equals(attribute));
                    return subAttributes.isEmpty() ? null : vsa.withAttributes(subAttributes);
                })
                .filter(Objects::nonNull)
                .collect(toList());

        return withAttributes(attributes);
    }

    /**
     * Removes all (sub)attributes of the given vendor and type.
     * <p>
     * If vendorId doesn't match childVendorId, will search sub-attributes.
     *
     * @param vendorId vendor ID, or -1
     * @param type     attribute type code
     * @return object of same type with removed attributes
     * @throws RadiusPacketException packet validation exceptions
     */
    default T removeAttributes(int vendorId, int type) throws RadiusPacketException {
        if (vendorId == getChildVendorId())
            return removeAttributes(type);

        final List<RadiusAttribute> attributes = getAttributes().stream()
                .map(a -> {
                    if (!(a instanceof VendorSpecificAttribute vsa))
                        return a;

                    final List<RadiusAttribute> subAttributes = vsa.getAttributes(sa -> sa.getType() != type || sa.getVendorId() != vendorId);
                    return subAttributes.isEmpty() ? null : vsa.withAttributes(subAttributes);
                })
                .filter(Objects::nonNull)
                .collect(toList());

        return withAttributes(attributes);
    }
}

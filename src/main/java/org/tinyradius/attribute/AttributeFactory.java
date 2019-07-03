package org.tinyradius.attribute;

public interface AttributeFactory<T extends RadiusAttribute> {

    T create(int attributeType, int vendorId);
}

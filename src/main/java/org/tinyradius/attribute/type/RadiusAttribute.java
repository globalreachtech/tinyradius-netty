package org.tinyradius.attribute.type;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;
import java.util.Optional;

public interface RadiusAttribute {
    byte[] getValue();

    byte getType();

    String getValueString();

    int getVendorId();

    Dictionary getDictionary();

    byte[] toByteArray();

    @Override
    String toString();

    String getAttributeName();

    List<RadiusAttribute> flatten();

    Optional<AttributeTemplate> getAttributeTemplate();
}

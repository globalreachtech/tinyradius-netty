package org.tinyradius.attribute.type.decorator;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class BaseDecorator implements RadiusAttribute {

    protected final RadiusAttribute delegate;

    public BaseDecorator(RadiusAttribute attribute) {
        delegate = Objects.requireNonNull(attribute);
    }

    @Override
    public int getVendorId() {
        return delegate.getVendorId();
    }

    @Override
    public byte getType() {
        return delegate.getType();
    }

    @Override
    public byte getTag() {
        return delegate.getTag();
    }

    @Override
    public byte[] getValue() {
        return delegate.getValue();
    }

    @Override
    public String getValueString() {
        return delegate.getValueString();
    }

    @Override
    public Dictionary getDictionary() {
        return delegate.getDictionary();
    }

    @Override
    public byte[] toByteArray() {
        return delegate.toByteArray();
    }

    @Override
    public String getAttributeName() {
        return delegate.getAttributeName();
    }

    @Override
    public List<RadiusAttribute> flatten() {
        return delegate.flatten();
    }

    @Override
    public Optional<AttributeTemplate> getAttributeTemplate() {
        return delegate.getAttributeTemplate();
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) {
        return this;
    }

    @Override
    public RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return this;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}

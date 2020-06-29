package org.tinyradius.attribute.type;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.attribute.encrypt.AttributeCodecType;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;
import java.util.Optional;

public class EncodedAttribute implements RadiusAttribute {

    private final OctetsAttribute delegate;

    public EncodedAttribute(OctetsAttribute attribute) {
        delegate = attribute;
    }

    @Override
    public byte[] getValue() {
        return delegate.getValue();
    }

    @Override
    public byte getType() {
        return delegate.getType();
    }

    @Override
    public String getValueString() {
        return delegate.getValueString();
    }

    @Override
    public int getVendorId() {
        return delegate.getVendorId();
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
    public EncodedAttribute encode(String secret, byte[] requestAuth) {
        return this;
    }

    @Override
    public OctetsAttribute decode(String secret, byte[] requestAuth) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, secret, requestAuth) : delegate;
    }

    @Override
    public String toString() {
        final AttributeCodecType codecType = getAttributeTemplate()
                .map(AttributeTemplate::getCodecType)
                .orElse(AttributeCodecType.NO_ENCRYPT);
        return "(" + codecType.name() + " encoded) " + getAttributeName() + ": " + getValueString();
    }
}

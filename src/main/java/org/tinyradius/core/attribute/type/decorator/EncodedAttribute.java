package org.tinyradius.core.attribute.type.decorator;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper around attributes encoded with one of {@link AttributeCodecType}
 */
public class EncodedAttribute implements RadiusAttribute {

    private final RadiusAttribute delegate;

    public EncodedAttribute(RadiusAttribute attribute) {
        delegate = Objects.requireNonNull(attribute);
        if (attribute instanceof EncodedAttribute)
            throw new IllegalArgumentException("Cannot wrap EncodedDecorator twice");
    }

    @Override
    public int getVendorId() {
        return delegate.getVendorId();
    }

    @Override
    public int getType() {
        return delegate.getType();
    }

    @Override
    public Optional<Byte> getTag() {
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
    public RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, requestAuth, secret) : delegate;
    }

    @Override
    public boolean isEncoded() {
        return true;
    }

    @Override
    public Dictionary getDictionary() {
        return delegate.getDictionary();
    }

    @Override
    public String toString() {
        final AttributeCodecType codecType = getAttributeTemplate()
                .map(AttributeTemplate::getCodecType)
                .orElse(AttributeCodecType.NO_ENCRYPT);
        return "[Encoded: " + codecType.name() + "] " + delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncodedAttribute)) return false;
        final EncodedAttribute that = (EncodedAttribute) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}

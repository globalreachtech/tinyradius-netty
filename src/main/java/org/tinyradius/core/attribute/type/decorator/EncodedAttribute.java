package org.tinyradius.core.attribute.type.decorator;

import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.RadiusPacketException;

import java.util.Objects;
import java.util.Optional;

public class EncodedAttribute extends BaseDecorator {

    public EncodedAttribute(RadiusAttribute attribute) {
        super(attribute);
        if (attribute instanceof EncodedAttribute)
            throw new IllegalArgumentException("Cannot wrap EncodedDecorator twice");
    }

    @Override
    public RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, requestAuth, secret) : delegate;
        // what if EncodedDecorator is not guaranteed to be outermost decorator?
    }

    @Override
    public boolean isEncoded() {
        return true;
    }

    @Override
    public byte[] toByteArray() {
        return delegate.toByteArray();
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

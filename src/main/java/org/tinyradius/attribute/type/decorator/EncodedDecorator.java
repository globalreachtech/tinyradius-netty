package org.tinyradius.attribute.type.decorator;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.attribute.codec.AttributeCodecType;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.util.RadiusPacketException;

import java.util.Objects;
import java.util.Optional;

public class EncodedDecorator extends BaseDecorator {

    public EncodedDecorator(RadiusAttribute attribute) {
        super(attribute);
        if (attribute instanceof EncodedDecorator)
            throw new IllegalArgumentException("Cannot wrap EncodedDecorator twice");
    }

    @Override
    public RadiusAttribute decode(String secret, byte[] requestAuth) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, secret, requestAuth) : delegate;
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
        if (!(o instanceof EncodedDecorator)) return false;
        final EncodedDecorator that = (EncodedDecorator) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}

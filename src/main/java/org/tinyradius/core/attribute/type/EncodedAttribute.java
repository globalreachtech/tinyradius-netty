package org.tinyradius.core.attribute.type;

import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;

import java.util.Objects;
import java.util.Optional;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Wrapper around attributes encoded with one of {@link AttributeCodecType}
 */
@EqualsAndHashCode
public class EncodedAttribute implements RadiusAttribute {

    @Delegate
    private final RadiusAttribute delegate;

    public EncodedAttribute(RadiusAttribute attribute) {
        delegate = Objects.requireNonNull(attribute);
        if (attribute instanceof EncodedAttribute)
            throw new IllegalArgumentException("Cannot wrap EncodedDecorator twice");
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
    public String toString() {
        var codecType = getAttributeTemplate()
                .map(AttributeTemplate::getCodecType)
                .orElse(NO_ENCRYPT);
        return "[Encoded: " + codecType.name() + "] " + delegate;
    }
}

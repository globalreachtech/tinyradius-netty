package org.tinyradius.core.attribute.type;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;

import java.util.Optional;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Wrapper around attributes encoded with one of {@link AttributeCodecType}
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class EncodedAttribute implements RadiusAttribute {

    @Delegate
    private final OctetsAttribute delegate;

    @Override
    public RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, requestAuth, secret) : delegate;
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        return RadiusAttribute.super.encode(requestAuth, secret);
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

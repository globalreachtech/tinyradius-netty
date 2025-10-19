package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Optional;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Wrapper around attributes encoded with one of {@link AttributeCodecType}
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class EncodedAttribute implements RadiusAttribute {

    private final OctetsAttribute delegate;

    @Override
    public RadiusAttribute decode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, requestAuth, secret) : delegate;
    }

    @Override
    public int getVendorId() {
        return delegate.getVendorId();
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
    public Dictionary getDictionary() {
        return delegate.getDictionary();
    }

    @Override
    public ByteBuf getData() {
        return delegate.getData();
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

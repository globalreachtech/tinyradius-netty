package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.codec.AttributeCodecType;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Optional;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Wrapper around attributes encoded with one of {@link AttributeCodecType}
 */
public record EncodedAttribute(OctetsAttribute delegate) implements RadiusAttribute {

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public RadiusAttribute decode(@NonNull byte[] requestAuth, @NonNull String secret) throws RadiusPacketException {
        var template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().decode(this, requestAuth, secret) : delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVendorId() {
        return delegate.getVendorId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Optional<Byte> getTag() {
        return delegate.getTag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public byte[] getValue() {
        return delegate.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getValueString() {
        return delegate.getValueString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Dictionary getDictionary() {
        return delegate.getDictionary();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ByteBuf getData() {
        return delegate.getData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public RadiusAttribute encode(@NonNull byte[] requestAuth, @NonNull String secret) throws RadiusPacketException {
        return RadiusAttribute.super.encode(requestAuth, secret);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEncoded() {
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        var codecType = getAttributeTemplate()
                .map(AttributeTemplate::getCodecType)
                .orElse(NO_ENCRYPT);
        return "[Encoded: " + codecType.name() + "] " + delegate;
    }
}

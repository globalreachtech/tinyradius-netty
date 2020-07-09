package org.tinyradius.core.attribute.type.decorator;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * Augments attribute with RFC2868 Tag. If using multiple wrapping decorators,
 * this should be innermost.
 */
public class TaggedAttribute extends AbstractDecorator {

    private final byte tag;

    public TaggedAttribute(byte tag, OctetsAttribute attribute) {
        super(attribute);
        this.tag = tag;
    }

    @Override
    public byte getTag() {
        return tag;
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, requestAuth, secret) :
                this;
    }

    @Override
    public String toString() {
        return "[Tagged: " + getTag() + "] " + delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaggedAttribute)) return false;
        final TaggedAttribute that = (TaggedAttribute) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}

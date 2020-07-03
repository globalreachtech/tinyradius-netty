package org.tinyradius.attribute.type.decorator;

import org.tinyradius.attribute.type.OctetsAttribute;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TaggedAttribute extends BaseDecorator {

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
    public byte[] toByteArray() {
        final int len = getValue().length + 3;
        return ByteBuffer.allocate(len)
                .put(getType())
                .put((byte) len)
                .put(getTag())
                .put(getValue())
                .array();
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

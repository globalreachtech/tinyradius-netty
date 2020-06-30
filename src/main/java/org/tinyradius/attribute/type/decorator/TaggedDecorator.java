package org.tinyradius.attribute.type.decorator;

import org.tinyradius.attribute.type.OctetsAttribute;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TaggedDecorator extends BaseDecorator {

    private final byte tag;

    public TaggedDecorator(OctetsAttribute attribute, byte tag) {
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
        return "[Tagged: " + String.valueOf(getTag()) + "] " + delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaggedDecorator)) return false;
        final TaggedDecorator that = (TaggedDecorator) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}

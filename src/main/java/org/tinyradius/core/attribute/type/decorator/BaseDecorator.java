package org.tinyradius.core.attribute.type.decorator;

import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Objects;

/**
 * Specifically override and use delegate instead of using default impls for properties that decorators may want to
 * change or impact.
 * <p>
 * Specifically exclude properties that need to be aware of outer decorators. Want to use the implementation provided
 * by the outermost decorator.
 * <p>
 * Rule of thumb is top level methods that aren't called by other methods can live in RadiusAttribute.
 * <p>
 * The problem is that any method must only call other methods that are in the same level or more inner decorator,
 * but how to enforce?
 */
abstract class BaseDecorator implements RadiusAttribute {

    protected final RadiusAttribute delegate;

    public BaseDecorator(RadiusAttribute attribute) {
        delegate = Objects.requireNonNull(attribute);
    }

    @Override
    public int getVendorId() {
        return delegate.getVendorId();
    }

    @Override
    public byte getType() {
        return delegate.getType();
    }

    @Override
    public byte getTag() {
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
}

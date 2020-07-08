package org.tinyradius.core.attribute.type.decorator;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.StringAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseDecoratorTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void transformationsDontLoseDecorator() {
        final RadiusAttribute attribute = new EncodedAttribute(new TaggedAttribute((byte) 0, new StringAttribute(dictionary, -1, (byte) 0, "myUsername")));
        final RadiusAttribute transformed = attribute.flatten().get(0);
        assertEquals(attribute, transformed);
    }

}
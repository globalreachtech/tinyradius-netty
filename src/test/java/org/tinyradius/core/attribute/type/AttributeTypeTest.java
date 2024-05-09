package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AttributeTypeTest {

    @Test
    void createFallbackFieldSizes() throws IOException {
        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/test_dictionary");

        assertTrue(dictionary.getVendor(4846).isPresent()); // format=2,1

        final IntegerAttribute attribute1 = IntegerAttribute.FACTORY.create(dictionary, 4846, 1, (byte) 1, new byte[4]);
        assertEquals(7, attribute1.toByteArray().length);

        assertFalse(dictionary.getVendor(9999).isPresent()); // default 1,1

        final IntegerAttribute attribute2 = IntegerAttribute.FACTORY.create(dictionary, 9999, 1, (byte) 1, new byte[4]);
        assertEquals(6, attribute2.toByteArray().length);

    }
}
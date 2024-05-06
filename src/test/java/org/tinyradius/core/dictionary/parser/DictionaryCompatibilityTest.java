package org.tinyradius.core.dictionary.parser;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO
class DictionaryCompatibilityTest {

    @Test
    @Disabled("throws NumberFormatException: For input string: \"241.26\"")
    void latestFreeRadiusDict() throws IOException {
        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary");
        final Optional<AttributeTemplate<?>> attributeTemplate = dictionary.getAttributeTemplate(6);
        assertTrue(attributeTemplate.isPresent());

        // sanity check
        assertEquals("Service-Type", attributeTemplate.get().getName());
        assertEquals("Login-User", attributeTemplate.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, 207).get().getName());
    }

    @Test
    @Disabled("throws IOException: Unknown attribute type while parsing VALUE: Framed-Compression, line: 20")
    void jradiusDict() throws IOException {
        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/jradius/dictionary");
        final Optional<AttributeTemplate<?>> attributeTemplate = dictionary.getAttributeTemplate(6);
        assertTrue(attributeTemplate.isPresent());

        // sanity check
        assertEquals("Service-Type", attributeTemplate.get().getName());
        assertEquals("Login-User", attributeTemplate.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, 207).get().getName());
    }

}
package org.tinyradius.dictionary;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.AttributeTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryParserTest {

    private static final String PACKAGE_PREFIX = "org/tinyradius/dictionary/";

    private static final String TEST_DICTIONARY = "test_dictionary";
    private static final String DEFAULT_DICTIONARY = "default_dictionary";
    private static final String RFC_DICTIONARY = "dictionary.rfc5904";

    @Test
    void classpathIncludeDict() throws IOException {
        final DictionaryParser parser = DictionaryParser.newClasspathParser();
        final Dictionary dictionary = parser.parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final Optional<AttributeTemplate> serviceTypeAttr = dictionary.getAttributeTemplate((byte) 6);
        assertTrue(serviceTypeAttr.isPresent());
        assertEquals("Service-Type", serviceTypeAttr.get().getName());
        assertEquals("Login-User", serviceTypeAttr.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, (byte) 207).get().getName());
    }

    @Test
    void fileSystemIncludeDict() throws IOException {
        final Path tmpPath = Files.createTempDirectory("tinyradius_test_");
        copyDict(tmpPath, TEST_DICTIONARY);
        copyDict(tmpPath, DEFAULT_DICTIONARY);
        copyDict(tmpPath, RFC_DICTIONARY);

        final DictionaryParser parser = DictionaryParser.newFileParser();
        final Dictionary dictionary = parser.parseDictionary(tmpPath + "/" + TEST_DICTIONARY);

        final Optional<AttributeTemplate> serviceTypeAttr = dictionary.getAttributeTemplate((byte) 6);
        assertTrue(serviceTypeAttr.isPresent());
        assertEquals("Service-Type", serviceTypeAttr.get().getName());
        assertEquals("Login-User", serviceTypeAttr.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, (byte) 207).get().getName());

        Files.delete(tmpPath.resolve(TEST_DICTIONARY));
        Files.delete(tmpPath.resolve(DEFAULT_DICTIONARY));
        Files.delete(tmpPath.resolve(RFC_DICTIONARY));
    }

    private static void copyDict(Path tempDir, String fileName) throws IOException {
        ClassLoader classLoader = DictionaryParserTest.class.getClassLoader();
        Files.copy(requireNonNull(classLoader.getResourceAsStream(PACKAGE_PREFIX + fileName)),
                tempDir.resolve(fileName), REPLACE_EXISTING);
    }
}
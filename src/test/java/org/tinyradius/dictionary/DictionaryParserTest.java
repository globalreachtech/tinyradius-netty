package org.tinyradius.dictionary;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.dictionary.parse.DictionaryParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryParserTest {

    private static final String PACKAGE_PREFIX = "org/tinyradius/dictionary/";
    private static final String TEST_DICTIONARY = "test_dictionary";

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
        copyDict(tmpPath, "default_dictionary");
        copyDict(tmpPath, "dictionary.rfc5904");
        copyDict(tmpPath, "dictionary.wispr");
        copyDict(tmpPath, "dictionary.ascend");

        final DictionaryParser parser = DictionaryParser.newFileParser();
        final Dictionary dictionary = parser.parseDictionary(tmpPath + "/" + TEST_DICTIONARY);

        final Optional<AttributeTemplate> serviceTypeAttr = dictionary.getAttributeTemplate((byte) 6);
        assertTrue(serviceTypeAttr.isPresent());
        assertEquals("Service-Type", serviceTypeAttr.get().getName());
        assertEquals("Login-User", serviceTypeAttr.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, (byte) 207).get().getName());

        Files.walk(tmpPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static void copyDict(Path tempDir, String fileName) throws IOException {
        ClassLoader classLoader = DictionaryParserTest.class.getClassLoader();
        Files.copy(requireNonNull(classLoader.getResourceAsStream(PACKAGE_PREFIX + fileName)),
                tempDir.resolve(fileName), REPLACE_EXISTING);
    }
}
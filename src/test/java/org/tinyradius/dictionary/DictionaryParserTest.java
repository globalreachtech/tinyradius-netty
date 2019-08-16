package org.tinyradius.dictionary;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.AttributeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DictionaryParserTest {

    private static final String PACKAGE_PREFIX = "org/tinyradius/dictionary/";

    private static final String TEST_DICTIONARY = "test_dictionary";
    private static final String DEFAULT_DICTIONARY = "default_dictionary";
    private static final String RFC_DICTIONARY = "dictionary.rfc5904";

    private static Path tmpPath;

    @BeforeAll
    static void setup() throws IOException {
        tmpPath = Files.createTempDirectory("tinyradius_test_");
        copyDic(tmpPath, TEST_DICTIONARY);
        copyDic(tmpPath, DEFAULT_DICTIONARY);
        copyDic(tmpPath, RFC_DICTIONARY);
    }

    @AfterAll
    static void tearDown() throws IOException {
        Files.delete(tmpPath.resolve(TEST_DICTIONARY));
        Files.delete(tmpPath.resolve(DEFAULT_DICTIONARY));
        Files.delete(tmpPath.resolve(RFC_DICTIONARY));
    }

    @Test
    void classpathIncludeDictionary() throws IOException {
        final DictionaryParser parser = DictionaryParser.newClasspathParser();

        final Dictionary dictionary = parser.parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeType serviceTypeAttr = dictionary.getAttributeTypeByCode(6);
        assertNotNull(serviceTypeAttr);
        assertEquals("Service-Type", serviceTypeAttr.getName());
        assertEquals("Login-User", serviceTypeAttr.getEnumeration(1));
    }

    @Test
    void fileSystemIncludeDictionary() throws IOException {
        final DictionaryParser parser = DictionaryParser.newFileParser();

        final Dictionary dictionary = parser.parseDictionary(tmpPath + "/" + TEST_DICTIONARY);

        final AttributeType serviceTypeAttr = dictionary.getAttributeTypeByCode(6);
        assertNotNull(serviceTypeAttr);
        assertEquals("Service-Type", serviceTypeAttr.getName());
        assertEquals("Login-User", serviceTypeAttr.getEnumeration(1));
    }

    private static void copyDic(Path tempDir, String fileName) throws IOException {
        ClassLoader classLoader = DictionaryParserTest.class.getClassLoader();
        Files.copy(requireNonNull(classLoader.getResourceAsStream(PACKAGE_PREFIX + fileName)),
                tempDir.resolve(fileName), REPLACE_EXISTING);
    }
}
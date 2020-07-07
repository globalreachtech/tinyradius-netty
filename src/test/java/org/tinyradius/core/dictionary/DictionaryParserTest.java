package org.tinyradius.core.dictionary;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.parse.DictionaryParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

class DictionaryParserTest {

    private static final String PACKAGE_PREFIX = "org/tinyradius/core/dictionary/";
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
    void testVendorAttrFlags() throws IOException {
        // VENDORATTR      14122   WISPr-Bandwidth-Min-Up         5       integer has_tag

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate attribute = dictionary.getAttributeTemplate("WISPr-Redirection-URL").get();
        assertFalse(attribute.hasTag());
        assertEquals(NO_ENCRYPT, attribute.getCodecType());

        final AttributeTemplate tagAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Min-Up").get();
        assertTrue(tagAttribute.hasTag());
        assertEquals(NO_ENCRYPT, tagAttribute.getCodecType());

        final AttributeTemplate encryptAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Min-Down").get();
        assertFalse(encryptAttribute.hasTag());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.getCodecType());

        final AttributeTemplate tagEncryptAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Max-Up").get();
        assertTrue(tagEncryptAttribute.hasTag());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.getCodecType());

        final AttributeTemplate encryptTagAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Max-Down").get();
        assertTrue(encryptTagAttribute.hasTag());
        assertEquals(ASCENT_SEND_SECRET, encryptTagAttribute.getCodecType());
    }

    @Test
    void testStatefulVendorAttributeFlags() throws IOException {
        // BEGIN-VENDOR	Ascend
        // ATTRIBUTE	Ascend-Max-Shared-Users			2	integer  has_tag

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate attribute = dictionary.getAttributeTemplate("Ascend-Test").get();
        assertFalse(attribute.hasTag());
        assertEquals(NO_ENCRYPT, attribute.getCodecType());

        final AttributeTemplate tagAttribute = dictionary.getAttributeTemplate("Ascend-Max-Shared-Users").get();
        assertTrue(tagAttribute.hasTag());
        assertEquals(NO_ENCRYPT, tagAttribute.getCodecType());

        final AttributeTemplate encryptAttribute = dictionary.getAttributeTemplate("Ascend-UU-Info").get();
        assertFalse(encryptAttribute.hasTag());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.getCodecType());

        final AttributeTemplate tagEncryptAttribute = dictionary.getAttributeTemplate("Ascend-CIR-Timer").get();
        assertTrue(tagEncryptAttribute.hasTag());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.getCodecType());

        final AttributeTemplate encryptTagAttribute = dictionary.getAttributeTemplate("Ascend-FR-08-Mode").get();
        assertTrue(encryptTagAttribute.hasTag());
        assertEquals(ASCENT_SEND_SECRET, encryptTagAttribute.getCodecType());
    }

    @Test
    void testAttributeFlags() throws IOException {
        // ATTRIBUTE PKM-SAID    141      short encrypt=1

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate attribute = dictionary.getAttributeTemplate("PKM-Config-Settings").get();
        assertFalse(attribute.hasTag());
        assertEquals(NO_ENCRYPT, attribute.getCodecType());

        final AttributeTemplate tagAttribute = dictionary.getAttributeTemplate("PKM-Cryptosuite-List").get();
        assertTrue(tagAttribute.hasTag());
        assertEquals(NO_ENCRYPT, tagAttribute.getCodecType());

        final AttributeTemplate encryptAttribute = dictionary.getAttributeTemplate("PKM-SAID").get();
        assertFalse(encryptAttribute.hasTag());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.getCodecType());

        final AttributeTemplate tagEncryptAttribute = dictionary.getAttributeTemplate("PKM-SA-Descriptor").get();
        assertTrue(tagEncryptAttribute.hasTag());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.getCodecType());

        final AttributeTemplate encryptTagAttribute = dictionary.getAttributeTemplate("PKM-Auth-Key").get();
        assertTrue(encryptTagAttribute.hasTag());
        assertEquals(ASCENT_SEND_SECRET, encryptTagAttribute.getCodecType());
    }

    @Test
    void valueEnumDeferred() throws IOException {
        // parse VALUE before corresponding ATTRIBUTE

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate template = dictionary.getAttributeTemplate("Timetra-Restrict-To-Home").get();

        assertEquals("true", template.getEnumeration(1));
        assertEquals("false", template.getEnumeration(2));
    }

    @Test
    void fileSystemIncludeDict() throws IOException {
        final Path tmpPath = Files.createTempDirectory("tinyradius_test_");
        copyDict(tmpPath, TEST_DICTIONARY);
        copyDict(tmpPath, "default_dictionary");
        copyDict(tmpPath, "dictionary.rfc5904");
        copyDict(tmpPath, "dictionary.wispr");
        copyDict(tmpPath, "dictionary.ascend");
        copyDict(tmpPath, "dictionary.alcatel.sr");

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
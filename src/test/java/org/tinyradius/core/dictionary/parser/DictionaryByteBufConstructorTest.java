package org.tinyradius.core.dictionary.parser;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

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

class DictionaryByteBufConstructorTest {

    private static final String PACKAGE_PREFIX = "org/tinyradius/core/dictionary/";
    private static final String TEST_DICTIONARY = "test_dictionary";

    @Test
    void classpathIncludeDict() throws IOException {
        final DictionaryParser parser = DictionaryParser.newClasspathParser();
        final Dictionary dictionary = parser.parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final Optional<AttributeTemplate> serviceTypeAttr = dictionary.getAttributeTemplate(6);
        assertTrue(serviceTypeAttr.isPresent());
        assertEquals("Service-Type", serviceTypeAttr.get().getName());
        assertEquals("Login-User", serviceTypeAttr.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, 207).get().getName());
    }

    @Test
    void testVendorAttrFlags() throws IOException {
        // VENDORATTR      14122   WISPr-Bandwidth-Min-Up         5       integer has_tag

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate attribute = dictionary.getAttributeTemplate("WISPr-Redirection-URL").get();
        assertFalse(attribute.isTagged());
        assertEquals(NO_ENCRYPT, attribute.getCodecType());

        final AttributeTemplate tagAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Min-Up").get();
        assertTrue(tagAttribute.isTagged());
        assertEquals(NO_ENCRYPT, tagAttribute.getCodecType());

        final AttributeTemplate encryptAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Min-Down").get();
        assertFalse(encryptAttribute.isTagged());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.getCodecType());

        final AttributeTemplate tagEncryptAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Max-Up").get();
        assertTrue(tagEncryptAttribute.isTagged());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.getCodecType());

        final AttributeTemplate encryptTagAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Max-Down").get();
        assertTrue(encryptTagAttribute.isTagged());
        assertEquals(ASCENT_SEND_SECRET, encryptTagAttribute.getCodecType());
    }

    @Test
    void testStatefulVendorAttributeFlags() throws IOException {
        // BEGIN-VENDOR	Ascend
        // ATTRIBUTE	Ascend-Max-Shared-Users			2	integer  has_tag

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate attribute = dictionary.getAttributeTemplate("Ascend-Test").get();
        assertFalse(attribute.isTagged());
        assertEquals(NO_ENCRYPT, attribute.getCodecType());

        final AttributeTemplate tagAttribute = dictionary.getAttributeTemplate("Ascend-Max-Shared-Users").get();
        assertTrue(tagAttribute.isTagged());
        assertEquals(NO_ENCRYPT, tagAttribute.getCodecType());

        final AttributeTemplate encryptAttribute = dictionary.getAttributeTemplate("Ascend-UU-Info").get();
        assertFalse(encryptAttribute.isTagged());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.getCodecType());

        final AttributeTemplate tagEncryptAttribute = dictionary.getAttributeTemplate("Ascend-CIR-Timer").get();
        assertTrue(tagEncryptAttribute.isTagged());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.getCodecType());

        final AttributeTemplate encryptTagAttribute = dictionary.getAttributeTemplate("Ascend-FR-08-Mode").get();
        assertTrue(encryptTagAttribute.isTagged());
        assertEquals(ASCENT_SEND_SECRET, encryptTagAttribute.getCodecType());
    }

    @Test
    void testAttributeFlags() throws IOException {
        // ATTRIBUTE PKM-SAID    141      short encrypt=1

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate attribute = dictionary.getAttributeTemplate("PKM-Config-Settings").get();
        assertFalse(attribute.isTagged());
        assertEquals(NO_ENCRYPT, attribute.getCodecType());

        final AttributeTemplate tagAttribute = dictionary.getAttributeTemplate("PKM-Cryptosuite-List").get();
        assertTrue(tagAttribute.isTagged());
        assertEquals(NO_ENCRYPT, tagAttribute.getCodecType());

        final AttributeTemplate encryptAttribute = dictionary.getAttributeTemplate("PKM-SAID").get();
        assertFalse(encryptAttribute.isTagged());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.getCodecType());

        final AttributeTemplate tagEncryptAttribute = dictionary.getAttributeTemplate("PKM-SA-Descriptor").get();
        assertTrue(tagEncryptAttribute.isTagged());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.getCodecType());

        final AttributeTemplate encryptTagAttribute = dictionary.getAttributeTemplate("PKM-Auth-Key").get();
        assertTrue(encryptTagAttribute.isTagged());
        assertEquals(ASCENT_SEND_SECRET, encryptTagAttribute.getCodecType());
    }

    @Test
    void valueEnumDeferred() throws IOException {
        // parse VALUE before corresponding ATTRIBUTE

        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate template = dictionary.getAttributeTemplate("Timetra-Restrict-To-Home").get();

        assertEquals("true", template.getEnumeration(1));
        assertEquals(1, template.getEnumeration("true"));
        assertEquals("false", template.getEnumeration(2));
        assertEquals(2, template.getEnumeration("false"));
    }

    @Test
    void valueAttributeNonDecimal() throws IOException {
        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate serviceType = dictionary.getAttributeTemplate("Service-Type").get();
        assertEquals(0x06300001, serviceType.getEnumeration("Annex-Authorize-Only"));
        assertEquals("Annex-Authorize-Only", serviceType.getEnumeration(0x06300001));

        final AttributeTemplate acctStatusType = dictionary.getAttributeTemplate("Acct-Status-Type").get();
        assertEquals(0x06300001, acctStatusType.getEnumeration("Annex-User-Reject"));
        assertEquals("Annex-User-Reject", acctStatusType.getEnumeration(0x06300001));
    }

    @Test
    void vendorFormatFlag() throws IOException {
        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final Vendor wispr = dictionary.getVendor("WISPr").get();
        assertSame(dictionary.getVendor(14122).get(), wispr);
        assertEquals(14122, wispr.getId());
        assertEquals("WISPr", wispr.getName());
        assertEquals(1, wispr.getTypeSize());
        assertEquals(1, wispr.getLengthSize());

        final Vendor lucent = dictionary.getVendor("Lucent").get();
        assertSame(dictionary.getVendor(4846).get(), lucent);
        assertEquals(4846, lucent.getId());
        assertEquals("Lucent", lucent.getName());
        assertEquals(2, lucent.getTypeSize());
        assertEquals(1, lucent.getLengthSize());
    }

    @Test
    void attributeCustomTypeSize() throws IOException {
        final Dictionary dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary(PACKAGE_PREFIX + TEST_DICTIONARY);

        final AttributeTemplate basic = dictionary.getAttributeTemplate("Lucent-Max-Shared-Users").get();
        assertEquals(2, basic.getType());

        final AttributeTemplate custom = dictionary.getAttributeTemplate("Lucent-Retrain-Reason").get();
        assertEquals(20119, custom.getType());
    }

    @Test
    void fileSystemIncludeDict() throws IOException {
        final Path tmpPath = Files.createTempDirectory("tinyradius_test_");
        copyDict(tmpPath, TEST_DICTIONARY,
                "default_dictionary",
                "dictionary.rfc5904",
                "dictionary.wispr",
                "dictionary.ascend",
                "dictionary.alcatel.sr");

        final DictionaryParser parser = DictionaryParser.newFileParser();
        final Dictionary dictionary = parser.parseDictionary(tmpPath + "/" + TEST_DICTIONARY);

        final Optional<AttributeTemplate> serviceTypeAttr = dictionary.getAttributeTemplate(6);
        assertTrue(serviceTypeAttr.isPresent());
        assertEquals("Service-Type", serviceTypeAttr.get().getName());
        assertEquals("Login-User", serviceTypeAttr.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, 207).get().getName());

        Files.walk(tmpPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private void copyDict(Path tempDir, String... files) throws IOException {
        for (final String file : files) {
            Files.copy(
                    requireNonNull(this.getClass().getClassLoader().getResourceAsStream(PACKAGE_PREFIX + file)),
                    tempDir.resolve(file), REPLACE_EXISTING);
        }
    }
}
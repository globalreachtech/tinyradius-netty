package org.tinyradius.core.dictionary.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.resolver.ClasspathResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.FileResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.ResourceResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class DictionaryParserTest {

    private static final String PACKAGE_PREFIX = "org/tinyradius/core/dictionary/";
    private static final String TEST_DICTIONARY = "test_dictionary";

    private static Dictionary dictionary;

    @BeforeAll
    static void setup() throws IOException {
        dictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/test_dictionary");
    }

    @Test
    void classpathIncludeDict() {
        var attributeTemplate = dictionary.getAttributeTemplate(6);
        assertTrue(attributeTemplate.isPresent());
        assertEquals("Service-Type", attributeTemplate.get().name());
        assertEquals("Login-User", attributeTemplate.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dictionary.getAttributeTemplate(-1, 207).get().name());
    }

    @Test
    void testVendorAttrFlags() {
        // VENDORATTR      14122   WISPr-Bandwidth-Min-Up         5       integer has_tag

        var attribute = dictionary.getAttributeTemplate("WISPr-Redirection-URL").get();
        assertFalse(attribute.tagged());
        assertEquals(NO_ENCRYPT, attribute.codecType());

        var tagAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Min-Up").get();
        assertTrue(tagAttribute.tagged());
        assertEquals(NO_ENCRYPT, tagAttribute.codecType());

        var encryptAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Min-Down").get();
        assertFalse(encryptAttribute.tagged());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.codecType());

        var tagEncryptAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Max-Up").get();
        assertTrue(tagEncryptAttribute.tagged());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.codecType());

        var encryptTagAttribute = dictionary.getAttributeTemplate("WISPr-Bandwidth-Max-Down").get();
        assertTrue(encryptTagAttribute.tagged());
        assertEquals(ASCEND_SECRET, encryptTagAttribute.codecType());
    }

    @Test
    void testStatefulVendorAttributeFlags() {
        // BEGIN-VENDOR	Ascend
        // ATTRIBUTE	Ascend-Max-Shared-Users			2	integer  has_tag

        var attribute = dictionary.getAttributeTemplate("Ascend-Test").get();
        assertFalse(attribute.tagged());
        assertEquals(NO_ENCRYPT, attribute.codecType());

        var tagAttribute = dictionary.getAttributeTemplate("Ascend-Max-Shared-Users").get();
        assertTrue(tagAttribute.tagged());
        assertEquals(NO_ENCRYPT, tagAttribute.codecType());

        var encryptAttribute = dictionary.getAttributeTemplate("Ascend-UU-Info").get();
        assertFalse(encryptAttribute.tagged());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.codecType());

        var tagEncryptAttribute = dictionary.getAttributeTemplate("Ascend-CIR-Timer").get();
        assertTrue(tagEncryptAttribute.tagged());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.codecType());

        var encryptTagAttribute = dictionary.getAttributeTemplate("Ascend-FR-08-Mode").get();
        assertTrue(encryptTagAttribute.tagged());
        assertEquals(ASCEND_SECRET, encryptTagAttribute.codecType());
    }

    @Test
    void testAttributeFlags() {
        // ATTRIBUTE PKM-SAID    141      short encrypt=1

        var attribute = dictionary.getAttributeTemplate("PKM-Config-Settings").get();
        assertFalse(attribute.tagged());
        assertEquals(NO_ENCRYPT, attribute.codecType());

        var tagAttribute = dictionary.getAttributeTemplate("PKM-Cryptosuite-List").get();
        assertTrue(tagAttribute.tagged());
        assertEquals(NO_ENCRYPT, tagAttribute.codecType());

        var encryptAttribute = dictionary.getAttributeTemplate("PKM-SAID").get();
        assertFalse(encryptAttribute.tagged());
        assertEquals(RFC2865_USER_PASSWORD, encryptAttribute.codecType());

        var tagEncryptAttribute = dictionary.getAttributeTemplate("PKM-SA-Descriptor").get();
        assertTrue(tagEncryptAttribute.tagged());
        assertEquals(RFC2868_TUNNEL_PASSWORD, tagEncryptAttribute.codecType());

        var encryptTagAttribute = dictionary.getAttributeTemplate("PKM-Auth-Key").get();
        assertTrue(encryptTagAttribute.tagged());
        assertEquals(ASCEND_SECRET, encryptTagAttribute.codecType());
    }

    @Test
    void valueEnumDeferred() {
        // parse VALUE before corresponding ATTRIBUTE

        var template = dictionary.getAttributeTemplate("Timetra-Restrict-To-Home").get();
        assertEquals("true", template.getEnumeration(1));
        assertEquals(1, template.getEnumeration("true"));
        assertEquals("false", template.getEnumeration(2));
        assertEquals(2, template.getEnumeration("false"));
    }

    @Test
    void attributeNonDecimal() {
        var template1 = dictionary.getAttributeTemplate("USR-Last-Number-Dialed-Out").get();
        assertEquals(102, template1.type());
        assertEquals("string", template1.dataType());
        assertEquals(429, template1.vendorId());

        var template2 = dictionary.getAttributeTemplate(429, 232).get();
        assertEquals("USR-Last-Number-Dialed-In-DNIS", template2.name());
        assertEquals("string", template2.dataType());
        assertEquals(429, template2.vendorId());
    }

    @Test
    void valueAttributeNonDecimal() {
        var serviceType = dictionary.getAttributeTemplate("Service-Type").get();
        assertEquals(0x06300001, serviceType.getEnumeration("Annex-Authorize-Only"));
        assertEquals("Annex-Authorize-Only", serviceType.getEnumeration(0x06300001));

        var acctStatusType = dictionary.getAttributeTemplate("Acct-Status-Type").get();
        assertEquals(0x06300001, acctStatusType.getEnumeration("Annex-User-Reject"));
        assertEquals("Annex-User-Reject", acctStatusType.getEnumeration(0x06300001));
    }

    @Test
    void vendorFormatFlag() {
        var wispr = dictionary.getVendor("WISPr").get();
        assertSame(dictionary.getVendor(14122).get(), wispr);
        assertEquals(14122, wispr.id());
        assertEquals("WISPr", wispr.name());
        assertEquals(1, wispr.typeSize());
        assertEquals(1, wispr.lengthSize());

        var lucent = dictionary.getVendor("Lucent").get();
        assertSame(dictionary.getVendor(4846).get(), lucent);
        assertEquals(4846, lucent.id());
        assertEquals("Lucent", lucent.name());
        assertEquals(2, lucent.typeSize());
        assertEquals(1, lucent.lengthSize());
    }

    @Test
    void attributeCustomTypeSize() {
        var basic = dictionary.getAttributeTemplate("Lucent-Max-Shared-Users").get();
        assertEquals(2, basic.type());

        var custom = dictionary.getAttributeTemplate("Lucent-Retrain-Reason").get();
        assertEquals(20119, custom.type());
    }

    @Test
    void fileSystemIncludeDict() throws IOException {
        var tmpPath = Files.createTempDirectory("tinyradius_test_");
        copyDict(tmpPath, TEST_DICTIONARY,
                "default_dictionary",
                "dictionary.rfc5904",
                "dictionary.wispr",
                "dictionary.ascend",
                "dictionary.alcatel.sr");

        var dict = DictionaryParser.newFileParser().parseDictionary(tmpPath + "/" + TEST_DICTIONARY);

        var attributeTemplate = dict.getAttributeTemplate(6);
        assertTrue(attributeTemplate.isPresent());
        assertEquals("Service-Type", attributeTemplate.get().name());
        assertEquals("Login-User", attributeTemplate.get().getEnumeration(1));
        assertEquals("Digest-Attributes", dict.getAttributeTemplate(-1, 207).get().name());

        try (var paths = Files.walk(tmpPath)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void resourceResolversEdgeCases() {
        var cpResolver = ClasspathResourceResolver.INSTANCE;
        assertEquals("", cpResolver.resolve("non_existent_dir/file", "other_non_existent"));
        assertEquals("org/tinyradius/core/dictionary/default_dictionary",
                cpResolver.resolve("root_resource", "org/tinyradius/core/dictionary/default_dictionary"));
        assertThrows(IOException.class, () -> cpResolver.openStream("non_existent_resource_xyz"));

        var fileResolver = FileResourceResolver.INSTANCE;
        assertEquals("", fileResolver.resolve("/non/existent/base", "non_existent_next"));
        assertThrows(IOException.class, () -> fileResolver.openStream("/non/existent/file_xyz"));
    }

    @Test
    void parserDirectivesAndErrorCases() throws IOException {
        var validDictContent = """
                # comment line
                VENDOR 999 LegacyVendor
                VENDOR FreeRadiusVendor 888 format=1,1
                BEGIN-VENDOR LegacyVendor
                BEGIN-TLV MyTlv 1
                END-TLV MyTlv
                PROTOCOL MyProto 1
                BEGIN-PROTOCOL MyProto
                END-PROTOCOL MyProto
                MEMBER MyMember 1 string
                STRUCT MyStruct 1
                ATTRIBUTE Legacy-Attr 10 string encrypt=User-Password
                ATTRIBUTE Skip-Sub-Attr 10.1 string
                END-VENDOR LegacyVendor
                BEGIN-VENDOR UnknownVendorName
                VENDOR V991 991 invalidFlag
                VENDOR V992 992 format=1,2,3
                VENDOR V993 993 format=a,b
                VALUE Unknown-Attr Some-Val 1
                """;
        var dict = parseString(validDictContent);
        assertTrue(dict.getVendor(999).isPresent());
        assertTrue(dict.getVendor(888).isPresent());
        var legacyAttr = dict.getAttributeTemplate("Legacy-Attr").get();
        assertEquals(RFC2865_USER_PASSWORD, legacyAttr.codecType());

        // Cached resource parse (include same file twice)
        var multiIncludeResolver = new ResourceResolver() {
            @Override
            public String resolve(String currentResource, String nextResource) {
                return nextResource;
            }

            @Override
            public InputStream openStream(String resource) {
                if ("root".equals(resource)) {
                    return new ByteArrayInputStream("$INCLUDE child\n$INCLUDE child\n".getBytes(StandardCharsets.UTF_8));
                }
                return new ByteArrayInputStream("VENDOR 777 ChildVendor\n".getBytes(StandardCharsets.UTF_8));
            }
        };
        var cachedDict = new DictionaryParser(multiIncludeResolver).parseDictionary("root");
        assertTrue(cachedDict.getVendor(777).isPresent());

        // Error cases
        assertThrows(IOException.class, () -> parseString("UNKNOWN-DIRECTIVE foo bar"));
        assertThrows(IOException.class, () -> parseString("ATTRIBUTE TooFewTokens 1"));
        assertThrows(IOException.class, () -> parseString("ATTRIBUTE TooMany 1 string flag1 extra"));
        assertThrows(IllegalArgumentException.class, () -> parseString("ATTRIBUTE OutOfBounds 999 string"));
        assertThrows(IllegalArgumentException.class, () -> parseString("ATTRIBUTE NegativeType -1 string"));
        assertThrows(IOException.class, () -> parseString("VALUE TooFew 1"));
        assertThrows(IOException.class, () -> parseString("$INCLUDE too many tokens"));
        assertThrows(IOException.class, () -> parseString("$INCLUDE missing_file"));
        assertThrows(IOException.class, () -> parseString("VENDORATTR TooFew 1 string"));
        assertThrows(IOException.class, () -> parseString("BEGIN-VENDOR"));
        assertThrows(IOException.class, () -> parseString("BEGIN-VENDOR a b c"));
        assertThrows(IOException.class, () -> parseString("END-VENDOR"));
        assertThrows(IOException.class, () -> parseString("VENDOR 999 V999\nEND-VENDOR V999"));
        assertThrows(IOException.class, () -> parseString("VENDOR TooFew"));
        assertThrows(IOException.class, () -> parseString("VENDOR a b c d"));
        assertThrows(IOException.class, () -> parseString("VENDOR NotANumber AlsoNotANumber"));
    }

    private Dictionary parseString(String content) throws IOException {
        var resolver = new ResourceResolver() {
            @Override
            public String resolve(String currentResource, String nextResource) {
                return "missing_file".equals(nextResource) ? "" : nextResource;
            }

            @Override
            public InputStream openStream(String resource) {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
        return new DictionaryParser(resolver).parseDictionary("mem");
    }

    private void copyDict(Path tempDir, String... files) throws IOException {
        for (var file : files) {
            Files.copy(
                    requireNonNull(this.getClass().getClassLoader().getResourceAsStream(PACKAGE_PREFIX + file)),
                    tempDir.resolve(file), REPLACE_EXISTING);
        }
    }
}
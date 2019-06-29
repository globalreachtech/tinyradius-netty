package org.tinyradius.dictionary;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.IpAttribute;
import org.tinyradius.attribute.Ipv6Attribute;
import org.tinyradius.attribute.Ipv6PrefixAttribute;
import org.tinyradius.packet.AccessRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DictionaryParserTest {

    private final String packagePath = "org/tinyradius/dictionary/";

    private final String TEST_DICTIONARY = "test_dictionary";

    private final DictionaryParser parser = new DictionaryParser();

    @Test
    void defaultDictionary() {
        Dictionary dictionary = DefaultDictionary.INSTANCE;

        AccessRequest ar = new AccessRequest("UserName", "UserPassword");
        ar.setDictionary(dictionary);
        ar.addAttribute("WISPr-Location-ID", "LocationID");
        ar.addAttribute(new IpAttribute(8, 1234567));
        ar.addAttribute(new Ipv6Attribute(168, "fe80::"));
        ar.addAttribute(new Ipv6PrefixAttribute(97, "fe80::/64"));
        ar.addAttribute(new Ipv6PrefixAttribute(97, "fe80::/128"));
        System.out.println(ar);
    }

    @Test
    void classpathIncludeDictionary() throws IOException {
        final MemoryDictionary dictionary = new MemoryDictionary();

        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream source = classLoader.getResourceAsStream(packagePath + TEST_DICTIONARY);

        parser.parseDictionary(source, dictionary);

        final AttributeType serviceTypeAttr = dictionary.getAttributeTypeByCode(6);
        assertNotNull(serviceTypeAttr);
        assertEquals("Service-Type", serviceTypeAttr.getName());
        assertEquals("Login-User", serviceTypeAttr.getEnumeration(1));

    }

    @Test
    void fileSystemIncludeDictionary() throws IOException {
        final Path path = copyDictionaryToTmp();
        final MemoryDictionary dictionary = new MemoryDictionary();

        parser.parseDictionary(Files.newInputStream(path.resolve(TEST_DICTIONARY)), dictionary);

        final AttributeType serviceTypeAttr = dictionary.getAttributeTypeByCode(6);
        assertNotNull(serviceTypeAttr);
        assertEquals("Service-Type", serviceTypeAttr.getName());
        assertEquals("Login-User", serviceTypeAttr.getEnumeration(1));
    }

    private Path copyDictionaryToTmp() throws IOException {
        Path tempDir = Files.createTempDirectory("tinyradius_test_");
        copyDic(tempDir, TEST_DICTIONARY);
        copyDic(tempDir, "default_dictionary");
        copyDic(tempDir, "dictionary.rfc5904");
        return tempDir;
    }

    private void copyDic(Path tempDir, String fileName) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        Files.copy(requireNonNull(classLoader.getResourceAsStream(packagePath + fileName)),
                tempDir.resolve(fileName), REPLACE_EXISTING);
    }
}
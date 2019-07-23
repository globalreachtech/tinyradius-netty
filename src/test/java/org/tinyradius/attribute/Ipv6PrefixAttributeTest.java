package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

class Ipv6PrefixAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void minAttributeLength() {
        final Ipv6PrefixAttribute prefixAttribute = new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[2]);
        assertEquals(2, prefixAttribute.getValue().length);
    }

    @Test
    void maxAttributeLength() {
        final Ipv6PrefixAttribute prefixAttribute = new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[18]);
        assertEquals(18, prefixAttribute.getValue().length);
    }

    @Test
    void LessThanMinAttributeLength() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[1]));
        exception.printStackTrace();
        assertTrue(exception.getMessage().toLowerCase().contains("should be 2-18 octets"));
    }

    @Test
    void MoreThanMaxAttributeLength() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[20]));
        assertTrue(exception.getMessage().toLowerCase().contains("should be 2-18 octets"));
    }

    @Test
    void getValueString() {
        final Ipv6PrefixAttribute attribute = new Ipv6PrefixAttribute(dictionary, -1, 97, "2001:db8:ac10:fe01:0:0:0:0/0");
        assertEquals("2001:db8:ac10:fe01:0:0:0:0/0", attribute.getValueString());
    }

    @Test
    void getValueStringEmpty() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Ipv6PrefixAttribute(dictionary, -1, 97, ""));
        exception.printStackTrace();
        assertTrue(exception.getMessage().toLowerCase().contains("invalid ipv6 prefix"));
    }

    @Test
    void foo() {
        createAttribute(dictionary, -1, 97, "fe80::/64"); // todo
    }
}
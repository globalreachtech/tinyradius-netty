package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;

class Ipv6PrefixAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void minAttributeLength() {
        // min
        final Ipv6PrefixAttribute prefixAttribute = (Ipv6PrefixAttribute) dictionary.createAttribute(-1, 97, new byte[2]); // Framed-IPv6-Prefix
        assertEquals(2, prefixAttribute.getValue().length);

        // min-1
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 97, new byte[1])); // Framed-IPv6-Prefix
        assertTrue(exception.getMessage().toLowerCase().contains("should be 2-18 octets"));
    }

    @Test
    void maxAttributeLength() {
        // max
        final Ipv6PrefixAttribute prefixAttribute = (Ipv6PrefixAttribute) dictionary.createAttribute(-1, 97, new byte[18]); // Framed-IPv6-Prefix
        assertEquals(2, prefixAttribute.getValue().length); // prefix-length set to 0, so everything else is trimmed

        // max+1
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 97, new byte[20])); // Framed-IPv6-Prefix
        assertTrue(exception.getMessage().toLowerCase().contains("should be 2-18 octets"));
    }

    @Test
    void stringOk() {
        final Ipv6PrefixAttribute attribute = (Ipv6PrefixAttribute) dictionary.createAttribute(-1, 97, "2001:db8:ac10:fe01:0:0:0:0/64"); // Framed-IPv6-Prefix
        assertEquals("2001:db8:ac10:fe01:0:0:0:0/64", attribute.getValueString());
    }

    @Test
    void stringEmpty() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 97, "")); // Framed-IPv6-Prefix
        assertTrue(exception.getMessage().toLowerCase().contains("invalid ipv6 prefix"));
    }

    @Test
    void bitsOutsidePrefixLengthIsZero() {
        // string constructor
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 97, "fe80:fe80:fe80:fe80:fe80:fe80:fe80:fe80/64")); // Framed-IPv6-Prefix

        assertTrue(e1.getMessage().toLowerCase().contains("bits outside of the prefix-length must be zero"));

        // byte constructor
        final byte[] bytes = new byte[18];
        bytes[1] = 8; // prefix length = 8
        bytes[2] = (byte) 0xff;
        bytes[3] = (byte) 0xff; // violates rfc requirement that "Bits outside of the Prefix-Length, if included, must be zero."

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 97, bytes)); // Framed-IPv6-Prefix

        assertTrue(e2.getMessage().toLowerCase().contains("bits outside of the prefix-length must be zero"));
    }

    @Test
    void validPrefixLessThan16Octets() {
        final byte[] bytes = new byte[5];
        bytes[1] = 16; // 16-bit prefix-length
        bytes[2] = (byte) 0b11111110;
        bytes[3] = 0; // set 8 bits to 0, but also part of prefix
        bytes[4] = 0; // empty octet as padding

        final RadiusAttribute attribute = dictionary.createAttribute(-1, 97, bytes); // Framed-IPv6-Prefix
        assertEquals("fe00:0:0:0:0:0:0:0/16", attribute.getValueString());
    }

    @Test
    void byteArraySizeLessThanDeclaredPrefixLength() {

        final byte[] bytes = new byte[4]; // prefix capacity 2
        bytes[1] = 16; // 16 bits require 2 bytes

        final RadiusAttribute attribute = dictionary.createAttribute(-1, 97, bytes); // Framed-IPv6-Prefix
        assertEquals("0:0:0:0:0:0:0:0/16", attribute.getValueString());

        bytes[1] = 17; // 17 bits require 3 bytes;
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 97, bytes));
        assertTrue(exception.getMessage().toLowerCase().contains("actual byte array only has space for 16 bits"));
        assertTrue(exception.getMessage().toLowerCase().contains("prefix-length declared 17 bits"));
    }
}
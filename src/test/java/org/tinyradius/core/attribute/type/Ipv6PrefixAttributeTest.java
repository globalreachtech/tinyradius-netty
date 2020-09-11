package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.TestUtils;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.type.AttributeType.IPV6_PREFIX;

class Ipv6PrefixAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void minAttributeLength() {
        // min
        final Ipv6PrefixAttribute prefixAttribute = (Ipv6PrefixAttribute)
                IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, new byte[2]); // Framed-IPv6-Prefix
        assertEquals(2, prefixAttribute.getValue().length);
        assertEquals("0:0:0:0:0:0:0:0/0", prefixAttribute.getValueString());

        // min-1
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, new byte[1])); // Framed-IPv6-Prefix
        assertTrue(TestUtils.getStackTrace(exception).contains("should be 2-18 octets"));
    }

    @Test
    void maxAttributeLength() {
        // max
        final Ipv6PrefixAttribute prefixAttribute = (Ipv6PrefixAttribute)
                IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, new byte[18]); // Framed-IPv6-Prefix
        assertEquals(18, prefixAttribute.getValue().length); // intentionally don't trim, avoid changing incoming data
        assertEquals("0:0:0:0:0:0:0:0/0", prefixAttribute.getValueString());

        // max+1
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, new byte[20])); // Framed-IPv6-Prefix
        assertTrue(TestUtils.getStackTrace(exception).contains("should be 2-18 octets"));
    }

    @Test
    void stringOk() {
        final Ipv6PrefixAttribute attribute = (Ipv6PrefixAttribute)
                IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, "2001:db8:ac10:fe01:0:0:0:0/64"); // Framed-IPv6-Prefix
        assertEquals("2001:db8:ac10:fe01:0:0:0:0/64", attribute.getValueString());
    }

    @Test
    void stringEmpty() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, "")); // Framed-IPv6-Prefix
        assertTrue(exception.getMessage().toLowerCase().contains("invalid ipv6 prefix"));
    }

    @Test
    void bitsOutsidePrefixLengthIsZero() {
        // string constructor
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, "fe80:fe80:fe80:fe80:fe80:fe80:fe80:fe80/64")); // Framed-IPv6-Prefix

        assertTrue(TestUtils.getStackTrace(e1).contains("bits outside of the Prefix-Length must be zero"));

        // byte constructor
        final byte[] bytes = new byte[18];
        bytes[1] = 8; // prefix length = 8
        bytes[2] = (byte) 0xff;
        bytes[3] = (byte) 0xff; // violates rfc requirement that "Bits outside of the Prefix-Length, if included, must be zero."

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, bytes)); // Framed-IPv6-Prefix

        assertTrue(TestUtils.getStackTrace(e2).contains("bits outside of the Prefix-Length must be zero"));
    }

    @Test
    void validPrefixLessThan16Octets() {
        final byte[] bytes = new byte[5];
        bytes[1] = 16; // 16-bit prefix-length
        bytes[2] = (byte) 0b11111110;
        bytes[3] = 0; // set 8 bits to 0, but also part of prefix
        bytes[4] = 0; // empty octet as padding

        final RadiusAttribute attribute = IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, bytes); // Framed-IPv6-Prefix
        assertEquals("fe00:0:0:0:0:0:0:0/16", attribute.getValueString());
    }

    @Test
    void byteArraySizeLessThanDeclaredPrefixLength() {

        final byte[] bytes = new byte[4]; // prefix capacity 2
        bytes[1] = 16; // 16 bits require 2 bytes

        final RadiusAttribute attribute = IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, bytes); // Framed-IPv6-Prefix
        assertEquals("0:0:0:0:0:0:0:0/16", attribute.getValueString());

        bytes[1] = 17; // 17 bits require 3 bytes;
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, bytes));
        assertTrue(TestUtils.getStackTrace(exception).contains("actual byte array only has space for 16 bits"));
        assertTrue(TestUtils.getStackTrace(exception).contains("Prefix-Length declared 17 bits"));
    }
}
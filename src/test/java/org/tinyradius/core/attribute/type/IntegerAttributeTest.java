package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.TestUtils;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.type.AttributeType.INTEGER;

class IntegerAttributeTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void intMaxUnsigned() {
        final IntegerAttribute attribute = (IntegerAttribute)
                INTEGER.create(dictionary, -1, 10, (byte) 0, Long.toString(0xffffffffL)); // Framed-Routing

        assertEquals(-1, attribute.getValueInt());
        assertEquals(0xffffffffL, attribute.getValueLong());
        assertEquals("4294967295", attribute.getValueString());
    }

    @Test
    void intMaxSigned() {
        final int value = Integer.MAX_VALUE;
        final IntegerAttribute attribute = (IntegerAttribute)
                INTEGER.create(dictionary, -1, 10, (byte) 0, String.valueOf(value)); // Framed-Routing

        assertEquals(value, attribute.getValueInt());
        assertEquals(value, attribute.getValueLong());
        assertEquals(String.valueOf(value), attribute.getValueString());
    }

    @Test
    void bytesOk() {
        final byte[] bytes = new byte[]{0, 0, (byte) 0xff, (byte) 0xff};
        final IntegerAttribute attribute = (IntegerAttribute)
                INTEGER.create(dictionary, -1, 10, (byte) 0, bytes); // Framed-Routing

        assertEquals(65535, attribute.getValueInt());
        assertEquals(65535, attribute.getValueLong());
        assertEquals("65535", attribute.getValueString());
    }

    @Test
    void bytesTooShort() throws IOException {
        // no tag
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> INTEGER.create(dictionary, -1, 10, (byte) 0, new byte[3])); // Framed-Routing
        assertTrue(TestUtils.getStackTrace(e1).contains("should be 4 octets, actual: 3"));

        // has tag
        final Dictionary dict = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary.rfc2868");
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> INTEGER.create(dict, -1, 64, (byte) 0, new byte[2])); // Tunnel-Type
        assertTrue(TestUtils.getStackTrace(e2).contains("should be 3 octets if has_tag, actual: 2"));
    }

    @Test
    void bytesTooLong() throws IOException {
        // no tag
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> INTEGER.create(dictionary, -1, 10, (byte) 0, new byte[5])); // Framed-Routing
        assertTrue(TestUtils.getStackTrace(e1).contains("should be 4 octets, actual: 5"));

        // has tag
        final Dictionary dict = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary.rfc2868");
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> INTEGER.create(dict, -1, 64, (byte) 0, new byte[4])); // Tunnel-Type
        assertTrue(TestUtils.getStackTrace(e2).contains("should be 3 octets if has_tag, actual: 4"));
    }

    @Test
    void stringOk() {
        final IntegerAttribute attribute = (IntegerAttribute)
                INTEGER.create(dictionary, -1, 10, (byte) 0, "12345"); // Framed-Routing

        assertEquals(12345, attribute.getValueInt());
        assertEquals(12345, attribute.getValueLong());
        assertEquals("12345", attribute.getValueString());
    }

    @Test
    void stringTooBig() {
        final String strLong = Long.toString(0xffffffffffL);
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> INTEGER.create(dictionary, -1, 10, (byte) 0, strLong)); // Framed-Routing

        assertTrue(exception.getMessage().toLowerCase().contains("exceeds range"));
    }

    @Test
    void stringEmpty() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> INTEGER.create(dictionary, -1, 10, (byte) 0, "")); // Framed-Routing

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"\""));
    }

    @Test
    void stringEnum() {
        final IntegerAttribute attribute = (IntegerAttribute)
                INTEGER.create(dictionary, -1, 6, (byte) 0, "Login-User"); // Service-Type

        assertEquals(1, attribute.getValueInt());
        assertEquals(1, attribute.getValueLong());
        assertEquals("Login-User", attribute.getValueString());
    }

    @Test
    void stringInvalid() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> INTEGER.create(dictionary, -1, 6, (byte) 0, "badString")); // Service-Type

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"badstring\""));
    }

    /**
     * <a href="https://tools.ietf.org/html/bcp158">BCP 158</a>
     * <p>
     * "Other limitations of the tagging mechanism are that when integer
     * values are tagged, the value portion is reduced to three bytes,
     * meaning only 24-bit numbers can be represented."
     */
    @Test
    void taggedEnum() throws IOException {
        // testing without tag
        // ATTRIBUTE	Service-Type		6	integer
        // VALUE		Service-Type		Callback-Login-User	3

        // from enum
        final IntegerAttribute serviceType = (IntegerAttribute) dictionary.getAttributeTemplate("Service-Type").get()
                .create(dictionary, (byte) 1, "Callback-Login-User");
        assertEquals(6, serviceType.getLength());
        assertEquals(6, serviceType.getData().readableBytes());
        assertEquals(6, serviceType.toByteArray().length);
        assertArrayEquals(new byte[]{0, 0, 0, 3}, serviceType.getValue());
        assertEquals(IntegerAttribute.class, serviceType.getClass());
        assertEquals("Service-Type=Callback-Login-User", serviceType.toString());
        assertEquals(3, serviceType.getValueInt());
        assertEquals("Callback-Login-User", serviceType.getValueString());
        assertFalse(serviceType.getTag().isPresent());

        // from int string
        final IntegerAttribute serviceType2 = (IntegerAttribute) dictionary.getAttributeTemplate("Service-Type").get()
                .create(dictionary, (byte) 1, "3");
        assertArrayEquals(serviceType.toByteArray(), serviceType2.toByteArray());

        // from byte[]
        final IntegerAttribute serviceType3 = (IntegerAttribute) dictionary.getAttributeTemplate("Service-Type").get()
                .create(dictionary, (byte) 1, new byte[]{0, 0, 0, 3});
        assertArrayEquals(serviceType.toByteArray(), serviceType3.toByteArray());

        // from parse
        final IntegerAttribute parsedServiceType = (IntegerAttribute) AttributeHolder.readAttribute(dictionary, -1, serviceType.toByteBuf().copy());
        assertArrayEquals(serviceType.toByteArray(), parsedServiceType.toByteArray());

        // testing has_tag
        // ATTRIBUTE	Tunnel-Type				64	integer	has_tag
        // VALUE	Tunnel-Type			L2F			2
        final Dictionary dict = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary.rfc2868");

        // from enum
        final IntegerAttribute vlan = (IntegerAttribute) dict.getAttributeTemplate("Tunnel-Type").get()
                .create(dict, (byte) 123, "L2F");
        assertEquals(6, vlan.getLength());
        assertEquals(6, vlan.getData().readableBytes());
        assertEquals(6, vlan.toByteArray().length);
        assertArrayEquals(new byte[]{0, 0, 2}, vlan.getValue());
        assertEquals(IntegerAttribute.class, vlan.getClass());
        assertEquals("Tunnel-Type:123=L2F", vlan.toString());
        assertEquals(2, vlan.getValueInt());
        assertEquals("L2F", vlan.getValueString());
        assertEquals((byte) 123, vlan.getTag().get());

        // from int string
        final IntegerAttribute vlan2 = (IntegerAttribute) dict.getAttributeTemplate("Tunnel-Type").get()
                .create(dict, (byte) 123, "2");
        assertArrayEquals(vlan.toByteArray(), vlan2.toByteArray());

        // from byte[]
        final IntegerAttribute vlan3 = (IntegerAttribute) dict.getAttributeTemplate("Tunnel-Type").get()
                .create(dict, (byte) 123, new byte[]{0, 0, 2});
        assertArrayEquals(vlan.toByteArray(), vlan3.toByteArray());

        // from parse
        final IntegerAttribute parsedVlan = (IntegerAttribute) AttributeHolder.readAttribute(dict, -1, vlan.toByteBuf().copy());
        assertArrayEquals(vlan.toByteArray(), parsedVlan.toByteArray());
    }
}
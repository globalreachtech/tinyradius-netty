package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.*;

class IntegerAttributeTest {

    private static final RadiusAttributeFactory<IntegerAttribute> FACTORY = IntegerAttribute.FACTORY;

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void intMaxUnsigned() {
        IntegerAttribute attribute = FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, Long.toString(0xffffffffL));

        assertEquals(-1, attribute.getValueInt());
        assertEquals(0xffffffffL, attribute.getValueLong());
        assertEquals("4294967295", attribute.getValueString());
    }

    @Test
    void intMaxSigned() {
        int value = Integer.MAX_VALUE;
        IntegerAttribute attribute = FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, String.valueOf(value));

        assertEquals(value, attribute.getValueInt());
        assertEquals(value, attribute.getValueLong());
        assertEquals(String.valueOf(value), attribute.getValueString());
    }

    @Test
    void bytesOk() {
        byte[] bytes = new byte[]{0, 0, (byte) 0xff, (byte) 0xff};
        IntegerAttribute attribute = FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, bytes);

        assertEquals(65535, attribute.getValueInt());
        assertEquals(65535, attribute.getValueLong());
        assertEquals("65535", attribute.getValueString());
    }

    @Test
    void bytesTooShort() throws IOException {
        // no tag
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, new byte[3]));
        assertThat(e1).hasMessageContaining("should be 4 octets, actual: 3");

        // has tag
        Dictionary dict = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary.rfc2868");
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(dict, -1, TUNNEL_TYPE, (byte) 0, new byte[2]));
        assertThat(e2).hasMessageContaining("should be 3 octets if has_tag, actual: 2");
    }

    @Test
    void bytesTooLong() throws IOException {
        // no tag
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, new byte[5]));
        assertThat(e1).hasMessageContaining("should be 4 octets, actual: 5");

        // has tag
        Dictionary dict = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary.rfc2868");
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(dict, -1, 64, (byte) 0, new byte[4])); // Tunnel-Type
        assertThat(e2).hasMessageContaining("should be 3 octets if has_tag, actual: 4");
    }

    @Test
    void stringOk() {
        IntegerAttribute attribute = FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, "12345");

        assertEquals(12345, attribute.getValueInt());
        assertEquals(12345, attribute.getValueLong());
        assertEquals("12345", attribute.getValueString());
    }

    @Test
    void stringTooBig() {
        String strLong = Long.toString(0xffffffffffL);
        NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, strLong));

        assertTrue(exception.getMessage().toLowerCase().contains("exceeds range"));
    }

    @Test
    void stringEmpty() {
        NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> FACTORY.create(dictionary, -1, FRAMED_ROUTING, (byte) 0, ""));

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"\""));
    }

    @Test
    void stringEnum() {
        IntegerAttribute attribute = FACTORY.create(dictionary, -1, SERVICE_TYPE, (byte) 0, "Login-User");

        assertEquals(1, attribute.getValueInt());
        assertEquals(1, attribute.getValueLong());
        assertEquals("Login-User", attribute.getValueString());
    }

    @Test
    void stringInvalid() {
        NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> FACTORY.create(dictionary, -1, SERVICE_TYPE, (byte) 0, "badString"));

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"badstring\""));
    }

    @Test
    void enumUntagged() {
        // testing without tag
        // ATTRIBUTE	Service-Type		6	integer
        // VALUE		Service-Type		Callback-Login-User	3

        // from enum
        IntegerAttribute serviceType = FACTORY.create(dictionary, -1, SERVICE_TYPE, (byte) 1, "Callback-Login-User");
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
        IntegerAttribute serviceType2 = FACTORY.create(dictionary, -1, SERVICE_TYPE, (byte) 1, "3");
        assertArrayEquals(serviceType.toByteArray(), serviceType2.toByteArray());

        // from byte[]
        IntegerAttribute serviceType3 = FACTORY.create(dictionary, -1, SERVICE_TYPE, (byte) 1, new byte[]{0, 0, 0, 3});
        assertArrayEquals(serviceType.toByteArray(), serviceType3.toByteArray());

        // from parse
        IntegerAttribute parsedServiceType = (IntegerAttribute) AttributeHolder.readAttribute(dictionary, -1, serviceType.toByteBuf().copy());
        assertArrayEquals(serviceType.toByteArray(), parsedServiceType.toByteArray());
    }

    /**
     * <a href="https://tools.ietf.org/html/bcp158">BCP 158</a>
     * <p>
     * "Other limitations of the tagging mechanism are that when integer
     * values are tagged, the value portion is reduced to three bytes,
     * meaning only 24-bit numbers can be represented."
     */
    @Test
    void enumTagged() throws IOException {
        // ATTRIBUTE	Tunnel-Type				64	integer	has_tag
        // VALUE	Tunnel-Type			L2F			2
        Dictionary dict = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/freeradius/dictionary.rfc2868");

        // from enum
        IntegerAttribute vlan = (IntegerAttribute) dict.getAttributeTemplate("Tunnel-Type").get()
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
        IntegerAttribute vlan2 = (IntegerAttribute) dict.getAttributeTemplate("Tunnel-Type").get()
                .create(dict, (byte) 123, "2");
        assertArrayEquals(vlan.toByteArray(), vlan2.toByteArray());

        // from byte[]
        IntegerAttribute vlan3 = (IntegerAttribute) dict.getAttributeTemplate("Tunnel-Type").get()
                .create(dict, (byte) 123, new byte[]{0, 0, 2});
        assertArrayEquals(vlan.toByteArray(), vlan3.toByteArray());

        // from parse
        IntegerAttribute parsedVlan = (IntegerAttribute) AttributeHolder.readAttribute(dict, -1, vlan.toByteBuf().copy());
        assertArrayEquals(vlan.toByteArray(), parsedVlan.toByteArray());
    }
}
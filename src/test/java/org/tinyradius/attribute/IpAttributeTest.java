package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class IpAttributeTest {

    @Test
    void maxIpV4UnsignedIntAsString() {
        final long maxValue = 0xffffffffL;
        final String maxValueStr = Long.toString(maxValue); // 2^32 - 1 = 4294967295
        final IpAttribute.V4 attribute = new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, maxValueStr);

        assertEquals("255.255.255.255", attribute.getValueString());
        assertEquals(maxValue, attribute.getValueLong());
    }

    @Test
    void ipV4BytesTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, new byte[2]));

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4BytesTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, new byte[5]));

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4StringTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, "0.0.0.0.0"));

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4StringIsEmpty() {
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, ""));

        assertTrue(exception.getMessage().toLowerCase().contains("address can't be empty"));
    }

    @Test
    void ipV6AsBytes() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        final IpAttribute.V6 attribute = new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, address.getAddress());
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6AsString() {
        final IpAttribute.V6 attribute = new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, "2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6StringTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, "200112:0DB8:AC10:FE01:0000:0000:0000:0000"));
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6StringTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, "200112:FE01:0000:0000:0000:0000"));
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6StringIsEmpty() {
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, ""));
        assertTrue(exception.getMessage().toLowerCase().contains("address can't be empty"));
    }
}
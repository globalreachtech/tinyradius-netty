package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class IpAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void maxIpV4AsString() {
        final long maxValue = 0xffffffffL;
        final String maxValueStr = Long.toString(maxValue); // 2^32 - 1 = 4294967295
        final IpAttribute.V4 attribute = (IpAttribute.V4) dictionary.createAttribute(-1, 8, maxValueStr); // Framed-IP-Address

        assertEquals("255.255.255.255", attribute.getValueString());
        assertEquals(maxValue, Integer.toUnsignedLong(attribute.getValueInt()));
    }

    @Test
    void ipV4BytesTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 8, new byte[2])); // Framed-IP-Address

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4BytesTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 8, new byte[5])); // Framed-IP-Address

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4AsString() {
        final IpAttribute.V4 attribute = (IpAttribute.V4) dictionary.createAttribute(-1, 8, "192.168.0.1"); // Framed-IP-Address
        assertEquals("192.168.0.1", attribute.getValueString());
    }

    @Test
    void ipV4StringTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 8, "0.0.0.0.0")); // Framed-IP-Address

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4StringIsEmpty() {
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dictionary.createAttribute(-1, 8, "")); // Framed-IP-Address

        assertTrue(exception.getMessage().toLowerCase().contains("address can't be empty"));
    }

    @Test
    void ipV6AsBytes() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        final IpAttribute.V6 attribute = (IpAttribute.V6) dictionary.createAttribute(-1, 95, address.getAddress()); // NAS-IPv6-Address
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6AsString() {
        final IpAttribute.V6 attribute = (IpAttribute.V6) dictionary.createAttribute(-1, 95, "2001:0DB8:AC10:FE01:0000:0000:0000:0000"); // NAS-IPv6-Address
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6StringTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 95, "20011:0DB8:AC10:FE01:0000:0000:0000:0000")); // NAS-IPv6-Address
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6StringTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 95, "2001:FE01:0000:0000:0000:0000")); // NAS-IPv6-Address
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6StringIsEmpty() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 95, "")); // NAS-IPv6-Address
        assertTrue(exception.getMessage().toLowerCase().contains("address can't be empty"));
    }

    @Test
    void mismatchIpVersions() {
        final IllegalArgumentException v6Exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 95, "192.168.0.1")); // NAS-IPv6-Address
        assertTrue(v6Exception.getMessage().toLowerCase().contains("expected inet6address"));

        final IllegalArgumentException v4Exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 8, "2001:4860:4860::8888")); // Framed-IP-Address
        assertTrue(v4Exception.getMessage().toLowerCase().contains("expected inet4address"));
    }
}
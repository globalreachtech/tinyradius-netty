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
    void maxIpV4UnsignedIntAsLong() {
        final long maxValue = 0xffffffffL;
        final IpAttribute.V4 attribute = new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, maxValue);

        assertEquals("255.255.255.255", attribute.getValueString());
        assertEquals(maxValue, attribute.getValueLong());
    }

    @Test
    void ipV4BadAddress() {
        byte[] data = new byte[2];
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V4(DefaultDictionary.INSTANCE, -1, 8, data));

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6AsBytes() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        byte[] bytes = a.getAddress();

        final IpAttribute.V6 attribute = new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, bytes);
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6AsString() {
        String address = "2001:0DB8:AC10:FE01:0000:0000:0000:0000";

        final IpAttribute.V6 attribute = new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, address);
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6BadAddress() {
        String address = "200112:0DB8:AC10:FE01:0000:0000:0000:0000";

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IpAttribute.V6(DefaultDictionary.INSTANCE, -1, 8, address));
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

}
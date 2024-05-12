package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.RfcAttributeTypes.FRAMED_IP_ADDRESS;
import static org.tinyradius.core.attribute.RfcAttributeTypes.NAS_IPV6_ADDRESS;

class IpAttributeTest {

    private static final RadiusAttributeFactory<IpAttribute.V4> V4FACTORY = IpAttribute.V4.FACTORY;
    private static final RadiusAttributeFactory<IpAttribute.V6> V6FACTORY = IpAttribute.V6.FACTORY;

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void maxIpV4AsString() {
        final long maxValue = 0xffffffffL;
        final String maxValueStr = Long.toString(maxValue); // 2^32 - 1 = 4294967295
        final IpAttribute.V4 attribute = V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, maxValueStr);

        assertEquals("255.255.255.255", attribute.getValueString());
        assertEquals(maxValue, Integer.toUnsignedLong(attribute.getValueInt()));
    }

    @Test
    void ipV4BytesTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, new byte[2]));

        assertThat(exception).hasMessageContaining("should be 4 octets");
    }

    @Test
    void ipV4BytesTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, new byte[5]));

        assertThat(exception).hasMessageContaining("should be 4 octets");
    }

    @Test
    void ipV4AsString() {
        final IpAttribute.V4 attribute = V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, "192.168.0.1");
        assertEquals("192.168.0.1", attribute.getValueString());
    }

    @Test
    void ipV4StringTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, "0.0.0.0.0"));

        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV4StringIsEmpty() {
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, ""));

        assertTrue(exception.getMessage().toLowerCase().contains("address can't be empty"));
    }

    @Test
    void ipV6AsBytes() throws UnknownHostException {
        final InetAddress address = InetAddress.getByName("2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        final IpAttribute.V6 attribute = V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, address.getAddress());
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6BytesTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, new byte[2]));

        assertThat(exception).hasMessageContaining("should be 16 octets");
    }

    @Test
    void ipV6BytesTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, new byte[17]));

        assertThat(exception).hasMessageContaining("should be 16 octets");
    }

    @Test
    void ipV6AsString() {
        final IpAttribute.V6 attribute = V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, "2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", attribute.getValueString());
    }

    @Test
    void ipV6StringTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, "20011:0DB8:AC10:FE01:0000:0000:0000:0000"));
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6StringTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, "2001:FE01:0000:0000:0000:0000"));
        assertTrue(exception.getMessage().toLowerCase().contains("bad address"));
    }

    @Test
    void ipV6StringIsEmpty() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, ""));
        assertTrue(exception.getMessage().toLowerCase().contains("address can't be empty"));
    }

    @Test
    void mismatchIpVersions() {
        final IllegalArgumentException v6Exception = assertThrows(IllegalArgumentException.class,
                () -> V6FACTORY.create(dictionary, -1, NAS_IPV6_ADDRESS, (byte) 0, "192.168.0.1"));
        assertThat(v6Exception).hasMessageContaining("should be 16 octets");

        final IllegalArgumentException v4Exception = assertThrows(IllegalArgumentException.class,
                () -> V4FACTORY.create(dictionary, -1, FRAMED_IP_ADDRESS, (byte) 0, "2001:4860:4860::8888"));
        assertThat(v4Exception).hasMessageContaining("should be 4 octets");
    }
}
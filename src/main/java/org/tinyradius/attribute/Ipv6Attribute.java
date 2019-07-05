package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class represents a Radius attribute for an IPv6 number.
 */
public class Ipv6Attribute extends RadiusAttribute {

    public static IpAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
        final int length = readLength(data, offset);
        if (length != 18)
            throw new RadiusException("IPv6 attribute: expected length 18, packet declared " + length);

        return new IpAttribute(dictionary, vendorId, readType(data, offset), readData(data, offset));
    }

    public Ipv6Attribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        super(dictionary, vendorId, type, data);
    }

    /**
     * Constructs an IPv6 attribute.
     *
     * @param type  attribute type code
     * @param value value, format:ipv6 address
     */
    public Ipv6Attribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, convertIpV6(value));
    }

    /**
     * Returns the attribute value (IPv6 number) as a string of the format ipv6 address
     */
    @Override
    public String getDataString() {
        byte[] data = getData();
        if (data == null || data.length != 16)
            throw new RuntimeException("ip attribute: expected 16 bytes attribute data");
        try {
            return Inet6Address.getByAddress(null, data).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad IPv6 address", e);
        }

    }

    /**
     * @return 16 octet representing IPv6 address
     * @throws IllegalArgumentException bad IP address
     */
    private static byte[] convertIpV6(String value) {
        try {
            final byte[] address = InetAddress.getByName(value).getAddress();
            if (address.length != 16)
                throw new IllegalArgumentException("IPv6 address expected, received IPv4 address: " + value);
            return address;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address: " + value, e);
        }
    }
}

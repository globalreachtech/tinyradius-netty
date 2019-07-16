package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;

import static java.lang.Byte.toUnsignedInt;

/**
 * This class represents a Radius attribute for an IPv6 prefix.
 */
public class Ipv6PrefixAttribute extends RadiusAttribute {

    Ipv6PrefixAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        super(dictionary, vendorId, type, data);
        if (data.length < 2 || data.length > 18)
            throw new IllegalArgumentException("IPv6 prefix attribute: expected length min 4, max 20, packet declared " + data.length);
    }

    /**
     * Constructs an IPv6 prefix attribute.
     *  @param type  attribute type code
     * @param value value, format: "ipv6 address"/prefix
     */
    Ipv6PrefixAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, convertValue(value));
    }

    /**
     * Returns the attribute value (IP number) as a string of the format "xx.xx.xx.xx".
     */
    @Override
    public String getDataString() {
        final byte[] data = getData();
        if (data == null || data.length != 18)
            throw new RuntimeException("ip attribute: expected 18 bytes attribute data");
        try {
            final int prefix = toUnsignedInt(data[1]);
            final Inet6Address addr = (Inet6Address) Inet6Address.getByAddress(null, Arrays.copyOfRange(data, 2, data.length));

            return addr.getHostAddress() + "/" + prefix;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad IPv6 prefix", e);
        }

    }

    /**
     * Sets the attribute value (IPv6 number/prefix). String format:
     * ipv6 address.
     *
     * @throws IllegalArgumentException bad IP address
     */
    private static byte[] convertValue(String value) {
        if (value == null || value.length() < 3)
            throw new IllegalArgumentException("bad IPv6 address : " + value);
        try {
            final byte[] data = new byte[18];
            data[0] = 0;
            //TODO better checking
            final int slashPos = value.indexOf("/");
            data[1] = (byte) (Integer.parseInt(value.substring(slashPos + 1)) & 0xff);

            final Inet6Address addr = (Inet6Address) Inet6Address.getByName(value.substring(0, slashPos));

            byte[] ipData = addr.getAddress();
            System.arraycopy(ipData, 0, data, 2, ipData.length);

            return data;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad IPv6 address : " + value, e);
        }
    }
}

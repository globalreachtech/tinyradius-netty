package org.tinyradius.attribute;

import org.tinyradius.util.RadiusException;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * This class represents a Radius attribute for an IPv6 prefix.
 */
public class Ipv6PrefixAttribute extends RadiusAttribute {

    /**
     * Constructs an empty IP attribute.
     */
    public Ipv6PrefixAttribute(int attributeType, int vendorId) {
        super(attributeType, vendorId);
    }

    /**
     * Constructs an IPv6 prefix attribute.
     *
     * @param type  attribute type code
     * @param value value, format: "ipv6 address"/prefix
     */
    public Ipv6PrefixAttribute(int type, String value) {
        this(type, -1);
        setAttributeValue(value);
    }

    /**
     * Returns the attribute value (IP number) as a string of the
     * format "xx.xx.xx.xx".
     *
     * @see org.tinyradius.attribute.RadiusAttribute#getAttributeValue()
     */
    public String getAttributeValue() {
        final byte[] data = getAttributeData();
        if (data == null || data.length != 18)
            throw new RuntimeException("ip attribute: expected 18 bytes attribute data");
        try {
            final int prefix = (data[1] & 0xff);
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
     * @see org.tinyradius.attribute.RadiusAttribute#setAttributeValue(String)
     */
    public void setAttributeValue(String value) {
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

            setAttributeData(data);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad IPv6 address : " + value, e);
        }
    }


    /**
     * Check attribute length.
     */
    @Override
    public void readAttribute(byte[] data, int offset) throws RadiusException {
        int length = data[offset + 1] & 0x0ff;
        if (length != 20)
            throw new RadiusException("IP attribute: expected 18 bytes data");
        super.readAttribute(data, offset);
    }

}

package org.tinyradius.core.attribute.rfc;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Rfc6911 {

    public static final byte FRAMED_IPV6_ADDRESS = (byte) 168;
    public static final byte DNS_SERVER_IPV6_ADDRESS = (byte) 169;
    public static final byte ROUTE_IPV6_INFORMATION = (byte) 170;
    public static final byte DELEGATED_IPV6_PREFIX_POOL = (byte) 171;
    public static final byte STATEFUL_IPV6_ADDRESS_POOL = (byte) 172;
}

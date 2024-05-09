package org.tinyradius.core.attribute.rfc;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Rfc2868 {

    public static final byte TUNNEL_TYPE = 64;
    public static final byte TUNNEL_MEDIUM_TYPE = 65;
    public static final byte TUNNEL_CLIENT_ENDPOINT = 66;
    public static final byte TUNNEL_SERVER_ENDPOINT = 67;
    public static final byte TUNNEL_PASSWORD = 69;
    public static final byte TUNNEL_PRIVATE_GROUP_ID = 81;
    public static final byte TUNNEL_ASSIGNMENT_ID = 82;
    public static final byte TUNNEL_PREFERENCE = 83;
    public static final byte TUNNEL_CLIENT_AUTH_ID = 90;
    public static final byte TUNNEL_SERVER_AUTH_ID = 91;
}

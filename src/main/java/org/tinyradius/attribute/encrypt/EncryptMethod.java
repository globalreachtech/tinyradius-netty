package org.tinyradius.attribute.encrypt;

public enum EncryptMethod {
    NONE((byte) 0),
    RFC2865_USER_PASSWORD((byte) 1),
    RFC2868_TUNNEL_PASSWORD((byte) 2),
    ASCENT_SEND_SECRET((byte) 3);

    private final byte id;

    EncryptMethod(byte id) {
        this.id = id;
    }

    public static EncryptMethod fromId(byte id) {
        switch (id) {
            case 1:
                return RFC2865_USER_PASSWORD;
            case 2:
                return RFC2868_TUNNEL_PASSWORD;
            case 3:
                return ASCENT_SEND_SECRET;
            case 0:
            default:
                return NONE;
        }
    }
}

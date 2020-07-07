package org.tinyradius.core.attribute.codec;

/**
 * Attribute encryption methods as used in FreeRadius dictionary files
 */
public enum AttributeCodecType {
    NO_ENCRYPT(
            (byte) 0, new NoOpCodec()),
    RFC2865_USER_PASSWORD(
            (byte) 1, new UserPasswordCodec()),
    RFC2868_TUNNEL_PASSWORD(
            (byte) 2, new TunnelPasswordCodec()),
    ASCENT_SEND_SECRET(
            (byte) 3, new AscendSendSecretCodec());

    private final byte id;
    private final BaseCodec codec;

    AttributeCodecType(byte id, BaseCodec codec) {
        this.id = id;
        this.codec = codec;
    }

    public byte getId() {
        return id;
    }

    public BaseCodec getCodec() {
        return codec;
    }

    public static AttributeCodecType fromId(byte id) {
        switch (id) {
            case 1:
                return RFC2865_USER_PASSWORD;
            case 2:
                return RFC2868_TUNNEL_PASSWORD;
            case 3:
                return ASCENT_SEND_SECRET;
            case 0:
            default:
                return NO_ENCRYPT;
        }
    }
}

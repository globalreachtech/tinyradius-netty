package org.tinyradius.core.attribute.codec;

import java.util.Arrays;

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
    ASCEND_SEND_SECRET(
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

    public static AttributeCodecType fromEncryptFlagId(byte id) {
        return Arrays.stream(values())
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(NO_ENCRYPT);
    }
}

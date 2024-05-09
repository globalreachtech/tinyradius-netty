package org.tinyradius.core.attribute.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * Attribute encryption methods as used in FreeRadius dictionary files
 */
@Getter
@AllArgsConstructor
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

    public static AttributeCodecType fromEncryptFlagId(byte id) {
        return Arrays.stream(values())
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(NO_ENCRYPT);
    }
}

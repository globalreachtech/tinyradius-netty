package org.tinyradius.core.attribute.codec;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * Attribute encryption methods as used in FreeRadius dictionary files
 */
@Getter
@RequiredArgsConstructor
public enum AttributeCodecType {
    NO_ENCRYPT(
            (byte) 0, "", new NoOpCodec()),
    RFC2865_USER_PASSWORD(
            (byte) 1, "User-Password", new UserPasswordCodec()),
    RFC2868_TUNNEL_PASSWORD(
            (byte) 2, "Tunnel-Password", new TunnelPasswordCodec()), // RFC 2548 SALT algo?
    ASCEND_SECRET(
            (byte) 3, "Ascend-Secret", new AscendSecretCodec());

    private final byte id;
    private final String name;
    private final BaseCodec codec;

    public static @NonNull AttributeCodecType fromName(String name) {
        return Arrays.stream(values())
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElse(NO_ENCRYPT);
    }

    public static @NonNull AttributeCodecType fromId(byte id) {
        return Arrays.stream(values())
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(NO_ENCRYPT);
    }
}

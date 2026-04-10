package org.tinyradius.core.attribute.codec;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * Attribute encryption and codec types used for RADIUS attributes.
 * These correspond to the encryption methods specified in RADIUS RFCs
 * and used in FreeRadius dictionary files.
 */
public enum AttributeCodecType {
    /**
     * No encryption/encoding applied to the attribute value.
     */
    NO_ENCRYPT((byte) 0, "", new NoOpCodec()),

    /**
     * PAP User-Password encryption as defined in RFC 2865.
     */
    RFC2865_USER_PASSWORD((byte) 1, "User-Password", new UserPasswordCodec()),

    /**
     * Tunnel-Password encryption with salt as defined in RFC 2868.
     */
    RFC2868_TUNNEL_PASSWORD((byte) 2, "Tunnel-Password", new TunnelPasswordCodec()), // RFC 2548 SALT algo?

    /**
     * Ascend-style secret encryption.
     */
    ASCEND_SECRET((byte) 3, "Ascend-Secret", new AscendSecretCodec());

    private final byte id;
    private final String codecName;
    private final BaseCodec codec;

    /**
     * Constructs an AttributeCodecType with the specified ID, name, and codec.
     *
     * @param id        the internal numeric ID
     * @param codecName the name of the codec
     * @param codec     the codec implementation
     */
    AttributeCodecType(byte id, String codecName, BaseCodec codec) {
        this.id = id;
        this.codecName = codecName;
        this.codec = codec;
    }

    /**
     * The internal numeric ID for the codec type, as defined in Radiator/FreeRadius dictionaries
     */
    public byte getId() {
        return id;
    }

    /**
     * The string name of the codec type, as used in dictionary files.
     */
    public String getCodecName() {
        return codecName;
    }

    /**
     * The codec implementation used to encode and decode attribute data.
     */
    public BaseCodec getCodec() {
        return codec;
    }

    /**
     * Returns the AttributeCodecType for the given name.
     *
     * @param name the name of the codec type
     * @return the corresponding AttributeCodecType, or NO_ENCRYPT if not found
     */
    public static @NonNull AttributeCodecType fromName(String name) {
        return Arrays.stream(values())
                .filter(t -> t.getCodecName().equals(name))
                .findFirst()
                .orElse(NO_ENCRYPT);
    }

    /**
     * Returns the AttributeCodecType for the given ID.
     *
     * @param id the ID of the codec type
     * @return the corresponding AttributeCodecType, or NO_ENCRYPT if not found
     */
    public static @NonNull AttributeCodecType fromId(byte id) {
        return Arrays.stream(values())
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(NO_ENCRYPT);
    }
}

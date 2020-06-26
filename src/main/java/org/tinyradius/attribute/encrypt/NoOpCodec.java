package org.tinyradius.attribute.encrypt;

/**
 * No-op encryption
 */
public class NoOpCodec implements AttributeCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) {
        return data;
    }
}

package org.tinyradius.attribute.encrypt;

/**
 * No-op encryption
 */
public class NoOpCodec extends AbstractCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] requestAuth) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] requestAuth) {
        return data;
    }
}

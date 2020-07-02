package org.tinyradius.attribute.codec;

/**
 * No-op encryption
 */
class NoOpCodec extends BaseCodec {

    @Override
    protected byte[] encodeData(byte[] data, byte[] auth, byte[] secret) {
        return data;
    }

    @Override
    protected byte[] decodeData(byte[] encodedData, byte[] auth, byte[] secret) {
        return encodedData;
    }
}

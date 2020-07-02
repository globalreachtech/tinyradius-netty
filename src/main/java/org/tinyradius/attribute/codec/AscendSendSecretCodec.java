package org.tinyradius.attribute.codec;

/**
 * *Stub Noop Codec to be implemented*
 * <p>
 * Attribute is encrypted as per Ascend's definitions for the Ascend-Send-Secret attribute
 */
class AscendSendSecretCodec extends BaseCodec {
    // todo

    @Override
    protected byte[] encodeData(byte[] data, byte[] auth, byte[] secret) {
        return data;
    }

    @Override
    protected byte[] decodeData(byte[] encodedData, byte[] auth, byte[] secret) {
        return encodedData;
    }
}

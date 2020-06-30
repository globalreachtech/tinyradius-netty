package org.tinyradius.attribute.codec;

/**
 * Attribute is encrypted as per Ascend's definitions for the Ascend-Send-Secret attribute
 * TODO
 */
class AscendSendSecretCodec extends BaseCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] requestAuth) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] requestAuth) {
        return data;
    }
}

package org.tinyradius.core.attribute.codec;

import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;

/**
 * Attribute is encrypted as per Ascend's definitions for the Ascend-Send-Secret attribute.
 * <p>
 * A Vernam cipher: the data is XORed with {@code MD5(Request-Authenticator || shared-secret)},
 * with the MD5 inputs in the <em>reverse order</em> from RFC 2865 User-Password.
 * Chaining for attributes longer than 16 bytes also uses the reverse order:
 * {@code MD5(previous-ciphertext-block || shared-secret)}.
 * <p>
 * {@see https://doc.freeradius.org/protocols_2radius_2base_8c.html#a0ff13a8aa36a1743846d351b2e972ea1}
 */
class AscendSecretCodec extends BaseCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte @NonNull [] encodeData(byte @NonNull [] data, byte @NonNull [] auth, byte @NonNull [] secret) {
        return cbcMd5Encode(data, auth, secret, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte @NonNull [] decodeData(byte @NonNull [] encodedData, byte @NonNull [] auth, byte @NonNull [] secret) throws RadiusPacketException {
        byte[] decoded = cbcMd5Decode(encodedData, auth, secret, false);
        return rTrim(decoded);
    }
}

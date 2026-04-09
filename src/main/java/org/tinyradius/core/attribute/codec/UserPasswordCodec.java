package org.tinyradius.core.attribute.codec;

import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;

/**
 * Attribute is encrypted with the method as defined in RFC2865 for the User-Password attribute
 */
class UserPasswordCodec extends BaseCodec {

    @Override
    protected byte @NonNull [] encodeData(byte @NonNull [] data, byte @NonNull [] auth, byte @NonNull [] secret) {
        return cbcMd5Encode(data, auth, secret, true);
    }

    @Override
    protected byte @NonNull [] decodeData(byte @NonNull [] encodedData, byte @NonNull [] auth, byte @NonNull [] secret) throws RadiusPacketException {
        byte[] decoded = cbcMd5Decode(encodedData, auth, secret, true);
        return rTrim(decoded);
    }
}

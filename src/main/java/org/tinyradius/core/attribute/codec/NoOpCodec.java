package org.tinyradius.core.attribute.codec;

import org.jspecify.annotations.NonNull;

/**
 * No-op encryption
 */
class NoOpCodec extends BaseCodec {

    @Override
    protected byte @NonNull [] encodeData(byte @NonNull [] data, byte @NonNull [] auth, byte @NonNull [] secret) {
        return data;
    }

    @Override
    protected byte @NonNull [] decodeData(byte @NonNull [] encodedData, byte @NonNull [] auth, byte @NonNull [] secret) {
        return encodedData;
    }
}

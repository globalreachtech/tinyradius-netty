package org.tinyradius.core.packet.response;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;

import java.util.List;

/**
 * Generic RADIUS response packet implementation.
 * <p>
 * This is a basic implementation of {@link RadiusResponse} that can encode
 * response packets (Access-Accept, Access-Reject, Access-Challenge, Accounting-Response).
 * Use {@link AccessResponse} for access-specific responses that require
 * Message-Authenticator support.
 */
public class GenericResponse extends BaseRadiusPacket<RadiusResponse> implements RadiusResponse {

    public GenericResponse(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public @NonNull RadiusResponse encodeResponse(@NonNull String sharedSecret, byte @NonNull [] requestAuth) throws RadiusPacketException {
        var response = withAttributes(encodeAttributes(requestAuth, sharedSecret));
        var auth = response.genHashedAuth(sharedSecret, requestAuth);
        return withAuthAttributes(auth, response.getAttributes());
    }

    @Override
    public @NonNull RadiusResponse decodeResponse(@NonNull String sharedSecret, byte @NonNull [] requestAuth) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, requestAuth);
        return withAttributes(decodeAttributes(requestAuth, sharedSecret));
    }

    @Override
    public @NonNull RadiusResponse withAuthAttributes(byte @Nullable [] auth, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        return RadiusResponse.create(getDictionary(), getType(), getId(), auth, attributes);
    }
}

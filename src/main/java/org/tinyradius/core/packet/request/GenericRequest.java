package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;

import java.util.List;

/**
 * Generic RADIUS request packet implementation.
 * <p>
 * This is a basic implementation of {@link RadiusRequest} that can encode
 * request packets (e.g., Accounting-Request).
 */
public class GenericRequest extends BaseRadiusPacket<RadiusRequest> implements RadiusRequest {

    /**
     * Constructs a GenericRequest.
     *
     * @param dictionary the dictionary to use
     * @param header     the packet header
     * @param attributes the packet attributes
     * @throws RadiusPacketException if there is an error creating the request
     */
    public GenericRequest(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    /**
     * Generates the authenticator for this request.
     *
     * @param sharedSecret to generate authenticator
     * @return new authenticator, must be idempotent
     */
    protected byte[] genAuth(String sharedSecret) {
        return genHashedAuth(sharedSecret, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RadiusRequest encodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        if (sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        byte[] auth = genAuth(sharedSecret);
        return withAuthAttributes(auth, encodeAttributes(auth, sharedSecret));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        var auth = verifyPacketAuth(sharedSecret, null);
        return withAttributes(decodeAttributes(auth, sharedSecret));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RadiusRequest withAuthAttributes(byte @Nullable [] auth, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        return RadiusRequest.create(getDictionary(), getType(), getId(), auth, attributes);
    }
}

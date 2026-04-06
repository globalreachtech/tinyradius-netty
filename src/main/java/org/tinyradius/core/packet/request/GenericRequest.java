package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;

public class GenericRequest extends BaseRadiusPacket<RadiusRequest> implements RadiusRequest {

    public GenericRequest(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    /**
     * @param sharedSecret to generate authenticator
     * @return new authenticator, must be idempotent
     */
    protected byte[] genAuth(String sharedSecret) {
        return genHashedAuth(sharedSecret, null);
    }

    @Override
    public @NonNull RadiusRequest encodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        if (sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        byte[] auth = genAuth(sharedSecret);
        return withAuthAttributes(auth, encodeAttributes(auth, sharedSecret));
    }

    @Override
    public @NonNull RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, null);
        return withAttributes(decodeAttributes(getAuthenticator(), sharedSecret));
    }

    @Override
    public @NonNull RadiusRequest withAuthAttributes(byte[] auth, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        return RadiusRequest.create(getDictionary(), getType(), getId(), auth, attributes);
    }
}

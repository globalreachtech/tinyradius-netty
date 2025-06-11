package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;

import java.util.List;

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
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        final byte[] auth = genAuth(sharedSecret);
        return withAuthAttributes(auth, encodeAttributes(auth, sharedSecret));
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, null);
        return withAttributes(decodeAttributes(getAuthenticator(), sharedSecret));
    }

    @Override
    public RadiusRequest withAuthAttributes(byte[] auth, List<RadiusAttribute> attributes) throws RadiusPacketException {
        return RadiusRequest.create(getDictionary(), getType(), getId(), auth, attributes);
    }
}

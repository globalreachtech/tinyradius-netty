package org.tinyradius.packet.response;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.packet.util.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessResponse extends BaseRadiusPacket<RadiusResponse> implements RadiusResponse, MessageAuthSupport<RadiusResponse> {

    public AccessResponse(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public AccessResponse encodeResponse(String sharedSecret, byte[] requestAuth) {
        final RadiusResponse response = encodeMessageAuth(sharedSecret, requestAuth);
        final byte[] newAuth = response.createHashedAuthenticator(sharedSecret, requestAuth);

        return new AccessResponse(response.getDictionary(), response.getType(), response.getId(), newAuth, response.getAttributes());
    }

    @Override
    public AccessResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        RadiusResponse.super.decodeResponse(sharedSecret, requestAuth);
        verifyMessageAuth(sharedSecret, requestAuth);
        return this;
    }

    @Override
    public AccessResponse withAttributes(List<RadiusAttribute> attributes) {
        return new AccessResponse(getDictionary(), getType(), getId(), getAuthenticator(), attributes);
    }
}

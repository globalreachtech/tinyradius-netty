package org.tinyradius.packet.response;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.util.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessResponse extends GenericResponse implements MessageAuthSupport<RadiusResponse> {

    public AccessResponse(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final byte[] auth = genHashedAuth(sharedSecret, requestAuth);
        return new AccessResponse(getDictionary(), getType(), getId(), auth, encodeAttributes(sharedSecret, auth))
                .encodeMessageAuth(sharedSecret, requestAuth);
    }

    @Override
    public RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyMessageAuth(sharedSecret, requestAuth);
        return super.decodeResponse(sharedSecret, requestAuth);
    }

    @Override
    public AccessResponse withAttributes(List<RadiusAttribute> attributes) {
        return new AccessResponse(getDictionary(), getType(), getId(), getAuthenticator(), attributes);
    }
}

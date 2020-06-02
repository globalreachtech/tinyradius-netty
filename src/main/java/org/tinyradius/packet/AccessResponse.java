package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.util.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessResponse extends RadiusResponse implements MessageAuthSupport<AccessResponse> {

    public AccessResponse(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public AccessResponse encodeResponse(String sharedSecret, byte[] requestAuth) {
        final AccessResponse response = encodeMessageAuth(sharedSecret, requestAuth);
        final byte[] newAuth = response.createHashedAuthenticator(sharedSecret, requestAuth);

        return new AccessResponse(response.getDictionary(), response.getType(), response.getId(), newAuth, response.getAttributes());
    }

    @Override
    public void verifyResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        super.verifyResponse(sharedSecret, requestAuth);
        verifyMessageAuth(sharedSecret, requestAuth);
    }

    @Override
    public AccessResponse copy() {
        return new AccessResponse(getDictionary(), getType(), getId(), getAuthenticator(), getAttributes());
    }
}

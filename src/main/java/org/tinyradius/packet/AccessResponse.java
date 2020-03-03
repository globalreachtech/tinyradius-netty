package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.auth.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.ArrayList;
import java.util.List;

public class AccessResponse extends RadiusResponse implements MessageAuthSupport<AccessResponse> {

    public AccessResponse(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public AccessResponse encodeResponse(String sharedSecret, byte[] requestAuth) {
        final AccessResponse response = encodeMessageAuth(sharedSecret, requestAuth);
        final byte[] newAuth = response.createHashedAuthenticator(sharedSecret, requestAuth);

        return new AccessResponse(response.getDictionary(), response.getType(), response.getIdentifier(), newAuth, response.getAttributes());
    }

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     *
     * @param sharedSecret shared secret, only applicable for PAP
     * @param requestAuth  authenticator for corresponding request
     */
    @Override
    public void verifyResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        super.verifyResponse(sharedSecret, requestAuth);
        verifyMessageAuth(sharedSecret, requestAuth);
    }

    @Override
    public AccessResponse copy() {
        return new AccessResponse(getDictionary(), getType(), getIdentifier(), getAuthenticator(), new ArrayList<>(getAttributes()));
    }
}

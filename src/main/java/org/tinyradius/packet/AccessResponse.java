package org.tinyradius.packet;

import org.tinyradius.attribute.Attributes;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.auth.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessResponse extends RadiusResponse implements MessageAuthSupport {

    public AccessResponse(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) {
        removeAttributes(MESSAGE_AUTHENTICATOR);
        final byte[] bytes = computeMessageAuth(sharedSecret, requestAuth);
        addAttribute(Attributes.create(getDictionary(), getVendorId(), getType(), bytes));
        return super.encodeResponse(sharedSecret, requestAuth);
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
}

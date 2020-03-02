package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.auth.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessEap extends AccessRequest {

    public AccessEap(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequest encodeAuthMechanism(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
        throw new RadiusPacketException("EAP Auth not yet implemented");
    }

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     * <p>
     * Instead it checks the User-Password/Challenge attributes
     * are present and attempts decryption.
     *
     * @param sharedSecret shared secret, only applicable for PAP
     */
    @Override
    protected void verifyAuthMechanism(String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> attrs = getAttributes(MessageAuthSupport.MESSAGE_AUTHENTICATOR);
        if (attrs.isEmpty()) {
            throw new RadiusPacketException("EAP-Message detected, but Message-Authenticator not found");
        }
    }

    @Override
    public AccessRequest copy() {
        return null;
    }
}

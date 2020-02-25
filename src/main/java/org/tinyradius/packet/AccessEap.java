package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessEap extends AccessRequest {

    private static final int MESSAGE_AUTHENTICATOR = 80;

    public AccessEap(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequest encodeRequest(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
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
     * @param ignored      ignored, not applicable for AccessRequest
     */
    @Override
    public void verify(String sharedSecret, byte[] ignored) throws RadiusPacketException {
        final List<RadiusAttribute> messageAuth = getAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuth.isEmpty())
            throw new RadiusPacketException("EAP-Message detected, but Message-Authenticator not found");
    }
}

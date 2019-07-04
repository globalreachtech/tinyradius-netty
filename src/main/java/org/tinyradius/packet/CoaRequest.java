package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.io.IOException;
import java.util.List;

/**
 * Represents CoA-Request and Disconnect-Request.
 */
public class CoaRequest extends RadiusPacket {

    /**
     * @param type          should be one of COA_REQUEST or DISCONNECT_REQUEST
     * @param identifier
     * @param authenticator
     */
    public CoaRequest(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    protected CoaRequest encodeRequest(String sharedSecret) throws IOException {
        final byte[] authenticator = createHashedAuthenticator(sharedSecret, getAttributeBytes(), new byte[16]);
        return new CoaRequest(getDictionary(), getPacketType(), getPacketIdentifier(), authenticator, getAttributes());
    }
}

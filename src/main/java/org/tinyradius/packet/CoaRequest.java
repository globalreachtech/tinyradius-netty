package org.tinyradius.packet;

import java.io.IOException;

/**
 * Represents CoA-Request and Disconnect-Request.
 */
public class CoaRequest extends RadiusPacket {

    /**
     * @param type          should be one of COA_REQUEST or DISCONNECT_REQUEST
     * @param identifier
     * @param authenticator
     */
    public CoaRequest(int type, int identifier, byte[] authenticator) {
        super(type, identifier, authenticator);
    }

    @Override
    protected RadiusPacket encodeRequest(String sharedSecret) throws IOException {
        return encodePacket(sharedSecret, new byte[16]);
    }
}

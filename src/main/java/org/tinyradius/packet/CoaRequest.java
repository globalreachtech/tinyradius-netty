package org.tinyradius.packet;

import java.io.IOException;

/**
 * Represents CoA-Request and Disconnect-Request.
 */
public class CoaRequest extends RadiusPacket {

    /**
     *
     * @param type should be one of COA_REQUEST or DISCONNECT_REQUEST
     * @param identifier
     */
    public CoaRequest(final int type, int identifier) {
        super(type, identifier);
    }

    @Override
    protected void encodeRequest(String sharedSecret) throws IOException {
        encodePacket(sharedSecret, new byte[16]);
    }
}

package org.tinyradius.packet;

import java.io.IOException;

/**
 * Represents CoA-Request and Disconnect-Request.
 */
public class CoaRequest extends RadiusPacket {

    public CoaRequest() {
        this(COA_REQUEST);
    }

    public CoaRequest(final int type) {
        super(type);
    }

    @Override
    protected void encodeRequest(String sharedSecret) throws IOException {
        encodePacket(sharedSecret, new byte[16]);
    }
}

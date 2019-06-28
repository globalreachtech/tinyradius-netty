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
        byte[] attributes = getAttributeBytes();
        int packetLength = RADIUS_HEADER_LENGTH + attributes.length;
        if (packetLength > MAX_PACKET_LENGTH)
            throw new RuntimeException("packet too long");

        authenticator = createHashedAuthenticator(sharedSecret, packetLength, attributes, new byte[16]);;
    }
}

package org.tinyradius.packet;

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

    /**
     * @see AccountingRequest#updateRequestAuthenticator(String, int, byte[])
     */
    protected byte[] updateRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes) {
        return createResponseAuthenticator(sharedSecret, packetLength, attributes, new byte[16]);
    }
}

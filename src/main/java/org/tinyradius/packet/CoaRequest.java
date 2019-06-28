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
     * Calculates the clientRequest authenticator as specified by RFC 5176, as defined in RFC 2866.
     */
    @Override
    protected byte[] createRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes) {
        return createHashedAuthenticator(sharedSecret, packetLength, attributes, new byte[16]);
    }
}

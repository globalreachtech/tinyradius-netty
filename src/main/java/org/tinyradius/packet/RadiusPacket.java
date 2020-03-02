package org.tinyradius.packet;

import org.tinyradius.attribute.AttributeHolder;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface RadiusPacket extends AttributeHolder {

    int HEADER_LENGTH = 20;

    /**
     * @return Radius packet type
     */
    int getType();

    /**
     * @return Radius packet identifier
     */
    int getIdentifier();

    /**
     * Returns the authenticator for this Radius packet.
     * <p>
     * For a Radius packet read from a stream, this will return the
     * authenticator sent by the server.
     * <p>
     * For a new Radius packet to be sent, this will return the authenticator created,
     * or null if no authenticator has been created yet.
     *
     * @return authenticator, 16 bytes
     */
    byte[] getAuthenticator();

    /**
     * @return list of RadiusAttributes in packet
     */
    List<RadiusAttribute> getAttributes();

    /**
     * @return the dictionary this Radius packet uses.
     */
    Dictionary getDictionary();

    RadiusPacket copy();

    /**
     * TODO move somewhere else
     *
     * @return
     */
    static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // never happens
        }
    }
}

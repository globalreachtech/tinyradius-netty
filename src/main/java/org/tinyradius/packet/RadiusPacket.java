package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.util.NestedAttributeHolder;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;

public interface RadiusPacket extends NestedAttributeHolder.Writable {

    /**
     * @return Radius packet type
     */
    byte getType();

    /**
     * @return Radius packet identifier
     */
    byte getId();

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
    @Override
    List<RadiusAttribute> getAttributes();

    /**
     * @return the dictionary this Radius packet uses.
     */
    @Override
    Dictionary getDictionary();

    /**
     * @return packet of same type as self, including intermediate fields
     */
    RadiusPacket copy();
}

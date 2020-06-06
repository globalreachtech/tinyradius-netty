package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.ACCOUNTING_REQUEST;

/**
 * This class represents a Radius packet of the type Accounting-Request.
 */
public class AccountingRequest extends BaseRadiusPacket<AccountingRequest> implements RadiusRequest {

    /**
     * Constructs an Accounting-Request packet to be sent to a Radius server.
     *
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes
     */
    public AccountingRequest(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCOUNTING_REQUEST, identifier, authenticator, attributes);
    }

    @Override
    public AccountingRequest encodeRequest(String sharedSecret) {
        final byte[] authenticator = createHashedAuthenticator(sharedSecret, new byte[16]);
        return new AccountingRequest(getDictionary(),getId(), authenticator, getAttributes());
    }

    @Override
    public AccountingRequest withAttributes(List<RadiusAttribute> attributes) {
        return new AccountingRequest(getDictionary(), getId(), getAuthenticator(), attributes);
    }
}

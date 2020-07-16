package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.PacketType;

import java.util.List;

import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

/**
 * This class represents a Radius packet of the type Accounting-Request.
 */
public class AccountingRequest extends GenericRequest {

    public AccountingRequest(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
        final byte type = header.getByte(0);
        if (type != ACCOUNTING_REQUEST)
            throw new IllegalArgumentException("First octet must be " + ACCOUNTING_REQUEST + ", actual: " + type);
    }

    @Override
    public AccountingRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        return new AccountingRequest(getDictionary(), headerWithAuth(genAuth(sharedSecret)), getAttributes());
    }

    @Override
    public AccountingRequest withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException {
        return new AccountingRequest(getDictionary(), getHeader(), attributes);
    }
}

package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;

import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

/**
 * This class represents a Radius packet of the type Accounting-Request.
 */
public class AccountingRequest extends GenericRequest {

    /**
     * Constructs an AccountingRequest.
     *
     * @param dictionary the dictionary to use
     * @param header     the packet header
     * @param attributes the packet attributes
     * @throws RadiusPacketException if there is an error creating the request
     */
    public AccountingRequest(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
        byte type = header.getByte(0);
        if (type != ACCOUNTING_REQUEST)
            throw new IllegalArgumentException("First octet must be " + ACCOUNTING_REQUEST + ", actual: " + type);
    }
}

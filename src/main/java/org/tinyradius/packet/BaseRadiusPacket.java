package org.tinyradius.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.packet.util.PacketType;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
public abstract class BaseRadiusPacket<T extends RadiusPacket<T>> implements RadiusPacket<T> {

    protected static final Logger logger = LogManager.getLogger();

    private static final int CHILD_VENDOR_ID = -1;

    private final byte type;
    private final byte id;
    private final List<RadiusAttribute> attributes;
    private final byte[] authenticator;

    private final Dictionary dictionary;

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusRequest#create(Dictionary, byte, byte, byte[], List)}
     * or {@link RadiusResponse#create(Dictionary, byte, byte, byte[], List)}
     * where possible as that automatically creates Request/Response
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param id            packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects (a shallow copy will be created)
     */
    public BaseRadiusPacket(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) {
        if (authenticator != null && authenticator.length != 16)
            throw new IllegalArgumentException("Authenticator must be 16 octets, actual: " + authenticator.length);

        this.type = type;
        this.id = id;
        this.authenticator = authenticator;
        this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
        this.dictionary = requireNonNull(dictionary, "Dictionary is null");
    }

    @Override
    public int getChildVendorId() {
        return CHILD_VENDOR_ID;
    }

    @Override
    public byte getId() {
        return id;
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public byte[] getAuthenticator() {
        return authenticator;
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }


    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append(PacketType.getPacketTypeName(getType()));
        s.append(", ID ");
        s.append(getId());
        for (RadiusAttribute attr : getAttributes()) {
            s.append("\n");
            s.append(attr.toString());
        }
        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseRadiusPacket)) return false;
        BaseRadiusPacket<?> that = (BaseRadiusPacket<?>) o;
        return type == that.type &&
                id == that.id &&
                Objects.equals(attributes, that.attributes) &&
                Arrays.equals(authenticator, that.authenticator) &&
                Objects.equals(dictionary, that.dictionary);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, id, attributes, dictionary);
        result = 31 * result + Arrays.hashCode(authenticator);
        return result;
    }
}

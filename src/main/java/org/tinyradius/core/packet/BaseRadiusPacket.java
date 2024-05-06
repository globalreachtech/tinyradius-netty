package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
public abstract class BaseRadiusPacket<T extends RadiusPacket<T>> implements RadiusPacket<T> {

    private static final Logger logger = LogManager.getLogger();

    private static final int HEADER_LENGTH = 20;
    private static final int CHILD_VENDOR_ID = -1;

    private final Dictionary dictionary;
    private final ByteBuf header;
    private final List<RadiusAttribute> attributes;

    protected BaseRadiusPacket(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        this.dictionary = Objects.requireNonNull(dictionary, "Dictionary is null");
        this.header = Objects.requireNonNull(header);
        this.attributes = List.copyOf(attributes);

        if (header.readableBytes() != HEADER_LENGTH)
            throw new IllegalArgumentException("Packet header must be length " + HEADER_LENGTH + ", actual: " + header.readableBytes());

        final int length = getLength();
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Packet too long - length max " + MAX_PACKET_LENGTH + ", actual: " + length);

        final short declaredLength = header.getShort(2);
        if (length != declaredLength)
            throw new RadiusPacketException("Packet length mismatch, " +
                    "actual length (" + length + ")  does not match declared length (" + declaredLength + ")");
    }

    @Override
    public int getChildVendorId() {
        return CHILD_VENDOR_ID;
    }

    @Override
    public ByteBuf getHeader() {
        return Unpooled.unreleasableBuffer(header);
    }

    @Override
    public byte getId() {
        return header.getByte(1);
    }

    @Override
    public byte getType() {
        return header.getByte(0);
    }

    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public byte[] getAuthenticator() {
        final byte[] array = header.slice(4, 16).copy().array();
        return Arrays.equals(array, new byte[array.length]) ?
                null : array;
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public T withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf newHeader = RadiusPacket.buildHeader(getType(), getId(), getAuthenticator(), attributes);
        return with(newHeader, attributes);
    }

    public T withAuthAttributes(byte[] auth, List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf newHeader = RadiusPacket.buildHeader(getType(), getId(), auth, attributes);
        return with(newHeader, attributes);
    }

    /**
     * Naive with(), does not recalculate packet lengths in header.
     *
     * @param header     Radius packet header
     * @param attributes Radius packet attributes
     * @return RadiusPacket with the specified headers and attributes
     * @throws RadiusPacketException packet validation exceptions
     */
    protected abstract T with(ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException;

    /**
     * @param sharedSecret shared secret
     * @param requestAuth  request authenticator if verifying response,
     *                     otherwise set to 16 zero octets
     * @throws RadiusPacketException if authenticator check fails
     */
    protected void verifyPacketAuth(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final byte[] expectedAuth = genHashedAuth(sharedSecret, requestAuth);
        final byte[] auth = getAuthenticator();
        if (auth == null)
            throw new RadiusPacketException("Packet Authenticator check failed - authenticator missing");

        if (auth.length != 16)
            throw new RadiusPacketException("Packet Authenticator check failed - must be 16 octets, actual " + auth.length);

        if (!Arrays.equals(expectedAuth, auth)) {
            // find attributes that should be encoded but aren't
            final boolean decodedAlready = getAttributes().stream()
                    .filter(a -> a.getAttributeTemplate()
                            .map(AttributeTemplate::isEncrypt)
                            .orElse(false))
                    .anyMatch(a -> !a.isEncoded());

            if (decodedAlready)
                logger.info("Skipping Packet Authenticator check - attributes have been decrypted already");
            else
                throw new RadiusPacketException("Packet Authenticator check failed - bad authenticator or shared secret");
        }
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder()
                .append(PacketType.getPacketTypeName(getType()))
                .append(", ID ").append(getId())
                .append(", len ").append(getLength());

        if (!getAttributes().isEmpty()) {
            s.append(", attributes={");
            for (RadiusAttribute attr : getAttributes()) {
                s.append("\n").append(attr.toString());
            }
            s.append("\n}");
        }

        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseRadiusPacket)) return false;
        final BaseRadiusPacket<?> that = (BaseRadiusPacket<?>) o;
        return header.equals(that.header) &&
                attributes.equals(that.attributes) &&
                dictionary.equals(that.dictionary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, attributes, dictionary);
    }
}

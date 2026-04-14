package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
public abstract class BaseRadiusPacket<T extends RadiusPacket<T>> implements RadiusPacket<T> {

    private static final Logger log = LogManager.getLogger(BaseRadiusPacket.class);
    private static final int HEADER_LENGTH = 20;

    private final Dictionary dictionary;

    private final ByteBuf header;
    private final List<RadiusAttribute> attributes;

    /**
     * Constructs a BaseRadiusPacket.
     *
     * @param dictionary the dictionary to use for attribute lookups
     * @param header     the 20-octet packet header
     * @param attributes the list of attributes for this packet
     * @throws RadiusPacketException if the packet length or header is invalid
     */
    protected BaseRadiusPacket(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        this.dictionary = dictionary;
        this.header = header;
        this.attributes = List.copyOf(attributes);

        if (header.readableBytes() != HEADER_LENGTH)
            throw new IllegalArgumentException("Packet header must be length " + HEADER_LENGTH + ", actual: " + header.readableBytes());

        int length = getLength();
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Packet too long - length max " + MAX_PACKET_LENGTH + ", actual: " + length);

        short declaredLength = header.getShort(2);
        if (length != declaredLength)
            throw new RadiusPacketException("Packet length mismatch, " +
                    "actual length (" + length + ")  does not match declared length (" + declaredLength + ")");
    }

    /**
     * Returns the dictionary used by this packet.
     *
     * @return the dictionary
     */
    @Override
    @NonNull
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * Returns the list of attributes in this packet.
     *
     * @return the attributes
     */
    @Override
    @NonNull
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ByteBuf getHeader() {
        return Unpooled.unreleasableBuffer(header);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public T withAttributes(@NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        return withAuthAttributes(getAuthenticator(), attributes);
    }

    /**
     * Creates a new packet with the given authenticator and attributes.
     *
     * @param auth       the authenticator
     * @param attributes the attributes
     * @return the new packet
     * @throws RadiusPacketException if there is an error creating the packet
     */
    @NonNull
    public abstract T withAuthAttributes(byte @Nullable [] auth, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException;

    /**
     * Verifies the packet authenticator.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  request authenticator if verifying response
     * @throws RadiusPacketException if the packet authenticator check fails
     */
    protected byte @NonNull [] verifyPacketAuth(@NonNull String sharedSecret, byte @Nullable [] requestAuth) throws RadiusPacketException {
        byte[] expectedAuth = genHashedAuth(sharedSecret, requestAuth);
        byte[] auth = getAuthenticator();
        if (auth == null)
            throw new RadiusPacketException("Packet Authenticator check failed - authenticator missing");

        if (auth.length != 16)
            throw new RadiusPacketException("Packet Authenticator check failed - must be 16 octets, actual " + auth.length);

        if (!Arrays.equals(expectedAuth, auth)) {
            // find attributes that can be encoded but aren't
            var decodedAlready = getAttribute(a ->
                    a.codecType() != NO_ENCRYPT && a.isDecoded()).isPresent();

            if (decodedAlready)
                log.debug("Skipping Packet Authenticator check - attributes have been decrypted already");
            else
                throw new RadiusPacketException("Packet Authenticator check failed - bad authenticator or shared secret");
        }
        return auth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toString() {
        var s = new StringBuilder()
                .append(PacketType.getPacketTypeName(getType()))
                .append(", ID ").append(getId())
                .append(", len ").append(getLength());

        if (!getAttributes().isEmpty()) {
            s.append(", attributes={");
            for (var attr : getAttributes()) {
                s.append("\n").append(attr.toString());
            }
            s.append("\n}");
        }

        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseRadiusPacket<?> that)) return false;
        return Objects.equals(header, that.header) && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, attributes);
    }
}

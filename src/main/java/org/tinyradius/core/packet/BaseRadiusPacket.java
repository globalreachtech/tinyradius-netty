package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Arrays;
import java.util.List;

import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
@Getter
@Log4j2
@EqualsAndHashCode
public abstract class BaseRadiusPacket<T extends RadiusPacket<T>> implements RadiusPacket<T> {

    private static final int HEADER_LENGTH = 20;

    private final Dictionary dictionary;

    private final ByteBuf header;
    private final List<RadiusAttribute> attributes;

    protected BaseRadiusPacket(@NonNull Dictionary dictionary, @NonNull ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        this.dictionary = dictionary;
        this.header = header;
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
    public ByteBuf getHeader() {
        return Unpooled.unreleasableBuffer(header);
    }

    @Override
    public T withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException {
        return withAuthAttributes(getAuthenticator(), attributes);
    }

    public abstract T withAuthAttributes(byte[] auth, List<RadiusAttribute> attributes) throws RadiusPacketException;

    /**
     * @param sharedSecret shared secret
     * @param requestAuth  request authenticator if verifying response, defaults to empty byte[16] if null
     * @throws RadiusPacketException if the packet authenticator check fails
     */
    protected void verifyPacketAuth(String sharedSecret, @Nullable byte[] requestAuth) throws RadiusPacketException {
        final byte[] expectedAuth = genHashedAuth(sharedSecret, requestAuth);
        final byte[] auth = getAuthenticator();
        if (auth == null)
            throw new RadiusPacketException("Packet Authenticator check failed - authenticator missing");

        if (auth.length != 16)
            throw new RadiusPacketException("Packet Authenticator check failed - must be 16 octets, actual " + auth.length);

        if (!Arrays.equals(expectedAuth, auth)) {
            // find attributes that can be encoded but aren't
            final boolean decodedAlready = getAttribute(a ->
                    a.codecType() != NO_ENCRYPT && a.isDecoded()).isPresent();

            if (decodedAlready)
                log.info("Skipping Packet Authenticator check - attributes have been decrypted already");
            else
                throw new RadiusPacketException("Packet Authenticator check failed - bad authenticator or shared secret");
        }
    }

    @Override
    public String toString() {
        var s = new StringBuilder()
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
}

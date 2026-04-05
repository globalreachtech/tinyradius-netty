package org.tinyradius.core.attribute;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.AnonSubAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.tinyradius.core.attribute.type.RadiusAttribute.HEX_FORMAT;

/**
 * Basic attribute holder, for VendorSpecificAttribute (to hold sub-attributes) or RadiusPackets
 * <p>
 * Should only hold a single layer of attributes
 * @param <T> The type of the attribute holder
 */
public interface AttributeHolder<T extends AttributeHolder<T>> {

    Logger attrHolderLogger = LogManager.getLogger();

    /**
     * Converts a list of attributes to a ByteBuf.
     *
     * @param attributes the list of attributes
     * @return a ByteBuf containing the serialized attributes
     */
    @NonNull
    static ByteBuf attributesToBytes(@NonNull List<RadiusAttribute> attributes) {
        return Unpooled.wrappedBuffer(attributes.stream()
                .map(RadiusAttribute::toByteBuf)
                .toArray(ByteBuf[]::new));
    }

    /**
     * Reads attributes and increments readerIndex.
     *
     * @param dictionary dictionary to parse attribute
     * @param vendorId   vendor Id to set attributes
     * @param data       byte array to parse
     * @return list of RadiusAttributes
     */
    @NonNull
    static List<RadiusAttribute> readAttributes(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        var vendor = dictionary.getVendor(vendorId);

        // if reading sub-attribute for undefined VSA, treat entire body of vsa (ex vendorId) as undistinguished bytes
        if (vendorId != -1 && vendor.isEmpty())
            return Collections.singletonList(new AnonSubAttribute(dictionary, vendorId, data));

        var attributes = new ArrayList<RadiusAttribute>();

        try {
            // at least 2 octets left (minimum size header)
            while (data.isReadable(2)) {
                attributes.add(readAttribute(dictionary, vendorId, data));
            }

            if (data.isReadable())
                throw new IllegalArgumentException("Attribute malformed, " + data.readableBytes() + " bytes remaining to parse (minimum 2 octets)");
        } catch (Exception e) {
            attrHolderLogger.trace("Could not extract all attributes: 0x{}",
                    HEX_FORMAT.formatHex(data.copy().array()));
            throw new IllegalArgumentException("Error reading attributes, already extracted attributes: " + attributes, e);
        }

        return attributes;
    }

    /**
     * Parses an attribute and increases readerIndex by size of attribute.
     *
     * @param dictionary dictionary to parse attribute
     * @param vendorId   vendor Id to set attributes
     * @param data       byte array to parse
     * @return a RadiusAttribute
     */
    @NonNull
    static RadiusAttribute readAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        var vendor = dictionary.getVendor(vendorId);

        int typeSize = vendor
                .map(Vendor::typeSize)
                .orElse(1);

        int type = switch (typeSize) {
            case 2 -> data.getShort(data.readerIndex());
            case 4 -> data.getInt(data.readerIndex());
            default -> Byte.toUnsignedInt(data.getByte(data.readerIndex()));
        };

        int lengthSize = vendor
                .map(Vendor::lengthSize)
                .orElse(1);

        int length = switch (lengthSize) {
            case 0 -> data.readableBytes();
            case 2 -> data.getShort(data.readerIndex() + typeSize);
            default -> Byte.toUnsignedInt(data.getByte(data.readerIndex() + typeSize)); // max 255
        };

        if (length < typeSize + lengthSize)
            throw new IllegalArgumentException("Invalid attribute length " + length + ", must be >= typeSize + lengthSize, " +
                    "but typeSize=" + typeSize + ", lengthSize=" + lengthSize);

        if (length > data.readableBytes())
            throw new IllegalArgumentException("Invalid attribute length " + length + ", parsable bytes " + data.readableBytes());

        return dictionary.createAttribute(vendorId, type, data.readSlice(length));
    }

    /**
     * Gets the VendorId to restrict (sub)attributes, or -1 for top level.
     * @return VendorId to restrict (sub)attributes, or -1 for top level
     */
    int getChildVendorId();

    /**
     * Gets the dictionary used by this attribute holder.
     * @return the dictionary
     */
    @NonNull
    Dictionary getDictionary();

    /**
     * Gets the list of attributes in this holder.
     * @return list of RadiusAttributes
     */
    @NonNull
    List<RadiusAttribute> getAttributes();

    /**
     * Convenience method to get a single attribute.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     */
    @NonNull
    default Optional<RadiusAttribute> getAttribute(@NonNull String type) {
        return getAttributes(type).stream().findFirst();
    }

    /**
     * Convenience method to get a single attribute filtered by a given predicate
     *
     * @param filter RadiusAttribute filter predicate
     * @return RadiusAttribute object or null if there is no such attribute
     */
    @NonNull
    default Optional<RadiusAttribute> getAttribute(@NonNull Predicate<RadiusAttribute> filter) {
        return getAttributes(filter).stream().findFirst();
    }

    /**
     * Convenience method to get a single attribute.
     *
     * @param type attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     */
    @NonNull
    default Optional<RadiusAttribute> getAttribute(int type) {
        return getAttributes(type).stream().findFirst();
    }

    /**
     * Returns all attributes of the given type, regardless of vendorId
     *
     * @param type type of attributes to get
     * @return list of RadiusAttribute objects, or empty list
     */
    @NonNull
    default List<RadiusAttribute> getAttributes(int type) {
        return getAttributes(a -> a.getType() == type);
    }

    /**
     * Returns attributes of the given type name.
     * Also searches sub-attributes if appropriate.
     *
     * @param name attribute type name
     * @return list of RadiusAttribute objects, or empty list
     */
    @NonNull
    default List<RadiusAttribute> getAttributes(@NonNull String name) {
        return getDictionary().getAttributeTemplate(name)
                .map(this::getAttributes)
                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute type name'" + name + "'"));
    }

    /**
     * Returns attributes filtered by the given predicate
     *
     * @param filter RadiusAttribute filter predicate
     * @return list of RadiusAttribute objects, or empty list
     */
    @NonNull
    default List<RadiusAttribute> getAttributes(@NonNull Predicate<RadiusAttribute> filter) {
        return getAttributes().stream()
                .filter(filter)
                .collect(toList());
    }

    /**
     * Returns attributes of the given attribute type.
     * Also searches sub-attributes if appropriate.
     *
     * @param type attribute type name
     * @return list of RadiusAttribute objects, or empty list
     */
    @NonNull
    default List<RadiusAttribute> getAttributes(@NonNull AttributeTemplate type) {
        if (type.getVendorId() == getChildVendorId())
            return getAttributes(type.getType());

        return Collections.emptyList();
    }

    /**
     * Encodes the attributes of this Radius packet to a byte array.
     *
     * @return byte array with encoded attributes
     */
    @NonNull
    default ByteBuf getAttributeByteBuf() {
        return attributesToBytes(getAttributes());
    }

    /**
     * Returns a new attribute holder with the given attributes.
     * @param attributes the new list of attributes
     * @return a new attribute holder with the given attributes
     * @throws RadiusPacketException if the packet is invalid
     */
    @NonNull
    T withAttributes(@NonNull List<RadiusAttribute> attributes) throws RadiusPacketException;

    /**
     * Adds an attribute to this attribute container.
     *
     * @param attribute attribute to add
     * @return object of the same type with appended attribute
     * @throws IllegalArgumentException if the attribute's vendorId does not match AttributeHolder's childVendorId
     * @throws RadiusPacketException    packet validation exceptions
     */
    @NonNull
    default T addAttribute(@NonNull RadiusAttribute attribute) throws RadiusPacketException {
        if (attribute.getVendorId() != getChildVendorId())
            throw new IllegalArgumentException("Attribute vendor ID doesn't match: " +
                    "required " + getChildVendorId() + ", actual " + attribute.getVendorId());

        var attributes = new ArrayList<>(getAttributes());
        attributes.add(attribute);
        return withAttributes(attributes);
    }

    /**
     * Adds a Radius attribute.
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @return object of the same type with appended attribute
     * @throws RadiusPacketException packet validation exceptions
     */
    @NonNull
    default T addAttribute(@NonNull String name, @NonNull String value) throws RadiusPacketException {
        return addAttribute(
                getDictionary().createAttribute(name, value));
    }

    /**
     * Adds a Radius attribute.
     *
     * @param type  attribute type code
     * @param value string value to set
     * @return object of the same type with appended attribute
     * @throws RadiusPacketException packet validation exceptions
     */
    @NonNull
    default T addAttribute(int type, @NonNull String value) throws RadiusPacketException {
        return addAttribute(
                getDictionary().createAttribute(getChildVendorId(), type, (byte) 0, value));
    }

    /**
     * Removes <i>all</i> instances of the specified attribute from this attribute container.
     *
     * @param attribute attributes to remove
     * @return object of the same type with removed attribute
     * @throws RadiusPacketException packet validation exceptions
     */
    @NonNull
    default T removeAttribute(@NonNull RadiusAttribute attribute) throws RadiusPacketException {
        return withAttributes(
                getAttributes(a -> !a.equals(attribute)));
    }

    /**
     * Removes all attributes from this packet which have got the specified type.
     *
     * @param type attribute type to remove
     * @return object of the same type with removed attributes
     * @throws RadiusPacketException packet validation exceptions
     */
    @NonNull
    default T removeAttributes(int type) throws RadiusPacketException {
        return withAttributes(
                getAttributes(a -> a.getType() != type));
    }

    /**
     * Removes the last occurrence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     * @return object of the same type with removed attribute
     * @throws RadiusPacketException packet validation exceptions
     */
    @NonNull
    default T removeLastAttribute(int type) throws RadiusPacketException {
        var attributes = getAttributes(type);
        if (attributes.isEmpty())
            return withAttributes(getAttributes());

        return removeAttribute(attributes.get(attributes.size() - 1));
    }

    /**
     * @param requestAuth  request authenticator to encode attributes
     * @param sharedSecret shared secret with server/client to encode attributes
     * @return encoded version of attributes
     * @throws RadiusPacketException errors encoding attributes
     */
    @NonNull
    default List<RadiusAttribute> encodeAttributes(@NonNull byte[] requestAuth, @NonNull String sharedSecret) throws RadiusPacketException {
        var encoded = new ArrayList<RadiusAttribute>();
        for (var a : getAttributes()) {
            var encode = a.encode(requestAuth, sharedSecret);
            encoded.add(encode);
        }
        return encoded;
    }

    /**
     * @param requestAuth  request authenticator to decode attributes
     * @param sharedSecret shared secret with server/client to decode attributes
     * @return decoded/original version of attributes
     * @throws RadiusPacketException errors decoding attributes
     */
    @NonNull
    default List<RadiusAttribute> decodeAttributes(@NonNull byte[] requestAuth, @NonNull String sharedSecret) throws RadiusPacketException {
        var decoded = new ArrayList<RadiusAttribute>();
        for (var a : getAttributes()) {
            var decode = a.decode(requestAuth, sharedSecret);
            decoded.add(decode);
        }
        return decoded;
    }
}

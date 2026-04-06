package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import static org.tinyradius.core.attribute.type.RadiusAttribute.HEX_FORMAT;

public interface RadiusAttributeFactory<T extends RadiusAttribute> {

    Logger log = LogManager.getLogger();

    /**
     * Creates a new instance of the attribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param value      the attribute data
     * @return the new instance
     */
    @NonNull
    T newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value);

    /**
     * Parses the string value of the attribute into a byte array.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param type       the attribute type
     * @param value      the string value
     * @return the byte array representation
     */
    @NonNull
    byte[] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value);

    /**
     * Creates a RadiusAttribute from raw byte data.
     *
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param data       ByteBuf for the entire attribute
     * @return new attribute
     */
    @NonNull
    default T create(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        try {
            var attribute = newInstance(dictionary, vendorId, data);
            log.trace("Creating RadiusAttribute: vendorId: {}, type: {}",
                    attribute.getVendorId(), attribute.getType());
            return attribute;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create attribute with vendorId: " + vendorId +
                    ", bytes: 0x" + HEX_FORMAT.formatHex(data.copy().array()) + " - " + e.getMessage(), e);
        }
    }

    /**
     * Creates a RadiusAttribute.
     *
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param type       attribute type code
     * @param tag        RFC2868 tag byte, ignored if vendorId/type does not support tags
     * @param value      attribute value as a byte array
     * @return new attribute
     */
    @NonNull
    default T create(@NonNull Dictionary dictionary, int vendorId, int type, byte tag, @NonNull byte[] value) {
        var vendor = dictionary.getVendor(vendorId);
        int headerSize = vendor.map(Vendor::getHeaderSize).orElse(2);

        byte[] tagBytes = toTagBytes(dictionary, vendorId, type, tag);
        int length = headerSize + tagBytes.length + value.length;
        byte[] typeBytes = vendor
                .map(v -> v.toTypeBytes(type))
                .orElse(new byte[]{(byte) type});
        byte[] lengthBytes = vendor
                .map(v -> v.toLengthBytes(length))
                .orElse(new byte[]{(byte) length});

        return create(dictionary, vendorId, Unpooled.wrappedBuffer(typeBytes, lengthBytes, tagBytes, value));
    }

    /**
     * Returns the tag bytes for the given attribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param type       the attribute type
     * @param tag        the tag byte
     * @return the tag bytes, or an empty array if not tagged
     */
    @NonNull
    private static byte[] toTagBytes(@NonNull Dictionary dictionary, int vendorId, int type, byte tag) {
        return dictionary.getAttributeTemplate(vendorId, type)
                .filter(AttributeTemplate::isTagged)
                .map(x -> new byte[]{tag})
                .orElse(new byte[0]);
    }

    /**
     * Creates a RadiusAttribute.
     *
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param type       attribute type code
     * @param tag        RFC2868 tag byte, ignored if vendorId/type does not support tags
     * @param value      attribute value as string, converted to byte array based on type defined by vendorId/type code
     * @return new attribute
     */
    @NonNull
    default T create(@NonNull Dictionary dictionary, int vendorId, int type, byte tag, @NonNull String value) {
        var bytes = parse(dictionary, vendorId, type, value);
        return create(dictionary, vendorId, type, tag, bytes);
    }

    /**
     * Returns a factory for the given data type.
     *
     * @param dataType the data type name
     * @return the factory
     */
    @NonNull
    static RadiusAttributeFactory<? extends RadiusAttribute> fromDataType(@NonNull String dataType) {
        return switch (dataType) {
            case "vsa" -> VendorSpecificAttribute.FACTORY;
            case "string", "text" -> StringAttribute.FACTORY;
            case "integer", "date", "enum" -> IntegerAttribute.FACTORY;
            case "ipaddr", "ipv4addr" -> IpAttribute.V4.FACTORY;
            case "ipv6addr" -> IpAttribute.V6.FACTORY;
            case "ipv6prefix" -> Ipv6PrefixAttribute.FACTORY; // nested attributes
            // compound types
            default -> OctetsAttribute.FACTORY;
        };
    }
}

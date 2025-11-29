package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.Optional;

import static org.tinyradius.core.attribute.type.RadiusAttribute.HEX_FORMAT;

public interface RadiusAttributeFactory<T extends RadiusAttribute> {

    Logger log = LogManager.getLogger();

    T newInstance(Dictionary dictionary, int vendorId, ByteBuf value);

    byte[] parse(Dictionary dictionary, int vendorId, int type, String value);

    /**
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param data       ByteBuf for entire attribute
     * @return new attribute
     */
    default T create(Dictionary dictionary, int vendorId, ByteBuf data) {
        try {
            final T attribute = newInstance(dictionary, vendorId, data);
            log.trace("Creating RadiusAttribute: vendorId: {}, type: {}",
                    attribute.getVendorId(), attribute.getType());
            return attribute;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create attribute with vendorId: " + vendorId +
                    ", bytes: 0x" + HEX_FORMAT.formatHex(data.copy().array()) + " - " + e.getMessage(), e);
        }
    }

    /**
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param type       attribute type code
     * @param tag        RFC2868 tag byte, ignored if vendorId/type does not support tags
     * @param value      attribute value as byte array
     * @return new attribute
     */
    default T create(Dictionary dictionary, int vendorId, int type, byte tag, byte[] value) {
        final Optional<Vendor> vendor = dictionary.getVendor(vendorId);
        final int headerSize = vendor.map(Vendor::getHeaderSize).orElse(2);

        final byte[] tagBytes = toTagBytes(dictionary, vendorId, type, tag);
        final int length = headerSize + tagBytes.length + value.length;
        final byte[] typeBytes = vendor
                .map(v -> v.toTypeBytes(type))
                .orElse(new byte[]{(byte) type});
        final byte[] lengthBytes = vendor
                .map(v -> v.toLengthBytes(length))
                .orElse(new byte[]{(byte) length});

        return create(dictionary, vendorId, Unpooled.wrappedBuffer(typeBytes, lengthBytes, tagBytes, value));
    }

    private static byte[] toTagBytes(Dictionary dictionary, int vendorId, int type, byte tag) {
        return dictionary.getAttributeTemplate(vendorId, type)
                .filter(AttributeTemplate::isTagged)
                .map(x -> new byte[]{tag})
                .orElse(new byte[0]);
    }

    /**
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param type       attribute type code
     * @param tag        RFC2868 tag byte, ignored if vendorId/type does not support tags
     * @param value      attribute value as string, converted to byte array based on type defined by vendorId/type code
     * @return new attribute
     */
    default T create(Dictionary dictionary, int vendorId, int type, byte tag, String value) {
        final byte[] bytes = parse(dictionary, vendorId, type, value);
        return create(dictionary, vendorId, type, tag, bytes);
    }

    static RadiusAttributeFactory<? extends RadiusAttribute> fromDataType(String dataType) {
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

package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import java.util.Optional;

public interface RadiusAttributeFactory<U extends RadiusAttribute> {

    Logger logger = LogManager.getLogger();

    U newInstance(Dictionary dictionary, int vendorId, ByteBuf value);

    byte[] parse(Dictionary dictionary, int vendorId, int type, String value);


    /**
     * @param dictionary dictionary to set attribute to use
     * @param vendorId   -1 for top level attributes, otherwise vendorId if sub-attribute
     * @param data       ByteBuf for entire attribute
     * @return new attribute
     */
    default U create(Dictionary dictionary, int vendorId, ByteBuf data) {
        try {
            final U attribute = newInstance(dictionary, vendorId, data);
            logger.trace("Created RadiusAttribute: vendorId: {}, type: {}",
                    attribute.getVendorId(), attribute.getType());
            return attribute;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create attribute - vendorId: " + vendorId +
                    ", bytes: 0x" + DatatypeConverter.printHexBinary(data.copy().array()), e);
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
    default U create(Dictionary dictionary, int vendorId, int type, byte tag, byte[] value) {
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
    default U create(Dictionary dictionary, int vendorId, int type, byte tag, String value) {
        final byte[] bytes = parse(dictionary, vendorId, type, value);
        return create(dictionary, vendorId, type, tag, bytes);
    }

    // TODO https://datatracker.ietf.org/doc/html/rfc8044
    static RadiusAttributeFactory<? extends RadiusAttribute> fromDataType(String dataType) {
        switch (dataType) {
            case "vsa":
                return VendorSpecificAttribute.FACTORY;
            case "string":
            case "text":
                return StringAttribute.FACTORY;
            case "integer":
            case "date":
            case "enum":
                return IntegerAttribute.FACTORY;
            case "ipaddr":
            case "ipv4addr":
                return IpAttribute.V4.FACTORY;
            case "ipv6addr":
                return IpAttribute.V6.FACTORY;
            case "ipv6prefix":
                return Ipv6PrefixAttribute.FACTORY;
            case "octets":
            case "ifid":
            case "integer64":
            case "ether":
            case "abinary":
            case "byte":
            case "short":
            case "signed":
            case "tlv": // nested attributes
            case "struct": // compound types
            case "ipv4prefix":
            default:
                return OctetsAttribute.FACTORY;
        }
    }
}

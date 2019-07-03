package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

public class AttributeBuilder {
    /**
     * Creates a RadiusAttribute object of the appropriate type.
     *
     * @param dictionary    Dictionary to use
     * @param vendorId      vendor ID or -1
     * @param attributeType attribute type
     * @return RadiusAttribute object
     */
    public static RadiusAttribute createRadiusAttribute(Dictionary dictionary, int vendorId, int attributeType, byte[] data) {
        final ByteArrayConstructor byteArrayConstructor = dictionary.getAttributeTypeByCode(vendorId, attributeType).getByteArrayConstructor();
        return byteArrayConstructor.newInstance(dictionary, attributeType, vendorId, data);
    }

    public static RadiusAttribute createRadiusAttribute(Dictionary dictionary, int vendorId, int attributeType, String data) {
        final StringConstructor stringConstructor = dictionary.getAttributeTypeByCode(vendorId, attributeType).getStringConstructor();
        return stringConstructor.newInstance(dictionary, attributeType, vendorId, data);
    }

    public static RadiusAttribute parseRadiusAttribute(Dictionary dictionary, int vendorId, int attributeType, byte[] data, int offset) throws RadiusException {
        final PacketParser packetParser = dictionary.getAttributeTypeByCode(vendorId, attributeType).getPacketParser();
        return packetParser.parse(dictionary, vendorId, data, offset);
    }

    public interface ByteArrayConstructor<T extends RadiusAttribute> {
        T newInstance(Dictionary dictionary, int type, int vendorId, byte[] data);
    }

    public interface StringConstructor<T extends RadiusAttribute> {
        T newInstance(Dictionary dictionary, int type, int vendorId, String data);
    }

    public interface PacketParser<T extends RadiusAttribute> {
        T parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException;
    }
}

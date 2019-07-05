package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

public class AttributeBuilder {
    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type
     * @param data       attribute data as byte array
     * @return RadiusAttribute object
     * @throws RadiusException if unable to create attribute for given attribute vendorId/type and data
     */
    public static RadiusAttribute createRadiusAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) throws RadiusException {
        final ByteArrayConstructor byteArrayConstructor = dictionary.getAttributeTypeByCode(vendorId, type).getByteArrayConstructor();
        return byteArrayConstructor.newInstance(dictionary, vendorId, type, data);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type
     * @param data       attribute data as String
     * @return RadiusAttribute object
     * @throws RadiusException if unable to create attribute for given attribute vendorId/type and data
     */
    public static RadiusAttribute createRadiusAttribute(Dictionary dictionary, int vendorId, int type, String data) throws RadiusException {
        final StringConstructor stringConstructor = dictionary.getAttributeTypeByCode(vendorId, type).getStringConstructor();
        return stringConstructor.newInstance(dictionary, vendorId, type, data);
    }

    /**
     * @param dictionary  Dictionary to use
     * @param vendorId    vendor ID or -1
     * @param type        attribute type
     * @param sourceArray source array to read data from
     * @param offset      offset in array to start reading from
     * @return RadiusAttribute object
     * @throws RadiusException if source data invalid or unable to create attribute for given attribute vendorId/type and data
     */
    public static RadiusAttribute parseRadiusAttribute(Dictionary dictionary, int vendorId, int type, byte[] sourceArray, int offset)
            throws RadiusException {
        final PacketParser packetParser = dictionary.getAttributeTypeByCode(vendorId, type).getPacketParser();
        return packetParser.parse(dictionary, vendorId, sourceArray, offset);
    }

    public interface ByteArrayConstructor<T extends RadiusAttribute> {
        T newInstance(Dictionary dictionary, int vendorId, int type, byte[] data) throws RadiusException;
    }

    public interface StringConstructor<T extends RadiusAttribute> {
        T newInstance(Dictionary dictionary, int vendorId, int type, String data) throws RadiusException;
    }

    public interface PacketParser<T extends RadiusAttribute> {
        T parse(Dictionary dictionary, int vendorId, byte[] sourceArray, int offset) throws RadiusException;
    }
}

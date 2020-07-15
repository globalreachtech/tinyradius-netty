package org.tinyradius.core.attribute.type;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
public class OctetsAttribute implements RadiusAttribute {

    private static final Logger logger = LogManager.getLogger();

    private final Dictionary dictionary;

    private final ByteBuffer backing;

    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as byte array, excluding type and length bytes
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, int type, byte[] value) {
        if (requireNonNull(value, "Attribute data not set").length > 253)
            throw new IllegalArgumentException("Attribute data too long, max 253 octets, actual: " + value.length);

        final Optional<Vendor> vendor = dictionary.getVendor(vendorId);
        final int typeSize = vendor.map(Vendor::getTypeSize)
                .orElse(1);
        final int lengthSize = vendor.map(Vendor::getLengthSize)
                .orElse(1);
        final int tagSize = dictionary.getAttributeTemplate(vendorId, type)
                .map(AttributeTemplate::isTagged)
                .orElse(false) ? 1 : 0;

        if (typeSize != 1 && typeSize != 2 && typeSize != 4)
            throw new IllegalArgumentException("Vendor " + vendorId + " typeSize " + typeSize + " octets, only 1/2/4 allowed");
        if (lengthSize < 0 || lengthSize > 2)
            throw new IllegalArgumentException("Vendor " + vendorId + " lengthSize " + lengthSize + " octets, only 0/1/2 allowed");

        final int length = typeSize + lengthSize + tagSize + value.length;
        // todo verify length matches length field?
        // only do when we have a proper parse method and raw 'length' field
        // current we dont have 'length' field
        final byte[] typeBytes = getTypeBytes(typeSize, type);
        final byte[] lengthBytes = getLengthBytes(lengthSize, length);

        this.dictionary = requireNonNull(dictionary, "Dictionary not set");

        backing = ByteBuffer.allocate(length)
                .put(typeBytes)
                .put(lengthBytes)
                .put(new byte[tagSize]) // todo implement
                .put(value);

        this.vendorId = vendorId;
    }

    private static byte[] getTypeBytes(int typeSize, int type) {
        switch (typeSize) {
            case 2:
                return ByteBuffer.allocate(Short.BYTES).putShort((short) type).array();
            case 4:
                return ByteBuffer.allocate(Integer.BYTES).putInt(type).array();
            case 1:
            default:
                return new byte[]{(byte) type};
        }
    }

    private static byte[] getLengthBytes(int lengthSize, int len) {
        switch (lengthSize) {
            case 0:
                return new byte[0];
            case 2:
                return ByteBuffer.allocate(Short.BYTES).putShort((short) len).array();
            case 1:
            default:
                return new byte[]{(byte) len};
        }
    }

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as hex string
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, DatatypeConverter.parseHexBinary(value));
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    @Override
    public int getType() {
        switch (getTypeSize()) {
            case 2:
                return backing.getShort(0);
            case 4:
                return backing.getInt(0);
            case 1:
            default:
                return Byte.toUnsignedInt(backing.get(0));
        }
    }

//    public int getLength() {
//        switch (getLengthSize()) {
//            case 0:
//                return backing.remaining() + typeSize; // position already moved by typeSize amount
//            case 1:
//                return Byte.toUnsignedInt(data.get()); // max 255
//            case 2:
//                return data.getShort();
//            default:
//                throw new IllegalArgumentException("Vendor " + vendorId + " lengthSize " + lengthSize + " octets, only 0/1/2 allowed");
//        }
//    }

    public int getTagSize(){

    }

    @Override
    public byte getTag() {
        return 0;
    }

    @Override
    public byte[] getValue() {
        return backing.get.get(getTypeSize() + getLengthSize() + getTagBytes().length);
    }

    @Override
    public String getValueString() {
        return DatatypeConverter.printHexBinary(value);
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public String toString() {
        return getAttributeName() + ": " + getValueString();
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, requestAuth, secret) :
                this;
    }

    // do not remove - for removing from list of attributes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OctetsAttribute that = (OctetsAttribute) o;
        return type == that.type &&
                vendorId == that.vendorId &&
                Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, vendorId);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }
}

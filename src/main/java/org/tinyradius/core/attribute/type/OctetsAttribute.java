package org.tinyradius.core.attribute.type;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.Vendor;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
public class OctetsAttribute implements RadiusAttribute {

    private final Dictionary dictionary;

    private final ByteBuffer data;
    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param tag
     * @param value      value of attribute as byte array, excluding type and length bytes
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, int type, byte tag, byte[] value) {
        requireNonNull(dictionary, "Dictionary not set");

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
        final byte[] typeBytes = toTypeBytes(typeSize, type);
        final byte[] lengthBytes = toLengthBytes(lengthSize, length);
        final byte[] tagBytes = toTagBytes(dictionary, vendorId, type, tag);

        this.dictionary = requireNonNull(dictionary, "Dictionary not set");
        this.vendorId = vendorId;
        this.data = ByteBuffer.allocate(length)
                .put(typeBytes)
                .put(lengthBytes)
                .put(tagBytes)
                .put(value);
    }

    public OctetsAttribute(Dictionary dictionary, int vendorId, byte[] data) {
        this(dictionary, vendorId, ByteBuffer.wrap(data));
    }

    private OctetsAttribute(Dictionary dictionary, int vendorId, ByteBuffer data) {
        this.dictionary = dictionary;
        this.vendorId = vendorId;
        this.data = data;
    }

    private static byte[] toTypeBytes(int typeSize, int type) {
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

    private static byte[] toLengthBytes(int lengthSize, int len) {
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

    private static byte[] toTagBytes(Dictionary dictionary, int vendorId, int type, byte tag) {
        return dictionary.getAttributeTemplate(vendorId, type)
                .filter(AttributeTemplate::isTagged)
                .map(x -> new byte[]{tag})
                .orElse(new byte[0]);
    }

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as hex string
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, int type, byte tag, String value) {
        this(dictionary, vendorId, type, tag, DatatypeConverter.parseHexBinary(value));
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    @Override
    public int getType() {
        switch (getTypeSize()) {
            case 2:
                return data.getShort(0);
            case 4:
                return data.getInt(0);
            case 1:
            default:
                return Byte.toUnsignedInt(data.get(0));
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

    /**
     * @return RFC2868 Tag
     */
    @Override
    public Optional<Byte> getTag() {
        return isTagged() ?
                Optional.of(data.get(getHeaderSize())) :
                Optional.empty();
    }

    private int getHeaderSize() {
        return getTypeSize() + getLengthSize();
    }

    @Override
    public byte[] getValue() {
        final int offset = getHeaderSize() + getTagSize();
        final int length = data.capacity() - offset;
        final byte[] bytes = new byte[length];
        data.get(bytes, offset, length);
        return bytes;
    }

    @Override
    public String getValueString() {
        return DatatypeConverter.printHexBinary(getValue());
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public String toString() {
        return isTagged() ?
                "[Tagged: " + getTag() + "] " + getAttributeName() + ": " + getValueString() :
                getAttributeName() + ": " + getValueString();
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
        if (!(o instanceof OctetsAttribute)) return false;
        OctetsAttribute that = (OctetsAttribute) o;
        return getVendorId() == that.getVendorId() &&
                data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, getVendorId());
    }
}

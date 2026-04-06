package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.Dictionary;

/**
 * Represents an unparsable VSA sub-attribute.
 * <p>
 * As per RFC 2865: The String field is one or more octets. The actual format of the
 * information is site or application specific, and a robust
 * implementation SHOULD support the field as undistinguished octets.
 * <p>
 * Typically used when Vendor can't be looked up, so we can't determine
 * size of sub-attribute 'type' and 'length' fields.
 * <p>
 * If Vendor is found, but attribute isn't, we typically use OctetsAttribute instead as
 * we can still read the sub-attribute metadata.
 */
public class AnonSubAttribute implements RadiusAttribute {

    @NonNull
    private final Dictionary dictionary;

    @NonNull
    private final ByteBuf data;
    private final int vendorId; // should not be -1 (should not be top level attribute)

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ByteBuf getData() {
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVendorId() {
        return vendorId;
    }

    /**
     * Creates a new AnonSubAttribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param data       the attribute data
     */
    public AnonSubAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        this.dictionary = dictionary;
        this.vendorId = vendorId;
        this.data = data;
        if (vendorId == -1) {
            throw new IllegalArgumentException("Undistinguished sub-attribute vendorId should not be -1, actual: " + vendorId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Optional<Byte> getTag() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte @NonNull [] getValue() {
        return data.copy().array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getValueString() {
        return "[Unparsable sub-attribute (vendorId " + vendorId + ", length " + data.readableBytes() + ")]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toString() {
        return getValueString();
    }
}

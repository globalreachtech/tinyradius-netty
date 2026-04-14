package org.tinyradius.core.dictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.attribute.AttributeTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * Default implementation
 * of <code>WritableDictionary</code>. It manages a dictionary held
 * in the RAM. This class is used together with the
 * <code>DictionaryParser</code> by the class {@link org.tinyradius.core.dictionary.DefaultDictionary}
 * which is a singleton object containing the default dictionary
 * that is used if there is no dictionary explicitly specified.
 * <p>
 * A dictionary that keeps the values and names in hash maps
 * in the memory. The dictionary has to be filled using the
 * methods <code>addAttributeTemplate</code> and
 * <code>addVendor</code>.
 */
public class MemoryDictionary implements WritableDictionary {

    private static final Logger log = LogManager.getLogger(MemoryDictionary.class);
    private final Map<Integer, Vendor> vendorsByCode = new HashMap<>();
    private final Map<Integer, Map<Integer, AttributeTemplate>> attributesByCode = new HashMap<>();
    private final Map<String, AttributeTemplate> attributesByName = new HashMap<>();

    /**
     * Returns an AttributeTemplate for the given vendor code and type code.
     *
     * @param vendorCode vendor code or -1 for common attributes
     * @param type       attribute type code
     * @return the AttributeTemplate or empty Optional
     */
    @Override
    @NonNull
    public Optional<AttributeTemplate> getAttributeTemplate(int vendorCode, int type) {
        var vendorAttributes = attributesByCode.get(vendorCode);
        return Optional.ofNullable(vendorAttributes)
                .map(va -> va.get(type));
    }

    /**
     * Returns an AttributeTemplate for the given attribute name.
     *
     * @param name attribute name
     * @return the AttributeTemplate or empty Optional
     */
    @Override
    @NonNull
    public Optional<AttributeTemplate> getAttributeTemplate(@NonNull String name) {
        return Optional.ofNullable(attributesByName.get(name));
    }

    /**
     * Returns the vendor with the given name.
     *
     * @param vendorName vendor name
     * @return the vendor or empty Optional
     */
    @Override
    @NonNull
    public Optional<Vendor> getVendor(@NonNull String vendorName) {
        return vendorsByCode.values()
                .stream()
                .filter(e -> e.name().equals(vendorName))
                .findFirst();
    }

    /**
     * Returns the vendor with the given vendor ID.
     *
     * @param vendorId vendor ID
     * @return the vendor or empty Optional
     */
    @Override
    @NonNull
    public Optional<Vendor> getVendor(int vendorId) {
        return Optional.ofNullable(vendorsByCode.get(vendorId));
    }

    /**
     * Adds the given vendor to the dictionary.
     *
     * @param vendor vendor to add
     * @return this MemoryDictionary
     * @throws IllegalArgumentException if the vendor code is already in use
     */
    @Override
    @NonNull
    public MemoryDictionary addVendor(@NonNull Vendor vendor) {
        var existing = getVendor(vendor.id());
        if (existing.isPresent()) {
            if (existing.get().equals(vendor)) {
                log.info("Ignoring duplicate vendor definition: {}", vendor);
                return this;
            } else {
                throw new IllegalArgumentException("Duplicate vendor code: " + vendor.id() +
                        " (adding " + vendor + ", but already set to " + existing.get() + ")");
            }
        }

        vendorsByCode.put(vendor.id(), vendor);
        return this;
    }

    /**
     * Adds an AttributeTemplate object to the cache.
     *
     * @param attributeTemplate AttributeTemplate object
     * @return this MemoryDictionary
     * @throws IllegalArgumentException duplicate attribute name/type code
     */
    @Override
    @NonNull
    public MemoryDictionary addAttributeTemplate(@NonNull AttributeTemplate attributeTemplate) {
        var vendorId = attributeTemplate.getVendorId();
        var typeCode = attributeTemplate.getType();
        var attributeName = attributeTemplate.getName();

        if (attributesByName.containsKey(attributeName)) {
            var existing = attributesByName.get(attributeName);
            if (existing.equals(attributeTemplate)) {
                log.info("Ignoring duplicate attribute definition: {} [{},{}] {}, hasTag={}, encrypt={} ",
                        existing.getName(), existing.getVendorId(), existing.getType(), existing.getDataType(),
                        existing.isTagged(), existing.getCodecType());
                return this;
            } else {
                throw new IllegalArgumentException("Duplicate attribute definition name, " +
                        "existing attribute not equal to new attribute: " + attributeName + ", vendorId: " + vendorId);
            }
        }
        attributesByName.put(attributeName, attributeTemplate);

        var vendorAttributes = attributesByCode
                .computeIfAbsent(vendorId, k -> new HashMap<>());

        // support multiple names with same code for compatibility
        if (vendorAttributes.containsKey(typeCode))
            log.warn("Duplicate type code [{},{}], overwriting {} with {}",
                    vendorId, Integer.toUnsignedLong(typeCode), vendorAttributes.get(typeCode).getName(), attributeTemplate.getName());

        vendorAttributes.put(typeCode, attributeTemplate);
        return this;
    }
}

package org.tinyradius.core.dictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.attribute.AttributeTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.Byte.toUnsignedInt;

/**
 * A dictionary that keeps the values and names in hash maps
 * in the memory. The dictionary has to be filled using the
 * methods <code>addAttributeTemplate</code> and
 * <code>addVendor</code>.
 */
public class MemoryDictionary implements WritableDictionary {

    private static final Logger logger = LogManager.getLogger();

    private final Map<Integer, String> vendorsByCode = new HashMap<>();
    private final Map<Integer, Map<Byte, AttributeTemplate>> attributesByCode = new HashMap<>();
    private final Map<String, AttributeTemplate> attributesByName = new HashMap<>();

    @Override
    public Optional<AttributeTemplate> getAttributeTemplate(int vendorCode, byte attributeId) {
        Map<Byte, AttributeTemplate> vendorAttributes = attributesByCode.get(vendorCode);
        return Optional.ofNullable(vendorAttributes)
                .map(va -> va.get(attributeId));
    }

    @Override
    public Optional<AttributeTemplate> getAttributeTemplate(String name) {
        return Optional.ofNullable(attributesByName.get(name));
    }

    @Override
    public int getVendorId(String vendorName) {
        for (Map.Entry<Integer, String> v : vendorsByCode.entrySet()) {
            if (v.getValue().equals(vendorName))
                return v.getKey();
        }
        return -1;
    }

    @Override
    public Optional<String> getVendorName(int vendorId) {
        return Optional.ofNullable(vendorsByCode.get(vendorId));
    }

    @Override
    public void addVendor(int vendorId, String vendorName) {
        if (vendorId < 0)
            throw new IllegalArgumentException("Vendor ID must be positive: " + vendorId + " (" + vendorName + ")");
        if (vendorName == null || vendorName.isEmpty())
            throw new IllegalArgumentException("Vendor name empty: " + vendorName + " (vendorId" + vendorId + ")");

        // todo check equality, not just name match
        final Optional<String> existing = getVendorName(vendorId);
        if (existing.isPresent())
            if (existing.get().equals(vendorName)) {
                logger.info("Ignoring duplicate vendor definition: {} {}", vendorName, vendorId);
                return;
            } else {
                throw new IllegalArgumentException("Duplicate vendor code: " + vendorId +
                        " (adding " + vendorName + ", but already set to " + existing.get() + ")");
            }

        vendorsByCode.put(vendorId, vendorName);
    }

    /**
     * Adds an AttributeTemplate object to the cache.
     *
     * @param attributeTemplate AttributeTemplate object
     * @throws IllegalArgumentException duplicate attribute name/type code
     */
    @Override
    public void addAttributeTemplate(AttributeTemplate attributeTemplate) {
        if (attributeTemplate == null)
            throw new IllegalArgumentException("Attribute definition must not be null");

        final int vendorId = attributeTemplate.getVendorId();
        final byte typeCode = attributeTemplate.getType();
        final String attributeName = attributeTemplate.getName();

        if (attributesByName.containsKey(attributeName)) {
            final AttributeTemplate existing = attributesByName.get(attributeName);
            if (existing.equals(attributeTemplate)) {
                logger.info("Ignoring duplicate attribute definition: {} [{},{}] {}, hasTag={}, encrypt={} ",
                        existing.getName(), existing.getVendorId(), existing.getType(), existing.getDataType(),
                        existing.hasTag(), existing.getCodecType());
                return;
            } else {
                throw new IllegalArgumentException("Duplicate attribute definition name, " +
                        "existing attribute not equal to new attribute: " + attributeName);
            }
        }
        attributesByName.put(attributeName, attributeTemplate);

        final Map<Byte, AttributeTemplate> vendorAttributes = attributesByCode
                .computeIfAbsent(vendorId, k -> new HashMap<>());

        // support multiple names with same code for compatibility
        if (vendorAttributes.containsKey(typeCode))
            logger.warn("Duplicate type code [{},{}], overwriting {} with {}",
                    vendorId, toUnsignedInt(typeCode), vendorAttributes.get(typeCode).getName(), attributeTemplate.getName());

        vendorAttributes.put(typeCode, attributeTemplate);
    }
}

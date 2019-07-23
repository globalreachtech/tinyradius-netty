package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.util.RadiusException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

class VendorSpecificAttributeTest {

    private static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createVsa() {
        String data = "myLocationId";
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, 2, data);
        vendorSpecificAttribute.addSubAttribute(createAttribute(dictionary, 14122, 2, data));
        assertTrue(!vendorSpecificAttribute.getSubAttributes().isEmpty());
        assertEquals(data, vendorSpecificAttribute.getSubAttribute(2).getValueString());
    }

    @Test
    void getVendorSpecificType() {
        VendorSpecificAttribute vendorSpecificAttribute =
                new VendorSpecificAttribute(DefaultDictionary.INSTANCE, 1, 1, new byte[4]);
        assertEquals(26, vendorSpecificAttribute.getType());
    }

    @Test
    void getVsaSubAttributeValueStringByAttributeType() throws RadiusException {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertTrue(!vendorSpecificAttribute.getSubAttributes().isEmpty());

        RadiusAttribute subAttribute = vendorSpecificAttribute.getSubAttribute("WISPr-Location-ID");
        assertEquals("myLocationId", vendorSpecificAttribute.getSubAttributeValue(subAttribute.getAttributeType().getName()));
    }

    @Test
    void getVsaSubAttributeValueStringByName() throws RadiusException {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertTrue(!vendorSpecificAttribute.getSubAttributes().isEmpty());
        assertEquals("myLocationId", vendorSpecificAttribute.getSubAttribute("WISPr-Location-ID").getValueString());
    }

    @Test
    void addNonVsaSubAttribute() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addSubAttribute("User-Password", "password"));
        assertTrue(exception.getMessage().toLowerCase().contains("is not a vendor-specific sub-attribute"));
    }

    @Test
    void addEmptySubAttribute() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addSubAttribute("", "myLocationId"));
        assertTrue(exception.getMessage().toLowerCase().contains("type name is empty"));
    }

    @Test
    void vsaToByteArray() throws RadiusException {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        RadiusAttribute radiusAttribute = new RadiusAttribute(dictionary, 14122, 26, new byte[8]);
        vendorSpecificAttribute.addSubAttribute(radiusAttribute);
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getSubAttributes().size());

        byte[] bytes = vendorSpecificAttribute.toByteArray();
        assertEquals(bytes.length, bytes[1]);
    }

    @Test
    void vsaToByteArrayTooLong() throws RadiusException {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        RadiusAttribute radiusAttribute = new RadiusAttribute(dictionary, 14122, 26, new byte[253]);
        vendorSpecificAttribute.addSubAttribute(radiusAttribute);
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getSubAttributes().size());

        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("vendor-specific attribute too long"));
    }
}
package org.tinyradius.attribute;

import net.jradius.packet.attribute.SubAttribute;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.util.RadiusException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class VendorSpecificAttributeTest {

    private static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void toByteArray() {

        // normal Attributes, we can check in constructor we aren't passing in too big a byte array
        // VSA we manipulate sub-attributes, and only get array when we call toByteArray()
        // todo test size isn't too big
    }

    @Test
    void getVendorSpecificType() {
        VendorSpecificAttribute vendorSpecificAttribute =
                new VendorSpecificAttribute(DefaultDictionary.INSTANCE, 1, 1, new byte[4]);
        assertEquals(26, vendorSpecificAttribute.getType());
    }

    @Test
    void getVsaSubAttributeByType() {
        final VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        final VendorSpecificAttribute subAttribute = new VendorSpecificAttribute(dictionary, 14122, 12, "Wisp");
        assertEquals(26, subAttribute.getType());
        vendorSpecificAttribute.addSubAttribute(subAttribute);
        assertTrue(!vendorSpecificAttribute.getSubAttributes().isEmpty());
        assertEquals(subAttribute, vendorSpecificAttribute.getSubAttribute(26));
    }

    @Test
    void getVsaSubAttributeValueStringByAttributeType() throws RadiusException {
        final VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertTrue(!vendorSpecificAttribute.getSubAttributes().isEmpty());
        RadiusAttribute subAttribute = vendorSpecificAttribute.getSubAttribute("WISPr-Location-ID");
        String subAttributeValue = vendorSpecificAttribute.getSubAttributeValue(subAttribute.getAttributeType().getName());
        assertEquals("myLocationId", subAttributeValue);
    }

    @Test
    void getVsaSubAttributeValueStringByName() throws RadiusException {
        final VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertTrue(!vendorSpecificAttribute.getSubAttributes().isEmpty());
        RadiusAttribute subAttribute = vendorSpecificAttribute.getSubAttribute("WISPr-Location-ID");
        assertEquals("myLocationId", subAttribute.getValueString());
    }

    @Test
    void addNonVsaSubAttribute() {
        final VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addSubAttribute("User-Password", "password"));
        assertTrue(exception.getMessage().toLowerCase().contains("is not a vendor-specific sub-attribute"));
    }

    @Test
    void addEmptySubAttribute() {
        final VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addSubAttribute("", "myLocationId"));
        assertTrue(exception.getMessage().toLowerCase().contains("type name is empty"));
    }

    @Test
    void vsaTooByteArrayTooLong() throws RadiusException {
        final VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        RadiusAttribute radiusAttribute = new RadiusAttribute(dictionary, 14122, 26, new byte[253]);
        vendorSpecificAttribute.addSubAttribute(radiusAttribute);
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getSubAttributes().size());

        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("vendor-specific attribute too long"));
    }
}
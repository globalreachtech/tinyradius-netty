package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.*;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.util.RadiusException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

class RadiusPacketTest {

    @Test
    void doesNotMutateOriginalAttributeList() {
        final List<RadiusAttribute> attributes = Collections.emptyList();
        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1, null, attributes);
        rp.addAttribute("WISPr-Location-ID", "myLocationId");

        assertEquals(0, attributes.size());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void addAttribute() {

        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1, null, Collections.emptyList());
        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        rp.addAttribute(createAttribute(rp.getDictionary(), -1, 8, "192.168.0.1"));
        rp.addAttribute(createAttribute(rp.getDictionary(), -1, 168, "fe80::"));
        rp.addAttribute(createAttribute(rp.getDictionary(), -1, 97, "fe80::/64"));
        rp.addAttribute(createAttribute(rp.getDictionary(), -1, 97, "fe80::/128"));

        final List<VendorSpecificAttribute> vendorAttributes = rp.getVendorAttributes(14122);
        assertEquals(1, vendorAttributes.size());

        final List<RadiusAttribute> wisprLocations = vendorAttributes.get(0).getSubAttributes();
        assertEquals(1, wisprLocations.size());
        assertEquals("myLocationId", wisprLocations.get(0).getDataString());

        assertEquals("myLocationId", rp.getAttribute(14122, 1).getDataString());
        final List<RadiusAttribute> wisprLocations2 = rp.getAttributes(14122, 1);
        assertEquals(1, wisprLocations2.size());
        assertEquals("myLocationId", wisprLocations2.get(0).getDataString());

        assertEquals("0.18.214.135", rp.getAttribute(8).getDataString());
        assertEquals("0.18.214.135", rp.getAttribute("Framed-IP-Address").getDataString());
        assertEquals("fe80:0:0:0:0:0:0:0", rp.getAttribute(168).getDataString());
        assertEquals("fe80:0:0:0:0:0:0:0", rp.getAttribute("Framed-IPv6-Address").getDataString());

        final List<RadiusAttribute> ipV6Attributes = rp.getAttributes(97);
        assertArrayEquals(new String[]{"fe80:0:0:0:0:0:0:0/64", "fe80:0:0:0:0:0:0:0/128"},
                ipV6Attributes.stream().map(RadiusAttribute::getDataString).toArray());
    }

    @Test
    void removeSpecificAttribute() {
        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1);
        RadiusAttribute ra = createAttribute(rp.getDictionary(), -1, 8, new byte[16]);
        rp.addAttribute(ra);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());

        rp.removeAttribute(ra);
        assertEquals(0, rp.getAttributes().size());
    }

    @Test
    void removeSpecificVendorAttributes() {
        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1);
        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        assertFalse(rp.getAttributes().isEmpty());

        rp.removeAttributes(14122, 1);
        assertTrue(rp.getAttributes().isEmpty());

        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        RadiusAttribute ra = rp.getAttribute(14122, 1);

        rp.removeAttribute(ra);
        assertEquals(0, rp.getAttributes().size());
    }

    @Test
    void removeAttributesByType() {
        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1);
        rp.addAttribute("Service-Type", "1");
        rp.addAttribute("Service-Type", "2");
        rp.addAttribute("User-Name", "user");
        assertEquals(3, rp.getAttributes().size());

        rp.removeAttributes(6);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void removeLastAttributeForType() {
        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1);
        rp.addAttribute("Service-Type", "1");
        rp.addAttribute("Service-Type", "2");
        rp.addAttribute("User-Name", "user");
        assertEquals(3, rp.getAttributes().size());

        rp.removeLastAttribute(6);
        RadiusAttribute attribute = rp.getAttribute(6);

        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(2, rp.getAttributes().size());
        assertEquals("Login-User", attribute.getDataString());
    }

}
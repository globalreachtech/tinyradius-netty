package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.util.Attributes.create;

class BaseRadiusPacketTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void doesNotMutateOriginalAttributeList() {
        final List<RadiusAttribute> attributes = Collections.emptyList();
        BaseRadiusPacket rp = new StubPacket();
        rp.addAttribute("WISPr-Location-ID", "myLocationId");

        assertEquals(0, attributes.size());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void addAttribute() {
        BaseRadiusPacket packet = new StubPacket();
        packet.addAttribute("WISPr-Location-ID", "myLocationId");
        packet.addAttribute(Attributes.create(packet.getDictionary(), -1, (byte) 8, "1234567"));
        packet.addAttribute(Attributes.create(packet.getDictionary(), -1, (byte) 168, "fe80::"));
        packet.addAttribute(Attributes.create(packet.getDictionary(), -1, (byte) 97, "fe80::/64"));
        packet.addAttribute(Attributes.create(packet.getDictionary(), -1, (byte) 97, "fe80::/128"));

        final List<VendorSpecificAttribute> vendorAttributes = packet.getVendorAttributes(14122);
        assertEquals(1, vendorAttributes.size());

        final List<RadiusAttribute> wisprLocations = vendorAttributes.get(0).getAttributes();
        assertEquals(1, wisprLocations.size());
        assertEquals("myLocationId", wisprLocations.get(0).getValueString());

        assertEquals("myLocationId", packet.getAttribute(14122, (byte) 1).getValueString());
        final List<RadiusAttribute> wisprLocations2 = packet.getAttributes(14122, (byte) 1);
        assertEquals(1, wisprLocations2.size());
        assertEquals("myLocationId", wisprLocations2.get(0).getValueString());

        assertEquals("0.18.214.135", packet.getAttribute((byte) 8).getValueString());
        assertEquals("0.18.214.135", packet.getAttribute("Framed-IP-Address").getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute((byte) 168).getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute("Framed-IPv6-Address").getValueString());

        final List<RadiusAttribute> ipV6Attributes = packet.getAttributes((byte) 97);
        assertArrayEquals(new String[]{"fe80:0:0:0:0:0:0:0/64", "fe80:0:0:0:0:0:0:0/128"},
                ipV6Attributes.stream().map(RadiusAttribute::getValueString).toArray());

        assertEquals("Access-Request, ID 1\n" +
                "Vendor-Specific: WISPr (14122)\n" +
                "  WISPr-Location-ID: myLocationId\n" +
                "Framed-IP-Address: 0.18.214.135\n" +
                "Framed-IPv6-Address: fe80:0:0:0:0:0:0:0\n" +
                "Framed-IPv6-Prefix: fe80:0:0:0:0:0:0:0/64\n" +
                "Framed-IPv6-Prefix: fe80:0:0:0:0:0:0:0/128", packet.toString());
    }

    @Test
    void removeSpecificAttribute() {
        BaseRadiusPacket rp = new StubPacket();
        RadiusAttribute ra = create(rp.getDictionary(), -1, (byte) 8, new byte[4]);
        rp.addAttribute(ra);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());

        rp.removeAttribute(ra);
        assertEquals(0, rp.getAttributes().size());
    }

    @Test
    void removeSpecificVendorAttributes() {
        BaseRadiusPacket rp = new StubPacket();
        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(1, rp.getAttributes().size());

        rp.removeAttributes(14122, (byte) 1);
        assertTrue(rp.getAttributes().isEmpty());

        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        RadiusAttribute ra = rp.getAttribute(14122, (byte) 1);

        rp.removeAttribute(ra);
        assertEquals(0, rp.getAttributes().size());
    }

    @Test
    void removeAttributesByType() {
        BaseRadiusPacket rp = new StubPacket();
        rp.addAttribute("Service-Type", "1");
        rp.addAttribute("Service-Type", "2");
        rp.addAttribute("User-Name", "user");
        assertEquals(3, rp.getAttributes().size());

        rp.removeAttributes((byte) 6);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void removeLastAttributeForType() {
        BaseRadiusPacket rp = new StubPacket();
        rp.addAttribute("Service-Type", "1");
        rp.addAttribute("Service-Type", "2");
        rp.addAttribute("User-Name", "user");
        assertEquals(3, rp.getAttributes().size());

        rp.removeLastAttribute((byte) 6);
        RadiusAttribute attribute = rp.getAttribute((byte) 6);

        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(2, rp.getAttributes().size());
        assertEquals("Login-User", attribute.getValueString());
    }

    @Test
    void testFlattenAttributes() {
        BaseRadiusPacket radiusPacket = new StubPacket();

        radiusPacket.addAttribute("Service-Type", "999");
        radiusPacket.addAttribute("Filter-Id", "abc");
        radiusPacket.addAttribute("Reply-Message", "foobar");

        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute("WISPr-Logoff-URL", "111");
        builder.addAttribute("WISPr-Logoff-URL", "222");
        radiusPacket.addAttribute(builder.build());

        final List<RadiusAttribute> attributes = radiusPacket.getFlattenedAttributes();

        assertEquals("Service-Type: 999", attributes.get(0).toString());
        assertEquals("Filter-Id: abc", attributes.get(1).toString());
        assertEquals("Reply-Message: foobar", attributes.get(2).toString());
        assertEquals("WISPr-Logoff-URL: 111", attributes.get(3).toString());
        assertEquals("WISPr-Logoff-URL: 222", attributes.get(4).toString());
        // getAttributes only gets the last subAttribute of VendorSpecificAttribute

        assertEquals(5, attributes.size());
    }

    private static class StubPacket extends BaseRadiusPacket {

        public StubPacket() {
            super(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        }
    }
}
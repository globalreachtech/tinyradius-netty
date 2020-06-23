package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseRadiusPacketTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void doesNotMutateOriginalAttributeList() {
        final List<RadiusAttribute> attributes = Collections.emptyList();
        StubPacket rp = new StubPacket()
                .addAttribute("WISPr-Location-ID", "myLocationId");

        assertEquals(0, attributes.size());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void addAttribute() {
        StubPacket packet = new StubPacket()
                .addAttribute("WISPr-Location-ID", "myLocationId")
                .addAttribute((byte) 8, "1234567")
                .addAttribute((byte) 168, "fe80::")
                .addAttribute((byte) 97, "fe80::/64")
                .addAttribute((byte) 97, "fe80::/128");

        final List<VendorSpecificAttribute> vendorAttributes = packet.getVendorAttributes(14122);
        assertEquals(1, vendorAttributes.size());

        final List<RadiusAttribute> wisprLocations = vendorAttributes.get(0).getAttributes();
        assertEquals(1, wisprLocations.size());
        assertEquals("myLocationId", wisprLocations.get(0).getValueString());

        assertEquals("myLocationId", packet.getAttribute(14122, (byte) 1).get().getValueString());
        final List<RadiusAttribute> wisprLocations2 = packet.filterAttributes(14122, (byte) 1);
        assertEquals(1, wisprLocations2.size());
        assertEquals("myLocationId", wisprLocations2.get(0).getValueString());

        assertEquals("0.18.214.135", packet.getAttribute((byte) 8).get().getValueString());
        assertEquals("0.18.214.135", packet.getAttribute("Framed-IP-Address").get().getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute((byte) 168).get().getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute("Framed-IPv6-Address").get().getValueString());

        final List<RadiusAttribute> ipV6Attributes = packet.filterAttributes((byte) 97);
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
        RadiusAttribute ra = dictionary.createAttribute(-1, (byte) 8, new byte[4]);
        StubPacket rp = new StubPacket()
                .addAttribute(ra);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());

        rp = rp.removeAttribute(ra);
        assertEquals(0, rp.getAttributes().size());
    }

    @Test
    void removeSpecificVendorAttributes() {
        final StubPacket rp1 = new StubPacket()
                .addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(1, rp1.getAttributes().size());
        assertTrue(rp1.getAttributes().get(0) instanceof VendorSpecificAttribute);

        final StubPacket rp2 = rp1.removeAttributes(14122, (byte) 1);
        assertTrue(rp2.getAttributes().isEmpty());

        final StubPacket rp3 = rp2.addAttribute("WISPr-Location-ID", "myLocationId");
        RadiusAttribute ra = rp3.getAttribute(14122, (byte) 1).get();

        final StubPacket rp4 = rp3.removeAttribute(ra);
        assertEquals(0, rp4.getAttributes().size());
    }

    @Test
    void removeAttributesByType() {
        StubPacket rp = new StubPacket()
                .addAttribute("Service-Type", "1")
                .addAttribute("Service-Type", "2")
                .addAttribute("User-Name", "user");
        assertEquals(3, rp.getAttributes().size());

        rp = rp.removeAttributes((byte) 6);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void removeLastAttributeForType() {
        StubPacket rp = new StubPacket()
                .addAttribute("Service-Type", "1")
                .addAttribute("Service-Type", "2")
                .addAttribute("User-Name", "user");

        // remove once
        final StubPacket rp2 = rp.removeLastAttribute((byte) 6);

        List<RadiusAttribute> rp2Attributes = rp2.filterAttributes((byte) 6);
        assertEquals(1, rp2Attributes.size());
        assertEquals("Login-User", rp2Attributes.get(0).getValueString());

        // remove again
        final StubPacket rp3 = rp2.removeLastAttribute((byte) 6);

        List<RadiusAttribute> rp3Attributes = rp3.filterAttributes((byte) 6);
        assertEquals(0, rp3Attributes.size());

        // last remove should do nothing
        final StubPacket rp4 = rp3.removeLastAttribute((byte) 6);

        List<RadiusAttribute> rp4Attributes = rp4.filterAttributes((byte) 6);
        assertEquals(0, rp4Attributes.size());

        assertEquals(3, rp.getAttributes().size());
        assertEquals(2, rp2.getAttributes().size());
        assertEquals(1, rp3.getAttributes().size());
        assertEquals(1, rp4.getAttributes().size());
    }

    @Test
    void testFlattenAttributes() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122,
                Arrays.asList(
                        dictionary.createAttribute("WISPr-Logoff-URL", "111"),
                        dictionary.createAttribute("WISPr-Logoff-URL", "222")
                ));

        StubPacket radiusPacket = new StubPacket()
                .addAttribute("Service-Type", "999")
                .addAttribute("Filter-Id", "abc")
                .addAttribute("Reply-Message", "foobar")
                .addAttribute(vsa);

        final List<RadiusAttribute> attributes = radiusPacket.getFlattenedAttributes();

        assertEquals("Service-Type: 999", attributes.get(0).toString());
        assertEquals("Filter-Id: abc", attributes.get(1).toString());
        assertEquals("Reply-Message: foobar", attributes.get(2).toString());
        assertEquals("WISPr-Logoff-URL: 111", attributes.get(3).toString());
        assertEquals("WISPr-Logoff-URL: 222", attributes.get(4).toString());
        // getAttributes only gets the last subAttribute of VendorSpecificAttribute

        assertEquals(5, attributes.size());
    }

    private static class StubPacket extends BaseRadiusPacket<StubPacket> {

        public StubPacket() {
            this(Collections.emptyList());
        }

        public StubPacket(List<RadiusAttribute> attributes) {
            super(dictionary, (byte) 1, (byte) 1, null, attributes);
        }

        @Override
        public StubPacket withAttributes(List<RadiusAttribute> attributes) {
            return new StubPacket(attributes);
        }
    }
}
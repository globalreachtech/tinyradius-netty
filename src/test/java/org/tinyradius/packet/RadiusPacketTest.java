package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

class RadiusPacketTest {

    @Test
    void addAttribute() {

        RadiusPacket rp = new RadiusPacket(ACCESS_REQUEST, 1);
        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        rp.addAttribute(new IpAttribute(8, 1234567));
        rp.addAttribute(new Ipv6Attribute(168, "fe80::"));
        rp.addAttribute(new Ipv6PrefixAttribute(97, "fe80::/64"));
        rp.addAttribute(new Ipv6PrefixAttribute(97, "fe80::/128"));

        final List<VendorSpecificAttribute> vendorAttributes = rp.getVendorAttributes(14122);
        assertEquals(1, vendorAttributes.size());

        final List<RadiusAttribute> wisprLocations = vendorAttributes.get(0).getSubAttributes();
        assertEquals(1, wisprLocations.size());
        assertEquals("myLocationId", wisprLocations.get(0).getAttributeValue());

        assertEquals("myLocationId", rp.getAttribute(14122, 1).getAttributeValue());
        final List<RadiusAttribute> wisprLocations2 = rp.getAttributes(14122, 1);
        assertEquals(1, wisprLocations2.size());
        assertEquals("myLocationId", wisprLocations2.get(0).getAttributeValue());

        assertEquals("0.18.214.135", rp.getAttribute(8).getAttributeValue());
        assertEquals("0.18.214.135", rp.getAttribute("Framed-IP-Address").getAttributeValue());
        assertEquals("fe80:0:0:0:0:0:0:0", rp.getAttribute(168).getAttributeValue());
        assertEquals("fe80:0:0:0:0:0:0:0", rp.getAttribute("Framed-IPv6-Address").getAttributeValue());

        System.out.println(rp);
        final List<RadiusAttribute> ipV6Attributes = rp.getAttributes(97);
        assertArrayEquals(new String[]{"fe80:0:0:0:0:0:0:0/64", "fe80:0:0:0:0:0:0:0/128"},
                ipV6Attributes.stream().map(RadiusAttribute::getAttributeValue).toArray());
    }
}
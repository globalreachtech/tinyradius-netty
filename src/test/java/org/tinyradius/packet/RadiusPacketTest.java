package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.*;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.util.RadiusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

class RadiusPacketTest {

    @Test
    void addAttribute() throws RadiusException {

        RadiusPacket rp = new RadiusPacket(DefaultDictionary.INSTANCE, ACCESS_REQUEST, 1);
        rp.addAttribute("WISPr-Location-ID", "myLocationId");
        rp.addAttribute(new IpAttribute(rp.getDictionary(), -1, 8, 1234567));
        rp.addAttribute(new Ipv6Attribute(rp.getDictionary(), -1, 168, "fe80::"));
        rp.addAttribute(new Ipv6PrefixAttribute(rp.getDictionary(), -1, 97, "fe80::/64"));
        rp.addAttribute(new Ipv6PrefixAttribute(rp.getDictionary(), -1, 97, "fe80::/128"));

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
}
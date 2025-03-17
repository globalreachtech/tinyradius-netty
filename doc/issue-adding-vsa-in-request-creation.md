# Issue Adding VSA in request/response constructor list

# Summary

When including a vendor specific attribute in the RadiusAttribute list as the last parameter in the constructor for RadiusRequest or RadiusResponse, that attribute is not added correctly and is sent in the wire as a non-vsa with the same code (e.g a VSA with vendorId 9 and type 1 is sent as a User-Name attribute).

For instance, here is a dump of a packet created as described, showing the logs at the client side (before being sent) and at the server side. Notice the `Cisco-AVPair` in the request, and the duplicated `User-Name` as received in the server side logs

```
# Client side logs
2025-03-12T21:43:20.292+01:00 DEBUG 68659 --- [ntLoopGroup-2-1] o.t.i.c.handler.ClientDatagramCodec      : Sending packet to /127.0.0.1:1812 - Access-Request, ID 1, len 153, attributes={
User-Name=test@test.com
[Encoded: RFC2865_USER_PASSWORD] User-Password=0xD7F3AF5F78AD1DD974A59921AFCC0A2DB16E50B589F50FA4492F43CF7C011A32DDADC80A6A3F6AD97D7A3908DF04EF8F
Cisco-AVPair=name=value
Proxy-State=0x34346466633961352D356362352D343236382D393732612D363965393462646435323566
Message-Authenticator=0x51FEFA1096797A6FD3AFA74AE253F593
}

# Server side logs
2025-03-12T21:43:20.300+01:00 DEBUG 68659 --- [ntLoopGroup-6-1] o.t.io.server.handler.ServerPacketCodec  : Received request from localhost/127.0.0.1:1900 - Access-Request, ID 1, len 153, attributes={
User-Name=test@test.com
[Encoded: RFC2865_USER_PASSWORD] User-Password=0xD7F3AF5F78AD1DD974A59921AFCC0A2DB16E50B589F50FA4492F43CF7C011A32DDADC80A6A3F6AD97D7A3908DF04EF8F
User-Name=name=value
Proxy-State=0x34346466633961352D356362352D343236382D393732612D363965393462646435323566
Message-Authenticator=0x51FEFA1096797A6FD3AFA74AE253F593
```
}

# Analysis

When adding attributes to the RadiusPacket, they are wrapped in VendorSpecificAttributes, as shown below, in the `addAttribute` method of `NestedAttributeHolder`

```java
    default T addAttribute(RadiusAttribute attribute) throws RadiusPacketException {
        final RadiusAttribute toAdd = attribute.getVendorId() == getChildVendorId() ?
                attribute :
                new VendorSpecificAttribute(getDictionary(), attribute.getVendorId(), Collections.singletonList(attribute));

        return AttributeHolder.super.addAttribute(toAdd);
    }
```

But the same is not done when creating a request and passing a list of attributes, in `RadiusRequest`

```java
    static RadiusRequest create(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf header = RadiusPacket.buildHeader(type, id, authenticator, attributes);
        return create(dictionary, header, attributes);
    }
```

The following test case demonstrates the issue (for example, in `VendorSpecificAttriuteTest`).

```java
    @Test
    void addToRequest() throws RadiusPacketException{

        // Create vendor specific attribute
        final String vendorAttrName = "WISPr-Location-ID";
        RadiusAttribute vsa = dictionary.createAttribute(vendorAttrName, "anything");

        // This Works --- Create request with empty attribute list and add vsa later
        RadiusRequest request1 = ((AccessRequest) RadiusRequest.create(dictionary, (byte)1, (byte) 1, null, List.of()));
        request1 = request1.addAttribute(vsa);
        // Check (ok). Retrieve the just inserted attribute and check name
        assertEquals(vendorAttrName, request1.getAttribute(vendorAttrName).get().getAttributeName());

        // This fails --- Create request with vsa in attribute list at creation time
        RadiusRequest request2 = ((AccessRequest) RadiusRequest.create(dictionary, (byte)1, (byte) 1, null, List.of(vsa)));
        // Check (fail). Try to retrieve the inserted attribute and verify its name. get() 
        assertEquals(vendorAttrName, request2.getAttribute(vendorAttrName).get().getAttributeName());
    }
```

# Fix

Can be done by using the same code as in `NestedAttributeHolder.addAttribute`

```java
    static RadiusRequest create(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) throws RadiusPacketException {
        // Wrap vendor specific attributes. Same as in NestedAttributesHolder
        List<RadiusAttribute> wrappedAttributes = attributes.stream().map(attr -> 
            attr.getVendorId() == -1 ? attr : new VendorSpecificAttribute(dictionary, attr.getVendorId(), Collections.singletonList(attr))
            ).collect(Collectors.toList());
        final ByteBuf header = RadiusPacket.buildHeader(type, id, authenticator, wrappedAttributes);
        return create(dictionary, header, wrappedAttributes);
    }
```

And the same for the `RadiusResponse`.




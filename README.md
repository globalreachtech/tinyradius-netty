# tinyradius-netty

[![CircleCI](https://circleci.com/gh/globalreachtech/tinyradius-netty.svg?style=svg)](https://circleci.com/gh/globalreachtech/tinyradius-netty)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/globalreachtech/tinyradius-netty.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/globalreachtech/tinyradius-netty/context:java)
[![Maintainability](https://api.codeclimate.com/v1/badges/a6b90f85717d753228eb/maintainability)](https://codeclimate.com/github/globalreachtech/tinyradius-netty/maintainability)
[![Coverage Status](https://coveralls.io/repos/github/globalreachtech/tinyradius-netty/badge.svg)](https://coveralls.io/github/globalreachtech/tinyradius-netty)
[![Download](https://api.bintray.com/packages/globalreachtech/grt-maven/tinyradius-netty/images/download.svg)](https://bintray.com/globalreachtech/grt-maven/tinyradius-netty)

tinyradius-netty is a fork of the TinyRadius Radius library, with some significant changes:
- Use netty for asynchronous IO, timeouts, thread management
- Most methods return Promises
- Use slf4j instead of commons-logging
- Use Generics and Java 8 language features
- Proxy uses Client to handle requests upstream, retries, and connection management
- More immutability
- More tests

## Documentation

[Javadocs](https://globalreachtech.github.io/tinyradius-netty/)

### Dictionary
 - Parses dictionary files in same format as [FreeRadius dictionaries](https://github.com/FreeRADIUS/freeradius-server/tree/master/share/dictionary).
 - `DefaultDictionary` uses a very limited subset that's included in the classpath.
   - Singleton is available at `DefaultDictionary.INSTANCE`
 - `DictionaryParser` parses custom resources and takes a `ResourceResolver` parameter.
   - `FileResourceResolver` and `ClasspathResourceResolver` resolves resources on file system and classpath respectively. Dictionaries can include other files to parse, and paths are resolved differently in each case.
 - Results of dictionary parses are stored as `AttributeType`.

### Attribute
 - `RadiusAttribute` is used for octets attributes and attributes without a dictionary entry or specific subtype.
   - Attribute subtypes such as `IntegerAttribute` store the same data, but have convenience methods for maniupulation and stricter data validation.
 - `AttributeType` contains the attribute type, name, data type, and enumeration of valid values if appropriate.
   - A `create()` method is used to create attributes directly for that type.
   - If only the attribute type ID is known, rather than the AttributeType object, use `Attributes.createAttribute()` which will create a basic `RadiusAttribute`. This is safer as it will always successfully create an attribute even if there's no dictionary entry for the type ID.
 - `VendorSpecificAttribute` stores lists of vendor-specific attributes instead of attribute data itself, and serializes its data by concatenating byte array representations of subattributes with its type/vendorId/length header.

### Packet
 - `RadiusPacket` represents packet and associated data.
   - Subtype packets are included with convenience methods.
   - `encodeRequest()` and `encodeResponse()` return a new copy of the current RadiusPacket with the Authenticator.
   - `AccessRequest` is also included which has a different way of encoding/verifying the packet Authenticator compared to other Radius request/response types and encodes the appropriate password attributes.
 - `RadiusPackets` contains utils for creating packets and using a more specific subclass where possible, and for generating a valid packet identifier.
 - `PacketType` contains list of constants of packet types. It's intentionally not an enum to allow for types that aren't included in the list / added in the future.
 - `PacketEncoder` contains methods for converting to/from netty Datagram and ByteBufs.

### Client
 - `RadiusClient` manages setting up sockets and netty, and the plumbing for the `communicate()` method to return a `Future<RadiusPacket>`
 - `RetryStrategy` contains a method which is called after every request is sent, with a Runnable to retry the request. The retry runnable is then scheduled or timeout triggered depending on the implementation.
 - `ClientHandler` is a thin wrapper around Netty SimpleChannelInboundHandler with two methods to be implemented.
   - `prepareDatagram()` takes a RadiusPacket and returns the Datagram that will be sent.
     - The same datagram contents are reused for retries. If retries should be different, considering making multiple calls to `RadiusClient#communicate()` and implementing retries manually.
     - A Promise is passed into the handler - this represents an open request, awaiting a response. The Promise should be stored so it can be looked up and completed when a valid response is received. It should also be removed after a timeout to avoid memory leaks. 
   - `handleResponse()` takes a Datagram as input.
     - If a corresponding request is found, the open promise for that request should be completed and removed from the store.
   - `ProxyStateClientHandler` appends a `Proxy-State` attribute to the packet and uses that to lookup requests.
   - `SimpleClientHandler` uses the remote socket and packet identifier to lookup requests.

### Server
  - `RadiusServer` sets up netty listeners and sockets.
  - `HandlerAdapter` is a wrapper around Netty SimpleChannelInboundHandler that converts Datagram to RadiusPacket and performs low level validation.
    - It calls a RequestHandler for business logic processing, and if the handler returns a packet, the Adapter replies with that as a response.
    - A `Class<T extends RadiusPacket>` parameter can be used to limit what subclasses of RadiusPacket this can handle, otherwise ignore the packet.
  - `RequestHandler` handles RadiusPackets. It's a generic interface, so can be used to handle only particular subtypes of RadiusPackets together with the HandlerAdapter parameter. Also extends `Lifecycle`, so implementations can have start/stop methods.
    - `AcctHandler` and `AuthHandler` are example implementations for handling Accounting-Request and Access-Requests respectively - they can be extended with more business logic.
    - `DeduplicatorHandler` also uses RequestHandler interface, but wraps around another Handler and doesn't return anything if a duplicate request is received within specified time period.
    - `ProxyRequestHandler` handles incoming requests, but instead of processing directly or delegating, proxies the request using an instance of RadiusClient. This is where the main proxying processing is done. 

## License
Copyright Matthias Wuttke (mw@teuto.net) and contributors.

Source code from
- http://tinyradius.sourceforge.net/
- https://github.com/ctran/TinyRadius

Licensed under LGPL 2.1
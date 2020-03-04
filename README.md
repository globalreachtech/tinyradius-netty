# tinyradius-netty

[![CircleCI](https://circleci.com/gh/globalreachtech/tinyradius-netty.svg?style=shield)](https://circleci.com/gh/globalreachtech/tinyradius-netty)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/globalreachtech/tinyradius-netty.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/globalreachtech/tinyradius-netty/context:java)
[![Maintainability](https://api.codeclimate.com/v1/badges/a6b90f85717d753228eb/maintainability)](https://codeclimate.com/github/globalreachtech/tinyradius-netty/maintainability)
[![Coverage Status](https://coveralls.io/repos/github/globalreachtech/tinyradius-netty/badge.svg)](https://coveralls.io/github/globalreachtech/tinyradius-netty)
[![Download](https://api.bintray.com/packages/globalreachtech/grt-maven/tinyradius-netty/images/download.svg)](https://bintray.com/globalreachtech/grt-maven/tinyradius-netty)
[![javadoc](https://javadoc.io/badge2/com.globalreachtech/tinyradius-netty/javadoc.svg)](https://javadoc.io/doc/com.globalreachtech/tinyradius-netty)

tinyradius-netty is a fork of the TinyRadius Radius library, with some significant changes:
- Use netty for asynchronous IO, timeouts, thread management
- Most methods return Promises or follow Netty's interceptor filter pattern
- Use log4j2 instead of commons-logging
- Use Generics and Java 8
- Proxy implementation directly uses Client to handle requests upstream, retries, and connection management
- More immutability
- More tests

### Dictionary
 - `DefaultDictionary` uses a very limited subset that's included in the classpath.
   - Singleton is available at `DefaultDictionary.INSTANCE`
 - `DictionaryParser` parses custom resources and takes a `ResourceResolver` parameter.
   - Factory methods `newFileParser()` and `newClasspathParser()` resolves resources on file system and classpath respectively. Dictionaries can include other files to parse, and paths are resolved differently in each case.
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
 - `TimeoutHandler` contains a method which is called after every request is sent, with a Runnable to retry. The retry runnable is then scheduled or timeout triggered depending on the implementation.
 - `PromiseAdapter` is a ChannelHandler that requires a promise to be passed in with the outbound request in a `PendingRequestCtx` wrapper.
   - The Promise passed into the handler is incomplete. The Promise is completed and removed from memory when a valid response is received or timeouts.
   - Appends a `Proxy-State` attribute to the packet and uses that to match request/responses.

### Server
  - `RadiusServer` sets up netty listeners and sockets.
  - Packets should go through `ServerPacketCodec` first to verify shared secrets and convert between Datagram and `RequestCtx` (a wrapper around `RadiusPacket`), and `ResponseCtx` for responses.
    - `BasicCachingHandler` should be used before the actual handler if required. It provides hooks to override for hit/miss events.
    - `RequestHandler` handles `RequestCtx`, but only accepts specific RadiusPacket subtypes (`AccessRequest`/`AccountingRequest`)
    - `ProxyHandler` handles incoming requests and proxies the request using an instance of RadiusClient.

## License
Copyright Matthias Wuttke and contributors:
- http://tinyradius.sourceforge.net/
- https://github.com/ctran/TinyRadius

Licensed under LGPL 2.1

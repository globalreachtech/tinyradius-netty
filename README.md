# tinyradius-netty

[![CircleCI](https://circleci.com/gh/globalreachtech/tinyradius-netty.svg?style=svg)](https://circleci.com/gh/globalreachtech/tinyradius-netty)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/globalreachtech/tinyradius-netty.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/globalreachtech/tinyradius-netty/context:java)
[![Maintainability](https://api.codeclimate.com/v1/badges/a6b90f85717d753228eb/maintainability)](https://codeclimate.com/github/globalreachtech/tinyradius-netty/maintainability)
[![Coverage Status](https://coveralls.io/repos/github/globalreachtech/tinyradius-netty/badge.svg)](https://coveralls.io/github/globalreachtech/tinyradius-netty)
[![Download](https://api.bintray.com/packages/globalreachtech/grt-maven/tinyradius-netty/images/download.svg)](https://bintray.com/globalreachtech/grt-maven/tinyradius-netty)

tinyradius-netty is a fork of the TinyRadius Radius library, with some significant changes:
- Use netty for asynchronous IO, timeouts, thread management
- Most methods return (netty) Promises
- Use slf4j instead of commons-logging
- Use Generics and Java 8 language features
- Proxy uses Client to handle requests upstream, retries, and connection management
- More immutability
- More tests

## Documentation

[Javadocs](https://globalreachtech.github.io/tinyradius-netty/)

### Dictionary
 - Parses dictionary files in same format as [FreeRadius dictionaries](https://github.com/FreeRADIUS/freeradius-server/tree/master/share/dictionary).
 - `DefaultDictionary.INSTANCE` uses a very limited subset that's included in the classpath.
 - Use `DictionaryParser` to parse custom resources.
   - `FileResourceResolver` and `ClasspathResourceResolver` resolves resources on file system and classpath respectively. Dictionaries can include other files to parse, and paths are resolved differently in each case.
 - Results of dictionary parses are stored as `AttributeType`.

### Attribute
 - `RadiusAttribute` is used for octets attributes and attributes without a dictionary entry or specific subtype.
   - Attribute subtypes store the same data, but have convenience methods for maniupulation and stricter data validation.
 - `AttributeType` contains the attribute type, name, data type, and enumeration of valid values if appropriate.
 - `AttributeType` has methods to create attributes directly for that type.
   - If only the attribute type ID is known, rather than the AttributeType object, use `Attributes.createAttribute` which will create a basic `RadiusAttribute` if no dictionary entry is found.
 - `VendorSpecificAttribute` stores lists of vendor-specific attributes instead of attribute data itself, and serializes its data by concatenating byte array representations of subattributes with its type/vendorId/length header.

### Packet
 - `RadiusPacket` represents packet and associated data.
 

### Client

### Server
    

## License
Copyright Matthias Wuttke (mw@teuto.net) and contributors.

Source code from
- http://tinyradius.sourceforge.net/
- https://github.com/ctran/TinyRadius

Licensed under LGPL 2.1
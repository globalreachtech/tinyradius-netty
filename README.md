[![CircleCI](https://circleci.com/gh/globalreachtech/tinyradius-netty.svg?style=svg)](https://circleci.com/gh/globalreachtech/tinyradius-netty)

# tinyradius-netty

TinyRadius is a simple, small and fast Java Radius library capable of
sending and receiving Radius packets of all types.

tinyradius-netty is a fork of the original, with some significant changes:
- Uses netty for asynchronous IO, timeouts thread management.
- Most methods return (netty) Promises.
- Uses slf4j instead of commons-logging
- Uses Gradle for builds
- Updated to use Generics and Java 8 language features
- Backported improvements from https://github.com/ctran/TinyRadius
- RadiusProxy uses RadiusClient to handle requests upstream and connection management


## License
Copyright Matthias Wuttke (mw@teuto.net) and contributors.

Source code from
- http://tinyradius.sourceforge.net/
- https://github.com/ctran/TinyRadius

Licensed under LGPL.
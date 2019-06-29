# tinyradius-netty

[![CircleCI](https://circleci.com/gh/globalreachtech/tinyradius-netty.svg?style=svg)](https://circleci.com/gh/globalreachtech/tinyradius-netty)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/globalreachtech/tinyradius-netty.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/globalreachtech/tinyradius-netty/alerts/)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=globalreachtech/tinyradius-netty)](https://dependabot.com)


TinyRadius is a simple, small and fast Java Radius library capable of
sending and receiving Radius packets of all types.

tinyradius-netty is a fork of the original, with some significant changes:
- Uses netty for asynchronous IO, timeouts thread management
- Most methods return (netty) Promises
- Uses slf4j instead of commons-logging
- Uses Gradle for builds
- Updated to use Generics and Java 8 language features
- Backported improvements from https://github.com/ctran/TinyRadius
- Proxy uses Client to handle requests upstream and connection management
- More immutability

## License
Copyright Matthias Wuttke (mw@teuto.net) and contributors.

Source code from
- http://tinyradius.sourceforge.net/
- https://github.com/ctran/TinyRadius

Licensed under LGPL 2.1
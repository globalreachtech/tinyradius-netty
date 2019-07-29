# tinyradius-netty

> WORK IN PROGRESS

[![CircleCI](https://circleci.com/gh/globalreachtech/tinyradius-netty.svg?style=svg)](https://circleci.com/gh/globalreachtech/tinyradius-netty)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/globalreachtech/tinyradius-netty.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/globalreachtech/tinyradius-netty/context:java)
[![Maintainability](https://api.codeclimate.com/v1/badges/a6b90f85717d753228eb/maintainability)](https://codeclimate.com/github/globalreachtech/tinyradius-netty/maintainability)
[![Coverage Status](https://coveralls.io/repos/github/globalreachtech/tinyradius-netty/badge.svg)](https://coveralls.io/github/globalreachtech/tinyradius-netty)

tinyradius-netty is a fork of the TinyRadius Radius library, with some significant changes:
- Use netty for asynchronous IO, timeouts, thread management
- Most methods return (netty) Promises
- Use slf4j instead of commons-logging
- Use Generics and Java 8 language features
- Proxy uses Client to handle requests upstream, retries, and connection management
- More immutability
- More tests

## Download
- Direct from GitHub
- https://bintray.com/globalreachtech/grt-maven/tinyradius-netty

## License
Copyright Matthias Wuttke (mw@teuto.net) and contributors.

Source code from
- http://tinyradius.sourceforge.net/
- https://github.com/ctran/TinyRadius

Licensed under LGPL 2.1
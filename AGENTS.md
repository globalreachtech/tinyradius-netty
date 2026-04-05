# AGENTS

## Coding Syntax

- ALWAYS declare `final` for class level variables
- NEVER declare `final` for local variables
- Prefer using `var` for declaring local object types that are not in a stream pipeline
- ALWAYS use latest syntax as allowed by the Java version defined in `build.gradle.kts`
- ALWAYS add JSpecify annotations to method parameters and signatures that are modified based on inferred nullability

## Development

- Anything related to sending / receiving packets should be in `io` package
- Anything related to parsing / validating / building packets should be in `core` package
- Ask before making backwards-incompatible changes to public APIs. Prefer convenience or overloaded methods.
- ALWAYS remove unused imports after making changes

## Domain

- ALWAYS conform with RFC specifications as annotated in the code
- Consult FreeRadius on logic implementation if needed
- Interoperability / alignment with [FreeRadius](https://www.freeradius.org/) is preferred, especially regarding dictionaries
- Interoperability with [Radiator](https://radiatorsoftware.com/) should also be considered
- Consult Netty documentation and discussions when using libraries, pay attention to memory leaks, idioms, and best
  practices

## Testing

- ALWAYS make sure code compiles after every change (tests do not have to pass)
- ALWAYS run and fix tests so they all pass before committing
- Add / update tests after making changes to ensure coverage
- Add assertions and steps to existing tests if similar test cases already exist, or create new tests for new test
  cases. Do not delete existing tests.

## Documentation

- ALWAYS update documentation for any changed methods / classes, including where they were missing before
- Documentation should be updated for JavaDoc for each method, the enclosing class, and `package-info.java`
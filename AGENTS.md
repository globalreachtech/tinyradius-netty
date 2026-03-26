# AGENTS

## Development

- Java version is defined in `build.gradle.kts` - always prefer latest syntax as allowed by the version
- Anything related to sending / receiving packets should be in `io` package
- Anything related to parsing / validating / building packets should be in `core` package
- Add JSpecify annotations to methods that are modified based on inferred nullability
- Ask before making backwards-incompatible changes to public APIs. Prefer convenience or overloaded methods.

## Domain

- Conformity with RFC specifications as annotated in the code is important
- Consult FreeRadius on logic implementation if needed
- Interoperability / alignment with FreeRadius is preferred, especially regarding dictionaries
- Consult Netty documentation and discussions when using libraries, pay attention to memory leaks, idioms, and best
  practices

## Testing

- After every change, tests do not have to pass, but everything should compile
- Always run and fix tests so they all pass before committing
- Add / update tests after making changes to ensure coverage

## Documentation

- After every change, add / update documentation for the changed methods / classes, including where they were missing
  before
- Documentation should be updated for JavaDoc for each method, the enclosing class, and `package-info.java`
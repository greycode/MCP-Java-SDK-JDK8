# MCP Java SDK (JDK 8 Version)

This is the JDK 8 compatible version of the Model Context Protocol (MCP) Java SDK, downgraded from the original JDK 17 implementation.

## Purpose

This version provides compatibility with Java 8 environments, making it possible to use the MCP SDK in applications and systems that haven't migrated to newer Java versions. The functionality remains the same as the JDK 17 version, but with code adaptations to ensure compatibility with Java 8.

## Key Differences from JDK 17 Version

- Uses Java 8 language features instead of modern Java features (no records, text blocks, etc.)
- Uses `javax.*` namespaces instead of `jakarta.*` namespaces
- Compatible with Spring Framework 5.x instead of Spring 6.x
- All functionality is preserved with compatible implementations

## Requirements

- Java 8 (JDK 1.8) or higher
- Maven 3.6.x or higher

## Installation

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>${mcp.version}</version>
</dependency>
```

## Modules

- **mcp** - Core SDK implementation
- **mcp-spring** - Spring Framework integration
  - **mcp-spring-webflux** - Spring WebFlux integration
  - **mcp-spring-webmvc** - Spring WebMVC integration
- **mcp-test** - Testing utilities
- **mcp-bom** - Bill of Materials for dependency management

## Usage

The API is identical to the JDK 17 version. Please refer to the main documentation for usage examples.

## License

MIT License 
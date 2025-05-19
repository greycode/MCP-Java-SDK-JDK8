# MCP Java SDK JDK 8 Downgrade - Task List

This document provides a detailed task list for downgrading the MCP Java SDK from JDK 17 to JDK 8. Tasks are organized by module and feature area.

## Core MCP Module

### Client Package
- [x] Convert `McpClientFeatures` record to regular class
- [x] Migrate `McpClient` interface 
- [x] Implement `McpSyncClient` with blocking operations
- [ ] Implement `McpAsyncClient` with reactive operations
- [ ] Migrate client transport interfaces
- [ ] Implement transport layer implementations

### API and Model Classes
- [ ] Convert `McpSchema` classes from records to regular classes
- [ ] Convert `McpError` classes from records to regular classes
- [ ] Implement remaining utility classes
- [ ] Update JSON serialization/deserialization for regular classes

### Utility Classes
- [ ] Convert `Assert` utility class
- [ ] Convert `Utils` class
- [ ] Implement any JDK 8 compatible versions of utility methods

## Transport Layer

### Session Handling
- [ ] Update `McpClientSession` interface
- [ ] Implement session handlers for JDK 8
- [ ] Convert any usage of JDK 17 features in session implementations

### JSON-RPC Implementation
- [ ] Migrate JSON-RPC message handling
- [ ] Convert request/response handlers
- [ ] Update notification mechanisms

## Testing Infrastructure

### Unit Tests
- [ ] Update test utilities for JDK 8 compatibility
- [ ] Migrate test fixtures and mocks
- [ ] Create tests for converted record classes

### Integration Tests
- [ ] Adapt integration test suite for JDK 8
- [ ] Create equivalence tests between JDK 17 and JDK 8 implementations

## Feature Conversion Status

This section tracks the specific Java features that need to be downgraded:

### Records to Classes
- [x] Created example conversion pattern (`RecordToClassExample.java`)
- [x] Converted `McpClientFeatures` records
- [ ] Convert all `McpSchema` records
- [ ] Convert all other record classes

### Pattern Matching to Traditional instanceof
- [x] Created example conversion pattern (`PatternMatchingExample.java`)
- [ ] Identify and convert all pattern matching instances

### Switch Expressions to Switch Statements
- [x] Created example conversion pattern (`SwitchExpressionExample.java`)
- [ ] Identify and convert all switch expressions

### Text Blocks to String Concatenation
- [x] Created example conversion pattern (`TextBlockExample.java`)
- [ ] Identify and convert all text blocks

### Other Features
- [ ] Replace var with explicit types
- [ ] Replace Stream API additions after Java 8
- [ ] Convert sealed classes/interfaces if present
- [ ] Replace JDK 17 API calls with JDK 8 alternatives

## Dependency Management

- [x] Update Reactor dependencies for Java 8 compatibility
- [ ] Review and update all Maven dependencies
- [ ] Ensure OSGi metadata is compatible with Java 8
- [ ] Convert Jakarta EE dependencies to Java EE equivalents

## Documentation

- [x] Create downgrade plan
- [x] Document conversion patterns for key Java features
- [x] Maintain progress tracking
- [ ] Update Javadoc for JDK 8 version
- [ ] Document API differences where relevant

## Next Steps

The immediate next tasks are:
1. Complete `McpAsyncClient` implementation
2. Convert the `McpSchema` record classes
3. Implement the transport layer implementations
4. Create more automated helpers for record class conversion 
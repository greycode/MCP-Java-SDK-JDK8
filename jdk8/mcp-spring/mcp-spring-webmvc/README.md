# Spring Web MVC MCP Transport Implementation

This module provides a Spring Web MVC implementation of the Model Context Protocol (MCP) transport layer using Server-Sent Events (SSE) for server-to-client communication and HTTP POST for client-to-server messaging.

## Features

- Implements bidirectional communication for the MCP protocol using standard HTTP mechanisms
- Uses SSE for efficient server-to-client messaging
- Provides a `RouterFunction` to easily integrate into Spring Web MVC applications
- Handles session management and reconnection automatically
- Compatible with Java 8 and above

## Usage

### Server-Side Implementation

```java
// Create an ObjectMapper for JSON serialization
ObjectMapper objectMapper = new ObjectMapper();

// Create the transport provider
WebMvcSseServerTransportProvider transportProvider = new WebMvcSseServerTransportProvider(
    objectMapper, 
    "/mcp/message"  // The endpoint where clients will send JSON-RPC messages
);

// Configure session factory
transportProvider.setSessionFactory(transport -> {
    // Create and configure server session
    return new McpServerSession("server", 
        Duration.ofSeconds(30), 
        transport,
        initRequestHandler,    // Handler for initialization requests 
        initNotificationHandler, // Handler for initialization notifications
        requestHandlers,       // Map of request handlers
        notificationHandlers   // Map of notification handlers
    );
});

// Get the router function and register it with Spring
RouterFunction<ServerResponse> routerFunction = transportProvider.getRouterFunction();

// Then in your Spring configuration:
@Bean
public RouterFunction<ServerResponse> mcpRouterFunction(WebMvcSseServerTransportProvider provider) {
    return provider.getRouterFunction();
}
```

## Implementation Details

The `WebMvcSseServerTransportProvider` class implements the `McpServerTransportProvider` interface, providing:

1. An SSE endpoint where clients establish their event stream connections
2. A message endpoint where clients send JSON-RPC messages via HTTP POST
3. Session management with unique IDs for reliable message delivery
4. Graceful shutdown capability with proper cleanup of resources

## Dependencies

- Spring Web MVC (spring-webmvc)
- Project Reactor for reactive programming
- Jackson for JSON serialization/deserialization 
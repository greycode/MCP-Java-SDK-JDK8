# Spring WebFlux MCP Transport Implementation

This module provides a Spring WebFlux implementation of the Model Context Protocol (MCP) transport layer, including both server-side and client-side components. It leverages the reactive programming model of WebFlux for efficient, non-blocking communication using Server-Sent Events (SSE).

## Features

- Full reactive implementation of the MCP transport layer
- Server-side component for hosting MCP services
- Client-side component for connecting to MCP services
- Uses SSE for efficient server-to-client messaging
- Supports bidirectional communication
- Compatible with Java 8 and above

## Components

### Server-Side Implementation

The `WebFluxSseServerTransportProvider` class implements the `McpServerTransportProvider` interface, providing:

1. An SSE endpoint where clients establish their event stream connections
2. A message endpoint where clients send JSON-RPC messages via HTTP POST
3. Session management with unique IDs for reliable message delivery
4. Graceful shutdown capability with proper cleanup of resources

### Client-Side Implementation

The `WebFluxSseClientTransport` class implements the `McpClientTransport` interface, providing:

1. Connection to an MCP server's SSE endpoint
2. Sending JSON-RPC messages to the server
3. Receiving messages from the server
4. Automatic reconnection in case of connection failure
5. Graceful shutdown capability

## Usage

### Server-Side Example

```java
// Create an ObjectMapper for JSON serialization
ObjectMapper objectMapper = new ObjectMapper();

// Create the transport provider
WebFluxSseServerTransportProvider transportProvider = new WebFluxSseServerTransportProvider(
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
public RouterFunction<ServerResponse> mcpRouterFunction(WebFluxSseServerTransportProvider provider) {
    return provider.getRouterFunction();
}
```

### Client-Side Example

```java
// Create an ObjectMapper for JSON serialization
ObjectMapper objectMapper = new ObjectMapper();

// Create the client transport
WebFluxSseClientTransport transport = new WebFluxSseClientTransport(
    objectMapper,
    "http://localhost:8080/sse"  // The server's SSE endpoint
);

// Connect to the server
transport.connect(messages -> messages.doOnNext(message -> {
    // Process incoming messages here
    System.out.println("Received message: " + message);
}).then()).subscribe();

// Send a message to the server
McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest(
    "2.0", "method", "id", params);
transport.sendMessage(request).subscribe();
```

## Dependencies

- Spring WebFlux (spring-webflux)
- Project Reactor for reactive programming
- Jackson for JSON serialization/deserialization
- Reactor Netty for the HTTP client (optional, for the client transport) 
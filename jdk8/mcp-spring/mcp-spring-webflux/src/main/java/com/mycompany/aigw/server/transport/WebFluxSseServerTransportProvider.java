/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.aigw.spec.*;
import com.mycompany.aigw.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side implementation of the Model Context Protocol (MCP) transport layer using
 * HTTP with Server-Sent Events (SSE) through Spring WebFlux. This implementation
 * leverages the reactive programming model of WebFlux to efficiently handle bi-
 * directional communication between the client and server.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Implements bidirectional communication using HTTP POST for client-to-server
 * messages and SSE for server-to-client messages</li>
 * <li>Manages client sessions with unique IDs for reliable message delivery</li>
 * <li>Supports graceful shutdown with proper session cleanup</li>
 * <li>Provides JSON-RPC message handling through configured endpoints</li>
 * <li>Includes built-in error handling and logging</li>
 * </ul>
 *
 * <p>
 * The transport operates on two main endpoints:
 * <ul>
 * <li>{@code /sse} - The SSE endpoint where clients establish their event stream
 * connection</li>
 * <li>A configurable message endpoint where clients send their JSON-RPC messages via HTTP
 * POST</li>
 * </ul>
 *
 * <p>
 * This implementation uses {@link ConcurrentHashMap} to safely manage multiple client
 * sessions in a thread-safe manner. Each client session is assigned a unique ID and
 * maintains its own SSE connection.
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 * @see McpServerTransportProvider
 * @see RouterFunction
 */
public class WebFluxSseServerTransportProvider implements McpServerTransportProvider {

	private static final Logger logger = LoggerFactory.getLogger(WebFluxSseServerTransportProvider.class);

	/**
	 * Event type for JSON-RPC messages sent through the SSE connection.
	 */
	public static final String MESSAGE_EVENT_TYPE = "message";

	/**
	 * Event type for sending the message endpoint URI to clients.
	 */
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/**
	 * Default SSE endpoint path as specified by the MCP transport specification.
	 */
	public static final String DEFAULT_SSE_ENDPOINT = "/sse";

	private final ObjectMapper objectMapper;

	private final String messageEndpoint;

	private final String sseEndpoint;

	private final String baseUrl;

	private final RouterFunction<ServerResponse> routerFunction;

	private McpServerSession.Factory sessionFactory;

	/**
	 * Map of active client sessions, keyed by session ID.
	 */
	private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	/**
	 * Flag indicating if the transport is shutting down.
	 */
	private volatile boolean isClosing = false;

	/**
	 * Constructs a new WebFluxSseServerTransportProvider instance with the default SSE
	 * endpoint.
	 * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
	 * of messages.
	 * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
	 * messages via HTTP POST. This endpoint will be communicated to clients through the
	 * SSE connection's initial endpoint event.
	 * @throws IllegalArgumentException if either objectMapper or messageEndpoint is null
	 */
	public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint) {
		this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT);
	}

	/**
	 * Constructs a new WebFluxSseServerTransportProvider instance.
	 * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
	 * of messages.
	 * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
	 * messages via HTTP POST. This endpoint will be communicated to clients through the
	 * SSE connection's initial endpoint event.
	 * @param sseEndpoint The endpoint URI where clients establish their SSE connections.
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
		this(objectMapper, "", messageEndpoint, sseEndpoint);
	}

	/**
	 * Constructs a new WebFluxSseServerTransportProvider instance.
	 * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
	 * of messages.
	 * @param baseUrl The base URL for the message endpoint, used to construct the full
	 * endpoint URL for clients.
	 * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
	 * messages via HTTP POST. This endpoint will be communicated to clients through the
	 * SSE connection's initial endpoint event.
	 * @param sseEndpoint The endpoint URI where clients establish their SSE connections.
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String baseUrl, String messageEndpoint,
			String sseEndpoint) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.notNull(baseUrl, "Message base URL must not be null");
		Assert.notNull(messageEndpoint, "Message endpoint must not be null");
		Assert.notNull(sseEndpoint, "SSE endpoint must not be null");

		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl;
		this.messageEndpoint = messageEndpoint;
		this.sseEndpoint = sseEndpoint;
		this.routerFunction = RouterFunctions.route()
			.GET(this.sseEndpoint, this::handleSseConnection)
			.POST(this.messageEndpoint, this::handleMessage)
			.build();
	}

	@Override
	public void setSessionFactory(McpServerSession.Factory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Broadcasts a notification to all connected clients through their SSE connections.
	 * The message is serialized to JSON and sent as an SSE event with type "message". If
	 * any errors occur during sending to a particular client, they are logged but don't
	 * prevent sending to other clients.
	 * @param method The method name for the notification
	 * @param params The parameters for the notification
	 * @return A Mono that completes when the broadcast attempt is finished
	 */
	@Override
	public Mono<Void> notifyClients(String method, Object params) {
		if (sessions.isEmpty()) {
			logger.debug("No active sessions to broadcast message to");
			return Mono.empty();
		}

		logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

		return Flux.fromIterable(sessions.values())
			.flatMap(session -> session.sendNotification(method, params)
				.doOnError(e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
				.onErrorComplete())
			.then();
	}

	/**
	 * Initiates a graceful shutdown of the transport. This method:
	 * <ul>
	 * <li>Sets the closing flag to prevent new connections</li>
	 * <li>Closes all active SSE connections</li>
	 * <li>Removes all session records</li>
	 * </ul>
	 * @return A Mono that completes when all cleanup operations are finished
	 */
	@Override
	public Mono<Void> closeGracefully() {
		return Flux.fromIterable(sessions.values())
			.doFirst(() -> {
				this.isClosing = true;
				logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
			})
			.flatMap(McpServerSession::closeGracefully)
			.then()
			.doOnSuccess(v -> logger.debug("Graceful shutdown completed"));
	}

	/**
	 * Returns the RouterFunction that defines the HTTP endpoints for this transport. The
	 * router function handles two endpoints:
	 * <ul>
	 * <li>GET /sse - For establishing SSE connections</li>
	 * <li>POST [messageEndpoint] - For receiving JSON-RPC messages from clients</li>
	 * </ul>
	 * @return The configured RouterFunction for handling HTTP requests
	 */
	public RouterFunction<ServerResponse> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * Handles new SSE connection requests from clients by creating a new session and
	 * establishing an SSE connection. This method:
	 * <ul>
	 * <li>Generates a unique session ID</li>
	 * <li>Creates an emitter to send SSE events to the client</li>
	 * <li>Creates a new session with the WebFlux transport</li>
	 * <li>Sends an initial endpoint event to inform the client where to send
	 * messages</li>
	 * <li>Returns a stream of ServerSentEvents to maintain the connection</li>
	 * </ul>
	 * @param request The incoming server request for SSE connection
	 * @return A Mono containing the SSE response
	 */
	private Mono<ServerResponse> handleSseConnection(ServerRequest request) {
		if (this.isClosing) {
			return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.TEXT_PLAIN)
				.bodyValue("Server is shutting down");
		}

		if (this.sessionFactory == null) {
			String errorMessage = "SessionFactory not initialized - did you forget to call setSessionFactory()?";
			logger.error(errorMessage);
			return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
				.bodyValue(errorMessage);
		}

		String sessionId = UUID.randomUUID().toString();
		// Processor to emit events to the client
		EmitterProcessor<ServerSentEvent<String>> processor = EmitterProcessor.create();
		FluxSink<ServerSentEvent<String>> sink = processor.sink();

		// Mono that signals completion when the client disconnects
		MonoProcessor<Void> disconnected = MonoProcessor.create();

		// Create the transport and session
		WebFluxMcpSessionTransport transport = new WebFluxMcpSessionTransport(sessionId, sink, disconnected);
		McpServerSession session = this.sessionFactory.create(transport);

		// Keep track of the session
		sessions.put(sessionId, session);

		logger.debug("New SSE connection established with session ID: {}", sessionId);

		// Send the initial endpoint event
		sink.next(ServerSentEvent.builder(this.baseUrl + this.messageEndpoint)
			.event(ENDPOINT_EVENT_TYPE)
			.id(String.valueOf(0))
			.build());

		// When the connection is terminated, clean up
		disconnected.doFinally(signal -> {
			logger.debug("SSE connection closed for session ID: {}, signal: {}", sessionId, signal);
			sessions.remove(sessionId);
			processor.onComplete();
		}).subscribe();

		// Return the SSE stream to the client
		return ServerResponse.ok()
			.contentType(MediaType.TEXT_EVENT_STREAM)
			.body(BodyInserters.fromServerSentEvents(processor.doOnCancel(() -> {
				logger.debug("SSE connection cancelled for session ID: {}", sessionId);
				disconnected.onComplete();
			})));
	}

	/**
	 * Handles incoming JSON-RPC messages sent via HTTP POST. This method:
	 * <ul>
	 * <li>Extracts the session ID from the request header</li>
	 * <li>Deserializes the message</li>
	 * <li>Routes the message to the appropriate session for processing</li>
	 * </ul>
	 * @param request The incoming HTTP request containing the JSON-RPC message
	 * @return A Mono containing the HTTP response
	 */
	private Mono<ServerResponse> handleMessage(ServerRequest request) {
		return request.headers().header("MCP-Session-ID")
			.stream()
			.findFirst()
			.map(sessionId -> {
				McpServerSession session = sessions.get(sessionId);
				if (session == null) {
					logger.error("Received message for unknown session ID: {}", sessionId);
					return ServerResponse.status(HttpStatus.NOT_FOUND)
						.contentType(MediaType.TEXT_PLAIN)
						.bodyValue("Unknown session ID");
				}

				return request.bodyToMono(String.class)
					.flatMap(messageJson -> {
						try {
							McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper,
									messageJson);
							return session.handle(message)
								.then(ServerResponse.ok().build())
								.onErrorResume(e -> {
									logger.error("Error handling message: {}", e.getMessage());
									return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.contentType(MediaType.TEXT_PLAIN)
										.bodyValue("Error handling message: " + e.getMessage());
								});
						}
						catch (Exception e) {
							logger.error("Error deserializing message: {}", e.getMessage());
							return ServerResponse.status(HttpStatus.BAD_REQUEST)
								.contentType(MediaType.TEXT_PLAIN)
								.bodyValue("Error deserializing message: " + e.getMessage());
						}
					});
			})
			.orElseGet(() -> {
				logger.error("Received message without session ID");
				return ServerResponse.status(HttpStatus.BAD_REQUEST)
					.contentType(MediaType.TEXT_PLAIN)
					.bodyValue("MCP-Session-ID header is required");
			});
	}

	/**
	 * Implementation of the {@link McpServerTransport} interface for WebFlux. This class
	 * bridges between the reactive transport API and the WebFlux SSE implementation.
	 */
	private class WebFluxMcpSessionTransport implements McpServerTransport {

		private final String sessionId;

		private final FluxSink<ServerSentEvent<String>> sink;

		private final MonoProcessor<Void> disconnected;

		private long eventId = 1;

		/**
		 * Constructs a new transport for a specific session.
		 * @param sessionId The unique ID for this session
		 * @param sink The sink for sending SSE events to the client
		 * @param disconnected A processor that completes when the client disconnects
		 */
		WebFluxMcpSessionTransport(String sessionId, FluxSink<ServerSentEvent<String>> sink,
				MonoProcessor<Void> disconnected) {
			this.sessionId = sessionId;
			this.sink = sink;
			this.disconnected = disconnected;
		}

		/**
		 * Sends a JSON-RPC message to the client via the SSE connection.
		 * @param message The message to send
		 * @return A Mono that completes when the message is sent
		 */
		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return Mono.fromRunnable(() -> {
				try {
					String serialized = objectMapper.writeValueAsString(message);
					sink.next(ServerSentEvent.builder(serialized)
						.event(MESSAGE_EVENT_TYPE)
						.id(String.valueOf(this.eventId++))
						.build());
				}
				catch (Exception e) {
					logger.error("Error serializing message for session {}: {}", sessionId, e.getMessage());
					throw new RuntimeException("Error serializing message: " + e.getMessage(), e);
				}
			});
		}

		/**
		 * Deserializes data from a JSON object to the specified type.
		 * @param <T> The expected result type
		 * @param data The data to unmarshal
		 * @param typeRef Type reference for deserialization
		 * @return The deserialized object
		 */
		@Override
		public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
			try {
				return objectMapper.convertValue(data, typeRef);
			}
			catch (Exception e) {
				throw new McpError("Error unmarshalling data: " + e.getMessage());
			}
		}

		/**
		 * Initiates a graceful shutdown of this transport.
		 * @return A Mono that completes when the shutdown is complete
		 */
		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(() -> {
				logger.debug("Closing session {}", sessionId);
				try {
					// Send a close event before completing
					sink.next(ServerSentEvent.builder("Session closed")
						.event("close")
						.id(String.valueOf(this.eventId++))
						.build());
				}
				catch (Exception e) {
					logger.error("Error sending close event: {}", e.getMessage());
				}
				finally {
					sessions.remove(sessionId);
					disconnected.onComplete();
				}
			});
		}

		/**
		 * Immediately closes this transport without waiting for pending operations.
		 */
		@Override
		public void close() {
			logger.debug("Force closing session {}", sessionId);
			sessions.remove(sessionId);
			disconnected.onComplete();
		}
	}
} 
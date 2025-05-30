/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mycompany.aigw.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of the MCP (Model Context Protocol) session that manages
 * bidirectional JSON-RPC communication between clients and servers. This implementation
 * follows the MCP specification for message exchange and transport handling.
 *
 * <p>
 * The session manages:
 * <ul>
 * <li>Request/response handling with unique message IDs</li>
 * <li>Notification processing</li>
 * <li>Message timeout management</li>
 * <li>Transport layer abstraction</li>
 * </ul>
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 */
public class McpClientSession implements McpSession {

	/** Logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(McpClientSession.class); //

	/** Duration to wait for request responses before timing out */
	private final Duration requestTimeout; //

	/** Transport layer implementation for message exchange */
	private final McpClientTransport transport; //

	/** Map of pending responses keyed by request ID */
	private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>(); //

	/** Map of request handlers keyed by method name */
	private final ConcurrentHashMap<String, RequestHandler<?>> requestHandlers = new ConcurrentHashMap<>(); //

	/** Map of notification handlers keyed by method name */
	private final ConcurrentHashMap<String, NotificationHandler> notificationHandlers = new ConcurrentHashMap<>(); //

	/** Session-specific prefix for request IDs */
	private final String sessionPrefix = UUID.randomUUID().toString().substring(0, 8); //

	/** Atomic counter for generating unique request IDs */
	private final AtomicLong requestCounter = new AtomicLong(0); //

	private final Disposable connection; //

	/**
	 * Functional interface for handling incoming JSON-RPC requests. Implementations
	 * should process the request parameters and return a response.
	 *
	 * @param <T> Response type
	 */
	@FunctionalInterface
	public interface RequestHandler<T> { //

		/**
		 * Handles an incoming request with the given parameters.
		 * @param params The request parameters
		 * @return A Mono containing the response object
		 */
		Mono<T> handle(Object params); //

	}

	/**
	 * Functional interface for handling incoming JSON-RPC notifications. Implementations
	 * should process the notification parameters without returning a response.
	 */
	@FunctionalInterface
	public interface NotificationHandler { //

		/**
		 * Handles an incoming notification with the given parameters.
		 * @param params The notification parameters
		 * @return A Mono that completes when the notification is processed
		 */
		Mono<Void> handle(Object params); //

	}

	/**
	 * Creates a new McpClientSession with the specified configuration and handlers.
	 * @param requestTimeout Duration to wait for responses
	 * @param transport Transport implementation for message exchange
	 * @param requestHandlers Map of method names to request handlers
	 * @param notificationHandlers Map of method names to notification handlers
	 */
	public McpClientSession(Duration requestTimeout, McpClientTransport transport,
							Map<String, RequestHandler<?>> requestHandlers, Map<String, NotificationHandler> notificationHandlers) { //

		Assert.notNull(requestTimeout, "The requestTimeout can not be null"); //
		Assert.notNull(transport, "The transport can not be null"); //
		Assert.notNull(requestHandlers, "The requestHandlers can not be null"); //
		Assert.notNull(notificationHandlers, "The notificationHandlers can not be null"); //

		this.requestTimeout = requestTimeout; //
		this.transport = transport; //
		this.requestHandlers.putAll(requestHandlers); //
		this.notificationHandlers.putAll(notificationHandlers); //

		// Reactor connection handling - compatible with SB 2.x + WebFlux
		// TODO: consider mono.transformDeferredContextual where the Context contains
		// the
		// Observation associated with the individual message - it can be used to
		// create child Observation and emit it together with the message to the
		// consumer
		this.connection = this.transport.connect(mono -> mono.doOnNext(this::handle)).subscribe(); //
	}

	private void handle(McpSchema.JSONRPCMessage message) { //
		// Replaced 'var' with explicit types
		if (message instanceof McpSchema.JSONRPCResponse) { //
			McpSchema.JSONRPCResponse response = (McpSchema.JSONRPCResponse) message;
			logger.debug("Received Response: {}", response); //
			MonoSink<McpSchema.JSONRPCResponse> sink = pendingResponses.remove(response.getId()); // Replace var
			if (sink == null) { //
				logger.warn("Unexpected response for unknown id {}", response.getId()); //
			}
			else {
				sink.success(response); //
			}
		}
		else if (message instanceof McpSchema.JSONRPCRequest) { //
			McpSchema.JSONRPCRequest request = (McpSchema.JSONRPCRequest) message;
			logger.debug("Received request: {}", request); //
			handleIncomingRequest(request).onErrorResume(error -> { //
				// Replaced 'var' with explicit type
				McpSchema.JSONRPCResponse errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null, //
						new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, //
								error.getMessage(), null)); //
				return this.transport.sendMessage(errorResponse).then(Mono.empty()); //
			}).flatMap(this.transport::sendMessage).subscribe(); //
		}
		else if (message instanceof McpSchema.JSONRPCNotification) { //
			McpSchema.JSONRPCNotification notification = (McpSchema.JSONRPCNotification) message;
			logger.debug("Received notification: {}", notification); //
			handleIncomingNotification(notification) //
					.doOnError(error -> logger.error("Error handling notification: {}", error.getMessage())) //
					.subscribe(); //
		}
		else {
			logger.warn("Received unknown message type: {}", message); //
		}
	}

	/**
	 * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
	 * @param request The incoming JSON-RPC request
	 * @return A Mono containing the JSON-RPC response
	 */
	private Mono<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) { //
		return Mono.defer(() -> { //
			// Replaced 'var' with explicit type
			RequestHandler<?> handler = this.requestHandlers.get(request.getMethod()); //
			if (handler == null) { //
				MethodNotFoundError error = getMethodNotFoundError(request.getMethod()); //
				return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null, //
						new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND, //
								error.message(), error.data()))); // Use getter methods
			}

			return handler.handle(request.getParams()) //
					.map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), result, null)) //
					.onErrorResume(error -> Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), //
							null, new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, //
							error.getMessage(), null)))); // TODO: add error message
			// through the data field
		});
	}

	// Replaced 'record' with a standard class for JDK 8 compatibility
	/**
	 * Represents information about a method not found error.
	 * Immutable class replacing the original record.
	 */
	private static final class MethodNotFoundError { //
		private final String method; //
		private final String message; //
		private final Object data; //

		MethodNotFoundError(String method, String message, Object data) { //
			this.method = method; //
			this.message = message; //
			this.data = data; //
		}

		public String method() { // getter
			return method; //
		}

		public String message() { // getter
			return message; //
		}

		public Object data() { // getter
			return data; //
		}

		@Override // Optional but good practice: equals
		public boolean equals(Object o) { //
			if (this == o) return true; //
			if (o == null || getClass() != o.getClass()) return false; //
			MethodNotFoundError that = (MethodNotFoundError) o; //
			return Objects.equals(method, that.method) && Objects.equals(message, that.message) && Objects.equals(data, that.data); //
		}

		@Override // Optional but good practice: hashCode
		public int hashCode() { //
			return Objects.hash(method, message, data); //
		}

		@Override // Optional but good practice: toString
		public String toString() { //
			return "MethodNotFoundError{" + //
					"method='" + method + '\'' + //
					", message='" + message + '\'' + //
					", data=" + data + //
					'}'; //
		}
	}

	// Replaced enhanced 'switch' and Map.of with traditional switch/if and JDK 8 map creation
	private MethodNotFoundError getMethodNotFoundError(String method) { //
		// Using traditional switch statement
        //
        if (method.equals(McpSchema.METHOD_ROOTS_LIST)) {// Using Collections.singletonMap for JDK 8 compatibility instead of Map.of
            return new MethodNotFoundError(method, "Roots not supported", //
                    Collections.singletonMap("reason", "Client does not have roots capability")); //
        }
        return new MethodNotFoundError(method, "Method not found: " + method, null); //
// Alternatively, using if-else:
		// if (McpSchema.METHOD_ROOTS_LIST.equals(method)) {
		// 	return new MethodNotFoundError(method, "Roots not supported",
		// 			Collections.singletonMap("reason", "Client does not have roots capability"));
		// } else {
		// 	return new MethodNotFoundError(method, "Method not found: " + method, null);
		// }
	}

	/**
	 * Handles an incoming JSON-RPC notification by routing it to the appropriate handler.
	 * @param notification The incoming JSON-RPC notification
	 * @return A Mono that completes when the notification is processed
	 */
	private Mono<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) { //
		return Mono.defer(() -> { //
			// Replaced 'var' with explicit type
			NotificationHandler handler = notificationHandlers.get(notification.getMethod()); //
			if (handler == null) { //
				logger.error("No handler registered for notification method: {}", notification.getMethod()); //
				return Mono.empty(); //
			}
			return handler.handle(notification.getParams()); //
		});
	}

	/**
	 * Generates a unique request ID in a non-blocking way. Combines a session-specific
	 * prefix with an atomic counter to ensure uniqueness.
	 * @return A unique request ID string
	 */
	private String generateRequestId() { //
		return this.sessionPrefix + "-" + this.requestCounter.getAndIncrement(); //
	}

	/**
	 * Sends a JSON-RPC request and returns the response.
	 * @param <T> The expected response type
	 * @param method The method name to call
	 * @param requestParams The request parameters
	 * @param typeRef Type reference for response deserialization
	 * @return A Mono containing the response
	 */
	@Override
	public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) { //
		String requestId = this.generateRequestId(); //

		// Reactor usage (Mono.create, timeout, handle) is compatible
		return Mono.deferContextual(ctx -> Mono.<McpSchema.JSONRPCResponse>create(sink -> { //
			this.pendingResponses.put(requestId, sink); //
			McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method, //
					requestId, requestParams); //
			this.transport.sendMessage(jsonrpcRequest) //
					.contextWrite(ctx) // contextWrite might need adjustment based on exact Reactor version, but concept exists
					// TODO: It's most efficient to create a dedicated Subscriber here
					.subscribe(v -> { // Empty subscriber action
					}, error -> { // Error handling
						this.pendingResponses.remove(requestId); //
						sink.error(error); //
					});
		})).timeout(this.requestTimeout).handle((jsonRpcResponse, sink) -> { // Type casting may be needed inside handle depending on generics interpretation
			if (jsonRpcResponse.getError() != null) { //
				logger.error("Error handling request: {}", jsonRpcResponse.getError()); //
				sink.error(new McpError(jsonRpcResponse.getError())); //
			}
			else {
				// Type checking using equals is standard Java
				if (typeRef.getType().equals(Void.class)) { //
					sink.complete(); //
				}
				else {
					// transport.unmarshalFrom assumed compatible
					sink.next(this.transport.unmarshalFrom(jsonRpcResponse.getResult(), typeRef)); //
				}
			}
		});
	}

	/**
	 * Sends a JSON-RPC notification.
	 * @param method The method name for the notification
	 * @param params The notification parameters
	 * @return A Mono that completes when the notification is sent
	 */
	@Override
	public Mono<Void> sendNotification(String method, Object params) { //
		McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, //
				method, params); //
		return this.transport.sendMessage(jsonrpcNotification); // transport.sendMessage assumed compatible
	}

	/**
	 * Closes the session gracefully, allowing pending operations to complete.
	 * @return A Mono that completes when the session is closed
	 */
	@Override
	public Mono<Void> closeGracefully() { //
		return Mono.defer(() -> { //
			// Disposable.dispose() is standard Reactor
			this.connection.dispose(); //
			// transport.closeGracefully() assumed compatible
			return transport.closeGracefully(); //
		});
	}

	/**
	 * Closes the session immediately, potentially interrupting pending operations.
	 */
	@Override
	public void close() { //
		// Disposable.dispose() is standard Reactor
		this.connection.dispose(); //
		// transport.close() assumed compatible
		transport.close(); //
	}

}
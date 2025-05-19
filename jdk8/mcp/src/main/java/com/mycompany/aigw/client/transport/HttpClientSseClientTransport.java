/*
 * Copyright 2024 - 2024 the original author or authors.
 */
package com.mycompany.aigw.client.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.aigw.client.transport.FlowSseClient.SseEvent;
import com.mycompany.aigw.spec.McpClientTransport;
import com.mycompany.aigw.spec.McpError;
import com.mycompany.aigw.spec.McpSchema;
import com.mycompany.aigw.spec.McpSchema.JSONRPCMessage;
import com.mycompany.aigw.spec.McpTransport;
import com.mycompany.aigw.util.Assert;
import com.mycompany.aigw.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Server-Sent Events (SSE) implementation of the
 * {@link McpTransport} that follows the MCP HTTP with SSE
 * transport specification, using Java's HttpURLConnection.
 *
 * <p>
 * This transport implementation establishes a bidirectional communication channel between
 * client and server using SSE for server-to-client messages and HTTP POST requests for
 * client-to-server messages. The transport:
 * <ul>
 * <li>Establishes an SSE connection to receive server messages</li>
 * <li>Handles endpoint discovery through SSE events</li>
 * <li>Manages message serialization/deserialization using Jackson</li>
 * <li>Provides graceful connection termination</li>
 * </ul>
 *
 * <p>
 * The transport supports two types of SSE events:
 * <ul>
 * <li>'endpoint' - Contains the URL for sending client messages</li>
 * <li>'message' - Contains JSON-RPC message payload</li>
 * </ul>
 *
 * @author Christian Tzolov
 * @see McpTransport
 * @see McpClientTransport
 */
public class HttpClientSseClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientSseClientTransport.class);

	/** SSE event type for JSON-RPC messages */
	private static final String MESSAGE_EVENT_TYPE = "message";

	/** SSE event type for endpoint discovery */
	private static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/** Default SSE endpoint path */
	private static final String DEFAULT_SSE_ENDPOINT = "/sse";

	/** Base URI for the MCP server */
	private final URI baseUri;

	/** SSE endpoint path */
	private final String sseEndpoint;

	/** SSE client for handling server-sent events. Uses the /sse endpoint */
	private final FlowSseClient sseClient;

	/** Headers for HTTP requests */
	private final Map<String, String> headers;

	/** Connection timeout in milliseconds */
	private final int connectTimeout;

	/** JSON object mapper for message serialization/deserialization */
	protected ObjectMapper objectMapper;

	/** Flag indicating if the transport is in closing state */
	private volatile boolean isClosing = false;

	/** Latch for coordinating endpoint discovery */
	private final CountDownLatch closeLatch = new CountDownLatch(1);

	/** Holds the discovered message endpoint URL */
	private final AtomicReference<String> messageEndpoint = new AtomicReference<>();

	/** Holds the SSE connection future */
	private final AtomicReference<CompletableFuture<Void>> connectionFuture = new AtomicReference<>();

	/** Executor service for handling asynchronous operations */
	private final ScheduledExecutorService executorService;

	/**
	 * Creates a new transport instance with default HTTP client and object mapper.
	 * @param baseUri the base URI of the MCP server
	 * @deprecated Use {@link HttpClientSseClientTransport#builder(String)} instead. This
	 * constructor will be removed in future versions.
	 */
	@Deprecated
	public HttpClientSseClientTransport(String baseUri) {
		this(baseUri, DEFAULT_SSE_ENDPOINT, new ObjectMapper(), new HashMap<>(), 10000);
	}

	/**
	 * Creates a new transport instance with custom parameters.
	 * @param baseUri the base URI of the MCP server
	 * @param sseEndpoint the SSE endpoint path
	 * @param objectMapper the object mapper for JSON serialization/deserialization
	 * @param headers HTTP headers to include in requests
	 * @param connectTimeout connection timeout in milliseconds
	 * @throws IllegalArgumentException if objectMapper is null
	 */
	public HttpClientSseClientTransport(String baseUri, String sseEndpoint, ObjectMapper objectMapper,
			Map<String, String> headers, int connectTimeout) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.hasText(baseUri, "baseUri must not be empty");
		Assert.hasText(sseEndpoint, "sseEndpoint must not be empty");
		Assert.notNull(headers, "headers must not be null");

		this.baseUri = URI.create(baseUri);
		this.sseEndpoint = sseEndpoint;
		this.objectMapper = objectMapper;
		this.headers = new HashMap<>(headers);
		this.connectTimeout = connectTimeout;
		this.executorService = Executors.newScheduledThreadPool(2);

		// Add content type header if not present
		if (!this.headers.containsKey("Content-Type")) {
			this.headers.put("Content-Type", "application/json");
		}

		this.sseClient = new FlowSseClient();
	}

	/**
	 * Creates a new builder for {@link HttpClientSseClientTransport}.
	 * @param baseUri the base URI of the MCP server
	 * @return a new builder instance
	 */
	public static Builder builder(String baseUri) {
		return new Builder().baseUri(baseUri);
	}

	/**
	 * Builder for {@link HttpClientSseClientTransport}.
	 */
	public static class Builder {

		private String baseUri;

		private String sseEndpoint = DEFAULT_SSE_ENDPOINT;

		private ObjectMapper objectMapper = new ObjectMapper();

		private Map<String, String> headers = new HashMap<>();

		private int connectTimeout = 10000;

		/**
		 * Creates a new builder instance.
		 */
		Builder() {
			// Default constructor
			headers.put("Content-Type", "application/json");
		}

		/**
		 * Creates a new builder with the specified base URI.
		 * @param baseUri the base URI of the MCP server
		 * @deprecated Use {@link HttpClientSseClientTransport#builder(String)} instead.
		 * This constructor is deprecated and will be removed or made {@code protected} or
		 * {@code private} in a future release.
		 */
		@Deprecated
		public Builder(String baseUri) {
			Assert.hasText(baseUri, "baseUri must not be empty");
			this.baseUri = baseUri;
			headers.put("Content-Type", "application/json");
		}

		/**
		 * Sets the base URI.
		 * @param baseUri the base URI
		 * @return this builder
		 */
		Builder baseUri(String baseUri) {
			Assert.hasText(baseUri, "baseUri must not be empty");
			this.baseUri = baseUri;
			return this;
		}

		/**
		 * Sets the SSE endpoint path.
		 * @param sseEndpoint the SSE endpoint path
		 * @return this builder
		 */
		public Builder sseEndpoint(String sseEndpoint) {
			Assert.hasText(sseEndpoint, "sseEndpoint must not be empty");
			this.sseEndpoint = sseEndpoint;
			return this;
		}

		/**
		 * Sets the connection timeout.
		 * @param connectTimeout the connection timeout in milliseconds
		 * @return this builder
		 */
		public Builder connectTimeout(int connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		/**
		 * Adds a header to the request.
		 * @param name the header name
		 * @param value the header value
		 * @return this builder
		 */
		public Builder header(String name, String value) {
			Assert.hasText(name, "Header name must not be empty");
			Assert.notNull(value, "Header value must not be null");
			this.headers.put(name, value);
			return this;
		}

		/**
		 * Sets the object mapper for JSON serialization/deserialization.
		 * @param objectMapper the object mapper
		 * @return this builder
		 */
		public Builder objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "objectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		/**
		 * Builds a new {@link HttpClientSseClientTransport} instance.
		 * @return a new transport instance
		 */
		public HttpClientSseClientTransport build() {
			return new HttpClientSseClientTransport(baseUri, sseEndpoint, objectMapper, headers, connectTimeout);
		}

	}

	/**
	 * Establishes the SSE connection with the server and sets up message handling.
	 *
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Initiates the SSE connection</li>
	 * <li>Handles endpoint discovery events</li>
	 * <li>Processes incoming JSON-RPC messages</li>
	 * </ul>
	 * @param handler the function to process received JSON-RPC messages
	 * @return a Mono that completes when the connection is established
	 */
	@Override
	public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		connectionFuture.set(future);

		URI clientUri = Utils.resolveUri(this.baseUri, this.sseEndpoint);
		sseClient.subscribe(clientUri.toString(), this.headers, this.connectTimeout,
				new FlowSseClient.SseEventHandler() {
					@Override
					public void onEvent(SseEvent event) {
						if (isClosing) {
							return;
						}

						try {
							if (ENDPOINT_EVENT_TYPE.equals(event.type())) {
								String endpoint = event.data();
								messageEndpoint.set(endpoint);
								closeLatch.countDown();
								future.complete(null);
							}
							else if (MESSAGE_EVENT_TYPE.equals(event.type())) {
								JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper,
										event.data());
								handler.apply(Mono.just(message)).subscribe();
							}
							else {
								logger.error("Received unrecognized SSE event type: {}", event.type());
							}
						}
						catch (IOException e) {
							logger.error("Error processing SSE event", e);
							future.completeExceptionally(e);
						}
					}

					@Override
					public void onError(Throwable error) {
						if (!isClosing) {
							logger.error("SSE connection error", error);
							future.completeExceptionally(error);
						}
					}
				});

		return Mono.fromFuture(future);
	}

	/**
	 * Sends a JSON-RPC message to the server.
	 *
	 * <p>
	 * This method waits for the message endpoint to be discovered before sending the
	 * message. The message is serialized to JSON and sent as an HTTP POST request.
	 * @param message the JSON-RPC message to send
	 * @return a Mono that completes when the message is sent
	 * @throws McpError if the message endpoint is not available or the wait times out
	 */
	@Override
	public Mono<Void> sendMessage(JSONRPCMessage message) {
		if (isClosing) {
			return Mono.empty();
		}

		try {
			if (!closeLatch.await(10, TimeUnit.SECONDS)) {
				return Mono.error(new McpError("Failed to wait for the message endpoint"));
			}
		}
		catch (InterruptedException e) {
			return Mono.error(new McpError("Failed to wait for the message endpoint"));
		}

		String endpoint = messageEndpoint.get();
		if (endpoint == null) {
			return Mono.error(new McpError("No message endpoint available"));
		}

		try {
			String jsonText = this.objectMapper.writeValueAsString(message);
			URI requestUri = Utils.resolveUri(baseUri, endpoint);

			CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
				try {
					HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
					connection.setRequestMethod("POST");
					connection.setConnectTimeout(connectTimeout);
					connection.setReadTimeout(connectTimeout);

					// Set headers
					for (Map.Entry<String, String> entry : headers.entrySet()) {
						connection.setRequestProperty(entry.getKey(), entry.getValue());
					}

					// Set up for output
					connection.setDoOutput(true);

					// Write request body
					try (OutputStream os = connection.getOutputStream()) {
						os.write(jsonText.getBytes(StandardCharsets.UTF_8));
						os.flush();
					}

					// Read response
					int statusCode = connection.getResponseCode();
					if (statusCode != 200 && statusCode != 201 && statusCode != 202 && statusCode != 206) {
						logger.error("Error sending message: {}", statusCode);
					}

					// Close connection
					connection.disconnect();
					return null;
				}
				catch (IOException e) {
					if (!isClosing) {
						throw new RuntimeException("Failed to send message", e);
					}
					return null;
				}
			}, executorService);

			return Mono.fromFuture(future);
		}
		catch (IOException e) {
			if (!isClosing) {
				return Mono.error(new RuntimeException("Failed to serialize message", e));
			}
			return Mono.empty();
		}
	}

	/**
	 * Gracefully closes the transport connection.
	 *
	 * <p>
	 * Sets the closing flag and cancels any pending connection future. This prevents new
	 * messages from being sent and allows ongoing operations to complete.
	 * @return a Mono that completes when the closing process is initiated
	 */
	@Override
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			isClosing = true;
			CompletableFuture<Void> future = connectionFuture.get();
			if (future != null && !future.isDone()) {
				future.cancel(true);
			}
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		});
	}

	/**
	 * Unmarshal data to the specified type using the configured object mapper.
	 * @param data the data to unmarshal
	 * @param typeRef the type reference for the target type
	 * @param <T> the target type
	 * @return the unmarshalled object
	 */
	@Override
	public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
		return this.objectMapper.convertValue(data, typeRef);
	}

}
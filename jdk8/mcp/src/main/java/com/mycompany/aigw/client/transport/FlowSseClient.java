/*
* Copyright 2024 - 2024 the original author or authors.
*/
package com.mycompany.aigw.client.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Server-Sent Events (SSE) client implementation using Java 8 compatible APIs for
 * stream processing. This client establishes a connection to an SSE endpoint and
 * processes the incoming event stream, parsing SSE-formatted messages into structured
 * events.
 *
 * <p>
 * The client supports standard SSE event fields including:
 * <ul>
 * <li>event - The event type (defaults to "message" if not specified)</li>
 * <li>id - The event ID</li>
 * <li>data - The event payload data</li>
 * </ul>
 *
 * <p>
 * Events are delivered to a provided {@link SseEventHandler} which can process events and
 * handle any errors that occur during the connection.
 *
 * @author Christian Tzolov
 * @see SseEventHandler
 * @see SseEvent
 */
public class FlowSseClient {

	private final ExecutorService executorService;

	/**
	 * Pattern to extract the data content from SSE data field lines. Matches lines
	 * starting with "data:" and captures the remaining content.
	 */
	private static final Pattern EVENT_DATA_PATTERN = Pattern.compile("^data:(.+)$", Pattern.MULTILINE);

	/**
	 * Pattern to extract the event ID from SSE id field lines. Matches lines starting
	 * with "id:" and captures the ID value.
	 */
	private static final Pattern EVENT_ID_PATTERN = Pattern.compile("^id:(.+)$", Pattern.MULTILINE);

	/**
	 * Pattern to extract the event type from SSE event field lines. Matches lines
	 * starting with "event:" and captures the event type.
	 */
	private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("^event:(.+)$", Pattern.MULTILINE);

	/**
	 * Class representing a Server-Sent Event with its standard fields.
	 */
	public static class SseEvent {

		private final String id;

		private final String type;

		private final String data;

		public SseEvent(String id, String type, String data) {
			this.id = id;
			this.type = type;
			this.data = data;
		}

		public String id() {
			return id;
		}

		public String type() {
			return type;
		}

		public String data() {
			return data;
		}

		@Override
		public String toString() {
			return "SseEvent{" + "id='" + id + '\'' + ", type='" + type + '\'' + ", data='" + data + '\'' + '}';
		}

	}

	/**
	 * Interface for handling SSE events and errors. Implementations can process received
	 * events and handle any errors that occur during the SSE connection.
	 */
	public interface SseEventHandler {

		/**
		 * Called when an SSE event is received.
		 * @param event the received SSE event containing id, type, and data
		 */
		void onEvent(SseEvent event);

		/**
		 * Called when an error occurs during the SSE connection.
		 * @param error the error that occurred
		 */
		void onError(Throwable error);

	}

	/**
	 * Creates a new FlowSseClient using a cached thread pool.
	 */
	public FlowSseClient() {
		this(Executors.newCachedThreadPool());
	}

	/**
	 * Creates a new FlowSseClient with the specified executor service.
	 * @param executorService the executor service to use for async processing
	 */
	public FlowSseClient(ExecutorService executorService) {
		this.executorService = executorService;
	}

	/**
	 * Subscribes to an SSE endpoint and processes the event stream.
	 *
	 * <p>
	 * This method establishes a connection to the specified URL and begins processing the
	 * SSE stream. Events are parsed and delivered to the provided event handler. The
	 * connection remains active until either an error occurs or the server closes the
	 * connection.
	 * @param url the SSE endpoint URL to connect to
	 * @param eventHandler the handler that will receive SSE events and error
	 * notifications
	 * @throws RuntimeException if the connection fails with a non-200 status code
	 */
	public void subscribe(String url, SseEventHandler eventHandler) {
		subscribe(url, null, 0, eventHandler);
	}

	/**
	 * Subscribes to an SSE endpoint with custom headers and connection timeout.
	 *
	 * <p>
	 * This method establishes a connection to the specified URL with custom HTTP headers
	 * and connection timeout. Events are parsed and delivered to the provided event
	 * handler. The connection remains active until either an error occurs or the server
	 * closes the connection.
	 * @param url the SSE endpoint URL to connect to
	 * @param headers custom HTTP headers to include in the request, may be null
	 * @param connectTimeout connection timeout in milliseconds, ignored if <= 0
	 * @param eventHandler the handler that will receive SSE events and error
	 * notifications
	 * @throws RuntimeException if the connection fails with a non-200 status code
	 */
	public void subscribe(String url, Map<String, String> headers, int connectTimeout, SseEventHandler eventHandler) {
		CompletableFuture.runAsync(() -> {
			try {
				HttpURLConnection connection = getHttpURLConnection(url, headers, connectTimeout);

				int status = connection.getResponseCode();
				if (status != 200 && status != 201 && status != 202 && status != 206) {
					throw new RuntimeException("Failed to connect to SSE stream. Unexpected status code: " + status);
				}

				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

				StringBuilder eventBuilder = new StringBuilder();
				AtomicReference<String> currentEventId = new AtomicReference<>();
				AtomicReference<String> currentEventType = new AtomicReference<>("message");

				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isEmpty()) {
						// Empty line means end of event
						if (eventBuilder.length() > 0) {
							String eventData = eventBuilder.toString();
							SseEvent event = new SseEvent(currentEventId.get(), currentEventType.get(),
									eventData.trim());
							eventHandler.onEvent(event);
							eventBuilder.setLength(0);
						}
					}
					else {
						if (line.startsWith("data:")) {
							Matcher matcher = EVENT_DATA_PATTERN.matcher(line);
							if (matcher.find()) {
								eventBuilder.append(matcher.group(1).trim()).append("\n");
							}
						}
						else if (line.startsWith("id:")) {
							Matcher matcher = EVENT_ID_PATTERN.matcher(line);
							if (matcher.find()) {
								currentEventId.set(matcher.group(1).trim());
							}
						}
						else if (line.startsWith("event:")) {
							Matcher matcher = EVENT_TYPE_PATTERN.matcher(line);
							if (matcher.find()) {
								currentEventType.set(matcher.group(1).trim());
							}
						}
					}
				}

				// Handle any remaining event data
				if (eventBuilder.length() > 0) {
					String eventData = eventBuilder.toString();
					SseEvent event = new SseEvent(currentEventId.get(), currentEventType.get(), eventData.trim());
					eventHandler.onEvent(event);
				}

				reader.close();
				connection.disconnect();

			}
			catch (IOException e) {
				eventHandler.onError(e);
			}
		}, executorService);
	}

	private static HttpURLConnection getHttpURLConnection(String url, Map<String, String> headers, int connectTimeout) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "text/event-stream");
		connection.setRequestProperty("Cache-Control", "no-cache");

		// Apply custom headers if provided
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				connection.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}

		// Set connection timeout if specified
		if (connectTimeout > 0) {
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(connectTimeout);
		}

		connection.setDoInput(true);
		return connection;
	}

}
/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycompany.aigw.mcp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.aigw.client.McpAsyncClient;
import com.mycompany.aigw.client.McpSyncClient;
import com.mycompany.aigw.sdk.tool.ToolCallback;
import com.mycompany.aigw.sdk.tool.ToolContext;
import com.mycompany.aigw.sdk.util.ModelOptionsUtils;
import com.mycompany.aigw.server.McpServerFeatures;
import com.mycompany.aigw.server.McpServerFeatures.AsyncToolSpecification;
import com.mycompany.aigw.server.McpSyncServerExchange;
import com.mycompany.aigw.spec.McpSchema;
import com.mycompany.aigw.spec.McpSchema.Role;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class that provides helper methods for working with Model Context Protocol
 * (MCP) tools in a Spring AI environment. This class facilitates the integration between
 * Spring AI's tool callbacks and MCP's tool system.
 *
 * <p>
 * The MCP tool system enables servers to expose executable functionality to language
 * models, allowing them to interact with external systems, perform computations, and take
 * actions in the real world. Each tool is uniquely identified by a name and includes
 * metadata describing its schema.
 *
 * <p>
 * This helper class provides methods to:
 * <ul>
 * <li>Convert Spring AI's {@link ToolCallback} instances to MCP tool specification</li>
 * <li>Generate JSON schemas for tool input validation</li>
 * </ul>
 *
 * @author Christian Tzolov
 */
public final class McpToolUtils {

	/**
	 * The name of tool context key used to store the MCP exchange object.
	 */
	public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";

	private McpToolUtils() {
	}

	public static String prefixedToolName(String prefix, String toolName) {

		if (StringUtils.hasText(prefix) || StringUtils.hasText(toolName)) {
			throw new IllegalArgumentException("Prefix or toolName cannot be null or empty");
		}

		String input = prefix + "_" + toolName;

		// Replace any character that isn't alphanumeric, underscore, or hyphen with
		// concatenation
		String formatted = input.replaceAll("[^a-zA-Z0-9_-]", "");

		formatted = formatted.replaceAll("-", "_");

		// If the string is longer than 64 characters, keep the last 64 characters
		if (formatted.length() > 64) {
			formatted = formatted.substring(formatted.length() - 64);
		}

		return formatted;
	}

	/**
	 * Converts a list of Spring AI tool callbacks to MCP synchronous tool specification.
	 * <p>
	 * This method processes multiple tool callbacks in bulk, converting each one to its
	 * corresponding MCP tool specification while maintaining synchronous execution
	 * semantics.
	 * @param toolCallbacks the list of tool callbacks to convert
	 * @return a list of MCP synchronous tool specification
	 */
	public static List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecification(
			List<ToolCallback> toolCallbacks) {
		// Implementation for JDK8
		return toolCallbacks.stream().map(McpToolUtils::toSyncToolSpecification).collect(Collectors.toList());
	}

	/**
	 * Convenience method to convert a variable number of tool callbacks to MCP
	 * synchronous tool specification.
	 * <p>
	 * This is a varargs wrapper around {@link #toSyncToolSpecification(List)} for easier
	 * usage when working with individual callbacks.
	 * @param toolCallbacks the tool callbacks to convert
	 * @return a list of MCP synchronous tool specification
	 */
	public static List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecifications(
			ToolCallback... toolCallbacks) {
		return toSyncToolSpecification(Arrays.asList(toolCallbacks));
	}

	/**
	 * Converts a Spring AI ToolCallback to an MCP SyncToolSpecification. This enables
	 * Spring AI functions to be exposed as MCP tools that can be discovered and invoked
	 * by language models.
	 *
	 * <p>
	 * The conversion process:
	 * <ul>
	 * <li>Creates an MCP Tool with the function's name and input schema</li>
	 * <li>Wraps the function's execution in a SyncToolSpecification that handles the MCP
	 * protocol</li>
	 * <li>Provides error handling and result formatting according to MCP
	 * specifications</li>
	 * </ul>
	 *
	 * You can use the ToolCallback builder to create a new instance of ToolCallback using
	 * either java.util.function.Function or Method reference.
	 * @param toolCallback the Spring AI function callback to convert
	 * @return an MCP SyncToolSpecification that wraps the function callback
	 * @throws RuntimeException if there's an error during the function execution
	 */
	public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback) {
		return toSyncToolSpecification(toolCallback, null);
	}

	/**
	 * Converts a Spring AI ToolCallback to an MCP SyncToolSpecification. This enables
	 * Spring AI functions to be exposed as MCP tools that can be discovered and invoked
	 * by language models.
	 *
	 * <p>
	 * The conversion process:
	 * <ul>
	 * <li>Creates an MCP Tool with the function's name and input schema</li>
	 * <li>Wraps the function's execution in a SyncToolSpecification that handles the MCP
	 * protocol</li>
	 * <li>Provides error handling and result formatting according to MCP
	 * specifications</li>
	 * </ul>
	 * @param toolCallback the Spring AI function callback to convert
	 * @param mimeType the MIME type of the output content
	 * @return an MCP SyncToolSpecification that wraps the function callback
	 * @throws RuntimeException if there's an error during the function execution
	 */
	public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback,
			MimeType mimeType) {

		McpSchema.Tool tool = new McpSchema.Tool(toolCallback.getToolDefinition().name(),
				toolCallback.getToolDefinition().description(), toolCallback.getToolDefinition().inputSchema());

		return new McpServerFeatures.SyncToolSpecification(tool, (exchange, request) -> {
			try {
				// Using ModelOptionsUtils to convert request to JSON string
				String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request),
						new ToolContext(Collections.singletonMap(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)));
				if (mimeType.toString().startsWith("image")) {
					List<Role> roles = new ArrayList<>();
					roles.add(Role.ASSISTANT);
					return new McpSchema.CallToolResult(Collections.singletonList(
							new McpSchema.ImageContent(roles, null, callResult, mimeType.toString())),
							false);
				}
				return new McpSchema.CallToolResult(Collections.singletonList(new McpSchema.TextContent(callResult)), false);
			}
			catch (Exception e) {
				return new McpSchema.CallToolResult(Collections.singletonList(new McpSchema.TextContent(e.getMessage())), true);
			}
		});
	}

	/**
	 * Retrieves the MCP exchange object from the provided tool context if it exists.
	 * @param toolContext the tool context from which to retrieve the MCP exchange
	 * @return the MCP exchange object, or null if not present in the context
	 */
	public static Optional<McpSyncServerExchange> getMcpExchange(ToolContext toolContext) {
		if (toolContext.getContext().containsKey(TOOL_CONTEXT_MCP_EXCHANGE_KEY)) {
			return Optional
				.ofNullable((McpSyncServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY));
		}
		return Optional.empty();
	}

	/**
	 * Converts a list of Spring AI tool callbacks to MCP asynchronous tool specification.
	 * <p>
	 * This method processes multiple tool callbacks in bulk, converting each one to its
	 * corresponding MCP tool specification while adding asynchronous execution
	 * capabilities. The resulting specifications will execute their tools on a bounded
	 * elastic scheduler.
	 * @param toolCallbacks the list of tool callbacks to convert
	 * @return a list of MCP asynchronous tool specifications
	 */
	public static List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecifications(
			List<ToolCallback> toolCallbacks) {
		return toolCallbacks.stream().map(McpToolUtils::toAsyncToolSpecification).collect(Collectors.toList());
	}

	/**
	 * Convenience method to convert a variable number of tool callbacks to MCP
	 * asynchronous tool specification.
	 * <p>
	 * This is a varargs wrapper around {@link #toAsyncToolSpecifications(List)} for
	 * easier usage when working with individual callbacks.
	 * @param toolCallbacks the tool callbacks to convert
	 * @return a list of MCP asynchronous tool specifications
	 * @see #toAsyncToolSpecifications(List)
	 */
	public static List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecifications(
			ToolCallback... toolCallbacks) {
		return toAsyncToolSpecifications(Arrays.asList(toolCallbacks));
	}

	/**
	 * Converts a Spring AI tool callback to an MCP asynchronous tool specification.
	 * <p>
	 * This method enables Spring AI tools to be exposed as asynchronous MCP tools that
	 * can be discovered and invoked by language models. The conversion process:
	 * <ul>
	 * <li>First converts the callback to a synchronous specification</li>
	 * <li>Wraps the synchronous execution in a reactive Mono</li>
	 * <li>Configures execution on a bounded elastic scheduler for non-blocking
	 * operation</li>
	 * </ul>
	 * <p>
	 * The resulting async specification will:
	 * <ul>
	 * <li>Execute the tool without blocking the calling thread</li>
	 * <li>Handle errors and results asynchronously</li>
	 * <li>Provide backpressure through Project Reactor</li>
	 * </ul>
	 * @param toolCallback the Spring AI tool callback to convert
	 * @return an MCP asynchronous tool specification that wraps the tool callback
	 * @see McpServerFeatures.AsyncToolSpecification
	 * @see Mono
	 * @see Schedulers#boundedElastic()
	 */
	public static McpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(ToolCallback toolCallback) {
		return toAsyncToolSpecification(toolCallback, null);
	}

	/**
	 * Converts a Spring AI tool callback to an MCP asynchronous tool specification with a
	 * specific MIME type for the output content.
	 * <p>
	 * This is an overloaded version of {@link #toAsyncToolSpecification(ToolCallback)}
	 * that allows specifying the MIME type of the tool's output content.
	 * @param toolCallback the Spring AI tool callback to convert
	 * @param mimeType the MIME type of the output content, or null if not applicable
	 * @return an MCP asynchronous tool specification that wraps the tool callback
	 * @see #toAsyncToolSpecification(ToolCallback)
	 */
	public static McpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(ToolCallback toolCallback,
			MimeType mimeType) {

		McpServerFeatures.SyncToolSpecification syncToolSpecification = toSyncToolSpecification(toolCallback, mimeType);
		
		return new AsyncToolSpecification(syncToolSpecification.getTool(),
				(exchange, map) -> Mono
					.fromCallable(() -> syncToolSpecification.getCall().apply(new McpSyncServerExchange(exchange), map))
					.subscribeOn(Schedulers.boundedElastic()));
	}

	/**
	 * Convenience method to retrieve tool callbacks from multiple synchronous MCP clients.
	 * @param mcpClients the MCP clients to retrieve callbacks from
	 * @return a list of tool callbacks from all the provided clients
	 */
	public static List<ToolCallback> getToolCallbacksFromSyncClients(McpSyncClient... mcpClients) {
		return getToolCallbacksFromSyncClients(Arrays.asList(mcpClients));
	}

	/**
	 * Retrieves tool callbacks from a list of synchronous MCP clients.
	 * <p>
	 * This method aggregates tool callbacks from multiple MCP clients into a single list,
	 * which can then be used to expose all their tools to a language model.
	 * @param mcpClients the list of MCP clients to retrieve callbacks from
	 * @return a list of tool callbacks from all the provided clients
	 */
	public static List<ToolCallback> getToolCallbacksFromSyncClients(List<McpSyncClient> mcpClients) {
		if (CollectionUtils.isEmpty(mcpClients)) {
			return Collections.emptyList();
		}

        return new ArrayList<>(new SyncMcpToolCallbackProvider(mcpClients).getToolCallbacks());
	}

	/**
	 * Convenience method to retrieve tool callbacks from multiple asynchronous MCP clients.
	 * @param asyncMcpClients the asynchronous MCP clients to retrieve callbacks from
	 * @return a list of tool callbacks from all the provided clients
	 */
	public static List<ToolCallback> getToolCallbacksFromAsyncClients(McpAsyncClient... asyncMcpClients) {
		return getToolCallbacksFromAsyncClients(Arrays.asList(asyncMcpClients));
	}

	/**
	 * Retrieves tool callbacks from a list of asynchronous MCP clients.
	 * @param asyncMcpClients the list of asynchronous MCP clients to retrieve callbacks from
	 * @return a list of tool callbacks from all the provided clients
	 */
	public static List<ToolCallback> getToolCallbacksFromAsyncClients(List<McpAsyncClient> asyncMcpClients) {
		if (CollectionUtils.isEmpty(asyncMcpClients)) {
			return Collections.emptyList();
		}

        return new ArrayList<>(new AsyncMcpToolCallbackProvider(asyncMcpClients).getToolCallbacks());
	}

	/**
	 * Class for handling base64 data in image responses.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static final class Base64Wrapper {
		@JsonAlias("mimetype") 
		@Nullable 
		private final MimeType mimeType;
		
		@JsonAlias({"base64", "b64", "imageData"}) 
		@Nullable 
		private final String data;

		private Base64Wrapper(@Nullable MimeType mimeType, @Nullable String data) {
			this.mimeType = mimeType;
			this.data = data;
		}

		@Nullable
		public MimeType getMimeType() {
			return mimeType;
		}

		@Nullable
		public String getData() {
			return data;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Base64Wrapper that = (Base64Wrapper) o;

			if (!Objects.equals(mimeType, that.mimeType)) return false;
			return Objects.equals(data, that.data);
		}

		@Override
		public int hashCode() {
			int result = mimeType != null ? mimeType.hashCode() : 0;
			result = 31 * result + (data != null ? data.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "Base64Wrapper{" +
					"mimeType=" + mimeType +
					", data='" + data + '\'' +
					'}';
		}
	}

} 
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

import com.mycompany.aigw.client.McpAsyncClient;
import com.mycompany.aigw.sdk.tool.ToolCallback;
import com.mycompany.aigw.sdk.tool.ToolContext;
import com.mycompany.aigw.sdk.tool.definition.DefaultToolDefinition;
import com.mycompany.aigw.sdk.tool.definition.ToolDefinition;
import com.mycompany.aigw.sdk.util.ModelOptionsUtils;
import com.mycompany.aigw.spec.McpSchema.CallToolRequest;
import com.mycompany.aigw.spec.McpSchema.Tool;

import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link ToolCallback} that adapts MCP tools to Spring AI's tool
 * interface with asynchronous execution support.
 * <p>
 * This class acts as a bridge between the Model Context Protocol (MCP) and Spring AI's
 * tool system, allowing MCP tools to be used seamlessly within Spring AI applications.
 * It:
 * <ul>
 * <li>Converts MCP tool definitions to Spring AI tool definitions</li>
 * <li>Handles the asynchronous execution of tool calls through the MCP client</li>
 * <li>Manages JSON serialization/deserialization of tool inputs and outputs</li>
 * </ul>
 * <p>
 * Example usage: <pre>{@code
 * McpAsyncClient mcpClient = // obtain MCP client
 * Tool mcpTool = // obtain MCP tool definition
 * ToolCallback callback = new AsyncMcpToolCallback(mcpClient, mcpTool);
 *
 * // Use the tool through Spring AI's interfaces
 * ToolDefinition definition = callback.getToolDefinition();
 * String result = callback.call("{\"param\": \"value\"}");
 * }</pre>
 *
 * @author Christian Tzolov
 * @see ToolCallback
 * @see McpAsyncClient
 * @see Tool
 */
public class AsyncMcpToolCallback implements ToolCallback {

	private final McpAsyncClient asyncMcpClient;

	private final Tool tool;

	/**
	 * Creates a new {@code AsyncMcpToolCallback} instance.
	 * @param mcpClient the MCP client to use for tool execution
	 * @param tool the MCP tool definition to adapt
	 */
	public AsyncMcpToolCallback(McpAsyncClient mcpClient, Tool tool) {
		this.asyncMcpClient = mcpClient;
		this.tool = tool;
	}

	/**
	 * Returns a Spring AI tool definition adapted from the MCP tool.
	 * <p>
	 * The tool definition includes:
	 * <ul>
	 * <li>The tool's name from the MCP definition</li>
	 * <li>The tool's description from the MCP definition</li>
	 * <li>The input schema converted to JSON format</li>
	 * </ul>
	 * @return the Spring AI tool definition
	 */
	@Override
	public ToolDefinition getToolDefinition() {
		return DefaultToolDefinition.builder()
			.name(McpToolUtils.prefixedToolName(this.asyncMcpClient.getClientInfo().getName(), this.tool.getName()))
			.description(this.tool.getDescription())
			.inputSchema(ModelOptionsUtils.toJsonString(this.tool.getInputSchema()))
			.build();
	}

	/**
	 * Executes the tool with the provided input asynchronously.
	 * <p>
	 * This method:
	 * <ol>
	 * <li>Converts the JSON input string to a map of arguments</li>
	 * <li>Calls the tool through the MCP client asynchronously</li>
	 * <li>Converts the tool's response content to a JSON string</li>
	 * </ol>
	 * @param functionInput the tool input as a JSON string
	 * @return the tool's response as a JSON string
	 */
	@Override
	public String call(String functionInput) {
		Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
		// Note that we use the original tool name here, not the adapted one from
		// getToolDefinition
		return Objects.requireNonNull(this.asyncMcpClient.callTool(new CallToolRequest(this.tool.getName(), arguments)).<String>handle((response, sink) -> {
            if (response.getIsError() != null && response.getIsError()) {
                sink.error(new IllegalStateException("Error calling tool: " + response.getContent()));
                return;
            }
            sink.next(ModelOptionsUtils.toJsonString(response.getContent()));
        }).block());
	}

	@Override
	public String call(String toolArguments, ToolContext toolContext) {
		// ToolContext is not supported by the MCP tools
		return this.call(toolArguments);
	}

} 
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

package com.mycompany.aigw.mcp.client.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration properties for Server-Sent Events (SSE) based MCP client connections.
 *
 * <p>
 * These properties allow configuration of multiple named SSE connections to MCP servers.
 * Each connection is configured with a URL endpoint for SSE communication.
 *
 * <p>
 * Example configuration: <pre>
 * spring.ai.mcp.client.sse:
 *   connections:
 *     server1:
 *       url: http://localhost:8080/events
 *     server2:
 *       url: http://otherserver:8081/events
 * </pre>
 *
 * @author Christian Tzolov
 * @since 1.0.0
 * @see SseParameters
 */
@ConfigurationProperties(McpSseClientProperties.CONFIG_PREFIX)
public class McpSseClientProperties {

	public static final String CONFIG_PREFIX = "spring.ai.mcp.client.sse";

	/**
	 * Map of named SSE connection configurations.
	 * <p>
	 * The key represents the connection name, and the value contains the SSE parameters
	 * for that connection.
	 */
	private final Map<String, SseParameters> connections = new HashMap<>();

	/**
	 * Returns the map of configured SSE connections.
	 * @return map of connection names to their SSE parameters
	 */
	public Map<String, SseParameters> getConnections() {
		return this.connections;
	}

	/**
	 * Parameters for configuring an SSE connection to an MCP server.
	 */
	public static class SseParameters {
		
		private final String url;
		private final String sseEndpoint;
		
		/**
		 * Constructor for the SSE parameters.
		 * @param url the URL endpoint for SSE communication with the MCP server
		 * @param sseEndpoint the SSE endpoint for the MCP server
		 */
		public SseParameters(String url, String sseEndpoint) {
			this.url = url;
			this.sseEndpoint = sseEndpoint;
		}
		
		/**
		 * Get the URL endpoint for SSE communication with the MCP server.
		 * @return the URL endpoint
		 */
		public String url() {
			return url;
		}
		
		/**
		 * Get the SSE endpoint for the MCP server.
		 * @return the SSE endpoint
		 */
		public String sseEndpoint() {
			return sseEndpoint;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SseParameters that = (SseParameters) o;
			return Objects.equals(url, that.url) && 
				   Objects.equals(sseEndpoint, that.sseEndpoint);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(url, sseEndpoint);
		}
		
		@Override
		public String toString() {
			return "SseParameters[url=" + url + ", sseEndpoint=" + sseEndpoint + "]";
		}
	}
} 
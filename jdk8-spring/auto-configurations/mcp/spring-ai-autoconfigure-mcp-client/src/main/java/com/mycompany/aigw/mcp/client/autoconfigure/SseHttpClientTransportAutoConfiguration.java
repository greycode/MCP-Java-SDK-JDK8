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

package com.mycompany.aigw.mcp.client.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.aigw.client.McpSyncClient;
import com.mycompany.aigw.client.transport.HttpClientSseClientTransport;
import com.mycompany.aigw.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import com.mycompany.aigw.mcp.client.autoconfigure.properties.McpSseClientProperties;
import com.mycompany.aigw.mcp.client.autoconfigure.properties.McpSseClientProperties.SseParameters;
import com.mycompany.aigw.spec.McpSchema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for Server-Sent Events (SSE) HTTP client transport in the Model
 * Context Protocol (MCP).
 *
 * <p>
 * This configuration class sets up the necessary beans for SSE-based HTTP client
 * transport when WebFlux is not available. It provides HTTP client-based SSE transport
 * implementation for MCP client communication.
 *
 * <p>
 * The configuration is activated after the WebFlux SSE transport auto-configuration to
 * ensure proper fallback behavior when WebFlux is not available.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Creates HTTP client-based SSE transports for configured MCP server connections
 * <li>Configures ObjectMapper for JSON serialization/deserialization
 * <li>Supports multiple named server connections with different URLs
 * </ul>
 *
 * @see HttpClientSseClientTransport
 * @see McpSseClientProperties
 */
@AutoConfiguration(after = SseWebFluxTransportAutoConfiguration.class)
@ConditionalOnClass({ McpSchema.class, McpSyncClient.class })
@ConditionalOnMissingClass("com.mycompany.aigw.client.transport.WebFluxSseClientTransport")
@EnableConfigurationProperties({ McpSseClientProperties.class, McpClientCommonProperties.class })
@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class SseHttpClientTransportAutoConfiguration {

	/**
	 * Creates a list of HTTP client-based SSE transports for MCP communication.
	 *
	 * <p>
	 * Each transport is configured with:
	 * <ul>
	 * <li>A new HttpClient instance
	 * <li>Server URL from properties
	 * <li>ObjectMapper for JSON processing
	 * </ul>
	 * @param sseProperties the SSE client properties containing server configurations
	 * @param objectMapperProvider the provider for ObjectMapper or a new instance if not
	 * available
	 * @return list of named MCP transports
	 */
	@Bean
	public List<NamedClientMcpTransport> mcpHttpClientTransports(McpSseClientProperties sseProperties,
			ObjectProvider<ObjectMapper> objectMapperProvider) {

		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);

		List<NamedClientMcpTransport> sseTransports = new ArrayList<>();

		for (Map.Entry<String, SseParameters> serverParameters : sseProperties.getConnections().entrySet()) {

			String baseUrl = serverParameters.getValue().url();
			String sseEndpoint = serverParameters.getValue().sseEndpoint() != null
					? serverParameters.getValue().sseEndpoint() : "/sse";
			
			HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl)
				.sseEndpoint(sseEndpoint)
				.objectMapper(objectMapper)
				.build();
				
			sseTransports.add(new NamedClientMcpTransport(serverParameters.getKey(), transport));
		}

		return sseTransports;
	}

} 
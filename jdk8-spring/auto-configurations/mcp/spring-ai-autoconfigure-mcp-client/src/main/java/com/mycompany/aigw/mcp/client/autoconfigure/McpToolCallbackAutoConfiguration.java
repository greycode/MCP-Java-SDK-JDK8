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

import com.mycompany.aigw.client.McpAsyncClient;
import com.mycompany.aigw.client.McpSyncClient;
import com.mycompany.aigw.mcp.AsyncMcpToolCallbackProvider;
import com.mycompany.aigw.mcp.SyncMcpToolCallbackProvider;
import com.mycompany.aigw.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import com.mycompany.aigw.sdk.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto-configuration for MCP tool callbacks in the Spring AI framework.
 *
 * <p>
 * This configuration class sets up tool callbacks for MCP clients, enabling integration
 * with Spring AI's tool execution framework. It is automatically activated when both MCP
 * clients and tool callbacks are enabled in properties.
 */
@AutoConfiguration(after = { McpClientAutoConfiguration.class })
@EnableConfigurationProperties(McpClientCommonProperties.class)
@Conditional(McpToolCallbackAutoConfiguration.McpToolCallbackAutoConfigurationCondition.class)
public class McpToolCallbackAutoConfiguration {

	/**
	 * Creates tool callbacks for all configured MCP sync clients.
	 *
	 * <p>
	 * These callbacks enable integration with Spring AI's tool execution framework,
	 * allowing MCP tools to be used as part of AI interactions.
	 * @param syncMcpClients provider of MCP sync clients
	 * @return list of tool callbacks for MCP integration
	 */
	@Bean
	@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public ToolCallbackProvider mcpToolCallbacks(ObjectProvider<List<McpSyncClient>> syncMcpClients) {
		List<McpSyncClient> mcpClients = syncMcpClients.stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());
		return new SyncMcpToolCallbackProvider(mcpClients);
	}

	/**
	 * Creates tool callbacks for all configured MCP async clients.
	 *
	 * <p>
	 * These callbacks enable integration with Spring AI's tool execution framework,
	 * allowing MCP tools to be used as part of AI interactions with asynchronous processing.
	 * @param mcpClientsProvider provider of MCP async clients
	 * @return async tool callbacks for MCP integration
	 */
	@Bean
	@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public ToolCallbackProvider mcpAsyncToolCallbacks(ObjectProvider<List<McpAsyncClient>> mcpClientsProvider) {
		List<McpAsyncClient> mcpClients = mcpClientsProvider.stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());
		return new AsyncMcpToolCallbackProvider(mcpClients);
	}

	/**
	 * Condition class that ensures both MCP client and tool callback features are enabled.
	 */
	public static class McpToolCallbackAutoConfigurationCondition extends AllNestedConditions {

		public McpToolCallbackAutoConfigurationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
				matchIfMissing = true)
		static class McpAutoConfigEnabled {
		}

		@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX + ".toolcallback", name = "enabled",
				havingValue = "true", matchIfMissing = false)
		static class ToolCallbackProviderEnabled {
		}
	}
} 
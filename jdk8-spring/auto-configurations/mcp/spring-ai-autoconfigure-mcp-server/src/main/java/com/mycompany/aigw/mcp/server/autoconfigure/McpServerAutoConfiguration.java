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

package com.mycompany.aigw.mcp.server.autoconfigure;

import com.mycompany.aigw.mcp.McpToolUtils;
import com.mycompany.aigw.sdk.tool.ToolCallback;
import com.mycompany.aigw.sdk.tool.ToolCallbackProvider;
import com.mycompany.aigw.server.*;
import com.mycompany.aigw.server.McpServer.AsyncSpecification;
import com.mycompany.aigw.server.McpServer.SyncSpecification;
import com.mycompany.aigw.server.McpServerFeatures.*;
import com.mycompany.aigw.server.transport.StdioServerTransportProvider;
import com.mycompany.aigw.spec.McpSchema;
import com.mycompany.aigw.spec.McpSchema.Implementation;
import com.mycompany.aigw.spec.McpServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.log.LogAccessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Model Context Protocol (MCP)
 * Server.
 * <p>
 * This configuration class sets up the core MCP server components with support for both
 * synchronous and asynchronous operation modes. The server type is controlled through the
 * {@code spring.ai.mcp.server.type} property, defaulting to SYNC mode.
 * <p>
 * Core features and capabilities include:
 * <ul>
 * <li>Tools: Extensible tool registration system supporting both sync and async
 * execution</li>
 * <li>Resources: Static and dynamic resource management with optional change
 * notifications</li>
 * <li>Prompts: Configurable prompt templates with change notification support</li>
 * <li>Transport: Flexible transport layer with built-in support for:
 * <ul>
 * <li>STDIO (default): Standard input/output based communication</li>
 * <li>WebMvc: HTTP-based transport when Spring MVC is available</li>
 * <li>WebFlux: Reactive transport when Spring WebFlux is available</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * The configuration is activated when:
 * <ul>
 * <li>The required MCP classes ({@link McpSchema} and {@link McpSyncServer}) are on the
 * classpath</li>
 * <li>The {@code spring.ai.mcp.server.enabled} property is true (default)</li>
 * </ul>
 * <p>
 * Server configuration is managed through {@link McpServerProperties} with support for:
 * <ul>
 * <li>Server identification (name, version)</li>
 * <li>Transport selection</li>
 * <li>Change notification settings for tools, resources, and prompts</li>
 * <li>Sync/Async operation mode selection</li>
 * </ul>
 * <p>
 * WebMvc transport support is provided separately by
 * {@link McpWebMvcServerAutoConfiguration}.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 * @see McpServerProperties
 * @see McpWebMvcServerAutoConfiguration
 * @see McpWebFluxServerAutoConfiguration
 * @see ToolCallback
 */
@AutoConfiguration(after = { McpWebMvcServerAutoConfiguration.class, McpWebFluxServerAutoConfiguration.class })
@ConditionalOnClass({ McpSchema.class, McpSyncServer.class })
@EnableConfigurationProperties(McpServerProperties.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class McpServerAutoConfiguration {

	private static final LogAccessor logger = new LogAccessor(McpServerAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public McpServerTransportProvider stdioServerTransport() {
		return new StdioServerTransportProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public McpSchema.ServerCapabilities.Builder capabilitiesBuilder() {
		return McpSchema.ServerCapabilities.builder();
	}

	@Bean
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public List<McpServerFeatures.SyncToolSpecification> syncTools(ObjectProvider<List<ToolCallback>> toolCalls,
			List<ToolCallback> toolCallbacksList, McpServerProperties serverProperties) {

		List<ToolCallback> tools = toolCalls.stream().flatMap(List::stream).collect(Collectors.toList());

		if (!CollectionUtils.isEmpty(toolCallbacksList)) {
			tools.addAll(toolCallbacksList);
		}

		return this.toSyncToolSpecifications(tools, serverProperties);
	}

	private List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecifications(List<ToolCallback> tools,
			McpServerProperties serverProperties) {

		// De-duplicate tools by their name, keeping the first occurrence of each tool
		// name
		Map<String, ToolCallback> toolMap = tools.stream()
			.collect(Collectors.toMap(
				tool -> tool.getToolDefinition().name(), // Key: tool name
				tool -> tool, // Value: the tool itself
				(existing, replacement) -> existing // On duplicate key, keep the existing tool
			));

		return toolMap.values().stream()
			.map(tool -> {
				String toolName = tool.getToolDefinition().name();
				MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
						? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
				return McpToolUtils.toSyncToolSpecification(tool, mimeType);
			})
			.collect(Collectors.toList());
	}

	@Bean
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public McpSyncServer mcpSyncServer(McpServerTransportProvider transportProvider,
			McpSchema.ServerCapabilities.Builder capabilitiesBuilder, McpServerProperties serverProperties,
			ObjectProvider<List<SyncToolSpecification>> tools,
			ObjectProvider<List<SyncResourceSpecification>> resources,
			ObjectProvider<List<SyncPromptSpecification>> prompts,
			ObjectProvider<List<SyncCompletionSpecification>> completions,
			ObjectProvider<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers,
			List<ToolCallbackProvider> toolCallbackProvider) {

		McpSchema.Implementation serverInfo = new Implementation(serverProperties.getName(),
				serverProperties.getVersion());

		// Create the server with both tool and resource capabilities
		SyncSpecification serverBuilder = McpServer.sync(transportProvider).serverInfo(serverInfo);

		List<SyncToolSpecification> toolSpecifications = tools.stream().flatMap(List::stream).collect(Collectors.toList());

		List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
			.map(pr -> Arrays.asList(pr.getToolCallbacks()))
			.flatMap(List::stream)
			.filter(fc -> fc instanceof ToolCallback)
			.map(fc -> (ToolCallback) fc)
			.collect(Collectors.toList());

		toolSpecifications.addAll(this.toSyncToolSpecifications(providerToolCallbacks, serverProperties));

		if (!CollectionUtils.isEmpty(toolSpecifications)) {
			serverBuilder.tools(toolSpecifications);
			capabilitiesBuilder.tools(serverProperties.isToolChangeNotification());
			logger.info("Registered tools: " + toolSpecifications.size() + ", notification: "
					+ serverProperties.isToolChangeNotification());
		}

		List<SyncResourceSpecification> resourceSpecifications = resources.stream().flatMap(List::stream).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(resourceSpecifications)) {
			serverBuilder.resources(resourceSpecifications);
			capabilitiesBuilder.resources(false, serverProperties.isResourceChangeNotification());
			logger.info("Registered resources: " + resourceSpecifications.size() + ", notification: "
					+ serverProperties.isResourceChangeNotification());
		}

		List<SyncPromptSpecification> promptSpecifications = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(promptSpecifications)) {
			serverBuilder.prompts(promptSpecifications);
			capabilitiesBuilder.prompts(serverProperties.isPromptChangeNotification());
			logger.info("Registered prompts: " + promptSpecifications.size() + ", notification: "
					+ serverProperties.isPromptChangeNotification());
		}

		List<SyncCompletionSpecification> completionSpecifications = completions.stream()
			.flatMap(List::stream)
			.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(completionSpecifications)) {
			serverBuilder.completions(completionSpecifications);
			capabilitiesBuilder.completions();
			logger.info("Registered completions: " + completionSpecifications.size());
		}

		rootsChangeConsumers.ifAvailable(consumer -> {
			serverBuilder.rootsChangeHandler(consumer);
			logger.info("Registered roots change consumer");
		});

		serverBuilder.capabilities(capabilitiesBuilder.build());

		serverBuilder.instructions(serverProperties.getInstructions());

		return serverBuilder.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public List<McpServerFeatures.AsyncToolSpecification> asyncTools(ObjectProvider<List<ToolCallback>> toolCalls,
			List<ToolCallback> toolCallbackList, McpServerProperties serverProperties) {

		List<ToolCallback> tools = toolCalls.stream().flatMap(List::stream).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(toolCallbackList)) {
			tools.addAll(toolCallbackList);
		}

		return this.toAsyncToolSpecification(tools, serverProperties);
	}

	private List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecification(List<ToolCallback> tools,
			McpServerProperties serverProperties) {

		// De-duplicate tools by their name, keeping the first occurrence of each tool
		// name
		Map<String, ToolCallback> toolMap = tools.stream()
			.collect(Collectors.toMap(
				tool -> tool.getToolDefinition().name(), // Key: tool name
				tool -> tool, // Value: the tool itself
				(existing, replacement) -> existing // On duplicate key, keep the existing tool
			));

		return toolMap.values().stream()
			.map(tool -> {
				String toolName = tool.getToolDefinition().name();
				MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
						? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
				return McpToolUtils.toAsyncToolSpecification(tool, mimeType);
			})
			.collect(Collectors.toList());
	}

	@Bean
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public McpAsyncServer mcpAsyncServer(McpServerTransportProvider transportProvider,
			McpSchema.ServerCapabilities.Builder capabilitiesBuilder, McpServerProperties serverProperties,
			ObjectProvider<List<AsyncToolSpecification>> tools,
			ObjectProvider<List<AsyncResourceSpecification>> resources,
			ObjectProvider<List<AsyncPromptSpecification>> prompts,
			ObjectProvider<List<AsyncCompletionSpecification>> completions,
			ObjectProvider<BiConsumer<McpAsyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumer,
			List<ToolCallbackProvider> toolCallbackProvider) {

		McpSchema.Implementation serverInfo = new Implementation(serverProperties.getName(),
				serverProperties.getVersion());

		// Create the server with both tool and resource capabilities
		AsyncSpecification serverBuilder = McpServer.async(transportProvider).serverInfo(serverInfo);

		List<AsyncToolSpecification> toolSpecifications = tools.stream().flatMap(List::stream).collect(Collectors.toList());

		List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
			.map(pr -> Arrays.asList(pr.getToolCallbacks()))
			.flatMap(List::stream)
			.filter(fc -> fc instanceof ToolCallback)
			.map(fc -> (ToolCallback) fc)
			.collect(Collectors.toList());

		toolSpecifications.addAll(this.toAsyncToolSpecification(providerToolCallbacks, serverProperties));

		if (!CollectionUtils.isEmpty(toolSpecifications)) {
			serverBuilder.tools(toolSpecifications);
			capabilitiesBuilder.tools(serverProperties.isToolChangeNotification());
			logger.info("Registered tools: " + toolSpecifications.size() + ", notification: "
					+ serverProperties.isToolChangeNotification());
		}

		List<AsyncResourceSpecification> resourceSpecifications = resources.stream().flatMap(List::stream).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(resourceSpecifications)) {
			serverBuilder.resources(resourceSpecifications);
			capabilitiesBuilder.resources(false, serverProperties.isResourceChangeNotification());
			logger.info("Registered resources: " + resourceSpecifications.size() + ", notification: "
					+ serverProperties.isResourceChangeNotification());
		}

		List<AsyncPromptSpecification> promptSpecifications = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(promptSpecifications)) {
			serverBuilder.prompts(promptSpecifications);
			capabilitiesBuilder.prompts(serverProperties.isPromptChangeNotification());
			logger.info("Registered prompts: " + promptSpecifications.size() + ", notification: "
					+ serverProperties.isPromptChangeNotification());
		}

		List<AsyncCompletionSpecification> completionSpecifications = completions.stream()
			.flatMap(List::stream)
			.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(completionSpecifications)) {
			serverBuilder.completions(completionSpecifications);
			capabilitiesBuilder.completions();
			logger.info("Registered completions: " + completionSpecifications.size());
		}

		rootsChangeConsumer.ifAvailable(consumer -> {
			serverBuilder.rootsChangeHandler((exchange, roots) -> Mono.fromRunnable(() -> consumer.accept(exchange, roots)));
			logger.info("Registered roots change consumer");
		});

		serverBuilder.capabilities(capabilitiesBuilder.build());

		serverBuilder.instructions(serverProperties.getInstructions());

		return serverBuilder.build();
	}
} 
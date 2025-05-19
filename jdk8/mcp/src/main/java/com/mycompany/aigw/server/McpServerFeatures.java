/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.server;

import com.mycompany.aigw.spec.McpSchema;
import com.mycompany.aigw.util.Assert;
import com.mycompany.aigw.util.Utils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * MCP server features specification that a particular server can choose to support.
 *
 * @author Dariusz Jędrzejczyk
 * @author Jihoon Kim
 */
public class McpServerFeatures {

	/**
	 * Asynchronous server features specification.
	 */
	public static class Async {

		private final McpSchema.Implementation serverInfo;

		private final McpSchema.ServerCapabilities serverCapabilities;

		private final List<McpServerFeatures.AsyncToolSpecification> tools;

		private final Map<String, AsyncResourceSpecification> resources;

		private final List<McpSchema.ResourceTemplate> resourceTemplates;

		private final Map<String, McpServerFeatures.AsyncPromptSpecification> prompts;

		private final Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions;

		private final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers;

		private final String instructions;

		/**
		 * Create an instance and validate the arguments.
		 * @param serverInfo The server implementation details
		 * @param serverCapabilities The server capabilities
		 * @param tools The list of tool specifications
		 * @param resources The map of resource specifications
		 * @param resourceTemplates The list of resource templates
		 * @param prompts The map of prompt specifications
		 * @param completions The map of completion specifications
		 * @param rootsChangeConsumers The list of consumers that will be notified when
		 * the roots list changes
		 * @param instructions The server instructions text
		 */
		public Async(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
				List<McpServerFeatures.AsyncToolSpecification> tools, Map<String, AsyncResourceSpecification> resources,
				List<McpSchema.ResourceTemplate> resourceTemplates,
				Map<String, McpServerFeatures.AsyncPromptSpecification> prompts,
				Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions,
				List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
				String instructions) {

			Assert.notNull(serverInfo, "Server info must not be null");

			this.serverInfo = serverInfo;
			this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
					: new McpSchema.ServerCapabilities(null, // completions
							null, // experimental
							new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
																					// logging
																					// by
																					// default
							!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
							!Utils.isEmpty(resources)
									? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
							!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

			this.tools = (tools != null) ? tools : new ArrayList<>();
			this.resources = (resources != null) ? resources : new HashMap<>();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates
					: new ArrayList<>();
			this.prompts = (prompts != null) ? prompts
					: new HashMap<>();
			this.completions = (completions != null) ? completions
					: new HashMap<>();
			this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers
					: new ArrayList<>();
			this.instructions = instructions;
		}

		/**
		 * Convert a synchronous specification into an asynchronous one and provide
		 * blocking code offloading to prevent accidental blocking of the non-blocking
		 * transport.
		 * @param syncSpec a potentially blocking, synchronous specification.
		 * @return a specification which is protected from blocking calls specified by the
		 * user.
		 */
		public static Async fromSync(Sync syncSpec) {
			List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
			for (McpServerFeatures.SyncToolSpecification tool : syncSpec.getTools()) {
				tools.add(AsyncToolSpecification.fromSync(tool));
			}

			Map<String, AsyncResourceSpecification> resources = new HashMap<>();
			for (Map.Entry<String, SyncResourceSpecification> entry : syncSpec.getResources().entrySet()) {
				resources.put(entry.getKey(), AsyncResourceSpecification.fromSync(entry.getValue()));
			}

			Map<String, AsyncPromptSpecification> prompts = new HashMap<>();
			for (Map.Entry<String, SyncPromptSpecification> entry : syncSpec.getPrompts().entrySet()) {
				prompts.put(entry.getKey(), AsyncPromptSpecification.fromSync(entry.getValue()));
			}

			Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions = new HashMap<>();
			for (Map.Entry<McpSchema.CompleteReference, SyncCompletionSpecification> entry : syncSpec.getCompletions()
					.entrySet()) {
				completions.put(entry.getKey(), AsyncCompletionSpecification.fromSync(entry.getValue()));
			}

			List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers = new ArrayList<>();

			for (final BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> rootChangeConsumer : syncSpec
					.getRootsChangeConsumers()) {
				rootChangeConsumers.add((exchange, list) -> Mono
						.<Void>fromRunnable(() -> rootChangeConsumer.accept(new McpSyncServerExchange(exchange), list))
						.subscribeOn(Schedulers.boundedElastic()));
			}

			return new Async(syncSpec.getServerInfo(), syncSpec.getServerCapabilities(), tools, resources,
					syncSpec.getResourceTemplates(), prompts, completions, rootChangeConsumers,
					syncSpec.getInstructions());
		}

		public McpSchema.Implementation getServerInfo() {
			return serverInfo;
		}

		public McpSchema.ServerCapabilities getServerCapabilities() {
			return serverCapabilities;
		}

		public List<McpServerFeatures.AsyncToolSpecification> getTools() {
			return tools;
		}

		public Map<String, AsyncResourceSpecification> getResources() {
			return resources;
		}

		public List<McpSchema.ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		public Map<String, McpServerFeatures.AsyncPromptSpecification> getPrompts() {
			return prompts;
		}

		public Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> getCompletions() {
			return completions;
		}

		public List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> getRootsChangeConsumers() {
			return rootsChangeConsumers;
		}

		public String getInstructions() {
			return instructions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Async async = (Async) o;
			return Objects.equals(serverInfo, async.serverInfo)
					&& Objects.equals(serverCapabilities, async.serverCapabilities)
					&& Objects.equals(tools, async.tools) && Objects.equals(resources, async.resources)
					&& Objects.equals(resourceTemplates, async.resourceTemplates)
					&& Objects.equals(prompts, async.prompts) && Objects.equals(completions, async.completions)
					&& Objects.equals(rootsChangeConsumers, async.rootsChangeConsumers)
					&& Objects.equals(instructions, async.instructions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts,
					completions, rootsChangeConsumers, instructions);
		}

		@Override
		public String toString() {
			return "Async{" + "serverInfo=" + serverInfo + ", serverCapabilities=" + serverCapabilities + ", tools="
					+ tools + ", resources=" + resources + ", resourceTemplates=" + resourceTemplates + ", prompts="
					+ prompts + ", completions=" + completions + ", rootsChangeConsumers=" + rootsChangeConsumers
					+ ", instructions='" + instructions + '\'' + '}';
		}

	}

	/**
	 * Synchronous server features specification.
	 */
	public static class Sync {

		private final McpSchema.Implementation serverInfo;

		private final McpSchema.ServerCapabilities serverCapabilities;

		private final List<McpServerFeatures.SyncToolSpecification> tools;

		private final Map<String, McpServerFeatures.SyncResourceSpecification> resources;

		private final List<McpSchema.ResourceTemplate> resourceTemplates;

		private final Map<String, McpServerFeatures.SyncPromptSpecification> prompts;

		private final Map<McpSchema.CompleteReference, McpServerFeatures.SyncCompletionSpecification> completions;

		private final List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers;

		private final String instructions;

		/**
		 * Create an instance and validate the arguments.
		 * @param serverInfo The server implementation details
		 * @param serverCapabilities The server capabilities
		 * @param tools The list of tool specifications
		 * @param resources The map of resource specifications
		 * @param resourceTemplates The list of resource templates
		 * @param prompts The map of prompt specifications
		 * @param completions The map of completion specifications
		 * @param rootsChangeConsumers The list of consumers that will be notified when
		 * the roots list changes
		 * @param instructions The server instructions text
		 */
		public Sync(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
				List<McpServerFeatures.SyncToolSpecification> tools,
				Map<String, McpServerFeatures.SyncResourceSpecification> resources,
				List<McpSchema.ResourceTemplate> resourceTemplates,
				Map<String, McpServerFeatures.SyncPromptSpecification> prompts,
				Map<McpSchema.CompleteReference, McpServerFeatures.SyncCompletionSpecification> completions,
				List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers,
				String instructions) {

			Assert.notNull(serverInfo, "Server info must not be null");

			this.serverInfo = serverInfo;
			this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
					: new McpSchema.ServerCapabilities(null, // completions
							null, // experimental
							new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
																					// logging
																					// by
																					// default
							!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
							!Utils.isEmpty(resources)
									? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
							!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

			this.tools = (tools != null) ? tools : new ArrayList<>();
			this.resources = (resources != null) ? resources : new HashMap<>();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : new ArrayList<>();
			this.prompts = (prompts != null) ? prompts : new HashMap<>();
			this.completions = (completions != null) ? completions : new HashMap<>();
			this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : new ArrayList<>();
			this.instructions = instructions;
		}

		public McpSchema.Implementation getServerInfo() {
			return serverInfo;
		}

		public McpSchema.ServerCapabilities getServerCapabilities() {
			return serverCapabilities;
		}

		public List<McpServerFeatures.SyncToolSpecification> getTools() {
			return tools;
		}

		public Map<String, McpServerFeatures.SyncResourceSpecification> getResources() {
			return resources;
		}

		public List<McpSchema.ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		public Map<String, McpServerFeatures.SyncPromptSpecification> getPrompts() {
			return prompts;
		}

		public Map<McpSchema.CompleteReference, McpServerFeatures.SyncCompletionSpecification> getCompletions() {
			return completions;
		}

		public List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> getRootsChangeConsumers() {
			return rootsChangeConsumers;
		}

		public String getInstructions() {
			return instructions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Sync sync = (Sync) o;
			return Objects.equals(serverInfo, sync.serverInfo)
					&& Objects.equals(serverCapabilities, sync.serverCapabilities) && Objects.equals(tools, sync.tools)
					&& Objects.equals(resources, sync.resources)
					&& Objects.equals(resourceTemplates, sync.resourceTemplates)
					&& Objects.equals(prompts, sync.prompts) && Objects.equals(completions, sync.completions)
					&& Objects.equals(rootsChangeConsumers, sync.rootsChangeConsumers)
					&& Objects.equals(instructions, sync.instructions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts,
					completions, rootsChangeConsumers, instructions);
		}

		@Override
		public String toString() {
			return "Sync{" + "serverInfo=" + serverInfo + ", serverCapabilities=" + serverCapabilities + ", tools="
					+ tools + ", resources=" + resources + ", resourceTemplates=" + resourceTemplates + ", prompts="
					+ prompts + ", completions=" + completions + ", rootsChangeConsumers=" + rootsChangeConsumers
					+ ", instructions='" + instructions + '\'' + '}';
		}

	}

	/**
	 * Asynchronous tool specification.
	 */
	public static class AsyncToolSpecification {

		private final McpSchema.Tool tool;

		private final BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call;

		public AsyncToolSpecification(McpSchema.Tool tool,
				BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
			this.tool = tool;
			this.call = call;
		}

		public static AsyncToolSpecification fromSync(SyncToolSpecification tool) {
			// FIXME: This is temporary, proper validation should be implemented
			if (tool == null) {
				return null;
			}
			return new AsyncToolSpecification(tool.getTool(),
					(exchange, map) -> Mono
							.fromCallable(() -> tool.getCall().apply(new McpSyncServerExchange(exchange), map))
							.subscribeOn(Schedulers.boundedElastic()));
		}

		public McpSchema.Tool getTool() {
			return tool;
		}

		public BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> getCall() {
			return call;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			AsyncToolSpecification that = (AsyncToolSpecification) o;
			return Objects.equals(tool, that.tool) && Objects.equals(call, that.call);
		}

		@Override
		public int hashCode() {
			return Objects.hash(tool, call);
		}

		@Override
		public String toString() {
			return "AsyncToolSpecification{" + "tool=" + tool + ", call=" + call + '}';
		}

	}

	/**
	 * Asynchronous resource specification.
	 */
	public static class AsyncResourceSpecification {

		private final McpSchema.Resource resource;

		private final BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler;

		public AsyncResourceSpecification(McpSchema.Resource resource,
				BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler) {
			this.resource = resource;
			this.readHandler = readHandler;
		}

		public static AsyncResourceSpecification fromSync(SyncResourceSpecification resource) {
			// FIXME: This is temporary, proper validation should be implemented
			if (resource == null) {
				return null;
			}
			return new AsyncResourceSpecification(resource.getResource(),
					(exchange, req) -> Mono
							.fromCallable(
									() -> resource.getReadHandler().apply(new McpSyncServerExchange(exchange), req))
							.subscribeOn(Schedulers.boundedElastic()));
		}

		public McpSchema.Resource getResource() {
			return resource;
		}

		public BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> getReadHandler() {
			return readHandler;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			AsyncResourceSpecification that = (AsyncResourceSpecification) o;
			return Objects.equals(resource, that.resource) && Objects.equals(readHandler, that.readHandler);
		}

		@Override
		public int hashCode() {
			return Objects.hash(resource, readHandler);
		}

		@Override
		public String toString() {
			return "AsyncResourceSpecification{" + "resource=" + resource + ", readHandler=" + readHandler + '}';
		}

	}

	/**
	 * Asynchronous prompt specification.
	 */
	public static class AsyncPromptSpecification {

		private final McpSchema.Prompt prompt;

		private final BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler;

		public AsyncPromptSpecification(McpSchema.Prompt prompt,
				BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler) {
			this.prompt = prompt;
			this.promptHandler = promptHandler;
		}

		public static AsyncPromptSpecification fromSync(SyncPromptSpecification prompt) {
			// FIXME: This is temporary, proper validation should be implemented
			if (prompt == null) {
				return null;
			}
			return new AsyncPromptSpecification(prompt.getPrompt(),
					(exchange, req) -> Mono
							.fromCallable(
									() -> prompt.getPromptHandler().apply(new McpSyncServerExchange(exchange), req))
							.subscribeOn(Schedulers.boundedElastic()));
		}

		public McpSchema.Prompt getPrompt() {
			return prompt;
		}

		public BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> getPromptHandler() {
			return promptHandler;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			AsyncPromptSpecification that = (AsyncPromptSpecification) o;
			return Objects.equals(prompt, that.prompt) && Objects.equals(promptHandler, that.promptHandler);
		}

		@Override
		public int hashCode() {
			return Objects.hash(prompt, promptHandler);
		}

		@Override
		public String toString() {
			return "AsyncPromptSpecification{" + "prompt=" + prompt + ", promptHandler=" + promptHandler + '}';
		}

	}

	/**
	 * Asynchronous completion specification.
	 */
	public static class AsyncCompletionSpecification {

		private final McpSchema.CompleteReference referenceKey;

		private final BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> completionHandler;

		public AsyncCompletionSpecification(McpSchema.CompleteReference referenceKey,
				BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> completionHandler) {
			this.referenceKey = referenceKey;
			this.completionHandler = completionHandler;
		}

		/**
		 * Converts a synchronous {@link SyncCompletionSpecification} into an
		 * {@link AsyncCompletionSpecification} by wrapping the handler in a bounded
		 * elastic scheduler for safe non-blocking execution.
		 * @param completion the synchronous completion specification
		 * @return an asynchronous wrapper of the provided sync specification, or
		 * {@code null} if input is null
		 */
		public static AsyncCompletionSpecification fromSync(SyncCompletionSpecification completion) {
			if (completion == null) {
				return null;
			}
			return new AsyncCompletionSpecification(completion.getReferenceKey(),
					(exchange, request) -> Mono.fromCallable(
							() -> completion.getCompletionHandler().apply(new McpSyncServerExchange(exchange), request))
							.subscribeOn(Schedulers.boundedElastic()));
		}

		public McpSchema.CompleteReference getReferenceKey() {
			return referenceKey;
		}

		public BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> getCompletionHandler() {
			return completionHandler;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			AsyncCompletionSpecification that = (AsyncCompletionSpecification) o;
			return Objects.equals(referenceKey, that.referenceKey)
					&& Objects.equals(completionHandler, that.completionHandler);
		}

		@Override
		public int hashCode() {
			return Objects.hash(referenceKey, completionHandler);
		}

		@Override
		public String toString() {
			return "AsyncCompletionSpecification{" + "referenceKey=" + referenceKey + ", completionHandler="
					+ completionHandler + '}';
		}

	}

	/**
	 * Synchronous tool specification.
	 */
	public static class SyncToolSpecification {

		private final McpSchema.Tool tool;

		private final BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call;

		public SyncToolSpecification(McpSchema.Tool tool,
				BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
			this.tool = tool;
			this.call = call;
		}

		public McpSchema.Tool getTool() {
			return tool;
		}

		public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> getCall() {
			return call;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SyncToolSpecification that = (SyncToolSpecification) o;
			return Objects.equals(tool, that.tool) && Objects.equals(call, that.call);
		}

		@Override
		public int hashCode() {
			return Objects.hash(tool, call);
		}

		@Override
		public String toString() {
			return "SyncToolSpecification{" + "tool=" + tool + ", call=" + call + '}';
		}

	}

	/**
	 * Synchronous resource specification.
	 */
	public static class SyncResourceSpecification {

		private final McpSchema.Resource resource;

		private final BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler;

		public SyncResourceSpecification(McpSchema.Resource resource,
				BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler) {
			this.resource = resource;
			this.readHandler = readHandler;
		}

		public McpSchema.Resource getResource() {
			return resource;
		}

		public BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> getReadHandler() {
			return readHandler;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SyncResourceSpecification that = (SyncResourceSpecification) o;
			return Objects.equals(resource, that.resource) && Objects.equals(readHandler, that.readHandler);
		}

		@Override
		public int hashCode() {
			return Objects.hash(resource, readHandler);
		}

		@Override
		public String toString() {
			return "SyncResourceSpecification{" + "resource=" + resource + ", readHandler=" + readHandler + '}';
		}

	}

	/**
	 * Synchronous prompt specification.
	 */
	public static class SyncPromptSpecification {

		private final McpSchema.Prompt prompt;

		private final BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler;

		public SyncPromptSpecification(McpSchema.Prompt prompt,
				BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler) {
			this.prompt = prompt;
			this.promptHandler = promptHandler;
		}

		public McpSchema.Prompt getPrompt() {
			return prompt;
		}

		public BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> getPromptHandler() {
			return promptHandler;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SyncPromptSpecification that = (SyncPromptSpecification) o;
			return Objects.equals(prompt, that.prompt) && Objects.equals(promptHandler, that.promptHandler);
		}

		@Override
		public int hashCode() {
			return Objects.hash(prompt, promptHandler);
		}

		@Override
		public String toString() {
			return "SyncPromptSpecification{" + "prompt=" + prompt + ", promptHandler=" + promptHandler + '}';
		}

	}

	/**
	 * Synchronous completion specification.
	 */
	public static class SyncCompletionSpecification {

		private final McpSchema.CompleteReference referenceKey;

		private final BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> completionHandler;

		public SyncCompletionSpecification(McpSchema.CompleteReference referenceKey,
				BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> completionHandler) {
			this.referenceKey = referenceKey;
			this.completionHandler = completionHandler;
		}

		public McpSchema.CompleteReference getReferenceKey() {
			return referenceKey;
		}

		public BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> getCompletionHandler() {
			return completionHandler;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SyncCompletionSpecification that = (SyncCompletionSpecification) o;
			return Objects.equals(referenceKey, that.referenceKey)
					&& Objects.equals(completionHandler, that.completionHandler);
		}

		@Override
		public int hashCode() {
			return Objects.hash(referenceKey, completionHandler);
		}

		@Override
		public String toString() {
			return "SyncCompletionSpecification{" + "referenceKey=" + referenceKey + ", completionHandler="
					+ completionHandler + '}';
		}

	}

}
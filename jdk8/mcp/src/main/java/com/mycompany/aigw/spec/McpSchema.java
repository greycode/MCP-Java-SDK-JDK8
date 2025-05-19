/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.spec;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.aigw.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Based on the <a href="http://www.jsonrpc.org/specification">JSON-RPC 2.0
 * specification</a> and the <a href=
 * "https://github.com/modelcontextprotocol/specification/blob/main/schema/2024-11-05/schema.ts">Model
 * Context Protocol Schema</a>.
 *
 * @author Christian Tzolov
 */
public final class McpSchema {

	private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

	private McpSchema() {
	}

	public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

	public static final String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// Method Names
	// ---------------------------

	// Lifecycle Methods
	public static final String METHOD_INITIALIZE = "initialize";

	public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	public static final String METHOD_PING = "ping";

	// Tool Methods
	public static final String METHOD_TOOLS_LIST = "tools/list";

	public static final String METHOD_TOOLS_CALL = "tools/call";

	public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	// Resources Methods
	public static final String METHOD_RESOURCES_LIST = "resources/list";

	public static final String METHOD_RESOURCES_READ = "resources/read";

	public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

	public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

	// Prompt Methods
	public static final String METHOD_PROMPT_LIST = "prompts/list";

	public static final String METHOD_PROMPT_GET = "prompts/get";

	public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	public static final String METHOD_COMPLETION_COMPLETE = "completion/complete";

	// Logging Methods
	public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// Roots Methods
	public static final String METHOD_ROOTS_LIST = "roots/list";

	public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// Sampling Methods
	public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// ---------------------------
	// JSON-RPC Error Codes
	// ---------------------------
	/**
	 * Standard error codes used in MCP JSON-RPC responses.
	 */
	public static final class ErrorCodes {

		/**
		 * Invalid JSON was received by the server.
		 */
		public static final int PARSE_ERROR = -32700;

		/**
		 * The JSON sent is not a valid Request object.
		 */
		public static final int INVALID_REQUEST = -32600;

		/**
		 * The method does not exist / is not available.
		 */
		public static final int METHOD_NOT_FOUND = -32601;

		/**
		 * Invalid method parameter(s).
		 */
		public static final int INVALID_PARAMS = -32602;

		/**
		 * Internal JSON-RPC error.
		 */
		public static final int INTERNAL_ERROR = -32603;

		private ErrorCodes() { // Prevent instantiation
		}

	}

	// Replaced sealed interface Request
	public interface Request {

		// Marker interface, specific request types below implement this

	}

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	/**
	 * Deserializes a JSON string into a JSONRPCMessage object.
	 * @param objectMapper The ObjectMapper instance to use for deserialization
	 * @param jsonText The JSON string to deserialize
	 * @return A JSONRPCMessage instance using either the {@link JSONRPCRequest},
	 * {@link JSONRPCNotification}, or {@link JSONRPCResponse} classes.
	 * @throws IOException If there's an error during deserialization
	 * @throws IllegalArgumentException If the JSON structure doesn't match any known
	 * message type
	 */
	public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
			throws IOException {

		logger.debug("Received JSON message: {}", jsonText);

		// Replaced 'var' with explicit type
		Map<String, Object> map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

		// Determine message type based on specific JSON structure
		if (map.containsKey("method") && map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCRequest.class);
		}
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCNotification.class);
		}
		else if (map.containsKey("result") || map.containsKey("error")) {
			return objectMapper.convertValue(map, JSONRPCResponse.class);
		}

		throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
	}

	// ---------------------------
	// JSON-RPC Message Types
	// ---------------------------
	// Replaced sealed interface JSONRPCMessage
	public interface JSONRPCMessage {

		String getJsonrpc(); // Changed from method name to getter convention

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record JSONRPCRequest with class
	public static final class JSONRPCRequest implements JSONRPCMessage, Serializable {

		private static final long serialVersionUID = 1L; // Added for Serializable

		private final String jsonrpc;

		private final String method;

		private final Object id;

		private final Object params;

		// Jackson needs a default constructor or one annotated with @JsonCreator
		// Assuming fields are set via @JsonProperty during deserialization
		// Add an explicit constructor if needed for creation logic
		public JSONRPCRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("method") String method,
				@JsonProperty("id") Object id, @JsonProperty("params") Object params) {
			this.jsonrpc = jsonrpc;
			this.method = method;
			this.id = id;
			this.params = params;
		}

		@JsonProperty("jsonrpc")
		public String getJsonrpc() {
			return jsonrpc;
		}

		@JsonProperty("method")
		public String getMethod() {
			return method;
		}

		@JsonProperty("id")
		public Object getId() {
			return id;
		}

		@JsonProperty("params")
		public Object getParams() {
			return params;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			JSONRPCRequest that = (JSONRPCRequest) o;
			return Objects.equals(jsonrpc, that.jsonrpc) && Objects.equals(method, that.method)
					&& Objects.equals(id, that.id) && Objects.equals(params, that.params);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jsonrpc, method, id, params);
		}

		@Override
		public String toString() {
			return "JSONRPCRequest{" + "jsonrpc='" + jsonrpc + '\'' + ", method='" + method + '\'' + ", id=" + id
					+ ", params=" + params + '}';
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record JSONRPCNotification with class
	public static final class JSONRPCNotification implements JSONRPCMessage, Serializable {

		private static final long serialVersionUID = 1L;

		private final String jsonrpc;

		private final String method;

		private final Object params;

		public JSONRPCNotification(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("method") String method,
				@JsonProperty("params") Object params) {
			this.jsonrpc = jsonrpc;
			this.method = method;
			this.params = params;
		}

		@JsonProperty("jsonrpc")
		public String getJsonrpc() {
			return jsonrpc;
		}

		@JsonProperty("method")
		public String getMethod() {
			return method;
		}

		@JsonProperty("params")
		public Object getParams() {
			return params;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			JSONRPCNotification that = (JSONRPCNotification) o;
			return Objects.equals(jsonrpc, that.jsonrpc) && Objects.equals(method, that.method)
					&& Objects.equals(params, that.params);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jsonrpc, method, params);
		}

		@Override
		public String toString() {
			return "JSONRPCNotification{" + "jsonrpc='" + jsonrpc + '\'' + ", method='" + method + '\'' + ", params="
					+ params + '}';
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record JSONRPCResponse with class
	public static final class JSONRPCResponse implements JSONRPCMessage, Serializable {

		private static final long serialVersionUID = 1L;

		private final String jsonrpc;

		private final Object id;

		private final Object result;

		private final JSONRPCError error;

		public JSONRPCResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
				@JsonProperty("result") Object result, @JsonProperty("error") JSONRPCError error) {
			this.jsonrpc = jsonrpc;
			this.id = id;
			this.result = result;
			this.error = error;
		}

		@JsonProperty("jsonrpc")
		public String getJsonrpc() {
			return jsonrpc;
		}

		@JsonProperty("id")
		public Object getId() {
			return id;
		}

		@JsonProperty("result")
		public Object getResult() {
			return result;
		}

		@JsonProperty("error")
		public JSONRPCError getError() {
			return error;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			JSONRPCResponse that = (JSONRPCResponse) o;
			return Objects.equals(jsonrpc, that.jsonrpc) && Objects.equals(id, that.id)
					&& Objects.equals(result, that.result) && Objects.equals(error, that.error);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jsonrpc, id, result, error);
		}

		@Override
		public String toString() {
			return "JSONRPCResponse{" + "jsonrpc='" + jsonrpc + '\'' + ", id=" + id + ", result=" + result + ", error="
					+ error + '}';
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		// Replaced record JSONRPCError with class
		public static final class JSONRPCError implements Serializable {

			private static final long serialVersionUID = 1L;

			private final int code;

			private final String message;

			private final Object data;

			public JSONRPCError(@JsonProperty("code") int code, @JsonProperty("message") String message,
					@JsonProperty("data") Object data) {
				this.code = code;
				this.message = message;
				this.data = data;
			}

			@JsonProperty("code")
			public int getCode() {
				return code;
			}

			@JsonProperty("message")
			public String getMessage() {
				return message;
			}

			@JsonProperty("data")
			public Object getData() {
				return data;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				JSONRPCError that = (JSONRPCError) o;
				return code == that.code && Objects.equals(message, that.message) && Objects.equals(data, that.data);
			}

			@Override
			public int hashCode() {
				return Objects.hash(code, message, data);
			}

			@Override
			public String toString() {
				return "JSONRPCError{" + "code=" + code + ", message='" + message + '\'' + ", data=" + data + '}';
			}

		}

	}

	// ---------------------------
	// Initialization
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record InitializeRequest with class
	public static final class InitializeRequest implements Request, Serializable {

		private static final long serialVersionUID = 1L;

		private final String protocolVersion;

		private final ClientCapabilities capabilities;

		private final Implementation clientInfo;

		public InitializeRequest(@JsonProperty("protocolVersion") String protocolVersion,
				@JsonProperty("capabilities") ClientCapabilities capabilities,
				@JsonProperty("clientInfo") Implementation clientInfo) {
			this.protocolVersion = protocolVersion;
			this.capabilities = capabilities;
			this.clientInfo = clientInfo;
		}

		@JsonProperty("protocolVersion")
		public String getProtocolVersion() {
			return protocolVersion;
		}

		@JsonProperty("capabilities")
		public ClientCapabilities getCapabilities() {
			return capabilities;
		}

		@JsonProperty("clientInfo")
		public Implementation getClientInfo() {
			return clientInfo;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			InitializeRequest that = (InitializeRequest) o;
			return Objects.equals(protocolVersion, that.protocolVersion)
					&& Objects.equals(capabilities, that.capabilities) && Objects.equals(clientInfo, that.clientInfo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(protocolVersion, capabilities, clientInfo);
		}

		@Override
		public String toString() {
			return "InitializeRequest{" + "protocolVersion='" + protocolVersion + '\'' + ", capabilities="
					+ capabilities + ", clientInfo=" + clientInfo + '}';
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record InitializeResult with class
	public static final class InitializeResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String protocolVersion;

		private final ServerCapabilities capabilities;

		private final Implementation serverInfo;

		private final String instructions;

		public InitializeResult(@JsonProperty("protocolVersion") String protocolVersion,
				@JsonProperty("capabilities") ServerCapabilities capabilities,
				@JsonProperty("serverInfo") Implementation serverInfo,
				@JsonProperty("instructions") String instructions) {
			this.protocolVersion = protocolVersion;
			this.capabilities = capabilities;
			this.serverInfo = serverInfo;
			this.instructions = instructions;
		}

		@JsonProperty("protocolVersion")
		public String getProtocolVersion() {
			return protocolVersion;
		}

		@JsonProperty("capabilities")
		public ServerCapabilities getCapabilities() {
			return capabilities;
		}

		@JsonProperty("serverInfo")
		public Implementation getServerInfo() {
			return serverInfo;
		}

		@JsonProperty("instructions")
		public String getInstructions() {
			return instructions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			InitializeResult that = (InitializeResult) o;
			return Objects.equals(protocolVersion, that.protocolVersion)
					&& Objects.equals(capabilities, that.capabilities) && Objects.equals(serverInfo, that.serverInfo)
					&& Objects.equals(instructions, that.instructions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(protocolVersion, capabilities, serverInfo, instructions);
		}

		@Override
		public String toString() {
			return "InitializeResult{" + "protocolVersion='" + protocolVersion + '\'' + ", capabilities=" + capabilities
					+ ", serverInfo=" + serverInfo + ", instructions='" + instructions + '\'' + '}';
		}

	}

	/**
	 * Clients can implement additional features to enrich connected MCP servers with
	 * additional capabilities. These capabilities can be used to extend the functionality
	 * of the server, or to provide additional information to the server about the
	 * client's capabilities.
	 *
	 * The following fields define client capabilities:
	 * <ul>
	 * <li>{@code experimental} - WIP</li>
	 * <li>{@code roots} - Define the boundaries of where servers can operate within the
	 * filesystem, allowing them to understand which directories and files they have
	 * access to.</li>
	 * <li>{@code sampling} - Provides a standardized way for servers to request LLM sampling
	 * ("completions" or "generations") from language models via clients.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ClientCapabilities with class
	public static final class ClientCapabilities implements Serializable {

		private static final long serialVersionUID = 1L;

		private final Map<String, Object> experimental;

		private final RootCapabilities roots;

		private final Sampling sampling;

		// Constructor used by Builder
		public ClientCapabilities(Map<String, Object> experimental, RootCapabilities roots, Sampling sampling) {
			this.experimental = experimental;
			this.roots = roots;
			this.sampling = sampling;
		}

		// Jackson constructor
		public ClientCapabilities(@JsonProperty("experimental") Map<String, Object> experimental,
				@JsonProperty("roots") RootCapabilities roots, @JsonProperty("sampling") Sampling sampling,
				boolean jackson) { // Dummy parameter to differentiate
			this(experimental, roots, sampling);
		}

		@JsonProperty("experimental")
		public Map<String, Object> getExperimental() {
			return experimental;
		}

		@JsonProperty("roots")
		public RootCapabilities getRoots() {
			return roots;
		}

		@JsonProperty("sampling")
		public Sampling getSampling() {
			return sampling;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ClientCapabilities that = (ClientCapabilities) o;
			return Objects.equals(experimental, that.experimental) && Objects.equals(roots, that.roots)
					&& Objects.equals(sampling, that.sampling);
		}

		@Override
		public int hashCode() {
			return Objects.hash(experimental, roots, sampling);
		}

		@Override
		public String toString() {
			return "ClientCapabilities{" + "experimental=" + experimental + ", roots=" + roots + ", sampling="
					+ sampling + '}';
		}

		/**
		 * Roots define the boundaries of where servers can operate within the filesystem,
		 * allowing them to understand which directories and files they have access to.
		 * Servers can request the list of roots from supporting clients and receive
		 * notifications when that list changes.
		 * 
		 * The {@code listChanged} field indicates whether the client would send notification 
		 * about roots has changed since the last time the server checked.
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		// Replaced record RootCapabilities with class
		public static final class RootCapabilities implements Serializable {

			private static final long serialVersionUID = 1L;

			private final Boolean listChanged;

			public RootCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			@JsonProperty("listChanged")
			public Boolean getListChanged() {
				return listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				RootCapabilities that = (RootCapabilities) o;
				return Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hash(listChanged);
			}

			@Override
			public String toString() {
				return "RootCapabilities{" + "listChanged=" + listChanged + '}';
			}

		}

		/**
		 * Provides a standardized way for servers to request LLM sampling ("completions"
		 * or "generations") from language models via clients. This flow allows clients to
		 * maintain control over model access, selection, and permissions while enabling
		 * servers to leverage AI capabilities—with no server API keys necessary. Servers
		 * can request text or image-based interactions and optionally include context
		 * from MCP servers in their prompts.
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		// Replaced record Sampling with class
		public static final class Sampling implements Serializable {

			private static final long serialVersionUID = 1L;

			// No fields in the original record

			public Sampling() {
				// Default constructor for Jackson and Builder
			}

			@Override
			public boolean equals(Object obj) {
				// Since there are no fields, all instances are considered equal
				return obj instanceof Sampling;
			}

			@Override
			public int hashCode() {
				// Since there are no fields, return a constant hash code
				return 1;
			}

			@Override
			public String toString() {
				return "Sampling{}";
			}

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Map<String, Object> experimental;

			private RootCapabilities roots;

			private Sampling sampling;

			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			public Builder roots(Boolean listChanged) {
				this.roots = new RootCapabilities(listChanged);
				return this;
			}

			public Builder sampling() {
				this.sampling = new Sampling();
				return this;
			}

			public ClientCapabilities build() {
				// Use the private constructor for building
				return new ClientCapabilities(experimental, roots, sampling);
			}

		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ServerCapabilities with class
	public static final class ServerCapabilities implements Serializable {

		private static final long serialVersionUID = 1L;

		private final CompletionCapabilities completions;

		private final Map<String, Object> experimental;

		private final LoggingCapabilities logging;

		private final PromptCapabilities prompts;

		private final ResourceCapabilities resources;

		private final ToolCapabilities tools;

		// Constructor used by Builder
        public ServerCapabilities(CompletionCapabilities completions, Map<String, Object> experimental,
                                  LoggingCapabilities logging, PromptCapabilities prompts, ResourceCapabilities resources,
                                  ToolCapabilities tools) {
			this.completions = completions;
			this.experimental = experimental;
			this.logging = logging;
			this.prompts = prompts;
			this.resources = resources;
			this.tools = tools;
		}

		// Jackson constructor
		public ServerCapabilities(@JsonProperty("completions") CompletionCapabilities completions,
				@JsonProperty("experimental") Map<String, Object> experimental,
				@JsonProperty("logging") LoggingCapabilities logging,
				@JsonProperty("prompts") PromptCapabilities prompts,
				@JsonProperty("resources") ResourceCapabilities resources,
				@JsonProperty("tools") ToolCapabilities tools, boolean jackson) { // Dummy
																					// parameter
			this(completions, experimental, logging, prompts, resources, tools);
		}

		@JsonProperty("completions")
		public CompletionCapabilities getCompletions() {
			return completions;
		}

		@JsonProperty("experimental")
		public Map<String, Object> getExperimental() {
			return experimental;
		}

		@JsonProperty("logging")
		public LoggingCapabilities getLogging() {
			return logging;
		}

		@JsonProperty("prompts")
		public PromptCapabilities getPrompts() {
			return prompts;
		}

		@JsonProperty("resources")
		public ResourceCapabilities getResources() {
			return resources;
		}

		@JsonProperty("tools")
		public ToolCapabilities getTools() {
			return tools;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ServerCapabilities that = (ServerCapabilities) o;
			return Objects.equals(completions, that.completions) && Objects.equals(experimental, that.experimental)
					&& Objects.equals(logging, that.logging) && Objects.equals(prompts, that.prompts)
					&& Objects.equals(resources, that.resources) && Objects.equals(tools, that.tools);
		}

		@Override
		public int hashCode() {
			return Objects.hash(completions, experimental, logging, prompts, resources, tools);
		}

		@Override
		public String toString() {
			return "ServerCapabilities{" + "completions=" + completions + ", experimental=" + experimental
					+ ", logging=" + logging + ", prompts=" + prompts + ", resources=" + resources + ", tools=" + tools
					+ '}';
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		// Replaced record CompletionCapabilities with class
		public static final class CompletionCapabilities implements Serializable {

			private static final long serialVersionUID = 1L;

			// No fields
			public CompletionCapabilities() {
			}

			@Override
			public boolean equals(Object obj) {
				return obj instanceof CompletionCapabilities;
			}

			@Override
			public int hashCode() {
				return 2;
			}

			@Override
			public String toString() {
				return "CompletionCapabilities{}";
			}

		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		// Replaced record LoggingCapabilities with class
		public static final class LoggingCapabilities implements Serializable {

			private static final long serialVersionUID = 1L;

			// No fields
			public LoggingCapabilities() {
			}

			@Override
			public boolean equals(Object obj) {
				return obj instanceof LoggingCapabilities;
			}

			@Override
			public int hashCode() {
				return 3;
			}

			@Override
			public String toString() {
				return "LoggingCapabilities{}";
			}

		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		// Replaced record PromptCapabilities with class
		public static final class PromptCapabilities implements Serializable {

			private static final long serialVersionUID = 1L;

			private final Boolean listChanged;

			public PromptCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			@JsonProperty("listChanged")
			public Boolean getListChanged() {
				return listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				PromptCapabilities that = (PromptCapabilities) o;
				return Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hash(listChanged);
			}

			@Override
			public String toString() {
				return "PromptCapabilities{listChanged=" + listChanged + '}';
			}

		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		// Replaced record ResourceCapabilities with class
		public static final class ResourceCapabilities implements Serializable {

			private static final long serialVersionUID = 1L;

			private final Boolean subscribe;

			private final Boolean listChanged;

			public ResourceCapabilities(@JsonProperty("subscribe") Boolean subscribe,
					@JsonProperty("listChanged") Boolean listChanged) {
				this.subscribe = subscribe;
				this.listChanged = listChanged;
			}

			@JsonProperty("subscribe")
			public Boolean getSubscribe() {
				return subscribe;
			}

			@JsonProperty("listChanged")
			public Boolean getListChanged() {
				return listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				ResourceCapabilities that = (ResourceCapabilities) o;
				return Objects.equals(subscribe, that.subscribe) && Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hash(subscribe, listChanged);
			}

			@Override
			public String toString() {
				return "ResourceCapabilities{subscribe=" + subscribe + ", listChanged=" + listChanged + '}';
			}

		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		// Replaced record ToolCapabilities with class
		public static final class ToolCapabilities implements Serializable {

			private static final long serialVersionUID = 1L;

			private final Boolean listChanged;

			public ToolCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			@JsonProperty("listChanged")
			public Boolean getListChanged() {
				return listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				ToolCapabilities that = (ToolCapabilities) o;
				return Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hash(listChanged);
			}

			@Override
			public String toString() {
				return "ToolCapabilities{listChanged=" + listChanged + '}';
			}

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private CompletionCapabilities completions;

			private Map<String, Object> experimental;

			private LoggingCapabilities logging = new LoggingCapabilities();

			private PromptCapabilities prompts;

			private ResourceCapabilities resources;

			private ToolCapabilities tools;

			public Builder completions() {
				this.completions = new CompletionCapabilities();
				return this;
			}

			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			public Builder logging() {
				this.logging = new LoggingCapabilities();
				return this;
			}

			public Builder prompts(Boolean listChanged) {
				this.prompts = new PromptCapabilities(listChanged);
				return this;
			}

			public Builder resources(Boolean subscribe, Boolean listChanged) {
				this.resources = new ResourceCapabilities(subscribe, listChanged);
				return this;
			}

			public Builder tools(Boolean listChanged) {
				this.tools = new ToolCapabilities(listChanged);
				return this;
			}

			public ServerCapabilities build() {
				// Use the private constructor for building
				return new ServerCapabilities(completions, experimental, logging, prompts, resources, tools);
			}

		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record Implementation with class
	public static final class Implementation implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private final String version;

		public Implementation(@JsonProperty("name") String name, @JsonProperty("version") String version) {
			this.name = name;
			this.version = version;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("version")
		public String getVersion() {
			return version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Implementation that = (Implementation) o;
			return Objects.equals(name, that.name) && Objects.equals(version, that.version);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, version);
		}

		@Override
		public String toString() {
			return "Implementation{name='" + name + "', version='" + version + "'}";
		}

	}

	// Existing Enums and Base Types (from previous implementation)
	public enum Role {

		@JsonProperty("user")
		USER, @JsonProperty("assistant")
		ASSISTANT

	}

	// ---------------------------
	// Resource Interfaces
	// ---------------------------
	/**
	 * Base for objects that include optional annotations for the client. The client can
	 * use annotations to inform how objects are used or displayed
	 */
	public interface Annotated {

		Annotations getAnnotations(); // Changed from method name to getter

	}

	/**
	 * Annotations provide metadata about a resource or other objects.
	 * <ul>
	 * <li>{@code audience} - Describes who the intended customer of this object or data is. It
	 * can be used for filtering capabilities in clients.</li>
	 * <li>{@code priority} - Describes how important this data is for operating the server. A
	 * higher priority means the data is more important.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record Annotations with class
	public static final class Annotations implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Role> audience;

		private final Double priority;

		public Annotations(@JsonProperty("audience") List<Role> audience, @JsonProperty("priority") Double priority) {
			this.audience = audience;
			this.priority = priority;
		}

		@JsonProperty("audience")
		public List<Role> getAudience() {
			return audience;
		}

		@JsonProperty("priority")
		public Double getPriority() {
			return priority;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Annotations that = (Annotations) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority);
		}

		@Override
		public int hashCode() {
			return Objects.hash(audience, priority);
		}

		@Override
		public String toString() {
			return "Annotations{audience=" + audience + ", priority=" + priority + '}';
		}

	}

	/**
	 * Represents a resource that can be accessed by the client.
	 * <ul>
	 * <li>{@code uri} - The URI of the resource.</li>
	 * <li>{@code name} - A human-readable name for this resource. This can be used by clients to
	 * present the resource to users.</li>
	 * <li>{@code description} - A description of what this resource represents. This can be used
	 * to provide more context to users.</li>
	 * <li>{@code mimeType} - The MIME type of this resource, if known.</li>
	 * <li>{@code annotations} - Optional annotations for the client. The client can use
	 * these to filter or prioritize resources.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record Resource with class
	public static final class Resource implements Annotated, Serializable {

		private static final long serialVersionUID = 1L;

		private final String uri;

		private final String name;

		private final String description;

		private final String mimeType;

		private final Annotations annotations;

		public Resource(@JsonProperty("uri") String uri, @JsonProperty("name") String name,
				@JsonProperty("description") String description, @JsonProperty("mimeType") String mimeType,
				@JsonProperty("annotations") Annotations annotations) {
			this.uri = uri;
			this.name = name;
			this.description = description;
			this.mimeType = mimeType;
			this.annotations = annotations;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("description")
		public String getDescription() {
			return description;
		}

		@JsonProperty("mimeType")
		public String getMimeType() {
			return mimeType;
		}

		@JsonProperty("annotations")
		public Annotations getAnnotations() {
			return annotations;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Resource resource = (Resource) o;
			return Objects.equals(uri, resource.uri) && Objects.equals(name, resource.name)
					&& Objects.equals(description, resource.description) && Objects.equals(mimeType, resource.mimeType)
					&& Objects.equals(annotations, resource.annotations);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, name, description, mimeType, annotations);
		}

		@Override
		public String toString() {
			return "Resource{uri='" + uri + "', name='" + name + "', description='" + description + "', mimeType='"
					+ mimeType + "', annotations=" + annotations + '}';
		}

	}

	/**
	 * Resource templates allow servers to expose parameterized resources using URI
	 * templates.
	 *
	 * <ul>
	 * <li>{@code uriTemplate} - A URI template that can be used to generate URIs for this
	 * resource.</li>
	 * <li>{@code name} - A human-readable name for this resource. This can be used by clients to
	 * populate UI elements.</li>
	 * <li>{@code description} - A description of what this resource represents. This can be used
	 * by clients to improve the LLM's understanding of available resources. It can be
	 * thought of like a "hint" to the model.</li>
	 * <li>{@code mimeType} - The MIME type of this resource, if known.</li>
	 * <li>{@code annotations} - Optional annotations for the client. The client can use
	 * annotations to inform how objects are used or displayed.</li>
	 * </ul>
	 * 
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ResourceTemplate with class
	public static final class ResourceTemplate implements Annotated, Serializable {

		private static final long serialVersionUID = 1L;

		private final String uriTemplate;

		private final String name;

		private final String description;

		private final String mimeType;

		private final Annotations annotations;

		public ResourceTemplate(@JsonProperty("uriTemplate") String uriTemplate, @JsonProperty("name") String name,
				@JsonProperty("description") String description, @JsonProperty("mimeType") String mimeType,
				@JsonProperty("annotations") Annotations annotations) {
			this.uriTemplate = uriTemplate;
			this.name = name;
			this.description = description;
			this.mimeType = mimeType;
			this.annotations = annotations;
		}

		@JsonProperty("uriTemplate")
		public String getUriTemplate() {
			return uriTemplate;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("description")
		public String getDescription() {
			return description;
		}

		@JsonProperty("mimeType")
		public String getMimeType() {
			return mimeType;
		}

		@JsonProperty("annotations")
		public Annotations getAnnotations() {
			return annotations;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ResourceTemplate that = (ResourceTemplate) o;
			return Objects.equals(uriTemplate, that.uriTemplate) && Objects.equals(name, that.name)
					&& Objects.equals(description, that.description) && Objects.equals(mimeType, that.mimeType)
					&& Objects.equals(annotations, that.annotations);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uriTemplate, name, description, mimeType, annotations);
		}

		@Override
		public String toString() {
			return "ResourceTemplate{uriTemplate='" + uriTemplate + "', name='" + name + "', description='"
					+ description + "', mimeType='" + mimeType + "', annotations=" + annotations + '}';
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ListResourcesResult with class
	public static final class ListResourcesResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Resource> resources;

		private final String nextCursor;

		public ListResourcesResult(@JsonProperty("resources") List<Resource> resources,
				@JsonProperty("nextCursor") String nextCursor) {
			this.resources = resources;
			this.nextCursor = nextCursor;
		}

		@JsonProperty("resources")
		public List<Resource> getResources() {
			return resources;
		}

		@JsonProperty("nextCursor")
		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ListResourcesResult that = (ListResourcesResult) o;
			return Objects.equals(resources, that.resources) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(resources, nextCursor);
		}

		@Override
		public String toString() {
			return "ListResourcesResult{resources=" + resources + ", nextCursor='" + nextCursor + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ListResourceTemplatesResult with class
	public static final class ListResourceTemplatesResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<ResourceTemplate> resourceTemplates;

		private final String nextCursor;

		public ListResourceTemplatesResult(@JsonProperty("resourceTemplates") List<ResourceTemplate> resourceTemplates,
				@JsonProperty("nextCursor") String nextCursor) {
			this.resourceTemplates = resourceTemplates;
			this.nextCursor = nextCursor;
		}

		@JsonProperty("resourceTemplates")
		public List<ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		@JsonProperty("nextCursor")
		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ListResourceTemplatesResult that = (ListResourceTemplatesResult) o;
			return Objects.equals(resourceTemplates, that.resourceTemplates)
					&& Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(resourceTemplates, nextCursor);
		}

		@Override
		public String toString() {
			return "ListResourceTemplatesResult{resourceTemplates=" + resourceTemplates + ", nextCursor='" + nextCursor
					+ "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ReadResourceRequest with class
	public static final class ReadResourceRequest implements Serializable {

		// Note:
		// ReadResourceRequest
		// was not
		// implementing
		// Request in
		// original,
		// keeping it
		// that way
		private static final long serialVersionUID = 1L;

		private final String uri;

		public ReadResourceRequest(@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ReadResourceRequest that = (ReadResourceRequest) o;
			return Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

		@Override
		public String toString() {
			return "ReadResourceRequest{uri='" + uri + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ReadResourceResult with class
	public static final class ReadResourceResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<ResourceContents> contents;

		public ReadResourceResult(@JsonProperty("contents") List<ResourceContents> contents) {
			this.contents = contents;
		}

		@JsonProperty("contents")
		public List<ResourceContents> getContents() {
			return contents;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ReadResourceResult that = (ReadResourceResult) o;
			return Objects.equals(contents, that.contents);
		}

		@Override
		public int hashCode() {
			return Objects.hash(contents);
		}

		@Override
		public String toString() {
			return "ReadResourceResult{contents=" + contents + '}';
		}

	}

	/**
	 * Sent from the client to request resources/updated notifications from the server
	 * whenever a particular resource changes.
	 *
	 * The {@code uri} field specifies the URI of the resource to subscribe to. The URI can 
	 * use any protocol; it is up to the server how to interpret it.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record SubscribeRequest with class
	public static final class SubscribeRequest implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String uri;

		public SubscribeRequest(@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SubscribeRequest that = (SubscribeRequest) o;
			return Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

		@Override
		public String toString() {
			return "SubscribeRequest{uri='" + uri + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record UnsubscribeRequest with class
	public static final class UnsubscribeRequest implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String uri;

		public UnsubscribeRequest(@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			UnsubscribeRequest that = (UnsubscribeRequest) o;
			return Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

		@Override
		public String toString() {
			return "UnsubscribeRequest{uri='" + uri + "'}";
		}

	}

	/**
	 * The contents of a specific resource or sub-resource.
	 */
	// Replaced sealed interface ResourceContents and changed JsonTypeInfo.Id.DEDUCTION to
	// Id.NAME
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type") // Changed
																						// from
																						// DEDUCTION
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
			@JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
	public interface ResourceContents {

		/**
		 * The URI of this resource.
		 * @return the URI of this resource.
		 */
		String getUri(); // Changed from method name to getter

		/**
		 * The MIME type of this resource.
		 * @return the MIME type of this resource.
		 */
		String getMimeType(); // Changed from method name to getter

	}

	/**
	 * Text contents of a resource.
	 *
	 * <ul>
	 * <li>{@code uri} - The URI of this resource.</li>
	 * <li>{@code mimeType} - The MIME type of this resource.</li>
	 * <li>{@code text} - The text of the resource. This must only be set if the resource can
	 * actually be represented as text (not binary data).</li>
	 * </ul>
	 */
	@JsonTypeName("text") // Added for Id.NAME strategy
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record TextResourceContents with class
	public static final class TextResourceContents implements ResourceContents, Serializable {

		private static final long serialVersionUID = 1L;

		private final String uri;

		private final String mimeType;

		private final String text;

		public TextResourceContents(@JsonProperty("uri") String uri, @JsonProperty("mimeType") String mimeType,
				@JsonProperty("text") String text) {
			this.uri = uri;
			this.mimeType = mimeType;
			this.text = text;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@JsonProperty("mimeType")
		public String getMimeType() {
			return mimeType;
		}

		@JsonProperty("text")
		public String getText() {
			return text;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TextResourceContents that = (TextResourceContents) o;
			return Objects.equals(uri, that.uri) && Objects.equals(mimeType, that.mimeType)
					&& Objects.equals(text, that.text);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, mimeType, text);
		}

		@Override
		public String toString() {
			return "TextResourceContents{uri='" + uri + "', mimeType='" + mimeType + "', text='" + text + "'}";
		}

	}

	/**
	 * Binary contents of a resource.
	 *
	 * <ul>
	 * <li>{@code uri} - The URI of this resource.</li>
	 * <li>{@code mimeType} - The MIME type of this resource.</li>
	 * <li>{@code blob} - A base64-encoded string representing the binary data of the resource.
	 * This must only be set if the resource can actually be represented as binary data
	 * (not text).</li>
	 * </ul>
	 */
	@JsonTypeName("blob") // Added for Id.NAME strategy
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record BlobResourceContents with class
	public static final class BlobResourceContents implements ResourceContents, Serializable {

		private static final long serialVersionUID = 1L;

		private final String uri;

		private final String mimeType;

		private final String blob;

		public BlobResourceContents(@JsonProperty("uri") String uri, @JsonProperty("mimeType") String mimeType,
				@JsonProperty("blob") String blob) {
			this.uri = uri;
			this.mimeType = mimeType;
			this.blob = blob;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@JsonProperty("mimeType")
		public String getMimeType() {
			return mimeType;
		}

		@JsonProperty("blob")
		public String getBlob() {
			return blob;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			BlobResourceContents that = (BlobResourceContents) o;
			return Objects.equals(uri, that.uri) && Objects.equals(mimeType, that.mimeType)
					&& Objects.equals(blob, that.blob);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, mimeType, blob);
		}

		@Override
		public String toString() {
			return "BlobResourceContents{uri='" + uri + "', mimeType='" + mimeType + "', blob='[hidden]'}";
		} // Avoid logging large blob

	}

	// ---------------------------
	// Prompt Interfaces
	// ---------------------------
	/**
	 * A prompt or prompt template that the server offers.
	 *
	 * <ul>
	 * <li>{@code name} - The name of the prompt or prompt template.</li>
	 * <li>{@code description} - An optional description of what this prompt provides.</li>
	 * <li>{@code arguments} - A list of arguments to use for templating the prompt.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record Prompt with class
	public static final class Prompt implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private final String description;

		private final List<PromptArgument> arguments;

		public Prompt(@JsonProperty("name") String name, @JsonProperty("description") String description,
				@JsonProperty("arguments") List<PromptArgument> arguments) {
			this.name = name;
			this.description = description;
			this.arguments = arguments;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("description")
		public String getDescription() {
			return description;
		}

		@JsonProperty("arguments")
		public List<PromptArgument> getArguments() {
			return arguments;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Prompt prompt = (Prompt) o;
			return Objects.equals(name, prompt.name) && Objects.equals(description, prompt.description)
					&& Objects.equals(arguments, prompt.arguments);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, arguments);
		}

		@Override
		public String toString() {
			return "Prompt{name='" + name + "', description='" + description + "', arguments=" + arguments + '}';
		}

	}

	/**
	 * Describes an argument that a prompt can accept.
	 *
	 * <ul>
	 * <li>{@code name} - The name of the argument.</li>
	 * <li>{@code description} - A human-readable description of the argument.</li>
	 * <li>{@code required} - Whether this argument must be provided.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record PromptArgument with class
	public static final class PromptArgument implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private final String description;

		private final Boolean required;

		public PromptArgument(@JsonProperty("name") String name, @JsonProperty("description") String description,
				@JsonProperty("required") Boolean required) {
			this.name = name;
			this.description = description;
			this.required = required;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("description")
		public String getDescription() {
			return description;
		}

		@JsonProperty("required")
		public Boolean getRequired() {
			return required;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PromptArgument that = (PromptArgument) o;
			return Objects.equals(name, that.name) && Objects.equals(description, that.description)
					&& Objects.equals(required, that.required);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, required);
		}

		@Override
		public String toString() {
			return "PromptArgument{name='" + name + "', description='" + description + "', required=" + required + '}';
		}

	}

	/**
	 * Describes a message returned as part of a prompt.
	 *
	 * This is similar to `SamplingMessage`, but also supports the embedding of resources
	 * from the MCP server.
	 *
	 * <ul>
	 * <li>{@code role} - The sender or recipient of messages and data in a conversation.</li>
	 * <li>{@code content} - The content of the message of type {@link Content}.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record PromptMessage with class
	public static final class PromptMessage implements Serializable {

		private static final long serialVersionUID = 1L;

		private final Role role;

		private final Content content; // Content interface remains the same (polymorphic)

		public PromptMessage(@JsonProperty("role") Role role, @JsonProperty("content") Content content) {
			this.role = role;
			this.content = content;
		}

		@JsonProperty("role")
		public Role getRole() {
			return role;
		}

		@JsonProperty("content")
		public Content getContent() {
			return content;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PromptMessage that = (PromptMessage) o;
			return role == that.role && Objects.equals(content, that.content);
		}

		@Override
		public int hashCode() {
			return Objects.hash(role, content);
		}

		@Override
		public String toString() {
			return "PromptMessage{role=" + role + ", content=" + content + '}';
		}

	}

	/**
	 * The server's response to a prompts/list request from the client.
	 *
	 * <ul>
	 * <li>{@code prompts} - A list of prompts that the server provides.</li>
	 * <li>{@code nextCursor} - An optional cursor for pagination. If present, indicates there
	 * are more prompts available.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ListPromptsResult with class
	public static final class ListPromptsResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Prompt> prompts;

		private final String nextCursor;

		public ListPromptsResult(@JsonProperty("prompts") List<Prompt> prompts,
				@JsonProperty("nextCursor") String nextCursor) {
			this.prompts = prompts;
			this.nextCursor = nextCursor;
		}

		@JsonProperty("prompts")
		public List<Prompt> getPrompts() {
			return prompts;
		}

		@JsonProperty("nextCursor")
		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ListPromptsResult that = (ListPromptsResult) o;
			return Objects.equals(prompts, that.prompts) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(prompts, nextCursor);
		}

		@Override
		public String toString() {
			return "ListPromptsResult{prompts=" + prompts + ", nextCursor='" + nextCursor + "'}";
		}

	}

	/**
	 * Used by the client to get a prompt provided by the server.
	 *
	 * <ul>
	 * <li>{@code name} - The name of the prompt or prompt template.</li>
	 * <li>{@code arguments} - Arguments to use for templating the prompt.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record GetPromptRequest with class
	public static final class GetPromptRequest implements Request, Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private final Map<String, Object> arguments;

		public GetPromptRequest(@JsonProperty("name") String name,
				@JsonProperty("arguments") Map<String, Object> arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("arguments")
		public Map<String, Object> getArguments() {
			return arguments;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			GetPromptRequest that = (GetPromptRequest) o;
			return Objects.equals(name, that.name) && Objects.equals(arguments, that.arguments);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, arguments);
		}

		@Override
		public String toString() {
			return "GetPromptRequest{name='" + name + "', arguments=" + arguments + '}';
		}

	}

	/**
	 * The result of getting a prompt.
	 * <ul>
	 * <li>{@code description} - An optional description for the prompt.</li>
	 * <li>{@code messages} - A list of messages to display as part of the prompt.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record GetPromptResult with class
	public static final class GetPromptResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String description;

		private final List<PromptMessage> messages;

		public GetPromptResult(@JsonProperty("description") String description,
				@JsonProperty("messages") List<PromptMessage> messages) {
			this.description = description;
			this.messages = messages;
		}

		@JsonProperty("description")
		public String getDescription() {
			return description;
		}

		@JsonProperty("messages")
		public List<PromptMessage> getMessages() {
			return messages;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			GetPromptResult that = (GetPromptResult) o;
			return Objects.equals(description, that.description) && Objects.equals(messages, that.messages);
		}

		@Override
		public int hashCode() {
			return Objects.hash(description, messages);
		}

		@Override
		public String toString() {
			return "GetPromptResult{description='" + description + "', messages=" + messages + '}';
		}

	}

	// ---------------------------
	// Tool Interfaces
	// ---------------------------
	/**
	 * The result of listing tools.
	 * <ul>
	 * <li>{@code tools} - A list of tools that the server provides.</li>
	 * <li>{@code nextCursor} - An optional cursor for pagination. If present, indicates there
	 * are more tools available.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ListToolsResult with class
	public static final class ListToolsResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Tool> tools;

		private final String nextCursor;

		public ListToolsResult(@JsonProperty("tools") List<Tool> tools, @JsonProperty("nextCursor") String nextCursor) {
			this.tools = tools;
			this.nextCursor = nextCursor;
		}

		@JsonProperty("tools")
		public List<Tool> getTools() {
			return tools;
		}

		@JsonProperty("nextCursor")
		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ListToolsResult that = (ListToolsResult) o;
			return Objects.equals(tools, that.tools) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(tools, nextCursor);
		}

		@Override
		public String toString() {
			return "ListToolsResult{tools=" + tools + ", nextCursor='" + nextCursor + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record JsonSchema with class
	public static final class JsonSchema implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String type;

		private final Map<String, Object> properties;

		private final List<String> required;

		private final Boolean additionalProperties;

		// Using '$' in field names is discouraged in Java, but keeping for compatibility
		@JsonProperty("$defs")
		private final Map<String, Object> defs;

		private final Map<String, Object> definitions;

		public JsonSchema(@JsonProperty("type") String type, @JsonProperty("properties") Map<String, Object> properties,
				@JsonProperty("required") List<String> required,
				@JsonProperty("additionalProperties") Boolean additionalProperties,
				@JsonProperty("$defs") Map<String, Object> defs,
				@JsonProperty("definitions") Map<String, Object> definitions) {
			this.type = type;
			this.properties = properties;
			this.required = required;
			this.additionalProperties = additionalProperties;
			this.defs = defs;
			this.definitions = definitions;
		}

		@JsonProperty("type")
		public String getType() {
			return type;
		}

		@JsonProperty("properties")
		public Map<String, Object> getProperties() {
			return properties;
		}

		@JsonProperty("required")
		public List<String> getRequired() {
			return required;
		}

		@JsonProperty("additionalProperties")
		public Boolean getAdditionalProperties() {
			return additionalProperties;
		}

		@JsonProperty("$defs")
		public Map<String, Object> getDefs() {
			return defs;
		}

		@JsonProperty("definitions")
		public Map<String, Object> getDefinitions() {
			return definitions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			JsonSchema that = (JsonSchema) o;
			return Objects.equals(type, that.type) && Objects.equals(properties, that.properties)
					&& Objects.equals(required, that.required)
					&& Objects.equals(additionalProperties, that.additionalProperties)
					&& Objects.equals(defs, that.defs) && Objects.equals(definitions, that.definitions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, properties, required, additionalProperties, defs, definitions);
		}

		@Override
		public String toString() {
			return "JsonSchema{type='" + type + "', properties=" + properties + ", required=" + required
					+ ", additionalProperties=" + additionalProperties + ", defs=" + defs + ", definitions="
					+ definitions + '}';
		}

	}

	/**
	 * A tool that can be called by the client.
	 * <ul>
	 * <li>{@code name} - A unique identifier for the tool. This name is used when calling the
	 * tool.</li>
	 * <li>{@code description} - A human-readable description of what the tool does. This can be
	 * displayed to users to help them understand the tool's functionality.</li>
	 * <li>{@code inputSchema} - A JSON Schema object that describes the expected structure of
	 * the tool's input parameters.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record Tool with class
	public static final class Tool implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private final String description;

		private final JsonSchema inputSchema;

		// Primary constructor for Jackson
		public Tool(@JsonProperty("name") String name, @JsonProperty("description") String description,
				@JsonProperty("inputSchema") JsonSchema inputSchema) {
			this.name = name;
			this.description = description;
			this.inputSchema = inputSchema;
		}

		// Convenience constructor from original record
		public Tool(String name, String description, String schema) {
			this(name, description, parseSchema(schema));
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("description")
		public String getDescription() {
			return description;
		}

		@JsonProperty("inputSchema")
		public JsonSchema getInputSchema() {
			return inputSchema;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Tool tool = (Tool) o;
			return Objects.equals(name, tool.name) && Objects.equals(description, tool.description)
					&& Objects.equals(inputSchema, tool.inputSchema);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, inputSchema);
		}

		@Override
		public String toString() {
			return "Tool{name='" + name + "', description='" + description + "', inputSchema=" + inputSchema + '}';
		}

	}

	// Kept private static method as is
	private static JsonSchema parseSchema(String schema) {
		try {
			return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Invalid schema: " + schema, e);
		}
	}

	/**
	 * A request to call a tool.
	 * <ul>
	 * <li>{@code name} - The name of the tool to call. This must match a tool name from
	 * the tools list.</li>
	 * <li>{@code arguments} - Arguments to pass to the tool. These must conform to the tool's
	 * input schema.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record CallToolRequest with class
	public static final class CallToolRequest implements Request, Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private final Map<String, Object> arguments;

		// Primary constructor for Jackson
		public CallToolRequest(@JsonProperty("name") String name,
				@JsonProperty("arguments") Map<String, Object> arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		// Convenience constructor from original record
		public CallToolRequest(String name, String jsonArguments) {
			this(name, parseJsonArguments(jsonArguments));
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("arguments")
		public Map<String, Object> getArguments() {
			return arguments;
		}

		// Kept private static method as is
		private static Map<String, Object> parseJsonArguments(String jsonArguments) {
			try {
				return OBJECT_MAPPER.readValue(jsonArguments, MAP_TYPE_REF);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CallToolRequest that = (CallToolRequest) o;
			return Objects.equals(name, that.name) && Objects.equals(arguments, that.arguments);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, arguments);
		}

		@Override
		public String toString() {
			return "CallToolRequest{name='" + name + "', arguments=" + arguments + '}';
		}

	}

	/**
	 * The result of calling a tool.
	 * <ul>
	 * <li>{@code content} - A list of content items representing the tool's output. Each item
	 * can be of different types (text, image, etc.).</li>
	 * <li>{@code isError} - If true, indicates that the tool execution failed and the content
	 * describes the error that occurred.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record CallToolResult with class
	public static final class CallToolResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Content> content;

		private final Boolean isError;

		// Constructor used by Builder and Jackson
		public CallToolResult(@JsonProperty("content") List<Content> content,
				@JsonProperty("isError") Boolean isError) {
			this.content = content;
			this.isError = isError;
		}

		/**
		 * Creates a new instance of {@link CallToolResult} with a string containing the
		 * tool result. Replaced List.of with JDK 8 compatible alternative.
		 * 
		 * <ul>
		 * <li>{@code content} - The content of the tool result. This will be mapped to a
		 * one-sized list with a {@link TextContent} element.</li>
		 * <li>{@code isError} - If true, indicates that the tool execution failed and the
		 * content contains error information. If false or absent, indicates successful
		 * execution.</li>
		 * </ul>
		 */
		public CallToolResult(String content, Boolean isError) {
			// Replaced List.of with Collections.singletonList (immutable) or
			// Arrays.asList (fixed-size)
			this(Collections.singletonList(new TextContent(content)), isError);
			// Alternatively, use Arrays.asList if mutability of the list itself is not
			// critical:
			// this(Arrays.asList(new TextContent(content)), isError);
		}

		@JsonProperty("content")
		public List<Content> getContent() {
			return content;
		}

		@JsonProperty("isError")
		public Boolean getIsError() {
			return isError;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CallToolResult that = (CallToolResult) o;
			return Objects.equals(content, that.content) && Objects.equals(isError, that.isError);
		}

		@Override
		public int hashCode() {
			return Objects.hash(content, isError);
		}

		@Override
		public String toString() {
			return "CallToolResult{content=" + content + ", isError=" + isError + '}';
		}

		/**
		 * Creates a builder for {@link CallToolResult}.
		 * @return a new builder instance
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * Builder for {@link CallToolResult}.
		 */
		public static class Builder {

			private List<Content> content = new ArrayList<>();

			private Boolean isError;

			/**
			 * Sets the content list for the tool result.
			 * 
			 * @return this builder
			 */
			public Builder content(List<Content> content) {
				Assert.notNull(content, "content must not be null");
				this.content = new ArrayList<>(content); // Defensive copy
				return this;
			}

			/**
			 * Sets the text content for the tool result.
			 * 
			 * @return this builder
			 */
			public Builder textContent(List<String> textContent) {
				Assert.notNull(textContent, "textContent must not be null");
				this.content = new ArrayList<>(); // Reset content list
				for (String text : textContent) {
					this.content.add(new TextContent(text));
				}
				return this;
			}

			/**
			 * Adds a content item to the tool result.
			 * 
			 * @return this builder
			 */
			public Builder addContent(Content contentItem) {
				Assert.notNull(contentItem, "contentItem must not be null");
				if (this.content == null) { // Should not happen due to initialization
					this.content = new ArrayList<>();
				}
				this.content.add(contentItem);
				return this;
			}

			/**
			 * Adds a text content item to the tool result.
			 * 
			 * @return this builder
			 */
			public Builder addTextContent(String text) {
				Assert.notNull(text, "text must not be null");
				return addContent(new TextContent(text));
			}

			/**
			 * Sets whether the tool execution resulted in an error.
			 * 
			 * @return this builder
			 */
			public Builder isError(Boolean isError) {
				// Original code didn't assert not null here, but it's good practice
				// Assert.notNull(isError, "isError must not be null");
				this.isError = isError;
				return this;
			}

			/**
			 * Builds a new {@link CallToolResult} instance.
			 * @return a new CallToolResult instance
			 */
			public CallToolResult build() {
				return new CallToolResult(content, isError);
			}

		}

	}

	// ---------------------------
	// Sampling Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ModelPreferences with class
	public static final class ModelPreferences implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<ModelHint> hints;

		private final Double costPriority;

		private final Double speedPriority;

		private final Double intelligencePriority;

		// Constructor for Jackson and Builder
		public ModelPreferences(@JsonProperty("hints") List<ModelHint> hints,
				@JsonProperty("costPriority") Double costPriority, @JsonProperty("speedPriority") Double speedPriority,
				@JsonProperty("intelligencePriority") Double intelligencePriority) {
			this.hints = hints;
			this.costPriority = costPriority;
			this.speedPriority = speedPriority;
			this.intelligencePriority = intelligencePriority;
		}

		@JsonProperty("hints")
		public List<ModelHint> getHints() {
			return hints;
		}

		@JsonProperty("costPriority")
		public Double getCostPriority() {
			return costPriority;
		}

		@JsonProperty("speedPriority")
		public Double getSpeedPriority() {
			return speedPriority;
		}

		@JsonProperty("intelligencePriority")
		public Double getIntelligencePriority() {
			return intelligencePriority;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ModelPreferences that = (ModelPreferences) o;
			return Objects.equals(hints, that.hints) && Objects.equals(costPriority, that.costPriority)
					&& Objects.equals(speedPriority, that.speedPriority)
					&& Objects.equals(intelligencePriority, that.intelligencePriority);
		}

		@Override
		public int hashCode() {
			return Objects.hash(hints, costPriority, speedPriority, intelligencePriority);
		}

		@Override
		public String toString() {
			return "ModelPreferences{hints=" + hints + ", costPriority=" + costPriority + ", speedPriority="
					+ speedPriority + ", intelligencePriority=" + intelligencePriority + '}';
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private List<ModelHint> hints;

			private Double costPriority;

			private Double speedPriority;

			private Double intelligencePriority;

			public Builder hints(List<ModelHint> hints) {
				this.hints = hints;
				return this;
			}

			public Builder addHint(String name) {
				if (this.hints == null) {
					this.hints = new ArrayList<>();
				}
				this.hints.add(new ModelHint(name));
				return this;
			}

			public Builder costPriority(Double costPriority) {
				this.costPriority = costPriority;
				return this;
			}

			public Builder speedPriority(Double speedPriority) {
				this.speedPriority = speedPriority;
				return this;
			}

			public Builder intelligencePriority(Double intelligencePriority) {
				this.intelligencePriority = intelligencePriority;
				return this;
			}

			public ModelPreferences build() {
				return new ModelPreferences(hints, costPriority, speedPriority, intelligencePriority);
			}

		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ModelHint with class
	public static final class ModelHint implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		public ModelHint(@JsonProperty("name") String name) {
			this.name = name;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		// Static factory method kept as is
		public static ModelHint of(String name) {
			return new ModelHint(name);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ModelHint modelHint = (ModelHint) o;
			return Objects.equals(name, modelHint.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public String toString() {
			return "ModelHint{name='" + name + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record SamplingMessage with class
	public static final class SamplingMessage implements Serializable {

		private static final long serialVersionUID = 1L;

		private final Role role;

		private final Content content;

		public SamplingMessage(@JsonProperty("role") Role role, @JsonProperty("content") Content content) {
			this.role = role;
			this.content = content;
		}

		@JsonProperty("role")
		public Role getRole() {
			return role;
		}

		@JsonProperty("content")
		public Content getContent() {
			return content;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SamplingMessage that = (SamplingMessage) o;
			return role == that.role && Objects.equals(content, that.content);
		}

		@Override
		public int hashCode() {
			return Objects.hash(role, content);
		}

		@Override
		public String toString() {
			return "SamplingMessage{role=" + role + ", content=" + content + '}';
		}

	}

	// Sampling and Message Creation
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record CreateMessageRequest with class
	public static final class CreateMessageRequest implements Request, Serializable {

		private static final long serialVersionUID = 1L;

		private final List<SamplingMessage> messages;

		private final ModelPreferences modelPreferences;

		private final String systemPrompt;

		private final ContextInclusionStrategy includeContext;

		private final Double temperature;

		private final Integer maxTokens; // Changed int to Integer to allow null/absence

		private final List<String> stopSequences;

		private final Map<String, Object> metadata;

		// Constructor for Jackson and Builder
		public CreateMessageRequest(@JsonProperty("messages") List<SamplingMessage> messages,
				@JsonProperty("modelPreferences") ModelPreferences modelPreferences,
				@JsonProperty("systemPrompt") String systemPrompt,
				@JsonProperty("includeContext") ContextInclusionStrategy includeContext,
				@JsonProperty("temperature") Double temperature, @JsonProperty("maxTokens") Integer maxTokens, // Changed
				@JsonProperty("stopSequences") List<String> stopSequences,
				@JsonProperty("metadata") Map<String, Object> metadata) {
			this.messages = messages;
			this.modelPreferences = modelPreferences;
			this.systemPrompt = systemPrompt;
			this.includeContext = includeContext;
			this.temperature = temperature;
			this.maxTokens = maxTokens;
			this.stopSequences = stopSequences;
			this.metadata = metadata;
		}

		@JsonProperty("messages")
		public List<SamplingMessage> getMessages() {
			return messages;
		}

		@JsonProperty("modelPreferences")
		public ModelPreferences getModelPreferences() {
			return modelPreferences;
		}

		@JsonProperty("systemPrompt")
		public String getSystemPrompt() {
			return systemPrompt;
		}

		@JsonProperty("includeContext")
		public ContextInclusionStrategy getIncludeContext() {
			return includeContext;
		}

		@JsonProperty("temperature")
		public Double getTemperature() {
			return temperature;
		}

		@JsonProperty("maxTokens")
		public Integer getMaxTokens() {
			return maxTokens;
		} // Changed

		@JsonProperty("stopSequences")
		public List<String> getStopSequences() {
			return stopSequences;
		}

		@JsonProperty("metadata")
		public Map<String, Object> getMetadata() {
			return metadata;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CreateMessageRequest that = (CreateMessageRequest) o;
			return Objects.equals(messages, that.messages) && Objects.equals(modelPreferences, that.modelPreferences)
					&& Objects.equals(systemPrompt, that.systemPrompt) && includeContext == that.includeContext
					&& Objects.equals(temperature, that.temperature) && Objects.equals(maxTokens, that.maxTokens)
					&& Objects.equals(stopSequences, that.stopSequences) && Objects.equals(metadata, that.metadata);
		}

		@Override
		public int hashCode() {
			return Objects.hash(messages, modelPreferences, systemPrompt, includeContext, temperature, maxTokens,
					stopSequences, metadata);
		}

		@Override
		public String toString() {
			return "CreateMessageRequest{messages=" + messages + ", modelPreferences=" + modelPreferences
					+ ", systemPrompt='" + systemPrompt + "', includeContext=" + includeContext + ", temperature="
					+ temperature + ", maxTokens=" + maxTokens + ", stopSequences=" + stopSequences + ", metadata="
					+ metadata + '}';
		}

		public enum ContextInclusionStrategy {

			@JsonProperty("none")
			NONE, @JsonProperty("thisServer")
			THIS_SERVER, @JsonProperty("allServers")
			ALL_SERVERS

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private List<SamplingMessage> messages;

			private ModelPreferences modelPreferences;

			private String systemPrompt;

			private ContextInclusionStrategy includeContext;

			private Double temperature;

			private Integer maxTokens; // Changed

			private List<String> stopSequences;

			private Map<String, Object> metadata;

			public Builder messages(List<SamplingMessage> messages) {
				this.messages = messages;
				return this;
			}

			public Builder modelPreferences(ModelPreferences modelPreferences) {
				this.modelPreferences = modelPreferences;
				return this;
			}

			public Builder systemPrompt(String systemPrompt) {
				this.systemPrompt = systemPrompt;
				return this;
			}

			public Builder includeContext(ContextInclusionStrategy includeContext) {
				this.includeContext = includeContext;
				return this;
			}

			public Builder temperature(Double temperature) {
				this.temperature = temperature;
				return this;
			}

			public Builder maxTokens(int maxTokens) { // Keep int here for convenience if
														// non-null expected
				this.maxTokens = maxTokens;
				return this;
			}

			public Builder maxTokens(Integer maxTokens) { // Allow Integer for null
				this.maxTokens = maxTokens;
				return this;
			}

			public Builder stopSequences(List<String> stopSequences) {
				this.stopSequences = stopSequences;
				return this;
			}

			public Builder metadata(Map<String, Object> metadata) {
				this.metadata = metadata;
				return this;
			}

			public CreateMessageRequest build() {
				return new CreateMessageRequest(messages, modelPreferences, systemPrompt, includeContext, temperature,
						maxTokens, stopSequences, metadata);
			}

		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record CreateMessageResult with class
	public static final class CreateMessageResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final Role role;

		private final Content content;

		private final String model;

		private final StopReason stopReason;

		// Constructor for Jackson and Builder
		public CreateMessageResult(@JsonProperty("role") Role role, @JsonProperty("content") Content content,
				@JsonProperty("model") String model, @JsonProperty("stopReason") StopReason stopReason) {
			this.role = role;
			this.content = content;
			this.model = model;
			this.stopReason = stopReason;
		}

		@JsonProperty("role")
		public Role getRole() {
			return role;
		}

		@JsonProperty("content")
		public Content getContent() {
			return content;
		}

		@JsonProperty("model")
		public String getModel() {
			return model;
		}

		@JsonProperty("stopReason")
		public StopReason getStopReason() {
			return stopReason;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CreateMessageResult that = (CreateMessageResult) o;
			return role == that.role && Objects.equals(content, that.content) && Objects.equals(model, that.model)
					&& stopReason == that.stopReason;
		}

		@Override
		public int hashCode() {
			return Objects.hash(role, content, model, stopReason);
		}

		@Override
		public String toString() {
			return "CreateMessageResult{role=" + role + ", content=" + content + ", model='" + model + "', stopReason="
					+ stopReason + '}';
		}

		public enum StopReason {

			@JsonProperty("endTurn")
			END_TURN, @JsonProperty("stopSequence")
			STOP_SEQUENCE, @JsonProperty("maxTokens")
			MAX_TOKENS

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Role role = Role.ASSISTANT;

			private Content content;

			private String model;

			private StopReason stopReason = StopReason.END_TURN;

			public Builder role(Role role) {
				this.role = role;
				return this;
			}

			public Builder content(Content content) {
				this.content = content;
				return this;
			}

			public Builder model(String model) {
				this.model = model;
				return this;
			}

			public Builder stopReason(StopReason stopReason) {
				this.stopReason = stopReason;
				return this;
			}

			public Builder message(String message) {
				this.content = new TextContent(message);
				return this;
			}

			public CreateMessageResult build() {
				return new CreateMessageResult(role, content, model, stopReason);
			}

		}

	}

	// ---------------------------
	// Pagination Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record PaginatedRequest with class
	public static class PaginatedRequest implements Serializable {

		// Made non-final for
		// potential extension
		private static final long serialVersionUID = 1L;

		private final String cursor;

		public PaginatedRequest(@JsonProperty("cursor") String cursor) {
			this.cursor = cursor;
		}

		@JsonProperty("cursor")
		public String getCursor() {
			return cursor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PaginatedRequest that = (PaginatedRequest) o;
			return Objects.equals(cursor, that.cursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(cursor);
		}

		@Override
		public String toString() {
			return "PaginatedRequest{cursor='" + cursor + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record PaginatedResult with class
	public static class PaginatedResult implements Serializable {

		// Made non-final for
		// potential extension
		private static final long serialVersionUID = 1L;

		private final String nextCursor;

		public PaginatedResult(@JsonProperty("nextCursor") String nextCursor) {
			this.nextCursor = nextCursor;
		}

		@JsonProperty("nextCursor")
		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PaginatedResult that = (PaginatedResult) o;
			return Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(nextCursor);
		}

		@Override
		public String toString() {
			return "PaginatedResult{nextCursor='" + nextCursor + "'}";
		}

	}

	// ---------------------------
	// Progress and Logging
	// ---------------------------
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ProgressNotification with class
	public static final class ProgressNotification implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String progressToken;

		private final double progress;

		private final Double total; // Kept as Double for optionality

		public ProgressNotification(@JsonProperty("progressToken") String progressToken,
				@JsonProperty("progress") double progress, @JsonProperty("total") Double total) {
			this.progressToken = progressToken;
			this.progress = progress;
			this.total = total;
		}

		@JsonProperty("progressToken")
		public String getProgressToken() {
			return progressToken;
		}

		@JsonProperty("progress")
		public double getProgress() {
			return progress;
		}

		@JsonProperty("total")
		public Double getTotal() {
			return total;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ProgressNotification that = (ProgressNotification) o;
			return Double.compare(that.progress, progress) == 0 && Objects.equals(progressToken, that.progressToken)
					&& Objects.equals(total, that.total);
		}

		@Override
		public int hashCode() {
			return Objects.hash(progressToken, progress, total);
		}

		@Override
		public String toString() {
			return "ProgressNotification{progressToken='" + progressToken + "', progress=" + progress + ", total="
					+ total + '}';
		}

	}

	/**
	 * A notification about a logging message.
	 * <ul>
	 * <li>{@code level} - The severity levels. The minimum log level is set by the client.</li>
	 * <li>{@code logger} - The logger that generated the message.</li>
	 * <li>{@code data} - JSON-serializable logging data.</li>
	 * </ul>
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record LoggingMessageNotification with class
	public static final class LoggingMessageNotification implements Serializable {

		private static final long serialVersionUID = 1L;

		private final LoggingLevel level;

		private final String logger;

		private final String data; // Assuming String representation of JSON data

		// Constructor for Jackson and Builder
		public LoggingMessageNotification(@JsonProperty("level") LoggingLevel level,
				@JsonProperty("logger") String logger, @JsonProperty("data") String data) {
			this.level = level;
			this.logger = logger;
			this.data = data;
		}

		@JsonProperty("level")
		public LoggingLevel getLevel() {
			return level;
		}

		@JsonProperty("logger")
		public String getLogger() {
			return logger;
		}

		@JsonProperty("data")
		public String getData() {
			return data;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			LoggingMessageNotification that = (LoggingMessageNotification) o;
			return level == that.level && Objects.equals(logger, that.logger) && Objects.equals(data, that.data);
		}

		@Override
		public int hashCode() {
			return Objects.hash(level, logger, data);
		}

		@Override
		public String toString() {
			return "LoggingMessageNotification{level=" + level + ", logger='" + logger + "', data='" + data + "'}";
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private LoggingLevel level = LoggingLevel.INFO;

			private String logger = "server";

			private String data;

			public Builder level(LoggingLevel level) {
				this.level = level;
				return this;
			}

			public Builder logger(String logger) {
				this.logger = logger;
				return this;
			}

			public Builder data(String data) {
				this.data = data;
				return this;
			}

			public LoggingMessageNotification build() {
				return new LoggingMessageNotification(level, logger, data);
			}

		}

	}

	public enum LoggingLevel {

		@JsonProperty("debug")
		DEBUG(0), @JsonProperty("info")
		INFO(1), @JsonProperty("notice")
		NOTICE(2), @JsonProperty("warning")
		WARNING(3), @JsonProperty("error")
		ERROR(4), @JsonProperty("critical")
		CRITICAL(5), @JsonProperty("alert")
		ALERT(6), @JsonProperty("emergency")
		EMERGENCY(7);

		private final int level;

		LoggingLevel(int level) {
			this.level = level;
		}

		// Changed from method name to getter
		public int getLevel() {
			return level;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record SetLevelRequest with class
	public static final class SetLevelRequest implements Serializable {

		private static final long serialVersionUID = 1L;

		private final LoggingLevel level;

		public SetLevelRequest(@JsonProperty("level") LoggingLevel level) {
			this.level = level;
		}

		@JsonProperty("level")
		public LoggingLevel getLevel() {
			return level;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SetLevelRequest that = (SetLevelRequest) o;
			return level == that.level;
		}

		@Override
		public int hashCode() {
			return Objects.hash(level);
		}

		@Override
		public String toString() {
			return "SetLevelRequest{level=" + level + '}';
		}

	}

	// ---------------------------
	// Autocomplete
	// ---------------------------
	// Replaced sealed interface CompleteReference
	public interface CompleteReference {

		String getType(); // Changed from method name to getter

		String getIdentifier(); // Changed from method name to getter

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record PromptReference with class
	public static final class PromptReference implements CompleteReference, Serializable {

		private static final long serialVersionUID = 1L;

		private final String type;

		private final String name;

		// Jackson constructor
		public PromptReference(@JsonProperty("type") String type, @JsonProperty("name") String name) {
			this.type = type;
			this.name = name;
		}

		// Convenience constructor from original record
		public PromptReference(String name) {
			this("ref/prompt", name);
		}

		@JsonProperty("type")
		public String getType() {
			return type;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@Override
		public String getIdentifier() {
			return getName(); // Use getter
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PromptReference that = (PromptReference) o;
			return Objects.equals(type, that.type) && Objects.equals(name, that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, name);
		}

		@Override
		public String toString() {
			return "PromptReference{type='" + type + "', name='" + name + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ResourceReference with class
	public static final class ResourceReference implements CompleteReference, Serializable {

		private static final long serialVersionUID = 1L;

		private final String type;

		private final String uri;

		// Jackson constructor
		public ResourceReference(@JsonProperty("type") String type, @JsonProperty("uri") String uri) {
			this.type = type;
			this.uri = uri;
		}

		// Convenience constructor from original record
		public ResourceReference(String uri) {
			this("ref/resource", uri);
		}

		@JsonProperty("type")
		public String getType() {
			return type;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@Override
		public String getIdentifier() {
			return getUri(); // Use getter
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ResourceReference that = (ResourceReference) o;
			return Objects.equals(type, that.type) && Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, uri);
		}

		@Override
		public String toString() {
			return "ResourceReference{type='" + type + "', uri='" + uri + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record CompleteRequest with class
	public static final class CompleteRequest implements Request, Serializable {

		private static final long serialVersionUID = 1L;

		private final CompleteReference ref;

		private final CompleteArgument argument;

		public CompleteRequest(@JsonProperty("ref") CompleteReference ref,
				@JsonProperty("argument") CompleteArgument argument) {
			this.ref = ref;
			this.argument = argument;
		}

		@JsonProperty("ref")
		public CompleteReference getRef() {
			return ref;
		}

		@JsonProperty("argument")
		public CompleteArgument getArgument() {
			return argument;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CompleteRequest that = (CompleteRequest) o;
			return Objects.equals(ref, that.ref) && Objects.equals(argument, that.argument);
		}

		@Override
		public int hashCode() {
			return Objects.hash(ref, argument);
		}

		@Override
		public String toString() {
			return "CompleteRequest{ref=" + ref + ", argument=" + argument + '}';
		}

		// Replaced nested record CompleteArgument with static nested class
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static final class CompleteArgument implements Serializable {

			private static final long serialVersionUID = 1L;

			private final String name;

			private final String value;

			public CompleteArgument(@JsonProperty("name") String name, @JsonProperty("value") String value) {
				this.name = name;
				this.value = value;
			}

			@JsonProperty("name")
			public String getName() {
				return name;
			}

			@JsonProperty("value")
			public String getValue() {
				return value;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				CompleteArgument that = (CompleteArgument) o;
				return Objects.equals(name, that.name) && Objects.equals(value, that.value);
			}

			@Override
			public int hashCode() {
				return Objects.hash(name, value);
			}

			@Override
			public String toString() {
				return "CompleteArgument{name='" + name + "', value='" + value + "'}";
			}

		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record CompleteResult with class
	public static final class CompleteResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final CompleteCompletion completion;

		public CompleteResult(@JsonProperty("completion") CompleteCompletion completion) {
			this.completion = completion;
		}

		@JsonProperty("completion")
		public CompleteCompletion getCompletion() {
			return completion;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CompleteResult that = (CompleteResult) o;
			return Objects.equals(completion, that.completion);
		}

		@Override
		public int hashCode() {
			return Objects.hash(completion);
		}

		@Override
		public String toString() {
			return "CompleteResult{completion=" + completion + '}';
		}

		// Replaced nested record CompleteCompletion with static nested class
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static final class CompleteCompletion implements Serializable {

			private static final long serialVersionUID = 1L;

			private final List<String> values;

			private final Integer total;

			private final Boolean hasMore;

			public CompleteCompletion(@JsonProperty("values") List<String> values, @JsonProperty("total") Integer total,
					@JsonProperty("hasMore") Boolean hasMore) {
				this.values = values;
				this.total = total;
				this.hasMore = hasMore;
			}

			@JsonProperty("values")
			public List<String> getValues() {
				return values;
			}

			@JsonProperty("total")
			public Integer getTotal() {
				return total;
			}

			@JsonProperty("hasMore")
			public Boolean getHasMore() {
				return hasMore;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				CompleteCompletion that = (CompleteCompletion) o;
				return Objects.equals(values, that.values) && Objects.equals(total, that.total)
						&& Objects.equals(hasMore, that.hasMore);
			}

			@Override
			public int hashCode() {
				return Objects.hash(values, total, hasMore);
			}

			@Override
			public String toString() {
				return "CompleteCompletion{values=" + values + ", total=" + total + ", hasMore=" + hasMore + '}';
			}

		}

	}

	// ---------------------------
	// Content Types
	// ---------------------------
	// Replaced sealed interface Content
	// Kept existing @JsonTypeInfo and @JsonSubTypes as they are generally JDK 8
	// compatible with appropriate Jackson version
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
			@JsonSubTypes.Type(value = ImageContent.class, name = "image"),
			@JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
	public interface Content {

		// No default method needed as type is handled by annotations
		// String getType(); // Could add abstract method if needed programmatically

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record TextContent with class
	@JsonTypeName("text") // Ensure type name is set for Id.NAME strategy
	public static final class TextContent implements Content, Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Role> audience;

		private final Double priority;

		private final String text;

		// Jackson constructor
		public TextContent(@JsonProperty("audience") List<Role> audience, @JsonProperty("priority") Double priority,
				@JsonProperty("text") String text) {
			this.audience = audience;
			this.priority = priority;
			this.text = text;
		}

		// Convenience constructor from original record
		public TextContent(String content) {
			this(null, null, content);
		}

		@JsonProperty("audience")
		public List<Role> getAudience() {
			return audience;
		}

		@JsonProperty("priority")
		public Double getPriority() {
			return priority;
		}

		@JsonProperty("text")
		public String getText() {
			return text;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TextContent that = (TextContent) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority)
					&& Objects.equals(text, that.text);
		}

		@Override
		public int hashCode() {
			return Objects.hash(audience, priority, text);
		}

		@Override
		public String toString() {
			return "TextContent{audience=" + audience + ", priority=" + priority + ", text='" + text + "'}";
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ImageContent with class
	@JsonTypeName("image") // Ensure type name is set for Id.NAME strategy
	public static final class ImageContent implements Content, Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Role> audience;

		private final Double priority;

		private final String data;

		private final String mimeType;

		public ImageContent(@JsonProperty("audience") List<Role> audience, @JsonProperty("priority") Double priority,
				@JsonProperty("data") String data, @JsonProperty("mimeType") String mimeType) {
			this.audience = audience;
			this.priority = priority;
			this.data = data;
			this.mimeType = mimeType;
		}

		@JsonProperty("audience")
		public List<Role> getAudience() {
			return audience;
		}

		@JsonProperty("priority")
		public Double getPriority() {
			return priority;
		}

		@JsonProperty("data")
		public String getData() {
			return data;
		}

		@JsonProperty("mimeType")
		public String getMimeType() {
			return mimeType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ImageContent that = (ImageContent) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority)
					&& Objects.equals(data, that.data) && Objects.equals(mimeType, that.mimeType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(audience, priority, data, mimeType);
		}

		@Override
		public String toString() {
			return "ImageContent{audience=" + audience + ", priority=" + priority + ", data='[hidden]', mimeType='"
					+ mimeType + "'}";
		} // Avoid logging image data

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record EmbeddedResource with class
	@JsonTypeName("resource") // Ensure type name is set for Id.NAME strategy
	public static final class EmbeddedResource implements Content, Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Role> audience;

		private final Double priority;

		private final ResourceContents resource; // Interface ResourceContents remains

		public EmbeddedResource(@JsonProperty("audience") List<Role> audience,
				@JsonProperty("priority") Double priority, @JsonProperty("resource") ResourceContents resource) {
			this.audience = audience;
			this.priority = priority;
			this.resource = resource;
		}

		@JsonProperty("audience")
		public List<Role> getAudience() {
			return audience;
		}

		@JsonProperty("priority")
		public Double getPriority() {
			return priority;
		}

		@JsonProperty("resource")
		public ResourceContents getResource() {
			return resource;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			EmbeddedResource that = (EmbeddedResource) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority)
					&& Objects.equals(resource, that.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(audience, priority, resource);
		}

		@Override
		public String toString() {
			return "EmbeddedResource{audience=" + audience + ", priority=" + priority + ", resource=" + resource + '}';
		}

	}

	// ---------------------------
	// Roots
	// ---------------------------
	/**
	 * Represents a root directory or file.
	 * <ul>
	 * <li>{@code uri} - The URI identifying the root. This *must* start with file:// for now.
	 * In the future, other protocols may be supported.</li>
	 * <li>{@code name} - An optional name for the root. This can be used to provide a
	 * human-readable identifier for the root.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record Root with class
	public static final class Root implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String uri;

		private final String name;

		public Root(@JsonProperty("uri") String uri, @JsonProperty("name") String name) {
			this.uri = uri;
			this.name = name;
		}

		@JsonProperty("uri")
		public String getUri() {
			return uri;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Root root = (Root) o;
			return Objects.equals(uri, root.uri) && Objects.equals(name, root.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, name);
		}

		@Override
		public String toString() {
			return "Root{uri='" + uri + "', name='" + name + "'}";
		}

	}

	/**
	 * The result of listing roots.
	 * <ul>
	 * <li>{@code roots} - An array of Root objects, each representing a root directory or file
	 * that the client has made available to the server.</li>
	 * </ul>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	// Replaced record ListRootsResult with class
	public static final class ListRootsResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Root> roots;

		public ListRootsResult(@JsonProperty("roots") List<Root> roots) {
			this.roots = roots;
		}

		@JsonProperty("roots")
		public List<Root> getRoots() {
			return roots;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ListRootsResult that = (ListRootsResult) o;
			return Objects.equals(roots, that.roots);
		}

		@Override
		public int hashCode() {
			return Objects.hash(roots);
		}

		@Override
		public String toString() {
			return "ListRootsResult{roots=" + roots + '}';
		}

	}

}
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.aigw.client.transport.ServerParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Configuration properties for the Model Context Protocol (MCP) stdio client.
 * <p>
 * This class manages configuration settings for MCP stdio client connections, including
 * server parameters, timeouts, and connection details. It supports both direct
 * configuration through properties and configuration through external resource files.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
@ConfigurationProperties(McpStdioClientProperties.CONFIG_PREFIX)
public class McpStdioClientProperties {

	public static final String CONFIG_PREFIX = "spring.ai.mcp.client.stdio";

	/**
	 * Resource containing the MCP servers configuration.
	 * <p>
	 * This resource should contain a JSON configuration defining the MCP servers and
	 * their parameters.
	 */
	private Resource serversConfiguration;

	/**
	 * Map of MCP stdio connections configurations.
	 * <p>
	 * Each entry represents a named connection with its specific configuration
	 * parameters.
	 */
	private final Map<String, Parameters> connections = new HashMap<>();

	public Resource getServersConfiguration() {
		return this.serversConfiguration;
	}

	public void setServersConfiguration(Resource stdioConnectionResources) {
		this.serversConfiguration = stdioConnectionResources;
	}

	public Map<String, Parameters> getConnections() {
		return this.connections;
	}

	private Map<String, ServerParameters> resourceToServerParameters() {
		try {
			Map<String, Map<String, Parameters>> stdioConnection = new ObjectMapper().readValue(
					this.serversConfiguration.getInputStream(),
					new TypeReference<Map<String, Map<String, Parameters>>>() {
					});

			Map<String, Parameters> mcpServerJsonConfig = stdioConnection.entrySet().iterator().next().getValue();

			return mcpServerJsonConfig.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey(), kv -> {
				Parameters parameters = kv.getValue();
				return ServerParameters.builder(parameters.command())
					.args(parameters.args())
					.env(parameters.env())
					.build();
			}));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to read stdio connection resource", e);
		}
	}

	public Map<String, ServerParameters> toServerParameters() {
		Map<String, ServerParameters> serverParameters = new HashMap<>();
		if (this.serversConfiguration != null) {
			serverParameters.putAll(resourceToServerParameters());
		}

		for (Map.Entry<String, Parameters> entry : this.connections.entrySet()) {
			serverParameters.put(entry.getKey(), entry.getValue().toServerParameters());
		}
		return serverParameters;
	}

	/**
	 * Class representing the parameters for an MCP server connection.
	 * <p>
	 * Includes the command to execute, command arguments, and environment variables.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	public static class Parameters {
		
		/**
		 * The command to execute for the MCP server.
		 */
		@JsonProperty("command")
		private final String command;
		
		/**
		 * List of command arguments.
		 */
		@JsonProperty("args")
		private final List<String> args;
		
		/**
		 * Map of environment variables for the server process.
		 */
		@JsonProperty("env")
		private final Map<String, String> env;
		
		/**
		 * Constructor for the server connection parameters.
		 * @param command The command to execute for the MCP server
		 * @param args List of command arguments
		 * @param env Map of environment variables for the server process
		 */
		public Parameters(String command, List<String> args, Map<String, String> env) {
			this.command = command;
			this.args = args;
			this.env = env;
		}
		
		/**
		 * Returns the command to execute for the MCP server.
		 * @return the command
		 */
		public String command() {
			return command;
		}
		
		/**
		 * Returns the list of command arguments.
		 * @return the arguments
		 */
		public List<String> args() {
			return args;
		}
		
		/**
		 * Returns the map of environment variables for the server process.
		 * @return the environment variables
		 */
		public Map<String, String> env() {
			return env;
		}
		
		/**
		 * Converts the parameters to ServerParameters for use with the MCP SDK.
		 * @return the server parameters
		 */
		public ServerParameters toServerParameters() {
			return ServerParameters.builder(this.command()).args(this.args()).env(this.env()).build();
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Parameters that = (Parameters) o;
			return Objects.equals(command, that.command) &&
				   Objects.equals(args, that.args) &&
				   Objects.equals(env, that.env);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(command, args, env);
		}
		
		@Override
		public String toString() {
			return "Parameters[command=" + command + ", args=" + args + ", env=" + env + "]";
		}
	}
} 
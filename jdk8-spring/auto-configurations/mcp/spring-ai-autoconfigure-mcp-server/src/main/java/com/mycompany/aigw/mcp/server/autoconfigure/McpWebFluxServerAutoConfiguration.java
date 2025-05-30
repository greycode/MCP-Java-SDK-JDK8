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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.aigw.server.transport.WebFluxSseServerTransportProvider;
import com.mycompany.aigw.spec.McpServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * {@link AutoConfiguration Auto-configuration} for MCP WebFlux Server Transport.
 * <p>
 * This configuration class sets up the WebFlux-specific transport components for the MCP
 * server, providing reactive Server-Sent Events (SSE) communication through Spring
 * WebFlux. It is activated when:
 * <ul>
 * <li>The WebFluxSseServerTransportProvider class is on the classpath (from
 * mcp-spring-webflux dependency)</li>
 * <li>Spring WebFlux's RouterFunction class is available (from
 * spring-boot-starter-webflux)</li>
 * <li>The {@code spring.ai.mcp.server.transport} property is set to {@code WEBFLUX}</li>
 * </ul>
 * <p>
 * The configuration provides:
 * <ul>
 * <li>A WebFluxSseServerTransportProvider bean for handling reactive SSE
 * communication</li>
 * <li>A RouterFunction bean that sets up the reactive SSE endpoint</li>
 * </ul>
 * <p>
 * Required dependencies: <pre>{@code
 * <dependency>
 *     <groupId>com.mycompany.aigw.sdk</groupId>
 *     <artifactId>mcp-spring-webflux</artifactId>
 * </dependency>
 * <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-webflux</artifactId>
 * </dependency>
 * }</pre>
 *
 * @author Christian Tzolov
 * @since 1.0.0
 * @see McpServerProperties
 * @see WebFluxSseServerTransportProvider
 */
@AutoConfiguration
@ConditionalOnClass({ WebFluxSseServerTransportProvider.class })
@ConditionalOnMissingBean(McpServerTransportProvider.class)
@Conditional(McpServerStdioDisabledCondition.class)
public class McpWebFluxServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WebFluxSseServerTransportProvider webFluxTransport(ObjectProvider<ObjectMapper> objectMapperProvider,
			McpServerProperties serverProperties) {
		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
		return new WebFluxSseServerTransportProvider(objectMapper, serverProperties.getSseMessageEndpoint(),
				serverProperties.getSseEndpoint());
	}

	// Router function for SSE transport used by Spring WebFlux to start an HTTP server.
	@Bean
	public RouterFunction<?> webfluxMcpRouterFunction(WebFluxSseServerTransportProvider webFluxProvider) {
		return webFluxProvider.getRouterFunction();
	}

} 
/*
 * Copyright 2023-2025 the original author or authors.
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

package com.mycompany.aigw.sdk.tool.method;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mycompany.aigw.sdk.tool.ToolCallback;
import com.mycompany.aigw.sdk.tool.ToolContext;
import com.mycompany.aigw.sdk.tool.definition.ToolDefinition;
import com.mycompany.aigw.sdk.tool.execution.DefaultToolCallResultConverter;
import com.mycompany.aigw.sdk.tool.execution.ToolCallResultConverter;
import com.mycompany.aigw.sdk.tool.execution.ToolExecutionException;
import com.mycompany.aigw.sdk.tool.metadata.ToolMetadata;
import com.mycompany.aigw.sdk.util.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;

/**
 * A {@link ToolCallback} implementation to invoke methods as tools.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class MethodToolCallback implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(MethodToolCallback.class);

	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

	private final ToolDefinition toolDefinition;

	private final ToolMetadata toolMetadata;

	private final Method toolMethod;

	@Nullable
	private final Object toolObject;

	private final ToolCallResultConverter toolCallResultConverter;

	public MethodToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Method toolMethod,
			@Nullable Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
		Assert.notNull(toolMethod, "toolMethod cannot be null");
		Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
				"toolObject cannot be null for non-static methods");
		this.toolDefinition = toolDefinition;
		this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
		this.toolMethod = toolMethod;
		this.toolObject = toolObject;
		this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
				: DEFAULT_RESULT_CONVERTER;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		Assert.hasText(toolInput, "toolInput cannot be null or empty");

		logger.debug("Starting execution of tool: {}", this.toolDefinition.name());

		validateToolContextSupport(toolContext);

		Map<String, Object> toolArguments = extractToolArguments(toolInput);

		Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

		Object result = callMethod(methodArguments);

		logger.debug("Successful execution of tool: {}", this.toolDefinition.name());

		Type returnType = this.toolMethod.getGenericReturnType();

		return this.toolCallResultConverter.convert(result, returnType);
	}

	private void validateToolContextSupport(@Nullable ToolContext toolContext) {
		boolean isNonEmptyToolContextProvided = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
		boolean isToolContextAcceptedByMethod = Arrays.stream(this.toolMethod.getParameterTypes())
			.anyMatch(type -> ClassUtils.isAssignable(type, ToolContext.class));
		if (isToolContextAcceptedByMethod && !isNonEmptyToolContextProvided) {
			throw new IllegalArgumentException("ToolContext is required by the method as an argument");
		}
	}

	private Map<String, Object> extractToolArguments(String toolInput) {
		return JsonParser.fromJson(toolInput, new TypeReference<Map<String, Object>>() {
		});
	}

	private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
		Parameter[] parameters = this.toolMethod.getParameters();
		Object[] methodArguments = new Object[parameters.length];
		
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			if (parameter.getType().isAssignableFrom(ToolContext.class)) {
				methodArguments[i] = toolContext;
			} else {
				Object rawArgument = toolInputArguments.get(parameter.getName());
				methodArguments[i] = buildTypedArgument(rawArgument, parameter.getType());
			}
		}
		
		return methodArguments;
	}

	@Nullable
	private Object buildTypedArgument(@Nullable Object value, Class<?> type) {
		if (value == null) {
			return null;
		}
		return JsonParser.toTypedObject(value, type);
	}

	@Nullable
	private Object callMethod(Object[] methodArguments) {
		if (isObjectNotPublic() || isMethodNotPublic()) {
			this.toolMethod.setAccessible(true);
		}

		Object result;
		try {
			result = this.toolMethod.invoke(this.toolObject, methodArguments);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
		}
		catch (InvocationTargetException ex) {
			throw new ToolExecutionException(this.toolDefinition, ex.getCause());
		}
		return result;
	}

	private boolean isObjectNotPublic() {
		return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
	}

	private boolean isMethodNotPublic() {
		return !Modifier.isPublic(this.toolMethod.getModifiers());
	}

	@Override
	public String toString() {
		return "MethodToolCallback{" + "toolDefinition=" + this.toolDefinition + ", toolMetadata=" + this.toolMetadata
				+ '}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private ToolDefinition toolDefinition;

		private ToolMetadata toolMetadata;

		private Method toolMethod;

		private Object toolObject;

		private ToolCallResultConverter toolCallResultConverter;

		private Builder() {
		}

		public Builder toolDefinition(ToolDefinition toolDefinition) {
			this.toolDefinition = toolDefinition;
			return this;
		}

		public Builder toolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		public Builder toolMethod(Method toolMethod) {
			this.toolMethod = toolMethod;
			return this;
		}

		public Builder toolObject(Object toolObject) {
			this.toolObject = toolObject;
			return this;
		}

		public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
			this.toolCallResultConverter = toolCallResultConverter;
			return this;
		}

		public MethodToolCallback build() {
			return new MethodToolCallback(this.toolDefinition, this.toolMetadata, this.toolMethod, this.toolObject,
					this.toolCallResultConverter);
		}
	}
} 
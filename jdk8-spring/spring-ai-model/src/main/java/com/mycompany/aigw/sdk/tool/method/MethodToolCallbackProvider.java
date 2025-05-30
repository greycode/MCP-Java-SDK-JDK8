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

import com.mycompany.aigw.sdk.tool.ToolCallback;
import com.mycompany.aigw.sdk.tool.ToolCallbackProvider;
import com.mycompany.aigw.sdk.tool.annotation.Tool;
import com.mycompany.aigw.sdk.tool.metadata.ToolMetadata;
import com.mycompany.aigw.sdk.tool.support.ToolDefinitions;
import com.mycompany.aigw.sdk.tool.support.ToolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A {@link ToolCallbackProvider} that builds {@link ToolCallback} instances from
 * {@link Tool}-annotated methods.
 *
 * @author Thomas Vitale
 * @author Christian Tzolov
 * @since 1.0.0
 */
public final class MethodToolCallbackProvider implements ToolCallbackProvider {

	private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);

	private final List<Object> toolObjects;

	private MethodToolCallbackProvider(List<Object> toolObjects) {
		Assert.notNull(toolObjects, "toolObjects cannot be null");
		Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
		assertToolAnnotatedMethodsPresent(toolObjects);
		this.toolObjects = toolObjects;
		validateToolCallbacks(getToolCallbacks().toArray(new ToolCallback[0]));
	}

	private void assertToolAnnotatedMethodsPresent(List<Object> toolObjects) {
		for (Object toolObject : toolObjects) {
			List<Method> toolMethods = findToolMethods(toolObject);
			if (toolMethods.isEmpty()) {
				throw new IllegalStateException("No @Tool annotated methods found in " + toolObject + "."
						+ "Did you mean to pass a ToolCallback or ToolCallbackProvider? If so, you have to use .toolCallbacks() instead of .tool()");
			}
		}
	}

	private List<Method> findToolMethods(Object toolObject) {
		List<Method> toolMethods = new ArrayList<>();
		for (Method method : ReflectionUtils.getDeclaredMethods(
				AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass())) {
			if (method.isAnnotationPresent(Tool.class) && !isFunctionalType(method)) {
				toolMethods.add(method);
			}
		}
		return toolMethods;
	}

	@Override
	public List<ToolCallback> getToolCallbacks() {
		List<ToolCallback> allToolCallbacks = new ArrayList<>();
		
		for (Object toolObject : this.toolObjects) {
			for (Method toolMethod : findToolMethods(toolObject)) {
				ToolCallback toolCallback = MethodToolCallback.builder()
					.toolDefinition(ToolDefinitions.from(toolMethod))
					.toolMetadata(ToolMetadata.from(toolMethod))
					.toolMethod(toolMethod)
					.toolObject(toolObject)
					.toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod))
					.build();
				allToolCallbacks.add(toolCallback);
			}
		}
		
		ToolCallback[] result = allToolCallbacks.toArray(new ToolCallback[0]);
		validateToolCallbacks(result);
		return Arrays.asList(result);
	}

	private boolean isFunctionalType(Method toolMethod) {
		boolean isFunction = ClassUtils.isAssignable(toolMethod.getReturnType(), Function.class)
				|| ClassUtils.isAssignable(toolMethod.getReturnType(), Supplier.class)
				|| ClassUtils.isAssignable(toolMethod.getReturnType(), Consumer.class);

		if (isFunction) {
			logger.warn("Method {} is annotated with @Tool but returns a functional type. "
					+ "This is not supported and the method will be ignored.", toolMethod.getName());
		}

		return isFunction;
	}

	private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
		if (!duplicateToolNames.isEmpty()) {
			throw new IllegalStateException(String.format("Multiple tools with the same name (%s) found in sources: %s",
					String.join(", ", duplicateToolNames),
					this.toolObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", "))));
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private List<Object> toolObjects;

		private Builder() {
		}

		public Builder toolObjects(Object... toolObjects) {
			Assert.notNull(toolObjects, "toolObjects cannot be null");
			this.toolObjects = Arrays.asList(toolObjects);
			return this;
		}

		public MethodToolCallbackProvider build() {
			return new MethodToolCallbackProvider(this.toolObjects);
		}
	}
} 
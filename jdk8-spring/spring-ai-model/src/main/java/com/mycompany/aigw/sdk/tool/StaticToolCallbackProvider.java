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

package com.mycompany.aigw.sdk.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A static provider of tool callbacks.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class StaticToolCallbackProvider implements ToolCallbackProvider {

	private final List<ToolCallback> toolCallbacks;

	private final String defaultToolContext;

	private StaticToolCallbackProvider(Builder builder) {
		this.toolCallbacks = Collections.unmodifiableList(new ArrayList<>(builder.toolCallbacks));
		this.defaultToolContext = builder.defaultToolContext;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public List<ToolCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	@Override
	public String getDefaultToolContext() {
		return this.defaultToolContext;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof StaticToolCallbackProvider)) {
			return false;
		}
		StaticToolCallbackProvider that = (StaticToolCallbackProvider) o;
		return Objects.equals(this.toolCallbacks, that.toolCallbacks)
				&& Objects.equals(this.defaultToolContext, that.defaultToolContext);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.toolCallbacks, this.defaultToolContext);
	}

	@Override
	public String toString() {
		return "StaticToolCallbackProvider{toolCallbacks=" + this.toolCallbacks + ", defaultToolContext='"
				+ this.defaultToolContext + '\'' + '}';
	}

	/**
	 * Builder for creating a {@link StaticToolCallbackProvider}.
	 */
	public static class Builder {

		private List<ToolCallback> toolCallbacks = Collections.emptyList();

		private String defaultToolContext = "{}";

		private Builder() {
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = Objects.requireNonNull(toolCallbacks);
			return this;
		}

		public Builder defaultToolContext(String defaultToolContext) {
			this.defaultToolContext = Objects.requireNonNull(defaultToolContext);
			return this;
		}

		public StaticToolCallbackProvider build() {
			return new StaticToolCallbackProvider(this);
		}

	}

} 
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

package com.mycompany.aigw.sdk.tool.definition;

import com.mycompany.aigw.sdk.util.ParsingUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Default implementation of {@link ToolDefinition}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class DefaultToolDefinition implements ToolDefinition {

	private final String name;
	
	private final String description;
	
	private final String inputSchema;

	public DefaultToolDefinition(String name, String description, String inputSchema) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String description() {
		return this.description;
	}

	@Override
	public String inputSchema() {
		return this.inputSchema;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultToolDefinition)) {
			return false;
		}
		DefaultToolDefinition that = (DefaultToolDefinition) o;
		return Objects.equals(this.name, that.name) && 
				Objects.equals(this.description, that.description) &&
				Objects.equals(this.inputSchema, that.inputSchema);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.description, this.inputSchema);
	}

	@Override
	public String toString() {
		return "DefaultToolDefinition{" + 
				"name='" + this.name + '\'' + 
				", description='" + this.description + '\'' +
				", inputSchema='" + this.inputSchema + '\'' +
				'}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private String name;

		private String description;

		private String inputSchema;

		private Builder() {
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder inputSchema(String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public ToolDefinition build() {
			if (!StringUtils.hasText(this.description)) {
				Assert.hasText(this.name, "toolName cannot be null or empty");
				this.description = ParsingUtils.reConcatenateCamelCase(this.name, " ");
			}
			return new DefaultToolDefinition(this.name, this.description, this.inputSchema);
		}
	}
} 
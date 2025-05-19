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

package com.mycompany.aigw.sdk.tool.metadata;

import java.util.Objects;

/**
 * Default implementation of {@link ToolMetadata}.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class DefaultToolMetadata implements ToolMetadata {

	private final boolean returnDirect;

	public DefaultToolMetadata(boolean returnDirect) {
		this.returnDirect = returnDirect;
	}

	@Override
	public boolean returnDirect() {
		return this.returnDirect;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultToolMetadata)) {
			return false;
		}
		DefaultToolMetadata that = (DefaultToolMetadata) o;
		return this.returnDirect == that.returnDirect;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.returnDirect);
	}

	@Override
	public String toString() {
		return "DefaultToolMetadata{returnDirect=" + this.returnDirect + '}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private boolean returnDirect = false;

		private Builder() {
		}

		public Builder returnDirect(boolean returnDirect) {
			this.returnDirect = returnDirect;
			return this;
		}

		public ToolMetadata build() {
			return new DefaultToolMetadata(this.returnDirect);
		}
	}
} 
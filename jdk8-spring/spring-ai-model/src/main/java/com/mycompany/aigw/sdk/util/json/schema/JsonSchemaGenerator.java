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

package com.mycompany.aigw.sdk.util.json.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import com.mycompany.aigw.sdk.tool.ToolContext;
import com.mycompany.aigw.sdk.tool.annotation.ToolParam;
import com.mycompany.aigw.sdk.util.json.JsonParser;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities to generate JSON Schemas from Java types and method signatures. It's designed
 * to work well in the context of tool calling and structured outputs, aiming at ensuring
 * consistency and robustness across different model providers.
 * <p>
 * Metadata such as descriptions and required properties can be specified using one of the
 * following supported annotations:
 * <p>
 * <ul>
 * <li>{@code @ToolParam(required = ..., description = ...)}</li>
 * <li>{@code @JsonProperty(required = ...)}</li>
 * <li>{@code @JsonClassDescription(...)}</li>
 * <li>{@code @JsonPropertyDescription(...)}</li>
 * <li>{@code @Schema(required = ..., description = ...)}</li>
 * <li>{@code @Nullable}</li>
 * </ul>
 * <p>
 * If none of these annotations are present, the default behavior is to consider the
 * property as required and not to include a description.
 * <p>
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public final class JsonSchemaGenerator {

	/**
	 * To ensure consistency and robustness across different model providers, all
	 * properties in the JSON Schema are considered required by default. This behavior can
	 * be overridden by setting the {@link ToolParam#required()},
	 * {@link JsonProperty#required()}, or {@link Schema#requiredMode()}} annotation.
	 */
	private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;

	private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;

	private static final SchemaGenerator SUBTYPE_SCHEMA_GENERATOR;

	/*
	 * Initialize JSON Schema generators.
	 */
	static {
		Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
		Module openApiModule = new Swagger2Module();
		Module springAiSchemaModule = PROPERTY_REQUIRED_BY_DEFAULT ? new SpringAiSchemaModule()
				: new SpringAiSchemaModule(SpringAiSchemaModule.Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);

		SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = new SchemaGeneratorConfigBuilder(
				SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
			.with(jacksonModule)
			.with(openApiModule)
			.with(springAiSchemaModule)
			.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
			.with(Option.PLAIN_DEFINITION_KEYS);

		SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
		TYPE_SCHEMA_GENERATOR = new SchemaGenerator(typeSchemaGeneratorConfig);

		SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder
			.without(Option.SCHEMA_VERSION_INDICATOR)
			.build();
		SUBTYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
	}

	private JsonSchemaGenerator() {
	}

	/**
	 * Generate a JSON Schema for a method's input parameters.
	 */
	public static String generateForMethodInput(Method method, SchemaOption... schemaOptions) {
		ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
		schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
		schema.put("type", "object");

		ObjectNode properties = schema.putObject("properties");
		List<String> required = new ArrayList<>();

		for (int i = 0; i < method.getParameterCount(); i++) {
			String parameterName = method.getParameters()[i].getName();
			Type parameterType = method.getGenericParameterTypes()[i];
			if (parameterType instanceof Class<?>) {
				Class<?> parameterClass = (Class<?>) parameterType;
				if (ClassUtils.isAssignable(parameterClass, ToolContext.class)) {
					// A ToolContext method parameter is not included in the JSON Schema
					// generation.
					// It's a special type used by Spring AI to pass contextual data to tools
					// outside the model interaction flow.
					continue;
				}
			}
			if (isMethodParameterRequired(method, i)) {
				required.add(parameterName);
			}
			ObjectNode parameterNode = SUBTYPE_SCHEMA_GENERATOR.generateSchema(parameterType);
			String parameterDescription = getMethodParameterDescription(method, i);
			if (StringUtils.hasText(parameterDescription)) {
				parameterNode.put("description", parameterDescription);
			}
			properties.set(parameterName, parameterNode);
		}

		ArrayNode requiredArray = schema.putArray("required");
		for (String requiredField : required) {
			requiredArray.add(requiredField);
		}

		processSchemaOptions(schemaOptions, schema);

		return schema.toPrettyString();
	}

	/**
	 * Generate a JSON Schema for a class type.
	 */
	public static String generateForType(Type type, SchemaOption... schemaOptions) {
		Assert.notNull(type, "type cannot be null");
		ObjectNode schema = TYPE_SCHEMA_GENERATOR.generateSchema(type);
		if ((type == Void.class) && !schema.has("properties")) {
			schema.putObject("properties");
		}
		processSchemaOptions(schemaOptions, schema);
		return schema.toPrettyString();
	}

	private static void processSchemaOptions(SchemaOption[] schemaOptions, ObjectNode schema) {
		if (Arrays.stream(schemaOptions)
			.noneMatch(option -> option == SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT)) {
			schema.put("additionalProperties", false);
		}
		if (Arrays.stream(schemaOptions).anyMatch(option -> option == SchemaOption.UPPER_CASE_TYPE_VALUES)) {
			convertTypeValuesToUpperCase(schema);
		}
	}

	/**
	 * Determines whether a property is required based on the presence of a series of *
	 * annotations.
	 *
	 * <p>
	 * <ul>
	 * <li>{@code @ToolParam(required = ...)}</li>
	 * <li>{@code @JsonProperty(required = ...)}</li>
	 * <li>{@code @Schema(required = ...)}</li>
	 * <li>{@code @Nullable}</li>
	 * </ul>
	 * <p>
	 *
	 * If none of these annotations are present, the default behavior is to consider the *
	 * property as required.
	 */
	private static boolean isMethodParameterRequired(Method method, int index) {
		Parameter parameter = method.getParameters()[index];

		ToolParam toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
		if (toolParamAnnotation != null) {
			return toolParamAnnotation.required();
		}

		JsonProperty propertyAnnotation = parameter.getAnnotation(JsonProperty.class);
		if (propertyAnnotation != null) {
			return propertyAnnotation.required();
		}

		Schema schemaAnnotation = parameter.getAnnotation(Schema.class);
		if (schemaAnnotation != null) {
			return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
					|| schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
		}

		Nullable nullableAnnotation = parameter.getAnnotation(Nullable.class);
		if (nullableAnnotation != null) {
			return false;
		}

		return PROPERTY_REQUIRED_BY_DEFAULT;
	}

	/**
	 * Determines a property description based on the presence of a series of annotations.
	 *
	 * <p>
	 * <ul>
	 * <li>{@code @ToolParam(description = ...)}</li>
	 * <li>{@code @JsonPropertyDescription(...)}</li>
	 * <li>{@code @Schema(description = ...)}</li>
	 * </ul>
	 * <p>
	 */
	@Nullable
	private static String getMethodParameterDescription(Method method, int index) {
		Parameter parameter = method.getParameters()[index];

		ToolParam toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
		if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
			return toolParamAnnotation.description();
		}

		JsonPropertyDescription jacksonAnnotation = parameter.getAnnotation(JsonPropertyDescription.class);
		if (jacksonAnnotation != null && StringUtils.hasText(jacksonAnnotation.value())) {
			return jacksonAnnotation.value();
		}

		Schema schemaAnnotation = parameter.getAnnotation(Schema.class);
		if (schemaAnnotation != null && StringUtils.hasText(schemaAnnotation.description())) {
			return schemaAnnotation.description();
		}

		return null;
	}

	/**
	 * Convert type values in the JSON schema to upper case.
	 */
	public static void convertTypeValuesToUpperCase(ObjectNode node) {
		JsonNode typeNode = node.get("type");
		if (typeNode != null && typeNode.isTextual()) {
			node.put("type", typeNode.asText().toUpperCase());
		}

		JsonNode propertiesNode = node.get("properties");
		if (propertiesNode != null && propertiesNode.isObject()) {
			ObjectNode propertiesObject = (ObjectNode) propertiesNode;
			Iterator<String> fieldNames = propertiesObject.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				JsonNode fieldValue = propertiesObject.get(fieldName);
				if (fieldValue.isObject()) {
					convertTypeValuesToUpperCase((ObjectNode) fieldValue);
				}
			}
		}

		JsonNode itemsNode = node.get("items");
		if (itemsNode != null && itemsNode.isObject()) {
			convertTypeValuesToUpperCase((ObjectNode) itemsNode);
		}

		JsonNode definitionsNode = node.get("$defs");
		if (definitionsNode != null && definitionsNode.isObject()) {
			ObjectNode definitionsObject = (ObjectNode) definitionsNode;
			Iterator<String> fieldNames = definitionsObject.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				JsonNode fieldValue = definitionsObject.get(fieldName);
				if (fieldValue.isObject()) {
					convertTypeValuesToUpperCase((ObjectNode) fieldValue);
				}
			}
		}
	}

	/**
	 * Options to customize the generated JSON Schema.
	 */
	public enum SchemaOption {

		/**
		 * Allow an object to contain additional key/values not defined in the schema.
		 */
		ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT,

		/**
		 * Convert all "type" values to upper case.
		 */
		UPPER_CASE_TYPE_VALUES

	}
} 
/*
 * Copyright 2023-2024 the original author or authors.
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

package com.mycompany.aigw.sdk.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.CollectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for manipulating ModelOptions objects.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @since 0.8.0
 */
public abstract class ModelOptionsUtils {

	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		.registerModules(JacksonUtils.instantiateAvailableModules())
		.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

	private static final List<String> BEAN_MERGE_FIELD_EXCISIONS = Collections.singletonList("class");

	private static final ConcurrentHashMap<Class<?>, List<String>> REQUEST_FIELD_NAMES_PER_CLASS = new ConcurrentHashMap<Class<?>, List<String>>();

	private static final AtomicReference<SchemaGenerator> SCHEMA_GENERATOR_CACHE = new AtomicReference<>();

	private static TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	/**
	 * Converts the given JSON string to a Map of String and Object using the default
	 * ObjectMapper.
	 * @param json the JSON string to convert to a Map.
	 * @return the converted Map.
	 */
	public static Map<String, Object> jsonToMap(String json) {
		return jsonToMap(json, OBJECT_MAPPER);
	}

	/**
	 * Converts the given JSON string to a Map of String and Object using a custom
	 * ObjectMapper.
	 * @param json the JSON string to convert to a Map.
	 * @param objectMapper the ObjectMapper to use for deserialization.
	 * @return the converted Map.
	 */
	public static Map<String, Object> jsonToMap(String json, ObjectMapper objectMapper) {
		try {
			return objectMapper.readValue(json, MAP_TYPE_REF);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the given JSON string to an Object of the given type.
	 * @param <T> the type of the object to return.
	 * @param json the JSON string to convert to an object.
	 * @param type the type of the object to return.
	 * @return Object instance of the given type.
	 */
	public static <T> T jsonToObject(String json, Class<T> type) {
		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to json: " + json, e);
		}
	}

	/**
	 * Converts the given object to a JSON string.
	 * @param object the object to convert to a JSON string.
	 * @return the JSON string.
	 */
	public static String toJsonString(Object object) {
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the given object to a JSON string.
	 * @param object the object to convert to a JSON string.
	 * @return the JSON string.
	 */
	public static String toJsonStringPrettyPrinter(Object object) {
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Merges the source object into the target object and returns an object represented
	 * by the given class. The JSON property names are used to match the fields to merge.
	 * The source non-null values override the target values with the same field name. The
	 * source null values are ignored. If the acceptedFieldNames is not empty, only the
	 * fields with the given names are merged and returned. If the acceptedFieldNames is
	 * empty, use the {@code @JsonProperty} names, inferred from the provided clazz.
	 * @param <T> they type of the class to return.
	 * @param source the source object to merge.
	 * @param target the target object to merge into.
	 * @param clazz the class to return.
	 * @param acceptedFieldNames the list of field names accepted for the target object.
	 * @return the merged object represented by the given class.
	 */
	public static <T> T merge(Object source, Object target, Class<T> clazz, List<String> acceptedFieldNames) {

		if (source == null) {
			source = Collections.emptyMap();
		}

		List<String> requestFieldNames = CollectionUtils.isEmpty(acceptedFieldNames)
				? REQUEST_FIELD_NAMES_PER_CLASS.computeIfAbsent(clazz, ModelOptionsUtils::getJsonPropertyValues)
				: acceptedFieldNames;

		if (CollectionUtils.isEmpty(requestFieldNames)) {
			throw new IllegalArgumentException("No @JsonProperty fields found in the " + clazz.getName());
		}

		Map<String, Object> sourceMap = ModelOptionsUtils.objectToMap(source);
		Map<String, Object> targetMap = ModelOptionsUtils.objectToMap(target);

		for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
			if (entry.getValue() != null) {
				targetMap.put(entry.getKey(), entry.getValue());
			}
		}

		Map<String, Object> filteredMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : targetMap.entrySet()) {
			if (requestFieldNames.contains(entry.getKey())) {
				filteredMap.put(entry.getKey(), entry.getValue());
			}
		}

		return ModelOptionsUtils.mapToClass(filteredMap, clazz);
	}

	/**
	 * Merges the source object into the target object and returns an object represented
	 * by the given class. The JSON property names are used to match the fields to merge.
	 * The source non-null values override the target values with the same field name. The
	 * source null values are ignored. Returns the only field names that match the
	 * {@code @JsonProperty} names, inferred from the provided clazz.
	 * @param <T> they type of the class to return.
	 * @param source the source object to merge.
	 * @param target the target object to merge into.
	 * @param clazz the class to return.
	 * @return the merged object represented by the given class.
	 */
	public static <T> T merge(Object source, Object target, Class<T> clazz) {
		return ModelOptionsUtils.merge(source, target, clazz, null);
	}

	/**
	 * Converts the given object to a Map.
	 * @param source the object to convert to a Map.
	 * @return the converted Map.
	 */
	public static Map<String, Object> objectToMap(Object source) {
		if (source == null) {
			return new HashMap<>();
		}
		try {
			String json = OBJECT_MAPPER.writeValueAsString(source);
			Map<String, Object> result = new HashMap<>();
			Map<String, Object> rawMap = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
			
			for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
				if (entry.getValue() != null) {
					result.put(entry.getKey(), entry.getValue());
				}
			}
			
			return result;
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the given Map to the given class.
	 * @param <T> the type of the class to return.
	 * @param source the Map to convert to the given class.
	 * @param clazz the class to convert the Map to.
	 * @return the converted class.
	 */
	public static <T> T mapToClass(Map<String, Object> source, Class<T> clazz) {
		try {
			String json = OBJECT_MAPPER.writeValueAsString(source);
			return OBJECT_MAPPER.readValue(json, clazz);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the list of JSON property values for the given class.
	 * @param clazz the class to get the JSON property values from.
	 * @return the list of JSON property values.
	 */
	public static List<String> getJsonPropertyValues(Class<?> clazz) {
		List<String> result = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
			if (jsonProperty != null) {
				result.add(jsonProperty.value().isEmpty() ? field.getName() : jsonProperty.value());
			}
		}
		return result;
	}

	/**
	 * Copies the source bean to a new instance of the target bean class.
	 * @param <I> the interface type.
	 * @param <S> the source type.
	 * @param <T> the target type.
	 * @param sourceBean the source bean to copy from.
	 * @param sourceInterfaceClazz the interface class.
	 * @param targetBeanClazz the target bean class.
	 * @return the new instance of the target bean class.
	 */
	public static <I, S extends I, T extends S> T copyToTarget(S sourceBean, Class<I> sourceInterfaceClazz,
			Class<T> targetBeanClazz) {
		try {
			T targetBean = targetBeanClazz.newInstance();
			return mergeBeans(sourceBean, targetBean, sourceInterfaceClazz, true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Merges the source bean into the target bean.
	 * @param <I> the interface type.
	 * @param <S> the source type.
	 * @param <T> the target type.
	 * @param source the source bean to merge from.
	 * @param target the target bean to merge into.
	 * @param sourceInterfaceClazz the interface class.
	 * @param overrideNonNullTargetValues whether to override non-null target values.
	 * @return the merged bean.
	 */
	public static <I, S extends I, T extends S> T mergeBeans(S source, T target, Class<I> sourceInterfaceClazz,
			boolean overrideNonNullTargetValues) {

		BeanWrapper sourceWrapper = new BeanWrapperImpl(source);
		BeanWrapper targetWrapper = new BeanWrapperImpl(target);

		for (PropertyDescriptor pd : sourceWrapper.getPropertyDescriptors()) {
			if (BEAN_MERGE_FIELD_EXCISIONS.contains(pd.getName())) {
				continue;
			}

			try {
				sourceInterfaceClazz.getMethod(toGetName(pd.getName()));
			}
			catch (NoSuchMethodException e) {
				continue;
			}

			Object sourceValue = sourceWrapper.getPropertyValue(pd.getName());
			Object targetValue = targetWrapper.getPropertyValue(pd.getName());

			if (sourceValue != null && (targetValue == null || overrideNonNullTargetValues)) {
				targetWrapper.setPropertyValue(pd.getName(), sourceValue);
			}
		}

		return target;
	}

	private static String toGetName(String name) {
		return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * Returns the JSON schema for the given input type.
	 * @param inputType the input type to get the JSON schema for.
	 * @param toUpperCaseTypeValues whether to convert the type values to uppercase.
	 * @return the JSON schema.
	 */
	public static String getJsonSchema(Type inputType, boolean toUpperCaseTypeValues) {
		SchemaGenerator generator = SCHEMA_GENERATOR_CACHE.updateAndGet(cached -> {
			if (cached != null) {
				return cached;
			}

			SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09,
					OptionPreset.PLAIN_JSON)
				.with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
				.with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED))
				.with(new Swagger2Module());

			SchemaGeneratorConfig config = configBuilder.build();
			return new SchemaGenerator(config);
		});

		JsonNode jsonSchema = generator.generateSchema(inputType);

		if (toUpperCaseTypeValues) {
			toUpperCaseTypeValues((ObjectNode) jsonSchema);
		}

		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to generate schema for: " + inputType, e);
		}
	}

	/**
	 * Converts the type values in the given node to uppercase.
	 * @param node the node to convert the type values in.
	 */
	public static void toUpperCaseTypeValues(ObjectNode node) {
		JsonNode typeNode = node.get("type");
		if (typeNode != null && typeNode.isTextual()) {
			node.put("type", typeNode.asText().toUpperCase());
		}

		JsonNode propertiesNode = node.get("properties");
		if (propertiesNode != null && propertiesNode.isObject()) {
			ObjectNode propertiesObject = (ObjectNode) propertiesNode;
			propertiesObject.fieldNames().forEachRemaining(fieldName -> {
				JsonNode fieldValue = propertiesObject.get(fieldName);
				if (fieldValue.isObject()) {
					toUpperCaseTypeValues((ObjectNode) fieldValue);
				}
			});
		}

		JsonNode itemsNode = node.get("items");
		if (itemsNode != null && itemsNode.isObject()) {
			toUpperCaseTypeValues((ObjectNode) itemsNode);
		}

		JsonNode definitionsNode = node.get("$defs");
		if (definitionsNode != null && definitionsNode.isObject()) {
			ObjectNode definitionsObject = (ObjectNode) definitionsNode;
			definitionsObject.fieldNames().forEachRemaining(fieldName -> {
				JsonNode fieldValue = definitionsObject.get(fieldName);
				if (fieldValue.isObject()) {
					toUpperCaseTypeValues((ObjectNode) fieldValue);
				}
			});
		}
	}

	/**
	 * Merges the runtime value with the default value.
	 * @param <T> the type of the values.
	 * @param runtimeValue the runtime value.
	 * @param defaultValue the default value.
	 * @return the merged value.
	 */
	public static <T> T mergeOption(T runtimeValue, T defaultValue) {
		if (runtimeValue == null) {
			return defaultValue;
		}
		return runtimeValue;
	}

} 
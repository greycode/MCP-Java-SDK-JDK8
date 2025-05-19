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

import java.util.*;

/**
 * Utility class providing JDK8 compatible implementations for collection factory methods
 * that were introduced in later Java versions.
 *
 * @author [Your Name]
 */
public final class CollectionUtils {

    private CollectionUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Creates an empty unmodifiable list.
     * @param <T> the list element type
     * @return an empty unmodifiable list
     */
    public static <T> List<T> ofList() {
        return Collections.emptyList();
    }

    /**
     * Creates an unmodifiable list containing a single element.
     * @param <T> the list element type
     * @param element the single element
     * @return an unmodifiable list containing the specified element
     */
    public static <T> List<T> of(T element) {
        return Collections.singletonList(element);
    }

    /**
     * Creates an unmodifiable list containing the given elements.
     * @param <T> the list element type
     * @param elements the elements to be contained in the list
     * @return an unmodifiable list containing the specified elements
     */
    @SafeVarargs
    public static <T> List<T> of(T... elements) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(elements)));
    }

    /**
     * Creates an empty unmodifiable map.
     * @param <K> the key type
     * @param <V> the value type
     * @return an empty unmodifiable map
     */
    public static <K, V> Map<K, V> ofMap() {
        return Collections.emptyMap();
    }

    /**
     * Creates an unmodifiable map containing a single mapping.
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 the key
     * @param v1 the value
     * @return an unmodifiable map containing the specified mapping
     */
    public static <K, V> Map<K, V> of(K k1, V v1) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates an unmodifiable map containing two mappings.
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @return an unmodifiable map containing the specified mappings
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates an unmodifiable map containing three mappings.
     * @param <K> the key type
     * @param <V> the value type
     * @param k1 the first key
     * @param v1 the first value
     * @param k2 the second key
     * @param v2 the second value
     * @param k3 the third key
     * @param v3 the third value
     * @return an unmodifiable map containing the specified mappings
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return Collections.unmodifiableMap(map);
    }
} 
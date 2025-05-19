/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.examples;

/**
 * This example demonstrates how to convert JDK17 text blocks to JDK8 compatible string
 * concatenation. It shows the original text block syntax and the JDK8 equivalent
 * implementation.
 *
 * @author MCP SDK Team
 */
public class TextBlockExample {

	/**
	 * JDK 17 Example: <pre>
	 * static String getJsonExample() {
	 *     return """
	 *         {
	 *             "name": "John Doe",
	 *             "age": 30,
	 *             "address": {
	 *                 "street": "123 Main St",
	 *                 "city": "Anytown",
	 *                 "state": "CA"
	 *             },
	 *             "phoneNumbers": [
	 *                 "555-1234",
	 *                 "555-5678"
	 *             ]
	 *         }
	 *         """;
	 * }
	 * </pre>
	 *
	 * JDK 8 equivalent implementation:
	 */
	static String getJsonExample() {
		return "{\n" + "    \"name\": \"John Doe\",\n" + "    \"age\": 30,\n" + "    \"address\": {\n"
				+ "        \"street\": \"123 Main St\",\n" + "        \"city\": \"Anytown\",\n"
				+ "        \"state\": \"CA\"\n" + "    },\n" + "    \"phoneNumbers\": [\n" + "        \"555-1234\",\n"
				+ "        \"555-5678\"\n" + "    ]\n" + "}";
	}

	/**
	 * JDK 17 Example with formatting: <pre>
	 * static String getHtmlExample(String name, int age) {
	 *     return """
	 *         <html>
	 *           <body>
	 *             <h1>User Profile</h1>
	 *             <div class="user-info">
	 *               <p>Name: %s</p>
	 *               <p>Age: %d</p>
	 *             </div>
	 *           </body>
	 *         </html>
	 *         """.formatted(name, age);
	 * }
	 * </pre>
	 *
	 * JDK 8 equivalent implementation:
	 */
	static String getHtmlExample(String name, int age) {
		return String.format("<html>\n" + "  <body>\n" + "    <h1>User Profile</h1>\n"
				+ "    <div class=\"user-info\">\n" + "      <p>Name: %s</p>\n" + "      <p>Age: %d</p>\n"
				+ "    </div>\n" + "  </body>\n" + "</html>", name, age);
	}

	/**
	 * JDK 17 Example with escape sequences: <pre>
	 * static String getSqlExample(String tableName) {
	 *     return """
	 *         SELECT
	 *             id,
	 *             first_name,
	 *             last_name,
	 *             email,
	 *             CASE
	 *                 WHEN age < 18 THEN "Minor"
	 *                 WHEN age >= 65 THEN "Senior"
	 *                 ELSE "Adult"
	 *             END AS age_category
	 *         FROM %s
	 *         WHERE active = true
	 *         ORDER BY last_name, first_name;
	 *         """.formatted(tableName);
	 * }
	 * </pre>
	 *
	 * JDK 8 equivalent implementation:
	 */
	static String getSqlExample(String tableName) {

        return "SELECT\n" + "    id,\n" + "    first_name,\n" + "    last_name,\n" +
                "    email,\n" + "    CASE\n" + "        WHEN age < 18 THEN \"Minor\"\n" +
                "        WHEN age >= 65 THEN \"Senior\"\n" + "        ELSE \"Adult\"\n" +
                "    END AS age_category\n" + "FROM " + tableName + "\n" +
                "WHERE active = true\n" + "ORDER BY last_name, first_name;";
	}

	/**
	 * Example usage of the converted methods.
	 */
	public static void main(String[] args) {
		System.out.println("JSON Example:");
		System.out.println(getJsonExample());

		System.out.println("\nHTML Example:");
		System.out.println(getHtmlExample("Alice Smith", 35));

		System.out.println("\nSQL Example:");
		System.out.println(getSqlExample("users"));
	}

}
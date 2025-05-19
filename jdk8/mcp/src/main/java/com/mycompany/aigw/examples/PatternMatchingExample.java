/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.examples;

/**
 * This example demonstrates how to convert JDK17 pattern matching to JDK8 compatible
 * code. It shows the original pattern matching syntax and the JDK8 equivalent
 * implementation.
 *
 * @author MCP SDK Team
 */
public class PatternMatchingExample {

	/**
	 * This method demonstrates converting JDK 17 pattern matching to JDK 8 compatible
	 * code.
	 *
	 * <pre>
	 * // JDK 17 Pattern Matching
	 * static String processObject(Object obj) {
	 *     if (obj instanceof String str) {
	 *         return "String with length: " + str.length();
	 *     }
	 *     else if (obj instanceof Integer num) {
	 *         return "Integer with value: " + num;
	 *     }
	 *     else if (obj instanceof Double dbl && dbl > 0) {
	 *         return "Positive double: " + dbl;
	 *     }
	 *     return "Unknown object type";
	 * }
	 * </pre>
	 * @param obj The object to process
	 * @return A string describing the object
	 */
	static String processObject(Object obj) {
		// JDK 8 equivalent
		if (obj instanceof String) {
			String str = (String) obj;
			return "String with length: " + str.length();
		}
		else if (obj instanceof Integer) {
			Integer num = (Integer) obj;
			return "Integer with value: " + num;
		}
		else if (obj instanceof Double) {
			Double dbl = (Double) obj;
			if (dbl > 0) {
				return "Positive double: " + dbl;
			}
		}
		return "Unknown object type";
	}

	/**
	 * Example of a more complex pattern matching conversion using a switch statement.
	 *
	 * <pre>
	 * // JDK 17 pattern matching with switch (available in JDK 17 preview)
	 * static String describePet(Object pet) {
	 *     return switch (pet) {
	 *         case Dog d -> "Dog named " + d.getName();
	 *         case Cat c -> "Cat named " + c.getName();
	 *         case Fish f when f.isColorful() -> "Colorful fish";
	 *         case Fish f -> "Regular fish";
	 *         case null -> "No pet";
	 *         default -> "Unknown pet type";
	 *     };
	 * }
	 * </pre>
	 */
	static String describePet(Object pet) {
		// JDK 8 compatible version
		if (pet == null) {
			return "No pet";
		}

		if (pet instanceof Dog) {
			Dog d = (Dog) pet;
			return "Dog named " + d.getName();
		}
		else if (pet instanceof Cat) {
			Cat c = (Cat) pet;
			return "Cat named " + c.getName();
		}
		else if (pet instanceof Fish) {
			Fish f = (Fish) pet;
			if (f.isColorful()) {
				return "Colorful fish";
			}
			else {
				return "Regular fish";
			}
		}

		return "Unknown pet type";
	}

	/**
	 * Example usage.
	 */
	public static void main(String[] args) {
		System.out.println(processObject("Hello"));
		System.out.println(processObject(42));
		System.out.println(processObject(3.14));
		System.out.println(processObject(-1.5));
		System.out.println(processObject(new Object()));

		System.out.println("\nPet examples:");
		System.out.println(describePet(new Dog("Rex")));
		System.out.println(describePet(new Cat("Whiskers")));
		System.out.println(describePet(new Fish(true)));
		System.out.println(describePet(new Fish(false)));
		System.out.println(describePet(null));
		System.out.println(describePet("Not a pet"));
	}

	// Pet classes for the example
	static class Pet {

		private final String name;

		Pet(String name) {
			this.name = name;
		}

		String getName() {
			return name;
		}

	}

	static class Dog extends Pet {

		Dog(String name) {
			super(name);
		}

	}

	static class Cat extends Pet {

		Cat(String name) {
			super(name);
		}

	}

	static class Fish {

		private final boolean colorful;

		Fish(boolean colorful) {
			this.colorful = colorful;
		}

		boolean isColorful() {
			return colorful;
		}

	}

}
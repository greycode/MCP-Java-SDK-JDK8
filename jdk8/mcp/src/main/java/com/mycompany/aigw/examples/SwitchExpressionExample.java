/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.examples;

/**
 * This example demonstrates how to convert JDK17 switch expressions to JDK8 compatible
 * switch statements. It shows the original switch expression syntax and the JDK8
 * equivalent implementation.
 *
 * @author MCP SDK Team
 */
public class SwitchExpressionExample {

	/**
	 * This enum is used in the switch examples.
	 */
	public enum Day {

		MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

	}

	/**
	 * JDK 17 Example: <pre>
	 * static String getDayType(Day day) {
	 *     return switch (day) {
	 *         case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
	 *         case SATURDAY, SUNDAY -> "Weekend";
	 *     };
	 * }
	 * </pre>
	 *
	 * JDK 8 equivalent implementation:
	 */
	static String getDayType(Day day) {
		String result;
		switch (day) {
			case MONDAY:
			case TUESDAY:
			case WEDNESDAY:
			case THURSDAY:
			case FRIDAY:
				result = "Weekday";
				break;
			case SATURDAY:
			case SUNDAY:
				result = "Weekend";
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + day);
		}
		return result;
	}

	/**
	 * JDK 17 Example with expression yield: <pre>
	 * static int calculateValue(Day day) {
	 *     return switch (day) {
	 *         case MONDAY -> {
	 *             System.out.println("It's Monday, starting fresh!");
	 *             yield 1;
	 *         }
	 *         case TUESDAY, WEDNESDAY, THURSDAY -> {
	 *             int midWeekBonus = day == Day.WEDNESDAY ? 5 : 3;
	 *             yield midWeekBonus;
	 *         }
	 *         case FRIDAY -> 7;
	 *         case SATURDAY, SUNDAY -> {
	 *             int restBonus = calculateRestBonus();
	 *             yield restBonus * 2;
	 *         }
	 *     };
	 * }
	 * </pre>
	 *
	 * JDK 8 equivalent implementation:
	 */
	static int calculateValue(Day day) {
		int result;
		switch (day) {
			case MONDAY:
				System.out.println("It's Monday, starting fresh!");
				result = 1;
				break;
			case TUESDAY:
			case WEDNESDAY:
			case THURSDAY:
                result = day == Day.WEDNESDAY ? 5 : 3;
				break;
			case FRIDAY:
				result = 7;
				break;
			case SATURDAY:
			case SUNDAY:
				int restBonus = calculateRestBonus();
				result = restBonus * 2;
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + day);
		}
		return result;
	}

	/**
	 * Example with traditional switch on strings. JDK 17 Example: <pre>
	 * static int getValueForMonth(String month) {
	 *     return switch (month.toLowerCase()) {
	 *         case "january" -> 1;
	 *         case "february" -> 2;
	 *         case "march" -> 3;
	 *         // ... other months
	 *         case "december" -> 12;
	 *         default -> throw new IllegalArgumentException("Unknown month: " + month);
	 *     };
	 * }
	 * </pre>
	 *
	 * JDK 8 equivalent implementation:
	 */
	static int getValueForMonth(String month) {
		int result;
		switch (month.toLowerCase()) {
			case "january":
				result = 1;
				break;
			case "february":
				result = 2;
				break;
			case "march":
				result = 3;
				break;
			// ... other months would be here
			case "december":
				result = 12;
				break;
			default:
				throw new IllegalArgumentException("Unknown month: " + month);
		}
		return result;
	}

	/**
	 * Helper method
	 */
	private static int calculateRestBonus() {
		return 10;
	}

	/**
	 * Example usage of the converted methods.
	 */
	public static void main(String[] args) {
		System.out.println("Monday is a " + getDayType(Day.MONDAY));
		System.out.println("Saturday is a " + getDayType(Day.SATURDAY));

		System.out.println("\nCalculated values for days:");
		System.out.println("Monday: " + calculateValue(Day.MONDAY));
		System.out.println("Wednesday: " + calculateValue(Day.WEDNESDAY));
		System.out.println("Sunday: " + calculateValue(Day.SUNDAY));

		System.out.println("\nMonth values:");
		System.out.println("January: " + getValueForMonth("January"));
		System.out.println("december: " + getValueForMonth("december"));
	}

}
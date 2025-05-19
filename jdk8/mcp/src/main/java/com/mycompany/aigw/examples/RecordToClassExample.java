/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mycompany.aigw.examples;

import java.util.Objects;

/**
 * This example demonstrates how to convert JDK17 Record classes to JDK8 compatible
 * classes. It shows the original Record syntax and the JDK8 equivalent implementation
 * with the same behavior.
 *
 * @author MCP SDK Team
 */
public class RecordToClassExample {

	/**
	 * This is a comment showing what the original JDK 17 record looked like:
	 *
	 * <pre>
	 * // JDK 17 Record
	 * public record UserInfo(String name, int age) {}
	 * </pre>
	 *
	 * Below is the JDK 8 compatible equivalent with identical behavior.
	 */
	public static final class UserInfo {

		private final String name;

		private final int age;

		public UserInfo(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String name() {
			return name;
		}

		public int age() {
			return age;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			UserInfo userInfo = (UserInfo) o;
			return age == userInfo.age && Objects.equals(name, userInfo.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, age);
		}

		@Override
		public String toString() {
			return "UserInfo[name=" + name + ", age=" + age + "]";
		}

	}

	/**
	 * Another example of a more complex record conversion:
	 *
	 * <pre>
	 * // JDK 17 Record
	 * public record ApiResponse(int code, String message, Object data, boolean success) {}
	 * </pre>
	 */
	public static final class ApiResponse {

		private final int code;

		private final String message;

		private final Object data;

		private final boolean success;

		public ApiResponse(int code, String message, Object data, boolean success) {
			this.code = code;
			this.message = message;
			this.data = data;
			this.success = success;
		}

		public int code() {
			return code;
		}

		public String message() {
			return message;
		}

		public Object data() {
			return data;
		}

		public boolean success() {
			return success;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ApiResponse that = (ApiResponse) o;
			return code == that.code && success == that.success && Objects.equals(message, that.message)
					&& Objects.equals(data, that.data);
		}

		@Override
		public int hashCode() {
			return Objects.hash(code, message, data, success);
		}

		@Override
		public String toString() {
			return "ApiResponse[code=" + code + ", message=" + message + ", data=" + data + ", success=" + success
					+ "]";
		}

	}

	/**
	 * Example usage of the classes that replaced records.
	 */
	public static void main(String[] args) {
		UserInfo user = new UserInfo("John", 30);
		System.out.println("User: " + user);

		ApiResponse response = new ApiResponse(200, "Success", user, true);
		System.out.println("Response: " + response);

		UserInfo sameUser = new UserInfo("John", 30);
		System.out.println("Users equal: " + user.equals(sameUser));
	}

}
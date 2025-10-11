package com.ebt.cel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterpreterExample {
  public static void main(final String[] args) {
    // Example 1: Simple arithmetic
    System.out.println("=== Simple Arithmetic ===");
    evaluate("2 + 3 * 4");
    evaluate("10 / 3");
    evaluate("10 % 3");

    // Example 2: String operations
    System.out.println("\n=== String Operations ===");
    evaluate("\"Hello\" + \" \" + \"World\"");
    evaluate("\"abc\" * 3");

    // Example 3: Logical operations
    System.out.println("\n=== Logical Operations ===");
    evaluate("true && false");
    evaluate("true || false");
    evaluate("!false");

    // Example 4: Variables
    System.out.println("\n=== Variables ===");
    final Map<String, Object> vars = new HashMap<>();
    vars.put("name", "Alice");
    vars.put("age", 30L);
    vars.put("score", 85.5);

    evaluateWithVars("name + \" is \" + string(age) + \" years old\"", vars);
    evaluateWithVars("age > 18 ? \"adult\" : \"minor\"", vars);
    evaluateWithVars("score >= 90.0 ? \"A\" : score >= 80.0 ? \"B\" : \"C\"", vars);

    // Example 5: Collections
    System.out.println("\n=== Collections ===");
    vars.clear();
    vars.put("numbers", List.of(1L, 2L, 3L, 4L, 5L));
    vars.put("fruits", List.of("apple", "banana", "cherry"));

    evaluateWithVars("size(numbers)", vars);
    evaluateWithVars("numbers[0]", vars);
    evaluateWithVars("3 in numbers", vars);
    evaluateWithVars("[1, 2] + [3, 4]", vars);

    // Example 6: Map operations
    System.out.println("\n=== Map Operations ===");
    final Map<String, Object> person = new HashMap<>();
    person.put("name", "Bob");
    person.put("age", 25L);
    person.put("city", "New York");

    vars.clear();
    vars.put("person", person);

    evaluateWithVars("person.name", vars);
    evaluateWithVars("person[\"city\"]", vars);
    evaluateWithVars("has(person, \"age\")", vars);

    // Example 7: Macro functions
    System.out.println("\n=== Macro Functions ===");
    vars.clear();
    vars.put("numbers", List.of(1L, 2L, 3L, 4L, 5L));

    evaluateWithVars("numbers.map(x, x * 2)", vars);
    evaluateWithVars("numbers.filter(x, x > 2)", vars);
    evaluateWithVars("numbers.all(x, x > 0)", vars);
    evaluateWithVars("numbers.exists(x, x > 4)", vars);
    evaluateWithVars("numbers.existsOne(x, x == 3)", vars);

    // Example 8: String methods
    System.out.println("\n=== String Methods ===");
    vars.clear();
    vars.put("text", "Hello World");

    evaluateWithVars("text.contains(\"World\")", vars);
    evaluateWithVars("text.startsWith(\"Hello\")", vars);
    evaluateWithVars("text.endsWith(\"World\")", vars);
    evaluateWithVars("text.toLowerCase()", vars);

    // Example 9: Complex expressions
    System.out.println("\n=== Complex Expressions ===");
    vars.clear();
    vars.put(
        "users",
        List.of(
            Map.of("name", "Alice", "age", 30L, "active", true),
            Map.of("name", "Bob", "age", 25L, "active", false),
            Map.of("name", "Charlie", "age", 35L, "active", true)));

    evaluateWithVars("users.filter(u, u.active).map(u, u.name)", vars);
    evaluateWithVars("users.all(u, u.age > 18)", vars);
    evaluateWithVars("users.exists(u, u.name == \"Bob\")", vars);

    // Example 10: Deep equality and comparison
    System.out.println("\n=== Deep Equality and Comparison ===");
    evaluate("[1, 2, 3] == [1, 2, 3]");
    evaluate("[1, 2] < [1, 3]");
    evaluate("[1, 2] < [1, 2, 3]");
  }

  private static void evaluate(final String expression) {
    try {
      final CelParser parser = new CelParser(expression);
      final Expression ast = parser.parse();
      final Interpreter interpreter = new Interpreter();
      final Object result = interpreter.evaluate(ast);
      System.out.println(expression + " = " + format(result));
    } catch (final Exception e) {
      System.out.println(expression + " => ERROR: " + e.getMessage());
    }
  }

  private static void evaluateWithVars(final String expression, final Map<String, Object> vars) {
    try {
      final CelParser parser = new CelParser(expression);
      final Expression ast = parser.parse();
      final Interpreter interpreter = new Interpreter(vars, null);
      final Object result = interpreter.evaluate(ast);
      System.out.println(expression + " = " + format(result));
    } catch (final Exception e) {
      System.out.println(expression + " => ERROR: " + e.getMessage());
    }
  }

  private static String format(final Object value) {
    if (value instanceof String) {
      return "\"" + value + "\"";
    }
    if (value instanceof List) {
      return value.toString();
    }
    if (value instanceof Map) {
      return value.toString();
    }
    return String.valueOf(value);
  }
}

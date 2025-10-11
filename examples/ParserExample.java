package examples;

/** Example demonstrating the CEL parser usage. */
public class ParserExample {
  public static void main(final String[] args) {
    // Example 1: Simple arithmetic
    demo("1 + 2 * 3");

    // Example 2: Conditional expression
    demo("x > 0 ? 'positive' : 'negative'");

    // Example 3: Field access and method call
    demo("user.age > 18 && user.isActive()");

    // Example 4: List literal
    demo("[1, 2, 3, 4, 5]");

    // Example 5: Map literal
    demo("{\"name\": \"John\", \"age\": 30}");

    // Example 6: Struct literal
    demo("Person{name: \"John\", age: 30}");

    // Example 7: List operations
    demo("list.filter(x, x > 0).map(x, x * 2)");

    // Example 8: String with escape sequences
    demo("\"Hello\\nWorld\\t!\"");

    // Example 9: In operator
    demo("'admin' in user.roles");

    // Example 10: Complex nested expression
    demo("users.all(u, u.age >= 18) ? 'All adults' : 'Contains minors'");
  }

  private static void demo(final String expression) {
    System.out.println("Input: " + expression);
    try {
      final CelParser parser = new CelParser(expression);
      final Expression ast = parser.parse();
      System.out.println("Parsed successfully: " + ast.getClass().getSimpleName());
      System.out.println();
    } catch (final ParseError e) {
      System.out.println("Parse error: " + e.getMessage());
      System.out.println();
    }
  }
}

import java.util.List;
import java.util.Map;

/** Quick start examples demonstrating the libcel library. */
public class QuickStart {
  public static void main(final String[] args) {
    final Cel cel = new Cel();

    System.out.println("=== Basic Expressions ===");

    // Arithmetic
    System.out.println(cel.eval("2 + 3 * 4", Map.of())); // 14
    System.out.println(cel.eval("(2 + 3) * 4", Map.of())); // 20

    // Variables
    final var vars = Map.of("x", 10, "y", 20);
    System.out.println(cel.eval("x + y", vars)); // 30
    System.out.println(cel.eval("x * 2 + y", vars)); // 40

    System.out.println("\n=== Working with Strings ===");

    // String concatenation
    System.out.println(cel.eval("'Hello, ' + 'World!'", Map.of())); // Hello, World!

    // String methods
    System.out.println(cel.eval("'hello'.toUpperCase()", Map.of())); // HELLO
    System.out.println(cel.eval("'  trim me  '.trim()", Map.of())); // trim me
    System.out.println(cel.eval("'a,b,c'.split(',')", Map.of())); // [a, b, c]

    System.out.println("\n=== Working with Collections ===");

    // Lists
    System.out.println(cel.eval("[1, 2, 3] + [4, 5]", Map.of())); // [1, 2, 3, 4, 5]
    System.out.println(cel.eval("[1, 2, 3][1]", Map.of())); // 2
    System.out.println(cel.eval("size([1, 2, 3])", Map.of())); // 3

    // Maps
    final var user = Map.of("name", "Alice", "age", 30);
    System.out.println(cel.eval("user.name", Map.of("user", user))); // Alice
    System.out.println(cel.eval("user['age']", Map.of("user", user))); // 30

    System.out.println("\n=== Boolean Logic ===");

    System.out.println(cel.eval("true && false", Map.of())); // false
    System.out.println(cel.eval("true || false", Map.of())); // true
    System.out.println(cel.eval("!false", Map.of())); // true

    // Comparisons
    System.out.println(cel.eval("10 > 5", Map.of())); // true
    System.out.println(cel.eval("'abc' < 'xyz'", Map.of())); // true

    // Conditional (ternary)
    System.out.println(cel.eval("age >= 18 ? 'adult' : 'minor'", Map.of("age", 25))); // adult

    System.out.println("\n=== Macro Functions ===");

    // map - Transform elements
    System.out.println(cel.eval("[1, 2, 3].map(x, x * 2)", Map.of())); // [2, 4, 6]

    // filter - Select elements
    System.out.println(cel.eval("[1, 2, 3, 4, 5].filter(x, x > 2)", Map.of())); // [3, 4, 5]

    // all - Check all elements
    System.out.println(cel.eval("[2, 4, 6].all(x, x % 2 == 0)", Map.of())); // true

    // exists - Check any element
    System.out.println(cel.eval("[1, 2, 3].exists(x, x > 2)", Map.of())); // true

    // existsOne - Check exactly one
    System.out.println(cel.eval("[1, 2, 3].existsOne(x, x == 2)", Map.of())); // true

    // Chaining macros
    System.out.println(cel.eval("[1, 2, 3, 4, 5].filter(x, x > 2).map(x, x * 10)", Map.of()));
    // [30, 40, 50]

    System.out.println("\n=== Working with Complex Data ===");

    final var data =
        Map.of(
            "users",
            List.of(
                Map.of("name", "Alice", "age", 30, "active", true),
                Map.of("name", "Bob", "age", 25, "active", false),
                Map.of("name", "Charlie", "age", 35, "active", true)));

    // Filter and map
    System.out.println(cel.eval("users.filter(u, u.active).map(u, u.name)", data));
    // [Alice, Charlie]

    // Complex conditions
    System.out.println(cel.eval("users.exists(u, u.age > 30 && u.active)", data)); // true

    System.out.println("\n=== Compiled Programs ===");

    // Compile once, evaluate many times
    final CelProgram program = cel.compile("price * quantity * (1 - discount)");

    System.out.println(program.evaluate(Map.of("price", 10, "quantity", 5, "discount", 0.1)));
    // 45.0

    System.out.println(program.evaluate(Map.of("price", 20, "quantity", 3, "discount", 0.2)));
    // 48.0

    System.out.println("\n=== Type Conversions ===");

    System.out.println(cel.eval("int('42')", Map.of())); // 42
    System.out.println(cel.eval("double(42)", Map.of())); // 42.0
    System.out.println(cel.eval("string(42)", Map.of())); // 42
    System.out.println(cel.eval("type([1, 2, 3])", Map.of())); // list

    System.out.println("\n=== Custom Functions ===");

    final var customCel = new Cel(new CustomFunctions());
    System.out.println(customCel.eval("reverse('hello')", Map.of())); // olleh
    System.out.println(customCel.eval("double('world')", Map.of())); // worldworld
  }

  /** Example custom function library extending standard functions. */
  static class CustomFunctions extends StandardFunctions {
    @Override
    public Object call(final String name, final List<Object> args) {
      return switch (name) {
        case "reverse" -> new StringBuilder((String) args.get(0)).reverse().toString();
        case "double" -> {
          final String str = (String) args.get(0);
          yield str + str;
        }
        default -> super.call(name, args);
      };
    }
  }
}

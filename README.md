# libcel - Common Expression Language for Java

A complete Java implementation of Google's [Common Expression Language (CEL)](https://github.com/google/cel-spec)
specification, ported from the Dart implementation.

## Overview

CEL is a non-Turing complete expression language designed for simplicity, speed, and safety. It's commonly used for
evaluating user-provided expressions in a secure sandbox environment.

## Features

- **Complete CEL Implementation**: All CEL operators, functions, and macros
- **Type Safe**: Leverages Java 17's type system with sealed interfaces and pattern matching
- **High Performance**: Hand-written recursive descent parser with AST compilation
- **Extensible**: Easy to add custom functions
- **Well Tested**: 119 comprehensive tests ensuring functional equivalence with Dart implementation
- **Zero External Dependencies**: Pure Java implementation (except JUnit for testing)

## Quick Start

### Basic Usage

```java
import com.libdbm.cel.CEL;

import java.util.Map;

public class Example {
    public static void main(String[] args) {
        final CEL cel = new CEL();

        // Simple expression evaluation
        System.out.println(cel.eval("2 + 3 * 4", Map.of())); // 14

        // Using variables
        final var vars = Map.of("name", "Alice", "age", 30);
        System.out.println(cel.eval("name + ' is ' + string(age) + ' years old'", vars));
        // Output: Alice is 30 years old

        // Boolean logic
        System.out.println(cel.eval("age >= 18 && age < 65", vars)); // true
    }
}
```

### Compiling and Reusing Programs

For better performance when evaluating the same expression multiple times:

```java
final Cel cel = new Cel();
final CelProgram program = cel.compile("price * quantity * (1 - discount)");

// Reuse with different variables
final var result1 = program.evaluate(Map.of("price", 10, "quantity", 5, "discount", 0.1));
final var result2 = program.evaluate(Map.of("price", 20, "quantity", 3, "discount", 0.2));
```

### Working with Complex Data

```java
final Cel cel = new Cel();
final var data = Map.of(
        "user", Map.of(
                "name", "Alice",
                "roles", List.of("admin", "user"),
                "metadata", Map.of("active", true)
        ),
        "permissions", List.of("read", "write", "delete")
);

// Check complex conditions
final var canDelete = cel.eval(
        "\"admin\" in user.roles && \"delete\" in permissions",
        data
); // true

// Use macro functions
final var activeAdmins = cel.eval(
        "users.filter(u, u.active).map(u, u.name)",
        Map.of("users", List.of(
                Map.of("name", "Alice", "active", true),
                Map.of("name", "Bob", "active", false),
                Map.of("name", "Charlie", "active", true)
        ))
); // ["Alice", "Charlie"]
```

### Custom Functions

Extend the standard library with custom functions:

```java
import com.libdbm.cel.StandardFunctions;

import java.util.List;

class CustomFunctions extends StandardFunctions {
    @Override
    public Object call(final String name, final List<Object> args) {
        if (name.equals("reverse")) {
            return new StringBuilder((String) args.get(0)).reverse().toString();
        }
        return super.callFunction(name, args);
    }
}

final Cel cel = new Cel(new CustomFunctions());
System.out.

println(cel.eval("reverse('hello')", Map.of())); // "olleh"
```

## Supported Features

### Literals

- Null: `null`
- Booleans: `true`, `false`
- Integers: `42`, `-7`, `0xFF` (hexadecimal)
- Unsigned: `42u`, `0xFFu`
- Doubles: `3.14`, `6.022e23`
- Strings: `"hello"`, `'world'`, `r"raw\nstring"`, `"""multi-line"""`
- Bytes: `b"data"`
- Lists: `[1, 2, 3]`
- Maps: `{"key": "value"}`

### Operators

- **Arithmetic**: `+`, `-`, `*`, `/`, `%`
- **Comparison**: `<`, `<=`, `>`, `>=`, `==`, `!=`
- **Logical**: `&&`, `||`, `!`
- **Conditional**: `condition ? trueValue : falseValue`
- **Membership**: `in` (for lists, maps, strings)

### Functions

- **Type conversions**: `int()`, `double()`, `string()`, `bool()`
- **Type checking**: `type()`
- **Collections**: `size()`, `has()`
- **String methods**: `contains()`, `startsWith()`, `endsWith()`, `toLowerCase()`, `toUpperCase()`, `trim()`,
  `replace()`, `split()`
- **Regex**: `matches()`
- **Math**: `max()`, `min()`

### Macro Functions

```java
// map - Transform each element
cel.eval("[1, 2, 3].map(x, x * 2)",Map.of()); // [2, 4, 6]

// filter - Keep elements matching condition
        cel.

eval("[1, 2, 3, 4].filter(x, x % 2 == 0)",Map.of()); // [2, 4]

// exists - Check if any element matches
        cel.

eval("[1, 2, 3].exists(x, x > 2)",Map.of()); // true

// all - Check if all elements match
        cel.

eval("[1, 2, 3].all(x, x > 0)",Map.of()); // true

// existsOne - Check if exactly one element matches
        cel.

eval("[1, 2, 3].existsOne(x, x == 2)",Map.of()); // true
```

## Building

```bash
# Compile the project
mvn clean compile

# Run tests (119 tests)
mvn test

# Package as JAR
mvn package

# Install to local Maven repository
mvn install
```

## Testing

The project includes comprehensive test coverage:

- 31 parser tests
- 21 interpreter tests
- 67 integration tests
- All tests from the Dart implementation ported and passing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CelTest

# Run specific test method
mvn test -Dtest=CelTest$CelParser#parsesLiterals
```

## Architecture

- **Expression.java**: Abstract Syntax Tree (AST) with sealed interface hierarchy
- **CelParser.java**: Hand-written recursive descent parser
- **Interpreter.java**: AST evaluator using Visitor pattern
- **Functions.java**: Extensible function library
- **Cel.java**: Main API entry point
- **CelProgram.java**: Compiled, reusable programs

## Functional Equivalence

This Java implementation is functionally equivalent to the [Dart libcel](https://pub.dev/packages/libcel)
implementation:

- Same AST structure and expression types
- Identical parsing rules and operator precedence
- Same evaluation semantics
- Equivalent macro function behavior
- Compatible error handling

All tests from the Dart version have been ported to ensure equivalence.

## Requirements

- Java 17 or higher
- Maven 3.6+ (for building)

## License

BSD 3-Clause License

## Acknowledgments

- Based on the [Common Expression Language](https://github.com/google/cel-spec) specification by Google
- Ported from the [Dart libcel](https://pub.dev/packages/libcel) implementation

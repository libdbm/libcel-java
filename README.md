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


## Releases and Snapshots

This project is configured to publish both release (non-SNAPSHOT) and snapshot artifacts to OSSRH via GitHub Actions.
The workflow file is at .github/workflows/maven-publish.yml.

Prerequisites
- Secrets configured in the repository settings:
  - OSSRH_USERNAME and OSSRH_PASSWORD
  - GPG_PRIVATE_KEY and GPG_PASSPHRASE (ASCII-armored key)
- GitHub Actions permissions allow pushing commits and tags using GITHUB_TOKEN (default for same-repo workflows).

How publishing works
- On main (or when manually dispatched), the workflow inspects the current POM version.
  - If the version ends with -SNAPSHOT, it deploys to the OSSRH snapshots repository.
  - If the version is a non-SNAPSHOT (e.g., 1.2.3), it will:
    1) Create and push an annotated tag v<version> if it doesn’t already exist.
    2) Perform a signed, staged deploy using the release profile to OSSRH (Maven Central flow).
    3) Compute the next development version by incrementing the patch by default and set it to <next>-SNAPSHOT in pom.xml.
    4) Commit and push the bumped pom.xml back to main with a [skip ci] commit message.
- If the workflow is triggered by pushing an existing tag v*, it performs a standard release deployment and exits (no version bump).

Publish a snapshot (to OSSRH snapshots)
Option A — Using main automatically
1) Ensure pom.xml version ends with -SNAPSHOT (e.g., 1.2.4-SNAPSHOT).
2) Push your changes to the main branch.
3) The workflow will run and deploy to the snapshots repo: https://s01.oss.sonatype.org/content/repositories/snapshots/

Option B — Manual run (workflow_dispatch)
1) Ensure pom.xml version ends with -SNAPSHOT.
2) In GitHub: Actions → Java CI and Publish → Run workflow → Run.
3) It will deploy to the OSSRH snapshots repository.

Publish a release (non-SNAPSHOT to Maven Central)
Option A — From main with auto-tagging and auto-bump
1) Set pom.xml <version> to a non-SNAPSHOT (e.g., 1.2.4).
2) Push to main (or trigger the workflow manually).
3) The workflow will:
   - Tag the repo with v1.2.4 (idempotent if it already exists), push the tag.
   - Deploy the release with GPG signing and staging to OSSRH.
   - Bump pom.xml to the next development version (e.g., 1.2.5-SNAPSHOT) and push that commit back to main.

Option B — By pushing a tag
1) Create and push a tag that matches v<version> (e.g., v1.2.4); ensure pom.xml reflects that version.
2) The workflow triggered by the tag will perform the release deploy and exit (no version bump).

Manual trigger (either case)
- Actions → Java CI and Publish → Run workflow. This supports both release and snapshot flows depending on the POM version and whether a tag triggered the run.

Notes and gotchas
- Version parsing assumes semantic versions X.Y.Z for computing the next development version. If not strictly semantic, it will fallback by appending .1-SNAPSHOT.
- The bump commit includes [skip ci] to prevent re-triggering the workflow.
- distributionManagement in pom.xml already points to OSSRH snapshot and staging endpoints.
- You can always see what the workflow decided by reading its logs: it prints the detected POM version and the path taken (snapshot vs release).

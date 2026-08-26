# libcel - Common Expression Language for Java

A complete Java implementation of Google's [Common Expression Language (CEL)](https://github.com/google/cel-spec)
specification, ported from the Dart implementation.

## Overview

CEL is a non-Turing complete expression language designed for simplicity, speed, and safety. It's commonly used for
evaluating user-provided expressions in a secure sandbox environment.

## Features

- **Complete CEL Implementation**: All CEL operators, functions, and macros
- **Extension Libraries**: strings, lists, sets, math, regex and base64 functions using the cel-go names
- **Round-trip Printing**: render a parsed expression back to CEL source, compact or pretty printed
- **Type Safe**: Leverages Java 17's type system with sealed interfaces and pattern matching
- **High Performance**: Hand-written recursive descent parser with AST compilation
- **Extensible**: Easy to add custom functions
- **Well Tested**: 247 comprehensive tests, including a specification conformance suite
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
- **Comparison**: `<`, `<=`, `>`, `>=`, `==`, `!=`, compared exactly across integer and double
  types, so a long beyond the range a double can represent is not mistaken for the double it
  would round to
- **Logical**: `&&`, `||`, `!`
- **Conditional**: `condition ? trueValue : falseValue`
- **Membership**: `in` (for lists, maps, strings)

### Functions

- **Type conversions**: `int()`, `uint()`, `double()`, `string()`, `bool()`, `bytes()`, `dyn()`
- **Type checking**: `type()`, which returns a type value comparable against the bare type names
  `null_type`, `bool`, `int`, `uint`, `double`, `string`, `bytes`, `list`, `map`, `timestamp`,
  `duration` and `type`
- **Collections**: `size()`, `has()`
- **Math**: `max()`, `min()`
- **Regex**: `matches()`, as a function and as a string method

### String functions

Receiver style, following the cel-go strings extension:

`charAt()`, `indexOf()`, `lastIndexOf()`, `lowerAscii()`, `upperAscii()`, `substring()`, `reverse()`,
`trim()`, `contains()`, `startsWith()`, `endsWith()`, `replace(from, to[, limit])`,
`split(separator[, limit])`, `join([separator])` on a list of strings, `format(list)`, and the global
`strings.quote()`.

```java
cel.eval("'hello'.charAt(1)", Map.of());                  // "e"
cel.eval("'a,b,c'.split(',', 2)", Map.of());              // ["a", "b,c"]
cel.eval("['a','b'].join('-')", Map.of());                // "a-b"
cel.eval("'%d apples at %.2f'.format([3, 1.5])", Map.of()); // "3 apples at 1.50"
```

`format()` supports the verbs `%s %d %f %e %b %o %x %X %%` with an optional precision such as `%.3f`.

Positions and sizes count Unicode code points, not UTF-16 code units, so `charAt()`, `substring()`,
`indexOf()`, `lastIndexOf()`, string indexing and `size()` never split a supplementary character:

```java
cel.eval("size('\\U0001F600b')", Map.of());        // 2
cel.eval("'\\U0001F600b'.charAt(0)", Map.of());    // the whole emoji, not a lone surrogate
```

### Regex functions

```java
cel.eval("regex.replace('a b c', ' ', '-')", Map.of());        // "a-b-c"
cel.eval("regex.replace('ab', '(\\w)', '[\\1]')", Map.of());   // "[a][b]"
cel.eval("regex.extract('id=123', '=([0-9]+)')", Map.of());    // "123"
cel.eval("regex.extractAll('a1b2', '[0-9]')", Map.of());       // ["1", "2"]
```

`regex.replace()` takes an optional fourth argument limiting the number of replacements. Capture
groups are referenced as `\1` through `\9`, and `\0` for the whole match.

### List and set functions

```java
cel.eval("[1, 2, 2, 3].distinct()", Map.of());        // [1, 2, 3]
cel.eval("[[1, 2], [3]].flatten()", Map.of());        // [1, 2, 3]
cel.eval("[3, 1, 2].sort()", Map.of());               // [1, 2, 3]
cel.eval("[1, 2, 3, 4].slice(1, 3)", Map.of());       // [2, 3]
cel.eval("[1, 2, 3].reverse()", Map.of());            // [3, 2, 1]
cel.eval("[1, 2].first()", Map.of());                 // 1
cel.eval("lists.range(3)", Map.of());                 // [0, 1, 2]
cel.eval("sets.contains([1, 2, 3], [1, 3])", Map.of()); // true
cel.eval("sets.equivalent([1, 2, 2], [2, 1])", Map.of()); // true
cel.eval("sets.intersects([1, 2], [2, 3])", Map.of());  // true
```

### Math functions

`math.greatest()`, `math.least()`, `math.abs()`, `math.ceil()`, `math.floor()`, `math.round()`,
`math.trunc()`, `math.sign()`, `math.sqrt()`, `math.isNaN()`, `math.isInf()`, `math.isFinite()`,
`math.bitAnd()`, `math.bitOr()`, `math.bitXor()`, `math.bitNot()`, `math.bitShiftLeft()`,
`math.bitShiftRight()`.

`abs()` and `sign()` preserve the numeric type of their argument; `math.greatest()` and
`math.least()` also accept a single list.

### Bytes and base64

Bytes literals evaluate to a Java `byte[]`. They support `size()`, indexing, concatenation,
comparison and `string()` conversion (decoded as UTF-8).

```java
cel.eval("base64.encode(b'hello')", Map.of());          // "aGVsbG8="
cel.eval("string(base64.decode('aGVsbG8='))", Map.of()); // "hello"
```

### Timestamps and durations

`timestamp()` produces a `java.time.Instant`, `duration()` a `java.time.Duration`. Durations accept
the full CEL syntax: signed, multi-unit and fractional values with the units `ns`, `us`, `ms`, `s`,
`m` and `h`, for example `1h30m`, `-1.5s` or `100ms`. Components are accumulated exactly, and a
duration beyond the specification's range of `Utilities.SPAN` seconds either side of zero is
rejected rather than silently clamped.

Accessors are available both as global functions and as methods, and default to UTC. Each takes an
optional IANA time zone as its last argument.

```java
cel.eval("timestamp('2024-03-05T14:30:45Z').getFullYear()", Map.of());              // 2024
cel.eval("getHours(timestamp('2024-03-05T14:30:45Z'), 'America/New_York')", Map.of()); // 9
cel.eval("duration('1h30m').getMinutes()", Map.of());                              // 90
```

The accessors are `getFullYear`, `getMonth` (zero based), `getDate`, `getDayOfWeek` (Sunday is 0),
`getDayOfYear` (zero based), `getHours`, `getMinutes`, `getSeconds` and `getMilliseconds`.

Timestamps and durations support arithmetic and comparison: `ts + dur`, `dur + ts`, `ts - dur`,
`ts - ts` (yielding a duration), `dur + dur`, `dur - dur` and `-dur`.

### Namespaced functions

`math`, `sets`, `lists`, `strings`, `base64` and `regex` are namespaces, not variables. A variable
of the same name always wins, so binding `math` in the evaluation variables makes `math.greatest`
resolve as an ordinary field selection again.

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

`sortBy` orders a list by a computed key, and `map` accepts an optional predicate between the
variable and the transform:

```java
cel.eval("users.sortBy(u, u.age).map(u, u.name)", data);
cel.eval("[1, 2, 3, 4].map(x, x % 2 == 0, x * 2)", Map.of()); // [4, 8]
```

Macros also accept a map as their target, iterating over its keys.

## Printing expressions

`Printer` renders a parsed expression back into CEL source. The compact form is a single line that
parses back into an equivalent expression, with parentheses only where precedence requires them.

```java
final var expr = new Parser("(a + b) * c").parse();
Printer.print(expr);                                 // "(a + b) * c"
Printer.print(new Parser("a + (b * c)").parse());    // "a + b * c"
```

The pretty form breaks lists, maps, structs, argument lists and logical chains once they no longer
fit the configured width:

```java
Printer.print(expr, Printer.Options.pretty());       // 2 space indent, 100 column width
Printer.print(expr, new Printer.Options(true, 4, 60));
```

Original spacing and redundant parentheses are not preserved, and comprehension nodes, which the
parser never produces, are rendered in a diagnostic form that is not valid CEL.

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

- 32 parser tests
- 21 interpreter tests
- 71 integration tests
- 14 printer tests
- 39 extension function tests
- 69 conformance tests organized after the google/cel-spec corpus
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

- **ast/Expression.java**: Abstract Syntax Tree (AST) with sealed interface hierarchy
- **parser/Parser.java**: Hand-written recursive descent parser
- **Interpreter.java**: AST evaluator using Visitor pattern
- **Type.java**: CEL type values returned by type()
- **Printer.java**: AST to CEL source renderer, also using Visitor pattern
- **Functions.java**: Extensible function library
- **StandardFunctions.java**: Built-in and extension function dispatch
- **Strings/Lists/Sets/Maths/Regexes/Codecs.java**: Extension function implementations
- **CEL.java**: Main API entry point
- **Program.java**: Compiled, reusable programs

## Functional Equivalence

This Java implementation is functionally equivalent to the [Dart libcel](https://pub.dev/packages/libcel)
implementation:

- Same AST structure and expression types
- Identical parsing rules and operator precedence
- Same evaluation semantics
- Equivalent macro function behavior
- Compatible error handling

All tests from the Dart version have been ported to ensure equivalence.

## Specification compliance

```java
cel.eval("1 / 2", Map.of());                 // 0, integer division truncates
cel.eval("1.0 / 0.0", Map.of());             // Infinity, not an error
cel.eval("has(a.b)", data);                  // presence test, false rather than an error
cel.eval("type(42) == int", Map.of());       // true
cel.eval("false && missing", Map.of());      // false, errors are absorbed by a false operand
```

Known deviations from the specification, kept deliberately:

- `uint` is represented as a signed `long`, so `type(1u)` is `int` and `1u == 1` is true.
- `"ab" * 3` and `[1] * 2` repeat a string or list, and `"a" in "abc"` tests for a substring. CEL
  defines none of these; they are extensions this library adds.
- `has(map, key)` is also accepted as a two-argument function alongside the `has(a.b)` macro.
- Map keys are not restricted to the specification's int, uint, bool and string types, and they are
  matched with the same numeric equality the rest of the language uses. A key stored as an
  `Integer` matches a `Long` probe, and `{1: "a"}[1.0]` resolves. Membership, indexing, presence
  tests and map equality all follow this one rule, so `key in map ? map[key] : fallback` never
  fails after its guard succeeds, and a map bound from Java compares equal to the literal that
  spells it.

### Evaluation limits

CEL exists to evaluate untrusted input, so operations whose size the expression itself controls are
bounded by `Utilities.LIMIT`, one million elements or characters. `lists.range(n)`, the repetition
operators and `format()` raise an `EvaluationError` rather than exhausting the heap:

```java
cel.eval("lists.range(9223372036854775807)", Map.of()); // EvaluationError
cel.eval("'x' * 2000000000", Map.of());                 // EvaluationError
cel.eval("'%.2000000f'.format([1.5])", Map.of());        // EvaluationError
```

The ceiling bounds each operation's **output**, not only its inputs, so two separately legal values
cannot be combined into one that exhausts the heap. `join()`, `replace()`, `regex.replace()`,
`base64.encode()` and string, list and bytes concatenation are all bounded this way:

```java
cel.eval("lists.range(1000000).map(x, 'a').join('x' * 1000000)", Map.of()); // EvaluationError
cel.eval("('x' * 1000000) + ('x' * 1000000)", Map.of());                    // EvaluationError
```

Collection operations resolve membership through a hash index rather than a scan, so `distinct()`
and the `sets` functions stay linear in their input and cannot burn CPU quadratically at the size
the limit allows.

This is a ceiling on individual operations, not a budget for a whole expression: a deeply nested
expression can still allocate a multiple of the limit.

## Breaking changes

- Bytes literals now evaluate to `byte[]` instead of `String`. Use `string(value)` to decode them as
  UTF-8, or `bytes(value)` to convert the other way.
- The timestamp accessors (`getFullYear`, `getMonth`, `getDate`, `getHours`, `getMinutes`,
  `getSeconds`) now default to UTC rather than the system time zone, and return `long`. Pass an
  IANA time zone as the last argument to keep a local reading.
- Integer division returns an integer: `1 / 2` is `0`, not `0.5`. Use a double operand for the old
  behavior. Double division by zero yields infinity instead of raising an error, and `%` now
  requires integer operands.
- Integer arithmetic raises an error on overflow instead of wrapping.
- `type()` returns a `Type` value rather than a `String`, so `type(42) == int` is true and
  `type(42) == "int"` is false. Use `string(type(x))` for the old string form.
- `size()` returns `long` rather than `int`, matching every other integer result.
- Several `Utilities` helpers changed return type and are therefore **binary incompatible** with
  earlier releases: `typeOf` returns `Type` instead of `String`, and `sizeOf`, `hoursOf`,
  `minutesOf` and `secondsOf` return `long` instead of `int`. Recompile against this version rather
  than dropping the jar onto an existing classpath.
- Mixed integer and double comparison is now exact rather than converted through a double, so
  `9223372036854775807 == 9223372036854775808.0` is false where it was previously true.
- `timestamp(n)` reads `n` as epoch **seconds**, not milliseconds, and `string(duration)` renders
  the CEL form (`"3600s"`) rather than ISO-8601.
- `&&`, `||`, `!` and `? :` now require boolean operands instead of treating any non-true value as
  false. The logical operators absorb errors: `false && error` is false and `true || error` is true.
  Macro predicates must likewise yield a boolean.
- `Program.text()` has been removed. Use `Printer.print(expr)`.

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

package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import com.libdbm.cel.parser.ParseError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the CEL parser, interpreter, and programs. Ported from the Dart libcel
 * test suite.
 */
class CELTests {
  @Nested
  class Parsing {
    private CEL cel;

    @BeforeEach
    void setup() {
      cel = new CEL();
    }

    @Test
    void parsesLiterals() {
      assertNull(cel.eval("null", Map.of()));
      assertTrue((Boolean) cel.eval("true", Map.of()));
      assertFalse((Boolean) cel.eval("false", Map.of()));
      assertEquals(42L, cel.eval("42", Map.of()));
      assertEquals(3.14, cel.eval("3.14", Map.of()));
      assertEquals("hello", cel.eval("\"hello\"", Map.of()));
      assertEquals("raw string", cel.eval("r\"raw string\"", Map.of()));
    }

    @Test
    void parsesHexadecimalIntegers() {
      assertEquals(16L, cel.eval("0x10", Map.of()));
      assertEquals(255L, cel.eval("0xFF", Map.of()));
      assertEquals(26L, cel.eval("0x1A", Map.of()));
      assertEquals(-16L, cel.eval("-0x10", Map.of()));
      assertEquals(16L, cel.eval("0x10u", Map.of()));
      assertEquals(255L, cel.eval("0xFFu", Map.of()));
    }

    @Test
    void parsesStringsWithOctalEscapeSequences() {
      assertEquals("A", cel.eval("\"\\101\"", Map.of())); // \101 = 'A' (65 in octal)
      assertEquals(" ", cel.eval("\"\\040\"", Map.of())); // \040 = space (32 in octal)
      assertEquals("abc", cel.eval("\"\\141\\142\\143\"", Map.of())); // \141=a, \142=b, \143=c
      assertEquals("\u0000", cel.eval("\"\\000\"", Map.of())); // null character
      assertEquals("\u00FF", cel.eval("\"\\377\"", Map.of())); // max octal value (255)
    }

    @Test
    void parsesStringsWithAdditionalEscapeSequences() {
      assertEquals("\u0007", cel.eval("\"\\a\"", Map.of())); // bell/alert
      assertEquals("\b", cel.eval("\"\\b\"", Map.of())); // backspace
      assertEquals("\f", cel.eval("\"\\f\"", Map.of())); // form feed
      assertEquals("\u000B", cel.eval("\"\\v\"", Map.of())); // vertical tab
      assertEquals("?", cel.eval("\"\\?\"", Map.of())); // question mark
      assertEquals("`", cel.eval("\"\\`\"", Map.of())); // backtick

      // Combined escape sequences
      assertEquals("Hello\bWorld", cel.eval("\"Hello\\bWorld\"", Map.of()));
      assertEquals("Line1\fLine2", cel.eval("\"Line1\\fLine2\"", Map.of()));
      assertEquals("Tab\u000Bhere", cel.eval("\"Tab\\vhere\"", Map.of()));
    }

    @Test
    void parsesBytesLiteralsWithUppercaseBPrefix() {
      assertEquals("hello", cel.eval("b\"hello\"", Map.of()));
      assertEquals("hello", cel.eval("B\"hello\"", Map.of()));
      assertEquals("world", cel.eval("b'world'", Map.of()));
      assertEquals("world", cel.eval("B'world'", Map.of()));
    }

    @Test
    void parsesTripleQuotedStrings() {
      // Basic triple-quoted strings
      assertEquals("hello world", cel.eval("\"\"\"hello world\"\"\"", Map.of()));
      assertEquals("hello world", cel.eval("'''hello world'''", Map.of()));

      // Multi-line strings
      assertEquals(
          "line 1\nline 2\nline 3", cel.eval("\"\"\"line 1\nline 2\nline 3\"\"\"", Map.of()));
      assertEquals("line 1\nline 2\nline 3", cel.eval("'''line 1\nline 2\nline 3'''", Map.of()));

      // Strings containing quotes
      assertEquals("She said \"Hello\"!", cel.eval("\"\"\"She said \"Hello\"!\"\"\"", Map.of()));
      assertEquals("It's a nice day", cel.eval("'''It's a nice day'''", Map.of()));

      // Triple-quoted strings with escape sequences
      assertEquals("hello\nworld", cel.eval("\"\"\"hello\\nworld\"\"\"", Map.of()));
      assertEquals("tab\there", cel.eval("\"\"\"tab\\there\"\"\"", Map.of()));

      // Raw triple-quoted strings (no escape processing)
      assertEquals("hello\\nworld", cel.eval("r\"\"\"hello\\nworld\"\"\"", Map.of()));
      assertEquals("tab\\there", cel.eval("R'''tab\\there'''", Map.of()));

      // Triple-quoted strings with embedded double quotes
      assertEquals(
          "He said \"\"Hello\"\" twice",
          cel.eval("\"\"\"He said \"\"Hello\"\" twice\"\"\"", Map.of()));
    }

    @Test
    void parsesIdentifiers() {
      assertEquals(10, cel.eval("x", Map.of("x", 10)));
      assertEquals("John", cel.eval("name", Map.of("name", "John")));
    }

    @Test
    void parsesArithmeticExpressions() {
      assertEquals(5L, cel.eval("2 + 3", Map.of()));
      assertEquals(6L, cel.eval("10 - 4", Map.of()));
      assertEquals(12L, cel.eval("3 * 4", Map.of()));
      assertEquals(5.0, cel.eval("15 / 3", Map.of()));
      assertEquals(2L, cel.eval("17 % 5", Map.of()));
    }

    @Test
    void respectsOperatorPrecedence() {
      assertEquals(14L, cel.eval("2 + 3 * 4", Map.of()));
      assertEquals(20L, cel.eval("(2 + 3) * 4", Map.of()));
      assertEquals(4L, cel.eval("10 - 2 * 3", Map.of()));
    }

    @Test
    void parsesComparisonOperators() {
      assertTrue((Boolean) cel.eval("3 < 5", Map.of()));
      assertTrue((Boolean) cel.eval("5 <= 5", Map.of()));
      assertTrue((Boolean) cel.eval("7 > 4", Map.of()));
      assertTrue((Boolean) cel.eval("8 >= 8", Map.of()));
      assertTrue((Boolean) cel.eval("5 == 5", Map.of()));
      assertTrue((Boolean) cel.eval("5 != 3", Map.of()));
    }

    @Test
    void parsesLogicalOperators() {
      assertTrue((Boolean) cel.eval("true && true", Map.of()));
      assertFalse((Boolean) cel.eval("true && false", Map.of()));
      assertTrue((Boolean) cel.eval("false || true", Map.of()));
      assertFalse((Boolean) cel.eval("false || false", Map.of()));
      assertFalse((Boolean) cel.eval("!true", Map.of()));
      assertTrue((Boolean) cel.eval("!false", Map.of()));
    }

    @Test
    void parsesConditionalExpressions() {
      assertEquals(1L, cel.eval("true ? 1 : 2", Map.of()));
      assertEquals(2L, cel.eval("false ? 1 : 2", Map.of()));
      assertEquals("big", cel.eval("x > 5 ? \"big\" : \"small\"", Map.of("x", 10)));
      assertEquals("small", cel.eval("x > 5 ? \"big\" : \"small\"", Map.of("x", 3)));
    }

    @Test
    void parsesListLiterals() {
      assertEquals(List.of(), cel.eval("[]", Map.of()));
      assertEquals(List.of(1L, 2L, 3L), cel.eval("[1, 2, 3]", Map.of()));
      assertEquals(List.of(1L, "two", true), cel.eval("[1, \"two\", true]", Map.of()));
    }

    @Test
    void parsesMapLiterals() {
      assertEquals(Map.of(), cel.eval("{}", Map.of()));
      assertEquals(Map.of("a", 1L, "b", 2L), cel.eval("{\"a\": 1, \"b\": 2}", Map.of()));
      assertEquals(Map.of(1L, "one", 2L, "two"), cel.eval("{1: \"one\", 2: \"two\"}", Map.of()));
    }

    @Test
    void parsesFieldSelection() {
      assertEquals(42, cel.eval("obj.field", Map.of("obj", Map.of("field", 42))));
      assertEquals(
          "value",
          cel.eval("obj.nested.field", Map.of("obj", Map.of("nested", Map.of("field", "value")))));
    }

    @Test
    void parsesIndexing() {
      assertEquals(1, cel.eval("list[0]", Map.of("list", List.of(1, 2, 3))));
      assertEquals(2, cel.eval("list[1]", Map.of("list", List.of(1, 2, 3))));
      assertEquals("value", cel.eval("map[\"key\"]", Map.of("map", Map.of("key", "value"))));
      assertEquals("h", cel.eval("str[0]", Map.of("str", "hello")));
    }

    @Test
    void parsesFunctionCalls() {
      assertEquals(5, cel.eval("size(\"hello\")", Map.of()));
      assertEquals(3, cel.eval("size([1, 2, 3])", Map.of()));
      assertEquals(42L, cel.eval("int(\"42\")", Map.of()));
      assertEquals("42", cel.eval("string(42)", Map.of()));
    }

    @Test
    void parsesMethodCalls() {
      assertTrue((Boolean) cel.eval("\"hello\".contains(\"ll\")", Map.of()));
      assertTrue((Boolean) cel.eval("\"hello\".startsWith(\"he\")", Map.of()));
      assertTrue((Boolean) cel.eval("\"hello\".endsWith(\"lo\")", Map.of()));
      assertEquals("hello", cel.eval("\"HELLO\".toLowerCase()", Map.of()));
      assertEquals("HELLO", cel.eval("\"hello\".toUpperCase()", Map.of()));
    }

    @Test
    void parsesInOperator() {
      assertTrue((Boolean) cel.eval("2 in [1, 2, 3]", Map.of()));
      assertFalse((Boolean) cel.eval("4 in [1, 2, 3]", Map.of()));
      assertTrue((Boolean) cel.eval("\"key\" in {\"key\": \"value\"}", Map.of()));
      assertFalse((Boolean) cel.eval("\"missing\" in {\"key\": \"value\"}", Map.of()));
      assertTrue((Boolean) cel.eval("\"ll\" in \"hello\"", Map.of()));
    }

    @Test
    void parsesUnaryOperators() {
      assertEquals(-5L, cel.eval("-5", Map.of()));
      assertEquals(5L, cel.eval("--5", Map.of()));
      assertFalse((Boolean) cel.eval("!true", Map.of()));
      assertTrue((Boolean) cel.eval("!!true", Map.of()));
    }

    @Test
    void parsesComplexExpressions() {
      assertTrue((Boolean) cel.eval("x > 0 && x < 10", Map.of("x", 5)));
      assertFalse((Boolean) cel.eval("x > 0 && x < 10", Map.of("x", 15)));

      assertEquals(20L, cel.eval("(x + y) * z", Map.of("x", 2, "y", 3, "z", 4)));

      assertTrue((Boolean) cel.eval("[1, 2, 3].contains(2)", Map.of()));

      assertEquals(
          "adult",
          cel.eval("user.age >= 18 ? \"adult\" : \"minor\"", Map.of("user", Map.of("age", 21))));
    }
  }

  @Nested
  class Interpreting {
    private CEL cel;

    @BeforeEach
    void setup() {
      cel = new CEL();
    }

    @Test
    void evalStringConcatenation() {
      assertEquals("hello world", cel.eval("\"hello\" + \" \" + \"world\"", Map.of()));
      assertEquals("value: 42", cel.eval("\"value: \" + string(42)", Map.of()));
    }

    @Test
    void evalListConcatenation() {
      assertEquals(List.of(1L, 2L, 3L, 4L), cel.eval("[1, 2] + [3, 4]", Map.of()));
    }

    @Test
    void evalStringMultiplication() {
      assertEquals("ababab", cel.eval("\"ab\" * 3", Map.of()));
    }

    @Test
    void evalListMultiplication() {
      assertEquals(List.of(1L, 2L, 1L, 2L), cel.eval("[1, 2] * 2", Map.of()));
    }

    @Test
    void evalShortCircuitLogicalOperators() {
      final TestCounter counter = new TestCounter();
      final CustomFunctions functions =
          new CustomFunctions(Map.of("check", args -> counter.incr((Boolean) args.get(0))));
      final CEL customCel = new CEL(functions);

      counter.reset();
      assertFalse((Boolean) customCel.eval("false && check(true)", Map.of()));
      assertEquals(0, counter.count);

      counter.reset();
      assertTrue((Boolean) customCel.eval("true || check(false)", Map.of()));
      assertEquals(0, counter.count);
    }

    @Test
    void evalTypeConversionFunctions() {
      assertEquals(3L, cel.eval("int(3.14)", Map.of()));
      assertEquals(42.0, cel.eval("double(42)", Map.of()));
      assertEquals("true", cel.eval("string(true)", Map.of()));
      assertTrue((Boolean) cel.eval("bool(1)", Map.of()));
      assertFalse((Boolean) cel.eval("bool(0)", Map.of()));
    }

    @Test
    void evalHasFunction() {
      assertTrue((Boolean) cel.eval("has(obj, \"field\")", Map.of("obj", Map.of("field", 1))));
      assertFalse((Boolean) cel.eval("has(obj, \"missing\")", Map.of("obj", Map.of("field", 1))));
    }

    @Test
    void evalMatchesFunction() {
      assertTrue((Boolean) cel.eval("matches(\"hello\", \"h.*o\")", Map.of()));
      assertTrue((Boolean) cel.eval("matches(\"hello\", \"^h\")", Map.of()));
      assertFalse((Boolean) cel.eval("matches(\"hello\", \"^e\")", Map.of()));
    }

    @Test
    void evalMaxAndMinFunctions() {
      assertEquals(3L, cel.eval("max(1, 2, 3)", Map.of()));
      assertEquals(1L, cel.eval("min(1, 2, 3)", Map.of()));
      assertEquals("c", cel.eval("max(\"a\", \"b\", \"c\")", Map.of()));
      assertEquals("a", cel.eval("min(\"a\", \"b\", \"c\")", Map.of()));
    }

    @Test
    void evalStringMethods() {
      assertEquals("hello", cel.eval("\"  hello  \".trim()", Map.of()));
      assertEquals(
          "hello dart", cel.eval("\"hello world\".replace(\"world\", \"dart\")", Map.of()));
      assertEquals(List.of("a", "b", "c"), cel.eval("\"a,b,c\".split(\",\")", Map.of()));
    }

    @Test
    void evalTypeFunction() {
      assertEquals("null", cel.eval("type(null)", Map.of()));
      assertEquals("bool", cel.eval("type(true)", Map.of()));
      assertEquals("int", cel.eval("type(42)", Map.of()));
      assertEquals("double", cel.eval("type(3.14)", Map.of()));
      assertEquals("string", cel.eval("type(\"hello\")", Map.of()));
      assertEquals("list", cel.eval("type([1, 2])", Map.of()));
      assertEquals("map", cel.eval("type({\"a\": 1})", Map.of()));
    }

    @Test
    void handlesNullValuesCorrectly() {
      assertTrue((Boolean) cel.eval("null == null", Map.of()));
      assertTrue((Boolean) cel.eval("null != 1", Map.of()));
      assertTrue((Boolean) cel.eval("1 != null", Map.of()));
    }

    @Test
    void comparesListsCorrectly() {
      assertTrue((Boolean) cel.eval("[1, 2] == [1, 2]", Map.of()));
      assertTrue((Boolean) cel.eval("[1, 2] != [2, 1]", Map.of()));
      assertTrue((Boolean) cel.eval("[1, 2] < [1, 3]", Map.of()));
      assertTrue((Boolean) cel.eval("[1, 2, 3] > [1, 2]", Map.of()));
    }

    @Test
    void comparesMapsCorrectly() {
      assertTrue((Boolean) cel.eval("{\"a\": 1} == {\"a\": 1}", Map.of()));
      assertTrue((Boolean) cel.eval("{\"a\": 1} != {\"b\": 1}", Map.of()));
      assertTrue((Boolean) cel.eval("{\"a\": 1, \"b\": 2} == {\"b\": 2, \"a\": 1}", Map.of()));
    }

    @Test
    void throwsErrorsForUndefinedVariables() {
      assertThrows(EvaluationError.class, () -> cel.eval("x", Map.of()));
    }

    @Test
    void throwsErrorsForInvalidOperations() {
      assertThrows(EvaluationError.class, () -> cel.eval("1 / 0", Map.of()));
      assertThrows(EvaluationError.class, () -> cel.eval("1 % 0", Map.of()));
      assertThrows(EvaluationError.class, () -> cel.eval("\"hello\" - \"world\"", Map.of()));
    }

    @Test
    void throwsErrorsForInvalidIndexing() {
      assertThrows(EvaluationError.class, () -> cel.eval("[1, 2][5]", Map.of()));
      assertThrows(EvaluationError.class, () -> cel.eval("[1, 2][-1]", Map.of()));
      assertThrows(EvaluationError.class, () -> cel.eval("{\"a\": 1}[\"b\"]", Map.of()));
    }
  }

  @Nested
  class Programs {
    private CEL cel;

    @BeforeEach
    void setup() {
      cel = new CEL();
    }

    @Test
    void canBeCompiledAndReused() {
      final Program program = cel.compile("x * 2 + y");

      assertEquals(10L, program.evaluate(Map.of("x", 3, "y", 4)));
      assertEquals(11L, program.evaluate(Map.of("x", 5, "y", 1)));
      assertEquals(7L, program.evaluate(Map.of("x", 0, "y", 7)));
    }

    @Test
    void throwsParseErrorsForInvalidExpressions() {
      assertThrows(ParseError.class, () -> cel.compile("x +"));
      assertThrows(ParseError.class, () -> cel.compile("(1 + 2"));
      assertThrows(ParseError.class, () -> cel.compile("1 2 3"));
    }
  }

  @Nested
  class StaticCalls {
    @Test
    void compileWithCustomFunction() {
      final var custom =
          new CustomFunctions(
              Map.of(
                  "double",
                  args -> {
                    final Object v = args.get(0);
                    if (v instanceof Number n) {
                      // Keep integer math in longs as the interpreter uses longs for ints
                      if (v instanceof Double || v instanceof Float) {
                        return n.doubleValue() * 2.0;
                      }
                      return n.longValue() * 2L;
                    }
                    throw new EvaluationError("double() expects numeric argument");
                  }));

      final var program = CEL.compile("double(x) + y", custom);

      assertEquals(25L, program.evaluate(Map.of("x", 10L, "y", 5L)));
      assertEquals(9L, program.evaluate(Map.of("x", 2L, "y", 5L)));
    }

    @Test
    void evalWithCustomFunction() {
      final var custom =
          new CustomFunctions(
              Map.of(
                  "double",
                  args -> {
                    final Number n = (Number) args.get(0);
                    return n.longValue() * 2L;
                  }));

      assertEquals(14L, CEL.eval("double(x)", custom, Map.of("x", 7L)));
    }

    @Test
    void throwsWithInvalidExpression() {
      final var custom = new StandardFunctions();

      assertThrows(ParseError.class, () -> CEL.compile("x +", custom));
      assertThrows(ParseError.class, () -> CEL.eval("(1 +", custom, Map.of()));
    }

    @Test
    void callsCustomFunctions() {
      // Override built-in size() to return a sentinel value to ensure custom functions are used
      final var custom =
          new CustomFunctions(Map.of("size", args -> 999L, "echo", args -> args.get(0)));

      assertEquals(999L, CEL.eval("size([1, 2, 3])", custom, Map.of()));
      final var program = CEL.compile("echo(a)", custom);
      assertEquals("hello", program.evaluate(Map.of("a", "hello")));
    }
  }

  @Nested
  class MacroFunctions {
    private CEL cel;

    @BeforeEach
    void setup() {
      cel = new CEL();
    }

    @Nested
    class MapFunction {
      @Test
      void transformsListElements() {
        assertEquals(List.of(2L, 4L, 6L), cel.eval("[1, 2, 3].map(x, x * 2)", Map.of()));
        assertEquals(List.of(11L, 12L, 13L), cel.eval("[1, 2, 3].map(n, n + 10)", Map.of()));
        assertEquals(
            List.of("a!", "b!", "c!"),
            cel.eval("[\"a\", \"b\", \"c\"].map(s, s + \"!\")", Map.of()));
      }

      @Test
      void worksWithComplexExpressions() {
        assertEquals(List.of(1L, 4L, 9L), cel.eval("[1, 2, 3].map(x, x * x)", Map.of()));
        assertEquals(
            List.of(false, true, false, true),
            cel.eval("[1, 2, 3, 4].map(x, x % 2 == 0)", Map.of()));
        assertEquals(
            List.of(1L, 2L, 30L), cel.eval("[1, 2, 3].map(x, x > 2 ? x * 10 : x)", Map.of()));
      }

      @Test
      void worksWithObjectsInList() {
        final Map<String, Object> data =
            Map.of(
                "users",
                List.of(
                    Map.of("name", "Alice", "age", 30),
                    Map.of("name", "Bob", "age", 25),
                    Map.of("name", "Charlie", "age", 35)));
        assertEquals(List.of("Alice", "Bob", "Charlie"), cel.eval("users.map(u, u.name)", data));
        assertEquals(List.of(60L, 50L, 70L), cel.eval("users.map(u, u.age * 2)", data));
        assertEquals(List.of(false, false, true), cel.eval("users.map(u, u.age > 30)", data));
      }

      @Test
      void handlesEmptyList() {
        assertEquals(List.of(), cel.eval("[].map(x, x * 2)", Map.of()));
      }

      @Test
      void preservesVariableScope() {
        final Map<String, Object> data = new HashMap<>();
        data.put("x", 100);
        assertEquals(List.of(2L, 4L, 6L), cel.eval("[1, 2, 3].map(x, x * 2)", data));
        assertEquals(100, cel.eval("x", data)); // Original x unchanged
      }
    }

    @Nested
    class FilterFunction {
      @Test
      void filtersListElements() {
        assertEquals(List.of(3L, 4L, 5L), cel.eval("[1, 2, 3, 4, 5].filter(x, x > 2)", Map.of()));
        assertEquals(List.of(2L, 4L), cel.eval("[1, 2, 3, 4, 5].filter(x, x % 2 == 0)", Map.of()));
        assertEquals(
            List.of("ab", "abc"),
            cel.eval("[\"a\", \"ab\", \"abc\"].filter(s, size(s) > 1)", Map.of()));
      }

      @Test
      void worksWithComplexConditions() {
        assertEquals(
            List.of(3L, 4L), cel.eval("[1, 2, 3, 4, 5, 6].filter(x, x > 2 && x < 5)", Map.of()));
        assertEquals(
            List.of(1L, 5L), cel.eval("[1, 2, 3, 4, 5].filter(x, x == 1 || x == 5)", Map.of()));
      }

      @Test
      void worksWithObjects() {
        final Map<String, Object> data =
            Map.of(
                "users",
                List.of(
                    Map.of("name", "Alice", "age", 30, "active", true),
                    Map.of("name", "Bob", "age", 25, "active", false),
                    Map.of("name", "Charlie", "age", 35, "active", true)));
        assertEquals(
            List.of(
                Map.of("name", "Alice", "age", 30, "active", true),
                Map.of("name", "Charlie", "age", 35, "active", true)),
            cel.eval("users.filter(u, u.age > 28)", data));

        assertEquals(
            List.of(
                Map.of("name", "Alice", "age", 30, "active", true),
                Map.of("name", "Charlie", "age", 35, "active", true)),
            cel.eval("users.filter(u, u.active)", data));

        assertEquals(
            List.of(Map.of("name", "Alice", "age", 30, "active", true)),
            cel.eval("users.filter(u, u.active && u.age < 35)", data));
      }

      @Test
      void handlesEmptyList() {
        assertEquals(List.of(), cel.eval("[].filter(x, x > 0)", Map.of()));
      }

      @Test
      void returnsEmptyWhenNoMatches() {
        assertEquals(List.of(), cel.eval("[1, 2, 3].filter(x, x > 10)", Map.of()));
      }
    }

    @Nested
    class AllFunction {
      @Test
      void checksIfAllElementsMatchCondition() {
        assertTrue((Boolean) cel.eval("[2, 4, 6].all(x, x % 2 == 0)", Map.of()));
        assertTrue((Boolean) cel.eval("[1, 2, 3].all(x, x > 0)", Map.of()));
        assertFalse((Boolean) cel.eval("[1, 2, 3].all(x, x > 2)", Map.of()));
      }

      @Test
      void returnsTrueForEmptyList() {
        assertTrue((Boolean) cel.eval("[].all(x, x > 0)", Map.of()));
      }

      @Test
      void shortCircuitsOnFirstFalse() {
        // This is tested implicitly - if it didn't short-circuit,
        // it would evaluate all elements unnecessarily
        assertFalse((Boolean) cel.eval("[1, 2, 3, 4, 5].all(x, x < 3)", Map.of()));
      }

      @Test
      void worksWithComplexConditions() {
        final Map<String, Object> data =
            Map.of(
                "users",
                List.of(
                    Map.of("name", "Alice", "age", 30),
                    Map.of("name", "Bob", "age", 25),
                    Map.of("name", "Charlie", "age", 35)));
        assertTrue((Boolean) cel.eval("users.all(u, u.age >= 25)", data));
        assertFalse((Boolean) cel.eval("users.all(u, u.age > 30)", data));
        assertTrue((Boolean) cel.eval("users.all(u, has(u, \"name\"))", data));
      }
    }

    @Nested
    class ExistsFunction {
      @Test
      void checksIfAnyElementMatchesCondition() {
        assertTrue((Boolean) cel.eval("[1, 2, 3].exists(x, x > 2)", Map.of()));
        assertFalse((Boolean) cel.eval("[1, 2, 3].exists(x, x > 10)", Map.of()));
        assertFalse((Boolean) cel.eval("[1, 3, 5].exists(x, x % 2 == 0)", Map.of()));
      }

      @Test
      void returnsFalseForEmptyList() {
        assertFalse((Boolean) cel.eval("[].exists(x, x > 0)", Map.of()));
      }

      @Test
      void shortCircuitsOnFirstTrue() {
        // This is tested implicitly - if it didn't short-circuit,
        // it would evaluate all elements unnecessarily
        assertTrue((Boolean) cel.eval("[1, 2, 3, 4, 5].exists(x, x > 2)", Map.of()));
      }

      @Test
      void worksWithObjects() {
        final Map<String, Object> data =
            Map.of(
                "users",
                List.of(
                    Map.of("name", "Alice", "age", 30),
                    Map.of("name", "Bob", "age", 25),
                    Map.of("name", "Charlie", "age", 35)));
        assertTrue((Boolean) cel.eval("users.exists(u, u.age > 33)", data));
        assertFalse((Boolean) cel.eval("users.exists(u, u.age > 40)", data));
        assertTrue((Boolean) cel.eval("users.exists(u, u.name == \"Bob\")", data));
      }
    }

    @Nested
    class ExistsOneFunction {
      @Test
      void checksIfExactlyOneElementMatches() {
        assertTrue((Boolean) cel.eval("[1, 2, 3].existsOne(x, x == 2)", Map.of()));
        assertTrue((Boolean) cel.eval("[1, 2, 3].existsOne(x, x > 2)", Map.of()));
        assertFalse((Boolean) cel.eval("[1, 2, 3].existsOne(x, x > 1)", Map.of())); // 2 matches
        assertFalse((Boolean) cel.eval("[1, 2, 3].existsOne(x, x > 10)", Map.of())); // 0 matches
      }

      @Test
      void returnsFalseForEmptyList() {
        assertFalse((Boolean) cel.eval("[].existsOne(x, x > 0)", Map.of()));
      }

      @Test
      void worksWithComplexData() {
        final Map<String, Object> data =
            Map.of(
                "users",
                List.of(
                    Map.of("name", "Alice", "age", 30),
                    Map.of("name", "Bob", "age", 25),
                    Map.of("name", "Charlie", "age", 35)));
        assertTrue((Boolean) cel.eval("users.existsOne(u, u.age == 25)", data));
        assertTrue(
            (Boolean) cel.eval("users.existsOne(u, u.age > 30)", data)); // Only Charlie is 35 > 30
        assertTrue((Boolean) cel.eval("users.existsOne(u, u.age > 33)", data)); // Only Charlie
      }
    }

    @Nested
    class NestedMacroOperations {
      @Test
      void canChainMacroFunctions() {
        assertEquals(
            List.of(6L, 8L, 10L),
            cel.eval("[1, 2, 3, 4, 5].filter(x, x > 2).map(x, x * 2)", Map.of()));
        assertEquals(
            List.of(6L, 8L), cel.eval("[1, 2, 3, 4].map(x, x * 2).filter(x, x > 4)", Map.of()));
      }

      @Test
      void canUseMacrosInExpressions() {
        final Map<String, Object> data =
            Map.of("lists", List.of(List.of(1L, 2L), List.of(3L, 4L), List.of(5L)));
        assertEquals(List.of(2, 2, 1), cel.eval("lists.map(l, size(l))", data));

        @SuppressWarnings("unchecked")
        final List<List<Long>> result =
            (List<List<Long>>) cel.eval("lists.filter(l, l.exists(x, x > 3))", data);
        assertEquals(2, result.size());
        assertIterableEquals(List.of(3L, 4L), result.get(0));
        assertIterableEquals(List.of(5L), result.get(1));
      }

      @Test
      void canNestMacros() {
        final Map<String, Object> data =
            Map.of(
                "matrix", List.of(List.of(1L, 2L, 3L), List.of(4L, 5L, 6L), List.of(7L, 8L, 9L)));
        // Map each row to its sum > 10
        assertEquals(
            List.of(false, true, true), cel.eval("matrix.map(row, row.exists(x, x > 5))", data));

        // Filter rows where all elements > 3
        @SuppressWarnings("unchecked")
        final List<List<Long>> result =
            (List<List<Long>>) cel.eval("matrix.filter(row, row.all(x, x > 3))", data);
        assertEquals(2, result.size());
        assertIterableEquals(List.of(4L, 5L, 6L), result.get(0));
        assertIterableEquals(List.of(7L, 8L, 9L), result.get(1));
      }
    }

    @Nested
    class ErrorHandling {
      @Test
      void throwsErrorForNonListTargets() {
        assertThrows(EvaluationError.class, () -> cel.eval("\"string\".map(x, x)", Map.of()));
        assertThrows(EvaluationError.class, () -> cel.eval("x.filter(y, y > 0)", Map.of("x", 123)));
      }

      @Test
      void throwsErrorForMissingArguments() {
        assertThrows(EvaluationError.class, () -> cel.eval("[1, 2, 3].map()", Map.of()));
      }

      @Test
      void throwsErrorForInvalidVariableName() {
        assertThrows(EvaluationError.class, () -> cel.eval("[1, 2, 3].map(123, x * 2)", Map.of()));
      }
    }
  }
}

/** Helper class for testing short-circuit evaluation. */
class TestCounter {
  int count = 0;

  boolean incr(final boolean value) {
    count++;
    return value;
  }

  void reset() {
    count = 0;
  }
}

/** Custom functions implementation for testing. */
class CustomFunctions extends StandardFunctions {
  private final Map<String, java.util.function.Function<List<Object>, Object>> local;

  CustomFunctions(final Map<String, java.util.function.Function<List<Object>, Object>> local) {
    this.local = local;
  }

  @Override
  public Object callFunction(final String name, final List<Object> args) {
    if (local.containsKey(name)) {
      return local.get(name).apply(args);
    }
    return super.callFunction(name, args);
  }
}

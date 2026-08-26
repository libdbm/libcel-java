package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import com.libdbm.cel.parser.Parser;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for rendering an expression tree back into CEL source text. */
class PrinterTests {

  private static String print(final String source) {
    return Printer.print(new Parser(source).parse());
  }

  private static void round(final String source) {
    final var once = print(source);
    final var twice = print(once);
    assertEquals(once, twice, "printing is not idempotent for: " + source);
  }

  @Test
  void testPrintsLiterals() {
    assertEquals("null", print("null"));
    assertEquals("true", print("true"));
    assertEquals("42", print("42"));
    assertEquals("255", print("0xFF"));
    assertEquals("42u", print("42u"));
    assertEquals("1.0", print("1.0"));
    assertEquals("1.0E10", print("1e10"));
    assertEquals("3.14", print("3.14"));
    assertEquals("\"hello\"", print("'hello'"));
    assertEquals("b\"data\"", print("b\"data\""));
  }

  @Test
  void testEscapesStringLiterals() {
    assertEquals("\"a\\nb\"", print("\"a\\nb\""));
    assertEquals("\"say \\\"hi\\\"\"", print("'say \"hi\"'"));
    assertEquals("\"tab\\there\"", print("\"tab\\there\""));
    assertEquals("\"café\"", print("\"café\""));
    // A supplementary character survives escaping as a whole character
    assertEquals("\"\uD83D\uDE00\"", print("'\\U0001F600'"));
    round("'\\U0001F600'");
    round("\"a\\nb\\\\c\\\"d\"");
  }

  @Test
  void testEscapesBytesLiterals() {
    assertEquals("b\"\\x00\\xff\"", print("b\"\\x00\\xff\""));
    round("b\"\\x00\\xff\"");
  }

  @Test
  void testOmitsRedundantParentheses() {
    assertEquals("a + b * c", print("a + b * c"));
    assertEquals("a + b * c", print("a + (b * c)"));
    assertEquals("a + b + c", print("(a + b) + c"));
    assertEquals("a * b + c", print("(a * b) + c"));
  }

  @Test
  void testKeepsRequiredParentheses() {
    assertEquals("(a + b) * c", print("(a + b) * c"));
    assertEquals("a - (b - c)", print("a - (b - c)"));
    assertEquals("a / (b / c)", print("a / (b / c)"));
    assertEquals("!(a && b)", print("!(a && b)"));
    assertEquals("(a || b) && c", print("(a || b) && c"));
    assertEquals("(a + b).size()", print("(a + b).size()"));
    assertEquals("(-a).size()", print("(-a).size()"));
  }

  @Test
  void testPrintsUnaryOperators() {
    assertEquals("!a", print("!a"));
    assertEquals("-a", print("-a"));
    assertEquals("--x", print("-(-x)"));
    assertEquals("!!x", print("!!x"));
    round("-(-x)");
  }

  @Test
  void testPrintsConditionals() {
    assertEquals("a ? b : c", print("a ? b : c"));
    assertEquals("a ? b : c ? d : e", print("a ? b : c ? d : e"));
    assertEquals("(a ? b : c) ? d : e", print("(a ? b : c) ? d : e"));
    assertEquals("a ? (b ? c : d) : e", print("a ? (b ? c : d) : e"));
  }

  @Test
  void testPrintsCollectionsAndMembers() {
    assertEquals("[1, 2, 3]", print("[1,2,3]"));
    assertEquals("[]", print("[]"));
    assertEquals("{\"a\": 1, \"b\": 2}", print("{'a': 1, 'b': 2}"));
    assertEquals("a.b.c", print("a.b.c"));
    assertEquals("a[0][1]", print("a[0][1]"));
    assertEquals("size(a)", print("size(a)"));
    assertEquals("a.size()", print("a.size()"));
    assertEquals("has(a.b)", print("has(a.b)"));
    assertEquals("Point{x: 1, y: 2}", print("Point{x: 1, y: 2}"));
  }

  @Test
  void testPrintsMacros() {
    assertEquals("[1, 2].map(x, x * 2)", print("[1,2].map(x, x*2)"));
    assertEquals("a.filter(x, x > 1).all(y, y < 9)", print("a.filter(x, x>1).all(y, y<9)"));
  }

  @Test
  void testRoundTripsParsedExpressions() {
    final var sources =
        List.of(
            "1 + 2 * 3 - 4 / 5 % 6",
            "a == b != c",
            "a < b && c >= d || !e",
            "'x' in ['x', 'y']",
            "{'k': [1, {'n': null}]}",
            "user.roles.filter(r, r != 'guest').size() > 0",
            "a.b[0].c(1, 2, 3)",
            "timestamp('2024-01-01T00:00:00Z') + duration('1h30m')");

    for (final var source : sources) {
      round(source);
      assertDoesNotThrow(() -> new Parser(print(source)).parse(), source);
    }
  }

  @Test
  void testPrettyKeepsShortExpressionsOnOneLine() {
    final var expr = new Parser("[1, 2, 3]").parse();
    assertEquals("[1, 2, 3]", Printer.print(expr, Printer.Options.pretty()));
  }

  @Test
  void testPrettyBreaksLongCollections() {
    final var source = "['aaaaaaaaaa', 'bbbbbbbbbb', 'cccccccccc', 'dddddddddd', 'eeeeeeeeee']";
    final var expr = new Parser(source).parse();
    final var text = Printer.print(expr, new Printer.Options(true, 2, 40));

    assertEquals(
        """
        [
          "aaaaaaaaaa",
          "bbbbbbbbbb",
          "cccccccccc",
          "dddddddddd",
          "eeeeeeeeee"
        ]""",
        text);
    assertEquals(print(source), print(text));
  }

  @Test
  void testPrettyBreaksNestedStructures() {
    final var source = "{'alpha': [1, 2, 3], 'beta': {'gamma': 'a long enough string value'}}";
    final var text = Printer.print(new Parser(source).parse(), new Printer.Options(true, 2, 40));

    assertEquals(
        """
        {
          "alpha": [1, 2, 3],
          "beta": {
            "gamma": "a long enough string value"
          }
        }""",
        text);
    assertEquals(print(source), print(text));
  }
}

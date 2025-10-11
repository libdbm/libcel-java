package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.libdbm.cel.ast.Expression;
import com.libdbm.cel.parser.Parser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InterpreterTests {

  @Test
  void testLiterals() {
    final Interpreter interp = new Interpreter();

    assertEquals(42L, eval("42"));
    assertEquals(3.14, eval("3.14"));
    assertEquals("hello", eval("\"hello\""));
    assertEquals(true, eval("true"));
    assertEquals(false, eval("false"));
    assertNull(eval("null"));
  }

  @Test
  void testArithmetic() {
    assertEquals(7L, eval("3 + 4"));
    assertEquals(12L, eval("3 * 4"));
    assertEquals(1L, eval("5 - 4"));
    assertEquals(2.5, eval("5 / 2"));
    assertEquals(1L, eval("5 % 2"));
  }

  @Test
  void testStringOperations() {
    assertEquals("hello world", eval("\"hello\" + \" world\""));
    assertEquals("aaa", eval("\"a\" * 3"));
  }

  @Test
  void testListOperations() {
    final Object result = eval("[1, 2] + [3, 4]");
    assertEquals(List.of(1L, 2L, 3L, 4L), result);

    final Object repeated = eval("[1, 2] * 3");
    assertEquals(List.of(1L, 2L, 1L, 2L, 1L, 2L), repeated);
  }

  @Test
  void testComparisons() {
    assertEquals(true, eval("5 > 3"));
    assertEquals(false, eval("5 < 3"));
    assertEquals(true, eval("5 >= 5"));
    assertEquals(true, eval("5 <= 5"));
    assertEquals(true, eval("5 == 5"));
    assertEquals(true, eval("5 != 3"));
  }

  @Test
  void testLogicalOperators() {
    assertEquals(true, eval("true && true"));
    assertEquals(false, eval("true && false"));
    assertEquals(true, eval("true || false"));
    assertEquals(false, eval("false || false"));
    assertEquals(false, eval("!true"));
    assertEquals(true, eval("!false"));
  }

  @Test
  void testShortCircuit() {
    // Should not throw because of short-circuit
    final Map<String, Object> vars = new HashMap<>();
    vars.put("x", 0L);

    final Interpreter interp = new Interpreter(vars, null);
    final Expression expr = new Parser("x == 0 || x / 0 > 1").parse();
    assertEquals(true, interp.evaluate(expr));
  }

  @Test
  void testVariables() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("name", "Alice");
    vars.put("age", 30L);

    final Interpreter interp = new Interpreter(vars, null);

    assertEquals("Alice", interp.evaluate(new Parser("name").parse()));
    assertEquals(30L, interp.evaluate(new Parser("age").parse()));
  }

  @Test
  void testMapAccess() {
    final Map<String, Object> person = new HashMap<>();
    person.put("name", "Bob");
    person.put("age", 25L);

    final Map<String, Object> vars = new HashMap<>();
    vars.put("person", person);

    final Interpreter interp = new Interpreter(vars, null);

    assertEquals("Bob", interp.evaluate(new Parser("person.name").parse()));
    assertEquals(25L, interp.evaluate(new Parser("person.age").parse()));
    assertEquals("Bob", interp.evaluate(new Parser("person[\"name\"]").parse()));
  }

  @Test
  void testListAccess() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("items", List.of(10L, 20L, 30L));

    final Interpreter interp = new Interpreter(vars, null);

    assertEquals(10L, interp.evaluate(new Parser("items[0]").parse()));
    assertEquals(20L, interp.evaluate(new Parser("items[1]").parse()));
    assertEquals(30L, interp.evaluate(new Parser("items[2]").parse()));
  }

  @Test
  void testTernaryOperator() {
    assertEquals(10L, eval("true ? 10 : 20"));
    assertEquals(20L, eval("false ? 10 : 20"));
  }

  @Test
  void testInOperator() {
    assertEquals(true, eval("2 in [1, 2, 3]"));
    assertEquals(false, eval("5 in [1, 2, 3]"));
    assertEquals(true, eval("\"ell\" in \"hello\""));
    assertEquals(false, eval("\"xyz\" in \"hello\""));
  }

  @Test
  void testMapMacro() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("nums", List.of(1L, 2L, 3L));

    final Interpreter interp = new Interpreter(vars, null);
    final Object result = interp.evaluate(new Parser("nums.map(x, x * 2)").parse());

    assertEquals(List.of(2L, 4L, 6L), result);
  }

  @Test
  void testFilterMacro() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("nums", List.of(1L, 2L, 3L, 4L, 5L));

    final Interpreter interp = new Interpreter(vars, null);
    final Object result = interp.evaluate(new Parser("nums.filter(x, x > 3)").parse());

    assertEquals(List.of(4L, 5L), result);
  }

  @Test
  void testAllMacro() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("nums", List.of(2L, 4L, 6L));

    final Interpreter interp = new Interpreter(vars, null);
    assertEquals(true, interp.evaluate(new Parser("nums.all(x, x % 2 == 0)").parse()));

    vars.put("nums", List.of(2L, 3L, 6L));
    assertEquals(false, interp.evaluate(new Parser("nums.all(x, x % 2 == 0)").parse()));
  }

  @Test
  void testExistsMacro() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("nums", List.of(1L, 2L, 3L));

    final Interpreter interp = new Interpreter(vars, null);
    assertEquals(true, interp.evaluate(new Parser("nums.exists(x, x > 2)").parse()));
    assertEquals(false, interp.evaluate(new Parser("nums.exists(x, x > 10)").parse()));
  }

  @Test
  void testExistsOneMacro() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("nums", List.of(1L, 2L, 3L));

    final Interpreter interp = new Interpreter(vars, null);
    assertEquals(true, interp.evaluate(new Parser("nums.existsOne(x, x == 2)").parse()));
    assertEquals(false, interp.evaluate(new Parser("nums.existsOne(x, x > 1)").parse()));
  }

  @Test
  void testDeepEquality() {
    assertEquals(true, eval("[1, 2, 3] == [1, 2, 3]"));
    assertEquals(false, eval("[1, 2, 3] == [1, 2, 4]"));
    assertEquals(false, eval("[1, 2] == [1, 2, 3]"));
  }

  @Test
  void testListComparison() {
    assertEquals(true, eval("[1, 2] < [1, 3]"));
    assertEquals(true, eval("[1, 2] < [1, 2, 3]"));
    assertEquals(false, eval("[1, 3] < [1, 2]"));
    assertEquals(true, eval("[1, 2] == [1, 2]"));
  }

  @Test
  void testFunctions() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("text", "hello");
    vars.put("items", List.of(1L, 2L, 3L));

    final Interpreter interp = new Interpreter(vars, null);

    assertEquals(5, interp.evaluate(new Parser("size(text)").parse()));
    assertEquals(3, interp.evaluate(new Parser("size(items)").parse()));
    assertEquals(true, interp.evaluate(new Parser("text.contains(\"ell\")").parse()));
    assertEquals(true, interp.evaluate(new Parser("text.startsWith(\"hel\")").parse()));
    assertEquals(true, interp.evaluate(new Parser("text.endsWith(\"lo\")").parse()));
  }

  @Test
  void testMacroVariableScoping() {
    final Map<String, Object> vars = new HashMap<>();
    vars.put("x", 100L);
    vars.put("nums", List.of(1L, 2L, 3L));

    final Interpreter interp = new Interpreter(vars, null);

    // Use x in macro, should shadow outer x
    interp.evaluate(new Parser("nums.map(x, x * 2)").parse());

    // x should be restored to original value
    assertEquals(100L, interp.evaluate(new Parser("x").parse()));
  }

  // Helper method to evaluate simple expressions
  private Object eval(final String expr) {
    final Interpreter interp = new Interpreter();
    final Parser parser = new Parser(expr);
    final Expression ast = parser.parse();
    return interp.evaluate(ast);
  }
}

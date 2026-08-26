package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for the math functions. */
class MathFunctionTests {
  private CEL cel;

  @BeforeEach
  void setup() {
    cel = new CEL();
  }

  private Object eval(final String source) {
    return cel.eval(source, Map.of());
  }

  @Test
  void testGreatestAndLeast() {
    assertEquals(3L, eval("math.greatest(1, 3, 2)"));
    assertEquals(1L, eval("math.least(1, 3, 2)"));
    assertEquals(3L, eval("math.greatest([1, 3, 2])"));
    assertEquals(2.5, eval("math.greatest(1, 2.5)"));
  }

  @Test
  void testAbsPreservesType() {
    assertEquals(5L, eval("math.abs(-5)"));
    assertEquals(5.5, eval("math.abs(-5.5)"));
  }

  @Test
  void testRounding() {
    assertEquals(2.0, eval("math.ceil(1.2)"));
    assertEquals(1.0, eval("math.floor(1.8)"));
    assertEquals(2.0, eval("math.round(1.5)"));
    assertEquals(-2.0, eval("math.round(-1.5)"));
    assertEquals(1.0, eval("math.trunc(1.9)"));
    assertEquals(-1.0, eval("math.trunc(-1.9)"));
  }

  @Test
  void testSignAndSqrt() {
    assertEquals(-1L, eval("math.sign(-4)"));
    assertEquals(0L, eval("math.sign(0)"));
    assertEquals(1.0, eval("math.sign(4.2)"));
    assertEquals(3.0, eval("math.sqrt(9)"));
  }

  @Test
  void testFloatingPointChecks() {
    assertEquals(false, eval("math.isNaN(1.0)"));
    assertEquals(true, eval("math.isFinite(1.0)"));
    assertEquals(false, eval("math.isInf(1.0)"));
  }

  @Test
  void testBitOperations() {
    assertEquals(8L, eval("math.bitAnd(12, 10)"));
    assertEquals(14L, eval("math.bitOr(12, 10)"));
    assertEquals(6L, eval("math.bitXor(12, 10)"));
    assertEquals(-1L, eval("math.bitNot(0)"));
    assertEquals(4L, eval("math.bitShiftLeft(1, 2)"));
    assertEquals(1L, eval("math.bitShiftRight(4, 2)"));
    assertThrows(IllegalArgumentException.class, () -> eval("math.bitShiftLeft(1, -1)"));
  }

  @Test
  void testArgumentChecking() {
    assertThrows(IllegalArgumentException.class, () -> eval("math.abs(1, 2)"));
    // An unknown name under a namespace is reported as an undefined variable
    assertThrows(EvaluationError.class, () -> eval("math.missing(1)"));
  }
}

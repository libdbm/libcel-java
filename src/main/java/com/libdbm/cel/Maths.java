package com.libdbm.cel;

import java.util.List;

/**
 * Numeric functions from the CEL math extension library.
 *
 * <p>Functions that take a single argument accept both integers and doubles and preserve the input
 * type where the operation allows it. Methods are public and static so library users can call them
 * directly.
 */
public final class Maths {
  private Maths() {}

  /**
   * Returns the largest of the given values.
   *
   * <p>A single list argument is treated as the list of values to compare.
   *
   * @param values the values to compare
   * @return the largest value
   * @throws IllegalArgumentException if no values are given or they are not comparable
   */
  public static Object greatest(final List<Object> values) {
    return Utilities.max(spread(values));
  }

  /**
   * Returns the smallest of the given values.
   *
   * <p>A single list argument is treated as the list of values to compare.
   *
   * @param values the values to compare
   * @return the smallest value
   * @throws IllegalArgumentException if no values are given or they are not comparable
   */
  public static Object least(final List<Object> values) {
    return Utilities.min(spread(values));
  }

  @SuppressWarnings("unchecked")
  private static List<Object> spread(final List<Object> values) {
    if (values.size() == 1 && values.get(0) instanceof List<?> nested) {
      return (List<Object>) nested;
    }
    return values;
  }

  /**
   * Returns the absolute value, preserving the numeric type.
   *
   * @param value an integer or double value
   * @return the absolute value
   * @throws IllegalArgumentException if the value is not numeric
   * @throws EvaluationError if the value is the minimum integer, whose magnitude is not
   *     representable
   */
  public static Object abs(final Object value) {
    if (isDecimal(value)) {
      return Math.abs(Utilities.asDouble(value));
    }
    final var number = Utilities.asInt(value);
    if (number == Long.MIN_VALUE) {
      throw new EvaluationError("Integer overflow");
    }
    return Math.abs(number);
  }

  /**
   * Rounds the value up to the nearest whole number.
   *
   * @param value an integer or double value
   * @return the rounded value as a double
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static double ceil(final Object value) {
    return Math.ceil(Utilities.asDouble(value));
  }

  /**
   * Rounds the value down to the nearest whole number.
   *
   * @param value an integer or double value
   * @return the rounded value as a double
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static double floor(final Object value) {
    return Math.floor(Utilities.asDouble(value));
  }

  /**
   * Rounds the value to the nearest whole number, with halves rounding away from zero.
   *
   * @param value an integer or double value
   * @return the rounded value as a double
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static double round(final Object value) {
    final var number = Utilities.asDouble(value);
    return number < 0 ? -Math.round(-number) : Math.round(number);
  }

  /**
   * Truncates the value towards zero.
   *
   * @param value an integer or double value
   * @return the truncated value as a double
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static double trunc(final Object value) {
    final var number = Utilities.asDouble(value);
    return number < 0 ? Math.ceil(number) : Math.floor(number);
  }

  /**
   * Returns -1, 0 or 1 according to the sign of the value, preserving the numeric type.
   *
   * @param value an integer or double value
   * @return the sign of the value
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static Object sign(final Object value) {
    if (isDecimal(value)) {
      return Math.signum(Utilities.asDouble(value));
    }
    return (long) Long.signum(Utilities.asInt(value));
  }

  /**
   * Returns the square root of the value.
   *
   * @param value an integer or double value
   * @return the square root as a double
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static double sqrt(final Object value) {
    return Math.sqrt(Utilities.asDouble(value));
  }

  /**
   * Reports whether the value is not a number.
   *
   * @param value an integer or double value
   * @return true if the value is NaN
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static boolean isNaN(final Object value) {
    return Double.isNaN(Utilities.asDouble(value));
  }

  /**
   * Reports whether the value is positive or negative infinity.
   *
   * @param value an integer or double value
   * @return true if the value is infinite
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static boolean isInf(final Object value) {
    return Double.isInfinite(Utilities.asDouble(value));
  }

  /**
   * Reports whether the value is finite.
   *
   * @param value an integer or double value
   * @return true if the value is neither NaN nor infinite
   * @throws IllegalArgumentException if the value is not numeric
   */
  public static boolean isFinite(final Object value) {
    return Double.isFinite(Utilities.asDouble(value));
  }

  /**
   * Returns the bitwise AND of two integers.
   *
   * @param left the first operand
   * @param right the second operand
   * @return the result of the operation
   * @throws IllegalArgumentException if either operand is not an integer
   */
  public static long and(final Object left, final Object right) {
    return Utilities.asInt(left) & Utilities.asInt(right);
  }

  /**
   * Returns the bitwise OR of two integers.
   *
   * @param left the first operand
   * @param right the second operand
   * @return the result of the operation
   * @throws IllegalArgumentException if either operand is not an integer
   */
  public static long or(final Object left, final Object right) {
    return Utilities.asInt(left) | Utilities.asInt(right);
  }

  /**
   * Returns the bitwise exclusive OR of two integers.
   *
   * @param left the first operand
   * @param right the second operand
   * @return the result of the operation
   * @throws IllegalArgumentException if either operand is not an integer
   */
  public static long xor(final Object left, final Object right) {
    return Utilities.asInt(left) ^ Utilities.asInt(right);
  }

  /**
   * Returns the bitwise complement of an integer.
   *
   * @param value the operand
   * @return the result of the operation
   * @throws IllegalArgumentException if the operand is not an integer
   */
  public static long not(final Object value) {
    return ~Utilities.asInt(value);
  }

  /**
   * Shifts an integer left by the given number of bits.
   *
   * @param value the operand
   * @param count the number of bits to shift; must not be negative
   * @return the shifted value
   * @throws IllegalArgumentException if the count is negative
   */
  public static long left(final Object value, final Object count) {
    final var bits = Utilities.asInt(count);
    if (bits < 0) {
      throw new IllegalArgumentException("Shift count must not be negative: " + bits);
    }
    return bits >= 64 ? 0L : Utilities.asInt(value) << bits;
  }

  /**
   * Shifts an integer right by the given number of bits, without sign extension.
   *
   * @param value the operand
   * @param count the number of bits to shift; must not be negative
   * @return the shifted value
   * @throws IllegalArgumentException if the count is negative
   */
  public static long right(final Object value, final Object count) {
    final var bits = Utilities.asInt(count);
    if (bits < 0) {
      throw new IllegalArgumentException("Shift count must not be negative: " + bits);
    }
    return bits >= 64 ? 0L : Utilities.asInt(value) >>> bits;
  }

  private static boolean isDecimal(final Object value) {
    return value instanceof Double || value instanceof Float;
  }
}

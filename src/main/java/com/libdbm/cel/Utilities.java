package com.libdbm.cel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility helper methods for libcel.
 *
 * <p>Methods are public and static so library users can call them directly.
 */
public final class Utilities {
  private Utilities() {}

  /**
   * Returns the size/length of the given value.
   *
   * <p>Supported types: - String: number of characters - List: number of elements - Map: number of
   * entries - null: 0
   *
   * @param value the value whose size should be computed; may be null
   * @return the size for supported types, or 0 for null
   * @throws IllegalArgumentException if the value type is unsupported
   */
  public static int sizeOf(final Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof String str) {
      return str.length();
    }
    if (value instanceof List<?> list) {
      return list.size();
    }
    if (value instanceof Map<?, ?> map) {
      return map.size();
    }
    throw new IllegalArgumentException(
        "size() not supported for type: " + value.getClass().getName());
  }

  /**
   * Converts the given value to a signed 64-bit integer (long).
   *
   * <p>Accepted inputs: Long, Integer, Double, Float, String (parsed as long), and Boolean (true=1,
   * false=0).
   *
   * @param value the value to convert
   * @return the converted long value
   * @throws IllegalArgumentException if the value cannot be converted
   */
  public static long asInt(final Object value) {
    if (value instanceof Long l) {
      return l;
    }
    if (value instanceof Integer i) {
      return i.longValue();
    }
    if (value instanceof Double d) {
      return d.longValue();
    }
    if (value instanceof Float f) {
      return f.longValue();
    }
    if (value instanceof String str) {
      return Long.parseLong(str);
    }
    if (value instanceof Boolean b) {
      return b ? 1L : 0L;
    }
    throw new IllegalArgumentException("Cannot convert to int: " + value);
  }

  /**
   * Converts the given value to an unsigned 64-bit integer represented as a long.
   *
   * <p>Negative inputs are not allowed and will result in an exception.
   *
   * @param value the value to convert
   * @return the non-negative long value
   * @throws IllegalArgumentException if the value is negative or cannot be converted
   */
  public static long asUInt(final Object value) {
    final long result = asInt(value);
    if (result < 0) {
      throw new IllegalArgumentException("Cannot convert negative value to uint: " + value);
    }
    return result;
  }

  /**
   * Converts the given value to a double.
   *
   * <p>Accepted inputs: Double, Float, Long, Integer, and String parsable as double.
   *
   * @param value the value to convert
   * @return the converted double value
   * @throws IllegalArgumentException if the value cannot be converted
   */
  public static double asDouble(final Object value) {
    if (value instanceof Double d) {
      return d;
    }
    if (value instanceof Float f) {
      return f.doubleValue();
    }
    if (value instanceof Long l) {
      return l.doubleValue();
    }
    if (value instanceof Integer i) {
      return i.doubleValue();
    }
    if (value instanceof String str) {
      return Double.parseDouble(str);
    }
    throw new IllegalArgumentException("Cannot convert to double: " + value);
  }

  /**
   * Converts the given value to its String representation.
   *
   * <p>Returns the literal string "null" for null values.
   *
   * @param value the value to stringify
   * @return the String representation
   */
  public static String asString(final Object value) {
    if (value == null) {
      return "null";
    }
    return value.toString();
  }

  /**
   * Converts the given value to a boolean using common truthiness rules.
   *
   * <p>Numbers are true if non-zero. Strings are true if non-empty. Collections/maps are true if
   * non-empty. Null is false.
   *
   * @param value the value to interpret
   * @return the boolean interpretation
   */
  public static boolean asBool(final Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof Long l) {
      return l != 0L;
    }
    if (value instanceof Integer i) {
      return i != 0;
    }
    if (value instanceof Double d) {
      return d != 0.0;
    }
    if (value instanceof Float f) {
      return f != 0.0f;
    }
    if (value instanceof String str) {
      return !str.isEmpty();
    }
    if (value instanceof List<?> list) {
      return !list.isEmpty();
    }
    if (value instanceof Map<?, ?> map) {
      return !map.isEmpty();
    }
    return value != null;
  }

  /**
   * Returns a simple type name for the given value.
   *
   * <p>Possible results: "null", "bool", "int", "double", "string", "list", "map", or "unknown".
   *
   * @param value the value whose type is to be described
   * @return the simple type name
   */
  public static String typeOf(final Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Boolean) {
      return "bool";
    }
    if (value instanceof Long || value instanceof Integer) {
      return "int";
    }
    if (value instanceof Double || value instanceof Float) {
      return "double";
    }
    if (value instanceof String) {
      return "string";
    }
    if (value instanceof List) {
      return "list";
    }
    if (value instanceof Map) {
      return "map";
    }
    return "unknown";
  }

  /**
   * Checks whether a map contains the given field name.
   *
   * @param target the map-like object to check (must be a Map to return true/false)
   * @param field the field/key to look for (must be a String)
   * @return true if target is a Map and contains the given key; false otherwise
   */
  public static boolean has(final Object target, final Object field) {
    if (target instanceof Map<?, ?> map && field instanceof String key) {
      return map.containsKey(key);
    }
    return false;
  }

  /**
   * Tests whether the given regular expression matches any part of the text.
   *
   * <p>Uses Pattern.compile(pattern).matcher(text).find().
   *
   * @param text the input text
   * @param pattern the regular expression pattern
   * @return true if the pattern matches anywhere in the text; false otherwise
   * @throws java.util.regex.PatternSyntaxException if the pattern is invalid
   */
  public static boolean matches(final String text, final String pattern) {
    final var regex = Pattern.compile(pattern);
    return regex.matcher(text).find();
  }

  /**
   * Parses or provides an Instant from the given value.
   *
   * <p>Accepted inputs: - null: returns Instant.now() - String: parsed via Instant.parse(ISO-8601)
   * - Long/Integer: treated as epoch milliseconds
   *
   * @param value the value to convert to an Instant
   * @return the resolved Instant
   * @throws IllegalArgumentException if the input type or format is invalid
   */
  public static Instant timestamp(final Object value) {
    if (value == null) {
      return Instant.now();
    }
    if (value instanceof String str) {
      return Instant.parse(str);
    }
    if (value instanceof Long millis) {
      return Instant.ofEpochMilli(millis);
    }
    if (value instanceof Integer millis) {
      return Instant.ofEpochMilli(millis.longValue());
    }
    throw new IllegalArgumentException("Invalid timestamp value: " + value);
  }

  /**
   * Parses a short duration string into a Duration.
   *
   * <p>Format: "&lt;number>&lt;unit>", where unit is one of: h (hours), m (minutes), s (seconds).
   * Examples: "5s", "10m", "2h".
   *
   * @param value the duration literal
   * @return the corresponding Duration
   * @throws IllegalArgumentException if the format or unit is invalid
   */
  public static Duration duration(final String value) {
    final var regex = Pattern.compile("^(\\d+)([hms])$");
    final var matcher = regex.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid duration format: " + value);
    }

    final int amount = Integer.parseInt(matcher.group(1));
    final String unit = matcher.group(2);

    return switch (unit) {
      case "h" -> Duration.ofHours(amount);
      case "m" -> Duration.ofMinutes(amount);
      case "s" -> Duration.ofSeconds(amount);
      default -> throw new IllegalArgumentException("Invalid duration unit: " + unit);
    };
  }

  /**
   * Returns the day of month for the given timestamp-like value.
   *
   * <p>Accepts an Instant or any value accepted by timestamp(Object). Uses the system default time
   * zone.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the day of month (1-31)
   */
  public static int dateOf(final Object value) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    final var date = instant.atZone(java.time.ZoneId.systemDefault());
    return date.getDayOfMonth();
  }

  /**
   * Returns the zero-based month for the given timestamp-like value.
   *
   * <p>January is 0, December is 11. Accepts an Instant or any value accepted by timestamp(Object).
   * Uses the system default time zone.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the month value in range 0-11
   */
  public static int monthOf(final Object value) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    final var date = instant.atZone(java.time.ZoneId.systemDefault());
    return date.getMonthValue() - 1;
  }

  /**
   * Returns the year for the given timestamp-like value using the system default time zone.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the four-digit year
   */
  public static int yearOf(final Object value) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    final var date = instant.atZone(java.time.ZoneId.systemDefault());
    return date.getYear();
  }

  /**
   * Returns the hour of day (0-23) for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the hour of day in 24-hour clock
   */
  public static int hoursOf(final Object value) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    final var date = instant.atZone(java.time.ZoneId.systemDefault());
    return date.getHour();
  }

  /**
   * Returns the minute of hour (0-59) for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the minute value
   */
  public static int minutesOf(final Object value) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    final var date = instant.atZone(java.time.ZoneId.systemDefault());
    return date.getMinute();
  }

  /**
   * Returns the second of minute (0-59) for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the second value
   */
  public static int secondsOf(final Object value) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    final var date = instant.atZone(java.time.ZoneId.systemDefault());
    return date.getSecond();
  }

  /**
   * Returns the maximum element from a non-empty list of values.
   *
   * <p>Comparison rules follow compare(Object, Object). All elements must be mutually comparable
   * according to those rules.
   *
   * @param values a non-empty list of values
   * @return the maximum value in the list
   * @throws IllegalArgumentException if the list is empty or values are not comparable
   */
  public static Object max(final List<Object> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("max() requires at least one argument");
    }

    var result = values.get(0);
    for (int i = 1; i < values.size(); i++) {
      if (compare(values.get(i), result) > 0) {
        result = values.get(i);
      }
    }
    return result;
  }

  /**
   * Returns the minimum element from a non-empty list of values.
   *
   * <p>Comparison rules follow compare(Object, Object). All elements must be mutually comparable
   * according to those rules.
   *
   * @param values a non-empty list of values
   * @return the minimum value in the list
   * @throws IllegalArgumentException if the list is empty or values are not comparable
   */
  public static Object min(final List<Object> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("min() requires at least one argument");
    }

    var result = values.get(0);
    for (int i = 1; i < values.size(); i++) {
      if (compare(values.get(i), result) < 0) {
        result = values.get(i);
      }
    }
    return result;
  }

  /**
   * Compares two values using a common set of rules.
   *
   * <p>Supported comparisons: numbers (by double value), strings (lexicographically), instants
   * (chronologically), or any pair of objects that implement Comparable of the same type. If values
   * are not comparable, an IllegalArgumentException is thrown.
   *
   * @param a the first value
   * @param b the second value
   * @return a negative integer, zero, or a positive integer as a is less than, equal to, or greater
   *     than b
   * @throws IllegalArgumentException if the values cannot be compared
   */
  @SuppressWarnings("unchecked")
  public static int compare(final Object a, final Object b) {
    if (a instanceof Number na && b instanceof Number nb) {
      return Double.compare(na.doubleValue(), nb.doubleValue());
    }
    if (a instanceof String sa && b instanceof String sb) {
      return sa.compareTo(sb);
    }
    if (a instanceof Instant ia && b instanceof Instant ib) {
      return ia.compareTo(ib);
    }
    if (a instanceof Comparable<?> ca && b instanceof Comparable<?> cb) {
      try {
        return ((Comparable<Object>) ca).compareTo(cb);
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Cannot compare values of different types");
      }
    }
    throw new IllegalArgumentException("Cannot compare values of different types");
  }
}

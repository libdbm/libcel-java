package com.libdbm.cel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility helper methods for libcel.
 *
 * <p>Methods are public and static so library users can call them directly.
 */
public final class Utilities {
  /**
   * The largest collection or string an expression may generate.
   *
   * <p>CEL evaluates untrusted input, so operations whose size is controlled by the expression
   * itself, such as lists.range() and the repetition operators, refuse to allocate beyond this
   * ceiling rather than exhausting the heap.
   */
  public static final int LIMIT = 1_000_000;

  /**
   * The largest number of seconds a duration may span, as defined by the CEL specification.
   *
   * <p>Ten thousand years either side of zero, matching the protobuf duration range.
   */
  public static final long SPAN = 315_576_000_000L;

  private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

  private static final Pattern DURATION =
      Pattern.compile("(\\d+(?:\\.\\d+)?)(ns|us|\u00b5s|ms|s|m|h)");
  private static final Pattern DURATION_FORM =
      Pattern.compile("^-?(?:\\d+(?:\\.\\d+)?(?:ns|us|\u00b5s|ms|s|m|h))+$");

  private Utilities() {}

  /**
   * Refuses a result that would exceed {@link #LIMIT}.
   *
   * <p>Bounding each operation's output, and not only its inputs, is what stops two separately
   * legal values from being combined into one that exhausts the heap.
   *
   * @param size the size the operation is about to produce
   * @param what the operation being bounded, named in the error message
   * @throws EvaluationError if the size exceeds the limit
   */
  public static void limit(final long size, final String what) {
    if (size > LIMIT) {
      throw new EvaluationError(what + " exceeds the evaluation limit of " + LIMIT);
    }
  }

  /**
   * Returns the size/length of the given value.
   *
   * <p>Supported types: - String: number of Unicode code points - List: number of elements - Map:
   * number of entries - null: 0
   *
   * @param value the value whose size should be computed; may be null
   * @return the size for supported types, or 0 for null
   * @throws IllegalArgumentException if the value type is unsupported
   */
  public static long sizeOf(final Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof String str) {
      return str.codePointCount(0, str.length());
    }
    if (value instanceof byte[] bytes) {
      return bytes.length;
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
   * <p>Accepted inputs: Long, Integer, Double, Float, String (parsed as long), Boolean (true=1,
   * false=0), and Instant (epoch seconds).
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
    if (value instanceof Instant instant) {
      return instant.getEpochSecond();
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
   * <p>Returns the literal string "null" for null values, decodes byte arrays as UTF-8, and renders
   * a Duration in the CEL form, for example "3600s" or "1.500000001s".
   *
   * @param value the value to stringify
   * @return the String representation
   */
  public static String asString(final Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    if (value instanceof Duration span) {
      return seconds(span);
    }
    return value.toString();
  }

  // Renders a duration the way the CEL specification does, as a seconds count with an "s" suffix
  private static String seconds(final Duration span) {
    if (span.isNegative()) {
      return "-" + seconds(span.negated());
    }
    if (span.getNano() == 0) {
      return span.getSeconds() + "s";
    }
    final var text =
        String.format("%d.%09d", span.getSeconds(), span.getNano()).replaceAll("0+$", "");
    return text + "s";
  }

  /**
   * Converts the given value to a byte array using UTF-8 for textual input.
   *
   * @param value a byte array or a String to convert
   * @return the resulting byte array
   * @throws IllegalArgumentException if the value cannot be converted
   */
  public static byte[] asBytes(final Object value) {
    if (value instanceof byte[] bytes) {
      return bytes;
    }
    if (value instanceof String str) {
      return str.getBytes(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException("Cannot convert to bytes: " + value);
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
   * Returns the CEL type of the given value.
   *
   * @param value the value whose type is to be described
   * @return the type value, or Type.UNKNOWN for a value this library does not recognise
   */
  public static Type typeOf(final Object value) {
    if (value == null) {
      return Type.NULL;
    }
    if (value instanceof Boolean) {
      return Type.BOOL;
    }
    if (value instanceof Long || value instanceof Integer) {
      return Type.INT;
    }
    if (value instanceof Double || value instanceof Float) {
      return Type.DOUBLE;
    }
    if (value instanceof String) {
      return Type.STRING;
    }
    if (value instanceof byte[]) {
      return Type.BYTES;
    }
    if (value instanceof Instant) {
      return Type.TIMESTAMP;
    }
    if (value instanceof Duration) {
      return Type.DURATION;
    }
    if (value instanceof List) {
      return Type.LIST;
    }
    if (value instanceof Map) {
      return Type.MAP;
    }
    if (value instanceof Type) {
      return Type.TYPE;
    }
    return Type.UNKNOWN;
  }

  /**
   * Checks whether a map contains the given field name.
   *
   * @param target the map-like object to check (must be a Map to return true/false)
   * @param field the field/key to look for
   * @return true if target is a Map and contains the given key; false otherwise
   */
  public static boolean has(final Object target, final Object field) {
    if (target instanceof Map<?, ?> map) {
      return contains(map, field);
    }
    return false;
  }

  /**
   * Reports whether the map holds the given key, comparing keys the way CEL compares values.
   *
   * <p>An integer key stored as an Integer therefore matches a Long probe, so membership, indexing
   * and presence tests all agree.
   *
   * @param map the map to search
   * @param key the key to look for
   * @return true if a matching key is present
   */
  public static boolean contains(final Map<?, ?> map, final Object key) {
    // An immutable map rejects a null probe outright, so only the scan is safe for one
    if (key != null && map.containsKey(key)) {
      return true;
    }
    for (final var candidate : map.keySet()) {
      if (equals(candidate, key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the value stored under the given key, comparing keys the way CEL compares values.
   *
   * @param map the map to read
   * @param key the key to look for
   * @return the matching value, or null when no key matches
   */
  public static Object select(final Map<?, ?> map, final Object key) {
    if (key != null && map.containsKey(key)) {
      return map.get(key);
    }
    for (final var entry : map.entrySet()) {
      if (equals(entry.getKey(), key)) {
        return entry.getValue();
      }
    }
    return null;
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
   * <p>Accepted inputs: null returns Instant.now(), a String is parsed as RFC3339, and a Long or
   * Integer is treated as epoch seconds.
   *
   * @param value the value to convert to an Instant
   * @return the resolved Instant
   * @throws IllegalArgumentException if the input type or format is invalid
   */
  public static Instant timestamp(final Object value) {
    if (value == null) {
      return Instant.now();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof String str) {
      try {
        return Instant.parse(str);
      } catch (final DateTimeParseException e) {
        try {
          return OffsetDateTime.parse(str).toInstant();
        } catch (final DateTimeParseException nested) {
          throw new IllegalArgumentException("Invalid timestamp value: " + value);
        }
      }
    }
    if (value instanceof Long seconds) {
      return epoch(seconds);
    }
    if (value instanceof Integer seconds) {
      return epoch(seconds.longValue());
    }
    throw new IllegalArgumentException("Invalid timestamp value: " + value);
  }

  // Converts epoch seconds to an Instant, reporting an out-of-range value as an argument error
  private static Instant epoch(final long seconds) {
    if (seconds < Instant.MIN.getEpochSecond() || seconds > Instant.MAX.getEpochSecond()) {
      throw new IllegalArgumentException("Timestamp out of range: " + seconds);
    }
    return Instant.ofEpochSecond(seconds);
  }

  /**
   * Parses a CEL duration string into a Duration.
   *
   * <p>Accepts a signed sequence of decimal numbers with unit suffixes, for example "300ms",
   * "-1.5h" or "2h45m". Valid units are "ns", "us", "\u00b5s", "ms", "s", "m" and "h".
   *
   * @param value the duration literal
   * @return the corresponding Duration
   * @throws IllegalArgumentException if the format or unit is invalid
   */
  public static Duration duration(final String value) {
    final var matcher = DURATION.matcher(value);
    if (!DURATION_FORM.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid duration format: " + value);
    }

    // Accumulated exactly: a double cannot hold the nanoseconds of a large but legal duration
    var nanos = BigDecimal.ZERO;
    while (matcher.find()) {
      final var amount = new BigDecimal(matcher.group(1));
      final var unit = matcher.group(2);
      final var scale =
          switch (unit) {
            case "ns" -> BigDecimal.ONE;
            case "us", "\u00b5s" -> THOUSAND;
            case "ms" -> THOUSAND.pow(2);
            case "s" -> THOUSAND.pow(3);
            case "m" -> THOUSAND.pow(3).multiply(BigDecimal.valueOf(60));
            case "h" -> THOUSAND.pow(3).multiply(BigDecimal.valueOf(3600));
            default -> throw new IllegalArgumentException("Invalid duration unit: " + unit);
          };
      nanos = nanos.add(amount.multiply(scale));
    }

    final var whole = nanos.setScale(0, RoundingMode.HALF_UP).toBigInteger();
    final var parts = whole.divideAndRemainder(BigInteger.valueOf(1_000_000_000L));
    final var seconds = parts[0];
    if (seconds.abs().compareTo(BigInteger.valueOf(SPAN)) > 0) {
      throw new IllegalArgumentException("Duration out of range: " + value);
    }

    final var result = Duration.ofSeconds(seconds.longValueExact(), parts[1].longValueExact());
    return value.startsWith("-") ? result.negated() : result;
  }

  /**
   * Converts the given value to a Duration.
   *
   * <p>Accepts a Duration (returned as is) or a duration literal accepted by duration(String).
   *
   * @param value the value to convert
   * @return the resulting Duration
   * @throws IllegalArgumentException if the value cannot be converted
   */
  public static Duration asDuration(final Object value) {
    if (value instanceof Duration result) {
      return result;
    }
    if (value instanceof String text) {
      return duration(text);
    }
    throw new IllegalArgumentException("Cannot convert to duration: " + value);
  }

  /**
   * Resolves a time zone identifier, defaulting to UTC.
   *
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the resolved zone
   * @throws IllegalArgumentException if the identifier is not a known zone
   */
  public static ZoneId zone(final Object zone) {
    if (zone == null) {
      return ZoneOffset.UTC;
    }
    if (zone instanceof String name) {
      try {
        return ZoneId.of(name);
      } catch (final Exception e) {
        throw new IllegalArgumentException("Invalid time zone: " + name);
      }
    }
    throw new IllegalArgumentException("Time zone must be a string: " + zone);
  }

  private static ZonedDateTime at(final Object value, final Object zone) {
    final var instant = value instanceof Instant i ? i : Utilities.timestamp(value);
    return instant.atZone(zone(zone));
  }

  /**
   * Returns the day of month (1-31) for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the day of month
   */
  public static int dateOf(final Object value, final Object zone) {
    return at(value, zone).getDayOfMonth();
  }

  /**
   * Returns the day of month (1-31) for the given timestamp-like value, in UTC.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the day of month
   */
  public static int dateOf(final Object value) {
    return dateOf(value, null);
  }

  /**
   * Returns the zero-based month (0-11) for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the month value where January is 0
   */
  public static int monthOf(final Object value, final Object zone) {
    return at(value, zone).getMonthValue() - 1;
  }

  /**
   * Returns the zero-based month (0-11) for the given timestamp-like value, in UTC.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the month value where January is 0
   */
  public static int monthOf(final Object value) {
    return monthOf(value, null);
  }

  /**
   * Returns the year for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the four-digit year
   */
  public static int yearOf(final Object value, final Object zone) {
    return at(value, zone).getYear();
  }

  /**
   * Returns the year for the given timestamp-like value, in UTC.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the four-digit year
   */
  public static int yearOf(final Object value) {
    return yearOf(value, null);
  }

  /**
   * Returns the hour of day (0-23) for the given timestamp or duration value.
   *
   * <p>For a Duration the result is the whole number of hours it spans.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the hour value
   */
  public static long hoursOf(final Object value, final Object zone) {
    if (value instanceof Duration span) {
      return span.toHours();
    }
    return at(value, zone).getHour();
  }

  /**
   * Returns the hour of day (0-23) for the given timestamp value, in UTC.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @return the hour value
   */
  public static long hoursOf(final Object value) {
    return hoursOf(value, null);
  }

  /**
   * Returns the minute for the given timestamp or duration value.
   *
   * <p>For a Duration the result is the whole number of minutes it spans.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the minute value
   */
  public static long minutesOf(final Object value, final Object zone) {
    if (value instanceof Duration span) {
      return span.toMinutes();
    }
    return at(value, zone).getMinute();
  }

  /**
   * Returns the minute of hour (0-59) for the given timestamp value, in UTC.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @return the minute value
   */
  public static long minutesOf(final Object value) {
    return minutesOf(value, null);
  }

  /**
   * Returns the second for the given timestamp or duration value.
   *
   * <p>For a Duration the result is the whole number of seconds it spans.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the second value
   */
  public static long secondsOf(final Object value, final Object zone) {
    if (value instanceof Duration span) {
      return span.toSeconds();
    }
    return at(value, zone).getSecond();
  }

  /**
   * Returns the second of minute (0-59) for the given timestamp value, in UTC.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @return the second value
   */
  public static long secondsOf(final Object value) {
    return secondsOf(value, null);
  }

  /**
   * Returns the millisecond for the given timestamp or duration value.
   *
   * <p>For a Duration the result is the whole number of milliseconds it spans.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the millisecond value
   */
  public static long millisecondsOf(final Object value, final Object zone) {
    if (value instanceof Duration span) {
      return span.toMillis();
    }
    return at(value, zone).getNano() / 1_000_000;
  }

  /**
   * Returns the millisecond of second (0-999) for the given timestamp value, in UTC.
   *
   * @param value an Instant, a Duration, or a value convertible by timestamp(Object)
   * @return the millisecond value
   */
  public static long millisecondsOf(final Object value) {
    return millisecondsOf(value, null);
  }

  /**
   * Returns the day of week for the given timestamp-like value, where Sunday is 0.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the day of week in range 0-6
   */
  public static int weekdayOf(final Object value, final Object zone) {
    return at(value, zone).getDayOfWeek().getValue() % 7;
  }

  /**
   * Returns the day of week for the given timestamp-like value in UTC, where Sunday is 0.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the day of week in range 0-6
   */
  public static int weekdayOf(final Object value) {
    return weekdayOf(value, null);
  }

  /**
   * Returns the zero-based day of year for the given timestamp-like value.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @param zone an IANA time zone identifier, or null for UTC
   * @return the day of year in range 0-365
   */
  public static int ordinalOf(final Object value, final Object zone) {
    return at(value, zone).getDayOfYear() - 1;
  }

  /**
   * Returns the zero-based day of year for the given timestamp-like value, in UTC.
   *
   * @param value an Instant or a value convertible by timestamp(Object)
   * @return the day of year in range 0-365
   */
  public static int ordinalOf(final Object value) {
    return ordinalOf(value, null);
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
   * Returns a hash code consistent with {@link #equals(Object, Object)}.
   *
   * <p>Values that compare equal under CEL rules hash alike, so equality can be resolved through a
   * hash table rather than a linear scan. Every number hashes by its double value, which is what
   * makes an Integer, a Long and a Double holding the same quantity agree. Two large longs that
   * differ only beyond the precision of a double therefore collide, which is permitted: the
   * collision is resolved by equals.
   *
   * @param value the value to hash; may be null
   * @return the hash code
   */
  public static int hash(final Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number number) {
      // Normalised because equals treats -0.0 and 0.0 as the same value
      return Double.hashCode(zero(number.doubleValue()));
    }
    if (value instanceof byte[] bytes) {
      return Arrays.hashCode(bytes);
    }
    if (value instanceof List<?> list) {
      var result = 1;
      for (final var item : list) {
        result = 31 * result + hash(item);
      }
      return result;
    }
    if (value instanceof Map<?, ?> map) {
      // Summed so that the result does not depend on iteration order
      var result = 0;
      for (final var entry : map.entrySet()) {
        result += 31 * hash(entry.getKey()) + hash(entry.getValue());
      }
      return result;
    }
    return value.hashCode();
  }

  /**
   * Orders two numbers exactly, without the precision loss of a double conversion.
   *
   * <p>Two integers compare as integers and two decimals as decimals. A mixed pair is compared
   * exactly, so a long above the range a double can represent is not mistaken for the double it
   * would round to.
   *
   * @param left the first number
   * @param right the second number
   * @return a negative integer, zero, or a positive integer as left is less than, equal to, or
   *     greater than right
   */
  public static int order(final Number left, final Number right) {
    if (!isDecimal(left) && !isDecimal(right)) {
      return Long.compare(left.longValue(), right.longValue());
    }
    if (isDecimal(left) && isDecimal(right)) {
      // Normalised because Double.compare separates the two zeros where equality does not
      return Double.compare(zero(left.doubleValue()), zero(right.doubleValue()));
    }

    // Exactly one side is a decimal; a non-finite one has no exact representation to compare
    // against
    final var scalar = isDecimal(left) ? left.doubleValue() : right.doubleValue();
    if (Double.isNaN(scalar) || Double.isInfinite(scalar)) {
      return Double.compare(zero(left.doubleValue()), zero(right.doubleValue()));
    }
    return exact(left).compareTo(exact(right));
  }

  private static double zero(final double value) {
    return value == 0.0 ? 0.0 : value;
  }

  private static BigDecimal exact(final Number value) {
    return isDecimal(value)
        ? new BigDecimal(value.doubleValue())
        : BigDecimal.valueOf(value.longValue());
  }

  private static boolean isDecimal(final Number value) {
    return value instanceof Double || value instanceof Float;
  }

  /**
   * Compares two values for deep equality.
   *
   * <p>Lists and maps are compared element by element, numbers are compared numerically across
   * integer and floating point types, and byte arrays are compared by content.
   *
   * @param left the first value; may be null
   * @param right the second value; may be null
   * @return true if the values are deeply equal
   */
  public static boolean equals(final Object left, final Object right) {
    if (left == null || right == null) {
      return left == right;
    }

    if (left instanceof byte[] l && right instanceof byte[] r) {
      return Arrays.equals(l, r);
    }

    if (left instanceof List<?> l && right instanceof List<?> r) {
      if (l.size() != r.size()) {
        return false;
      }
      for (int i = 0; i < l.size(); i++) {
        if (!equals(l.get(i), r.get(i))) {
          return false;
        }
      }
      return true;
    }

    if (left instanceof Map<?, ?> l && right instanceof Map<?, ?> r) {
      if (l.size() != r.size()) {
        return false;
      }
      // Indexed by CEL key equality, so a key held as an Integer matches one held as a Long
      final var indexed = new HashMap<Key, Object>(r.size());
      for (final var entry : r.entrySet()) {
        indexed.put(new Key(entry.getKey()), entry.getValue());
      }
      for (final var entry : l.entrySet()) {
        final var key = new Key(entry.getKey());
        if (!indexed.containsKey(key) || !equals(entry.getValue(), indexed.get(key))) {
          return false;
        }
      }
      return true;
    }

    if (left instanceof Number ln && right instanceof Number rn) {
      if (Double.isNaN(ln.doubleValue()) || Double.isNaN(rn.doubleValue())) {
        return false;
      }
      return order(ln, rn) == 0;
    }

    return left.equals(right);
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
      return order(na, nb);
    }
    if (a instanceof String sa && b instanceof String sb) {
      return sa.compareTo(sb);
    }
    if (a instanceof Instant ia && b instanceof Instant ib) {
      return ia.compareTo(ib);
    }
    if (a instanceof byte[] ba && b instanceof byte[] bb) {
      return Arrays.compare(ba, bb);
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

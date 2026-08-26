package com.libdbm.cel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * String functions from the CEL strings extension library.
 *
 * <p>Methods are public and static so library users can call them directly.
 */
public final class Strings {
  private Strings() {}

  /**
   * Returns the character at the given index as a single character string.
   *
   * <p>Indices count Unicode code points, so a supplementary character such as an emoji is one
   * position and is never split.
   *
   * @param value the source string
   * @param index the zero-based index
   * @return a string containing the selected character, or an empty string at the end
   * @throws IllegalArgumentException if the index is out of range
   */
  public static String charAt(final String value, final long index) {
    final var at = offset(value, index, "charAt");
    if (at == value.length()) {
      return "";
    }
    return new String(Character.toChars(value.codePointAt(at)));
  }

  /**
   * Returns the index of the first occurrence of a substring, or -1 when absent.
   *
   * <p>The offset and the result both count Unicode code points.
   *
   * @param value the source string
   * @param search the substring to find
   * @param offset the zero-based index to start searching from
   * @return the index of the first occurrence, or -1
   * @throws IllegalArgumentException if the offset is out of range
   */
  public static long indexOf(final String value, final String search, final long offset) {
    final var found = value.indexOf(search, offset(value, offset, "indexOf"));
    return found < 0 ? -1 : value.codePointCount(0, found);
  }

  /**
   * Returns the index of the last occurrence of a substring, or -1 when absent.
   *
   * @param value the source string
   * @param search the substring to find
   * @return the index of the last occurrence, or -1
   */
  public static long lastIndexOf(final String value, final String search) {
    final var found = value.lastIndexOf(search);
    return found < 0 ? -1 : value.codePointCount(0, found);
  }

  /**
   * Returns the index of the last occurrence of a substring at or before the given offset.
   *
   * <p>The offset and the result both count Unicode code points.
   *
   * @param value the source string
   * @param search the substring to find
   * @param offset the zero-based index at which the search starts, moving backwards
   * @return the index of the last occurrence, or -1
   * @throws IllegalArgumentException if the offset is out of range
   */
  public static long lastIndexOf(final String value, final String search, final long offset) {
    final var found = value.lastIndexOf(search, offset(value, offset, "lastIndexOf"));
    return found < 0 ? -1 : value.codePointCount(0, found);
  }

  // Translates a code point index into the UTF-16 offset the String methods expect
  private static int offset(final String value, final long index, final String name) {
    if (index < 0 || index > value.codePointCount(0, value.length())) {
      throw new IllegalArgumentException(name + "() index out of range: " + index);
    }
    return value.offsetByCodePoints(0, (int) index);
  }

  /**
   * Lowercases the ASCII characters of the given string, leaving other characters untouched.
   *
   * @param value the source string
   * @return the converted string
   */
  public static String lower(final String value) {
    final var result = new StringBuilder(value.length());
    for (var i = 0; i < value.length(); i++) {
      final var ch = value.charAt(i);
      result.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + 32) : ch);
    }
    return result.toString();
  }

  /**
   * Uppercases the ASCII characters of the given string, leaving other characters untouched.
   *
   * @param value the source string
   * @return the converted string
   */
  public static String upper(final String value) {
    final var result = new StringBuilder(value.length());
    for (var i = 0; i < value.length(); i++) {
      final var ch = value.charAt(i);
      result.append(ch >= 'a' && ch <= 'z' ? (char) (ch - 32) : ch);
    }
    return result.toString();
  }

  /**
   * Returns the substring from the given index to the end.
   *
   * @param value the source string
   * @param start the zero-based start index, in code points, inclusive
   * @return the selected substring
   * @throws IllegalArgumentException if the index is out of range
   */
  public static String substring(final String value, final long start) {
    return value.substring(offset(value, start, "substring"));
  }

  /**
   * Returns the substring between the given indices.
   *
   * <p>Indices count Unicode code points, so a supplementary character is never split.
   *
   * @param value the source string
   * @param start the zero-based start index, inclusive
   * @param end the zero-based end index, exclusive
   * @return the selected substring
   * @throws IllegalArgumentException if the range is invalid
   */
  public static String substring(final String value, final long start, final long end) {
    if (start > end) {
      throw new IllegalArgumentException("substring() range out of bounds: " + start + ":" + end);
    }
    return value.substring(offset(value, start, "substring"), offset(value, end, "substring"));
  }

  /**
   * Reverses the given string, preserving surrogate pairs.
   *
   * @param value the source string
   * @return the reversed string
   */
  public static String reverse(final String value) {
    return new StringBuilder(value).reverse().toString();
  }

  /**
   * Replaces occurrences of a literal substring.
   *
   * @param value the source string
   * @param from the literal text to replace
   * @param to the replacement text
   * @param limit the maximum number of replacements, or -1 for all
   * @return the resulting string
   */
  public static String replace(
      final String value, final String from, final String to, final long limit) {
    if (limit == 0 || from.isEmpty()) {
      return value;
    }

    final var result = new StringBuilder();
    var index = 0;
    var count = 0L;
    while (limit < 0 || count < limit) {
      final var found = value.indexOf(from, index);
      if (found < 0) {
        break;
      }
      result.append(value, index, found).append(to);
      Utilities.limit(result.length(), "replace()");
      index = found + from.length();
      count++;
    }
    result.append(value.substring(index));
    Utilities.limit(result.length(), "replace()");
    return result.toString();
  }

  /**
   * Splits a string around occurrences of a literal separator.
   *
   * @param value the source string
   * @param separator the literal separator
   * @param limit the maximum number of parts, or -1 for no limit
   * @return the resulting parts
   */
  public static List<String> split(final String value, final String separator, final long limit) {
    if (limit == 0) {
      return List.of();
    }
    final var parts = value.split(Pattern.quote(separator), limit < 0 ? -1 : (int) limit);
    return List.of(parts);
  }

  /**
   * Joins a list of strings with the given separator.
   *
   * @param values the values to join; each must be a string
   * @param separator the separator to place between values
   * @return the joined string
   * @throws IllegalArgumentException if any element is not a string
   */
  public static String join(final List<?> values, final String separator) {
    // Projected before anything is allocated: two bounded inputs can still combine into a huge one
    var projected = (long) Math.max(0, values.size() - 1) * separator.length();
    for (final var value : values) {
      if (!(value instanceof String text)) {
        throw new IllegalArgumentException("join() requires a list of strings");
      }
      projected += text.length();
      Utilities.limit(projected, "join()");
    }

    final var result = new StringBuilder((int) projected);
    for (final var value : values) {
      if (!result.isEmpty()) {
        result.append(separator);
      }
      result.append((String) value);
    }
    return result.toString();
  }

  /**
   * Formats a string using the CEL format verbs.
   *
   * <p>Supported verbs are %s, %d, %f, %e, %b, %o, %x, %X and %%, each accepting an optional
   * precision such as %.3f.
   *
   * @param format the format string
   * @param args the values substituted for the verbs, in order
   * @return the formatted string
   * @throws IllegalArgumentException if a verb is unknown or the argument count does not match
   */
  public static String format(final String format, final List<?> args) {
    final var result = new StringBuilder();
    var next = 0;
    var i = 0;

    while (i < format.length()) {
      final var ch = format.charAt(i);
      if (ch != '%') {
        result.append(ch);
        i++;
        continue;
      }
      if (i + 1 >= format.length()) {
        throw new IllegalArgumentException("format() has a trailing '%'");
      }
      if (format.charAt(i + 1) == '%') {
        result.append('%');
        i += 2;
        continue;
      }

      var precision = -1;
      var at = i + 1;
      if (format.charAt(at) == '.') {
        at++;
        final var digits = at;
        while (at < format.length() && Character.isDigit(format.charAt(at))) {
          at++;
        }
        if (at == digits) {
          throw new IllegalArgumentException("format() precision requires digits");
        }
        precision = number(format.substring(digits, at));
      }
      if (at >= format.length()) {
        throw new IllegalArgumentException("format() has an incomplete verb");
      }
      if (next >= args.size()) {
        throw new IllegalArgumentException("format() has more verbs than arguments");
      }

      result.append(convert(format.charAt(at), precision, args.get(next)));
      if (result.length() > Utilities.LIMIT) {
        throw new EvaluationError(
            "format() output exceeds the evaluation limit of " + Utilities.LIMIT);
      }
      next++;
      i = at + 1;
    }

    if (next != args.size()) {
      throw new IllegalArgumentException("format() has more arguments than verbs");
    }
    return result.toString();
  }

  // Reads a precision, refusing one large enough to make the formatted output exhaust the heap
  private static int number(final String digits) {
    // Parsed as a long so that a value beyond int range is rejected rather than failing to parse
    final var value = digits.length() > 18 ? Long.MAX_VALUE : Long.parseLong(digits);
    if (value > Utilities.LIMIT) {
      throw new EvaluationError(
          "format() precision exceeds the evaluation limit of " + Utilities.LIMIT);
    }
    return (int) value;
  }

  private static String convert(final char verb, final int precision, final Object value) {
    return switch (verb) {
      case 's' -> Utilities.asString(value);
      case 'd' -> Long.toString(Utilities.asInt(value));
      case 'f' -> fixed(Utilities.asDouble(value), precision < 0 ? 6 : precision);
      case 'e' -> scientific(Utilities.asDouble(value), precision < 0 ? 6 : precision);
      case 'b' ->
          value instanceof Boolean flag
              ? Boolean.toString(flag)
              : Long.toBinaryString(Utilities.asInt(value));
      case 'o' -> Long.toOctalString(Utilities.asInt(value));
      case 'x' -> hex(value, false);
      case 'X' -> hex(value, true);
      default -> throw new IllegalArgumentException("format() has an unknown verb: %" + verb);
    };
  }

  private static String fixed(final double value, final int precision) {
    return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).toPlainString();
  }

  private static String scientific(final double value, final int precision) {
    if (value == 0.0) {
      return fixed(0.0, precision) + "e+00";
    }
    final var exponent = (int) Math.floor(Math.log10(Math.abs(value)));
    final var mantissa = value / Math.pow(10, exponent);
    final var sign = exponent < 0 ? "-" : "+";
    final var magnitude = Math.abs(exponent);
    return fixed(mantissa, precision) + "e" + sign + (magnitude < 10 ? "0" : "") + magnitude;
  }

  private static String hex(final Object value, final boolean upper) {
    final String result;
    if (value instanceof byte[] bytes) {
      final var text = new StringBuilder(bytes.length * 2);
      for (final var b : bytes) {
        text.append(String.format("%02x", b));
      }
      result = text.toString();
    } else if (value instanceof String text) {
      result = hex(text.getBytes(StandardCharsets.UTF_8), false);
    } else {
      result = Long.toHexString(Utilities.asInt(value));
    }
    return upper ? result.toUpperCase() : result;
  }

  /**
   * Escapes the given string for inclusion in a CEL string literal, without the enclosing quotes.
   *
   * @param value the text to escape
   * @return the escaped text
   */
  public static String escape(final String value) {
    final var result = new StringBuilder(value.length() + 2);
    for (var i = 0; i < value.length(); i++) {
      final var ch = value.charAt(i);
      switch (ch) {
        case '\\' -> result.append("\\\\");
        case '"' -> result.append("\\\"");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        case '\b' -> result.append("\\b");
        case '\f' -> result.append("\\f");
        case 0x07 -> result.append("\\a");
        case 0x0B -> result.append("\\v");
        default -> {
          if (ch < 0x20 || ch == 0x7F) {
            result.append(String.format("\\u%04x", (int) ch));
          } else {
            result.append(ch);
          }
        }
      }
    }
    return result.toString();
  }

  /**
   * Returns the given string as a quoted CEL string literal.
   *
   * @param value the text to quote
   * @return the quoted literal, including the enclosing double quotes
   */
  public static String quote(final String value) {
    return "\"" + escape(value) + "\"";
  }

  /**
   * Escapes the given bytes for inclusion in a CEL bytes literal, without the enclosing quotes.
   *
   * @param value the bytes to escape
   * @return the escaped text
   */
  public static String escape(final byte[] value) {
    final var result = new StringBuilder(value.length + 2);
    for (final var b : value) {
      final var ch = (char) (b & 0xFF);
      switch (ch) {
        case '\\' -> result.append("\\\\");
        case '"' -> result.append("\\\"");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        default -> {
          if (ch < 0x20 || ch >= 0x7F) {
            result.append(String.format("\\x%02x", (int) ch));
          } else {
            result.append(ch);
          }
        }
      }
    }
    return result.toString();
  }
}

package com.libdbm.cel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regular expression functions from the CEL regex extension library.
 *
 * <p>Replacement strings refer to capture groups as {@code \1} through {@code \9}, and {@code \0}
 * for the whole match. Methods are public and static so library users can call them directly.
 */
public final class Regexes {
  private Regexes() {}

  /**
   * Replaces matches of the pattern with the given replacement.
   *
   * @param value the text to search
   * @param pattern the regular expression
   * @param replacement the replacement text, which may reference capture groups
   * @param limit the maximum number of replacements, or -1 for all
   * @return the resulting text
   * @throws IllegalArgumentException if the pattern or a group reference is invalid
   */
  public static String replace(
      final String value, final String pattern, final String replacement, final long limit) {
    final var matcher = compile(pattern).matcher(value);
    final var target = rewrite(replacement, matcher.groupCount(), pattern);
    final var result = new StringBuilder();
    var count = 0L;

    while (matcher.find()) {
      if (limit >= 0 && count >= limit) {
        break;
      }
      matcher.appendReplacement(result, target);
      Utilities.limit(result.length(), "regex.replace()");
      count++;
    }
    matcher.appendTail(result);
    Utilities.limit(result.length(), "regex.replace()");
    return result.toString();
  }

  /**
   * Returns the first match of the pattern, or null when there is none.
   *
   * <p>If the pattern declares a capture group, the first group is returned instead of the whole
   * match.
   *
   * @param value the text to search
   * @param pattern the regular expression
   * @return the matched text, or null
   * @throws IllegalArgumentException if the pattern is invalid or declares more than one group
   */
  public static String extract(final String value, final String pattern) {
    final var regex = compile(pattern);
    if (regex.matcher("").groupCount() > 1) {
      throw new IllegalArgumentException(
          "extract() supports at most one capture group: " + pattern);
    }
    final var matcher = regex.matcher(value);
    if (!matcher.find()) {
      return null;
    }
    return matcher.groupCount() == 1 ? matcher.group(1) : matcher.group();
  }

  /**
   * Returns every match of the pattern in the given text.
   *
   * <p>If the pattern declares a capture group, the first group of each match is returned instead
   * of the whole match.
   *
   * @param value the text to search
   * @param pattern the regular expression
   * @return the matched texts, in order
   * @throws IllegalArgumentException if the pattern is invalid or declares more than one group
   */
  public static List<String> extractAll(final String value, final String pattern) {
    final var regex = compile(pattern);
    if (regex.matcher("").groupCount() > 1) {
      throw new IllegalArgumentException(
          "extractAll() supports at most one capture group: " + pattern);
    }
    final var matcher = regex.matcher(value);
    final var result = new ArrayList<String>();
    while (matcher.find()) {
      final var found = matcher.groupCount() == 1 ? matcher.group(1) : matcher.group();
      if (found != null) {
        result.add(found);
      }
    }
    return result;
  }

  private static Pattern compile(final String pattern) {
    try {
      return Pattern.compile(pattern);
    } catch (final PatternSyntaxException e) {
      throw new IllegalArgumentException("Invalid regular expression: " + pattern);
    }
  }

  // Translates backslash group references into the dollar form the Java matcher expects
  private static String rewrite(final String replacement, final int groups, final String pattern) {
    final var result = new StringBuilder();
    var i = 0;

    while (i < replacement.length()) {
      final var ch = replacement.charAt(i);
      if (ch == '$') {
        result.append("\\$");
        i++;
      } else if (ch == '\\' && i + 1 < replacement.length()) {
        final var next = replacement.charAt(i + 1);
        if (Character.isDigit(next)) {
          final var group = next - '0';
          if (group > groups) {
            throw new IllegalArgumentException(
                "Replacement refers to group " + group + " but " + pattern + " has " + groups);
          }
          result.append('$').append(group);
        } else {
          result.append("\\\\").append(next);
        }
        i += 2;
      } else {
        result.append(ch);
        i++;
      }
    }
    return result.toString();
  }
}

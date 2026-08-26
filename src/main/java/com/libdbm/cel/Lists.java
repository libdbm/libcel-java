package com.libdbm.cel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * List functions from the CEL lists extension library.
 *
 * <p>Methods are public and static so library users can call them directly. All of them return new
 * lists and never modify their input.
 */
public final class Lists {
  private Lists() {}

  /**
   * Removes duplicate elements, preserving the order of first appearance.
   *
   * @param values the source list
   * @return a list without duplicates
   */
  public static List<Object> distinct(final List<?> values) {
    final var seen = new HashSet<Key>(values.size());
    final var result = new ArrayList<>();
    for (final var value : values) {
      if (seen.add(new Key(value))) {
        result.add(value);
      }
    }
    return result;
  }

  /**
   * Flattens nested lists to the requested depth.
   *
   * @param values the source list
   * @param depth how many levels to flatten; must be at least 1
   * @return the flattened list
   * @throws IllegalArgumentException if the depth is less than 1
   */
  public static List<Object> flatten(final List<?> values, final long depth) {
    if (depth < 1) {
      throw new IllegalArgumentException("flatten() depth must be at least 1");
    }
    final var result = new ArrayList<>();
    for (final var value : values) {
      if (value instanceof List<?> nested) {
        result.addAll(depth > 1 ? flatten(nested, depth - 1) : nested);
      } else {
        result.add(value);
      }
    }
    return result;
  }

  /**
   * Returns the elements of the given list in reverse order.
   *
   * @param values the source list
   * @return the reversed list
   */
  public static List<Object> reverse(final List<?> values) {
    final var result = new ArrayList<Object>(values);
    Collections.reverse(result);
    return result;
  }

  /**
   * Returns the elements between the given indices.
   *
   * @param values the source list
   * @param start the zero-based start index, inclusive
   * @param end the zero-based end index, exclusive
   * @return the selected elements
   * @throws IllegalArgumentException if the range is invalid
   */
  public static List<Object> slice(final List<?> values, final long start, final long end) {
    if (start < 0 || end > values.size() || start > end) {
      throw new IllegalArgumentException("slice() range out of bounds: " + start + ":" + end);
    }
    return new ArrayList<Object>(values.subList((int) start, (int) end));
  }

  /**
   * Sorts the given list using the standard CEL ordering.
   *
   * @param values the source list
   * @return the sorted list
   * @throws IllegalArgumentException if the elements are not mutually comparable
   */
  public static List<Object> sort(final List<?> values) {
    final var result = new ArrayList<Object>(values);
    result.sort(Utilities::compare);
    return result;
  }

  /**
   * Returns the first element of the given list.
   *
   * @param values the source list
   * @return the first element
   * @throws IllegalArgumentException if the list is empty
   */
  public static Object first(final List<?> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("first() requires a non-empty list");
    }
    return values.get(0);
  }

  /**
   * Returns the last element of the given list.
   *
   * @param values the source list
   * @return the last element
   * @throws IllegalArgumentException if the list is empty
   */
  public static Object last(final List<?> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("last() requires a non-empty list");
    }
    return values.get(values.size() - 1);
  }

  /**
   * Returns the integers from zero up to, but not including, the given bound.
   *
   * @param count the exclusive upper bound; negative values yield an empty list
   * @return the generated list
   * @throws EvaluationError if the count exceeds {@link Utilities#LIMIT}
   */
  public static List<Object> range(final long count) {
    if (count > Utilities.LIMIT) {
      throw new EvaluationError("range() exceeds the evaluation limit of " + Utilities.LIMIT);
    }
    final var result = new ArrayList<>(count > 0 ? (int) count : 0);
    for (var i = 0L; i < count; i++) {
      result.add(i);
    }
    return result;
  }

  /**
   * Reports whether the list contains the given value, using deep equality.
   *
   * @param values the list to search
   * @param value the value to look for
   * @return true if the value is present
   */
  public static boolean contains(final List<?> values, final Object value) {
    for (final var item : values) {
      if (Utilities.equals(item, value)) {
        return true;
      }
    }
    return false;
  }
}

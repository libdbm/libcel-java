package com.libdbm.cel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Set functions from the CEL sets extension library.
 *
 * <p>Lists are treated as unordered collections; duplicates do not affect the results. Methods are
 * public and static so library users can call them directly.
 */
public final class Sets {
  private Sets() {}

  /**
   * Reports whether every element of the second list appears in the first.
   *
   * @param values the containing list
   * @param subset the list whose elements are looked for
   * @return true if all elements of the subset are present
   */
  public static boolean contains(final List<?> values, final List<?> subset) {
    final var indexed = index(values);
    for (final var value : subset) {
      if (!indexed.contains(new Key(value))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Reports whether the two lists contain the same elements, ignoring order and duplicates.
   *
   * @param left the first list
   * @param right the second list
   * @return true if each list contains every element of the other
   */
  public static boolean equivalent(final List<?> left, final List<?> right) {
    return contains(left, right) && contains(right, left);
  }

  /**
   * Reports whether the two lists share at least one element.
   *
   * @param left the first list
   * @param right the second list
   * @return true if any element appears in both lists
   */
  public static boolean intersects(final List<?> left, final List<?> right) {
    final var indexed = index(left);
    for (final var value : right) {
      if (indexed.contains(new Key(value))) {
        return true;
      }
    }
    return false;
  }

  // Indexes a list by CEL equality so membership resolves without scanning
  private static Set<Key> index(final List<?> values) {
    final var result = new HashSet<Key>(values.size());
    for (final var value : values) {
      result.add(new Key(value));
    }
    return result;
  }
}

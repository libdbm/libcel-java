package com.libdbm.cel;

/**
 * A value wrapped for use as a hash table key under CEL equality.
 *
 * <p>Java collections compare with {@code equals} and {@code hashCode}, neither of which matches
 * CEL semantics: a byte array compares by identity, and an Integer never equals a Long. Wrapping a
 * value here lets set and map operations resolve in constant time instead of scanning.
 *
 * @param value the wrapped value; may be null
 */
record Key(Object value) {
  @Override
  public boolean equals(final Object other) {
    return other instanceof Key key && Utilities.equals(value, key.value());
  }

  @Override
  public int hashCode() {
    return Utilities.hash(value);
  }
}

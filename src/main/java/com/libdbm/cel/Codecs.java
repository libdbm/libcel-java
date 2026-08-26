package com.libdbm.cel;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encoding functions from the CEL encoders extension library.
 *
 * <p>Methods are public and static so library users can call them directly.
 */
public final class Codecs {
  private Codecs() {}

  /**
   * Encodes the given value as a base64 string.
   *
   * @param value a byte array, or a string encoded as UTF-8 first
   * @return the base64 representation
   * @throws IllegalArgumentException if the value cannot be converted to bytes
   */
  public static String encode(final Object value) {
    final var bytes = Utilities.asBytes(value);
    Utilities.limit(4 * ((bytes.length + 2L) / 3), "base64.encode()");
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Decodes the given base64 string into bytes.
   *
   * @param value the base64 text
   * @return the decoded bytes
   * @throws IllegalArgumentException if the text is not valid base64
   */
  public static byte[] decode(final String value) {
    try {
      return Base64.getDecoder().decode(value);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid base64 input: " + value);
    }
  }

  /**
   * Decodes the given base64 string into a UTF-8 string.
   *
   * @param value the base64 text
   * @return the decoded text
   * @throws IllegalArgumentException if the text is not valid base64
   */
  public static String text(final String value) {
    return new String(decode(value), StandardCharsets.UTF_8);
  }
}

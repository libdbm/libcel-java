package com.libdbm.cel.ast;

/**
 * Enumeration representing the type of literal values in CEL expressions.
 *
 * <p>This enum defines the various literal types that can appear in CEL expressions, including null
 * values, boolean, integer, unsigned integer, double, string, and bytes literals.
 */
public enum LiteralType {
  /**
   * Represents a null literal value in CEL expressions.
   *
   * <p>This constant indicates that a literal expression represents a null value in the CEL
   * language. It is used in conjunction with the LiteralType enum to distinguish between different
   * types of literal values in expressions.
   */
  NULL_VALUE,
  /**
   * Represents a boolean literal value in CEL expressions.
   *
   * <p>This enum constant indicates that a literal value is of boolean type, containing either true
   * or false.
   */
  BOOL,
  /**
   * Integer literal type constant.
   *
   * <p>Represents integer values in CEL expressions, including both signed and unsigned integer
   * literals. This type is used to distinguish integer values from other numeric types such as
   * doubles or unsigned integers.
   */
  INT,
  /**
   * Represents an unsigned integer literal type in CEL expressions.
   *
   * <p>This enumeration value indicates that a literal expression represents an unsigned integer
   * value, which can be used in arithmetic operations and comparisons within CEL expressions.
   */
  UINT,
  /**
   * Represents a double precision floating-point literal value in CEL expressions.
   *
   * <p>This enumeration constant is used to identify double literal types within the LiteralType
   * enum, distinguishing them from other literal value types such as integers, booleans, strings,
   * and bytes.
   */
  DOUBLE,
  /**
   * Represents a string literal value in CEL expressions.
   *
   * <p>This enum constant indicates that a literal value is a string type, containing character
   * sequences enclosed in double quotes.
   */
  STRING,
  /**
   * Represents a bytes literal type in CEL expressions.
   *
   * <p>This enum constant indicates that a literal value represents a bytes type, which is a
   * sequence of bytes typically used for binary data in expressions.
   */
  BYTES
}

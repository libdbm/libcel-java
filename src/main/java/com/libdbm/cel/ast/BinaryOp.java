package com.libdbm.cel.ast;

/**
 * Binary operators supported in CEL expressions.
 *
 * <p>Represents the various operators that can be applied to two operands in binary operations.
 * Includes arithmetic operators, comparison operators, logical operators, modulo operation, and
 * membership testing.
 */
public enum BinaryOp {
  /**
   * Binary addition operator.
   *
   * <p>Represents the addition operation between two operands in binary expressions. Supports
   * numeric addition and string concatenation.
   */
  ADD,
  /**
   * Binary subtraction operator.
   *
   * <p>Represents the subtraction operation between two numeric operands in a binary expression.
   * The result is the difference obtained by subtracting the right operand from the left operand.
   */
  SUBTRACT,
  /**
   * Binary multiplication operator.
   *
   * <p>Represents the multiplication operation between two numeric operands in CEL expressions. The
   * result is the product of the left and right operands.
   */
  MULTIPLY,
  /**
   * Represents the division operation in binary expressions.
   *
   * <p>Used to indicate that a division operation should be performed between two operands.
   */
  DIVIDE,
  /**
   * Represents the modulo operation in binary expressions.
   *
   * <p>When applied to two numeric operands, this operator returns the remainder of the division of
   * the left operand by the right operand.
   */
  MODULO,
  /**
   * Represents the equality comparison operator in binary operations.
   *
   * <p>This enum constant corresponds to the '==' operator used to test whether two operands are
   * equal. It is one of the binary operators defined in the BinaryOp enumeration.
   */
  EQUAL,
  /**
   * Represents the not equal comparison operator in binary operations.
   *
   * <p>This operator evaluates to true if the two operands are not equal in value.
   */
  NOT_EQUAL,
  /**
   * Represents the less-than comparison operator in binary operations.
   *
   * <p>Used to compare two operands and determine if the left operand is less than the right
   * operand. The result is a boolean value indicating the outcome of the comparison.
   */
  LESS,
  /**
   * Binary comparison operator representing "less than or equal to".
   *
   * <p>Used to compare two operands where the left-hand operand is evaluated to be less than or
   * equal to the right-hand operand. Returns a boolean value indicating the result of the
   * comparison.
   */
  LESS_EQUAL,
  /**
   * Binary operator representing the greater than comparison.
   *
   * <p>Used in binary operations to test if the left operand is greater than the right operand.
   * Returns true if the left operand is greater than the right operand, false otherwise.
   */
  GREATER,
  /**
   * Binary operator representing the greater than or equal comparison.
   *
   * <p>Used to compare two operands where the left-hand operand is greater than or equal to the
   * right-hand operand. Returns true if the condition holds, false otherwise.
   */
  GREATER_EQUAL,
  /**
   * Represents the logical AND operation in binary expressions.
   *
   * <p>Used to combine two boolean expressions, returning true only if both operands evaluate to
   * true.
   */
  LOGICAL_AND,
  /**
   * Represents the logical OR operator in binary operations.
   *
   * <p>Used to combine two boolean expressions, returning true if at least one of the operands is
   * true.
   */
  LOGICAL_OR,
  /**
   * Represents the membership testing operator 'in' in binary operations.
   *
   * <p>Used to test whether a value exists within a collection or map, returning true if the value
   * is found and false otherwise.
   */
  IN
}

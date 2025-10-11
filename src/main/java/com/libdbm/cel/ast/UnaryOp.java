package com.libdbm.cel.ast;

/**
 * Enumeration representing unary operators supported in CEL expressions.
 *
 * <p>Unary operators are operations that take a single operand and produce a result. The supported
 * operations include logical negation (NOT) and numeric negation (NEGATE).
 */
public enum UnaryOp {
  /**
   * Logical negation operator for boolean expressions.
   *
   * <p>Represents the NOT unary operator that inverts the boolean value of its operand. When
   * applied to a boolean expression, it returns true if the operand is false, and false if the
   * operand is true.
   */
  NOT,
  /**
   * Represents the numeric negation unary operator in CEL expressions.
   *
   * <p>This operator negates the numeric value of its operand, effectively changing its sign. For
   * example, if the operand is 5, the result will be -5. If the operand is -3, the result will be
   * 3.
   *
   * <p>When used in an expression tree, this operator is represented by the NEGATE enum constant
   * within the UnaryOp enumeration, which is part of the Unary record that encapsulates unary
   * operations.
   */
  NEGATE
}

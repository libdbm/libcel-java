package com.libdbm.cel.ast;

/**
 * Represents field selection in CEL expressions.
 *
 * <p>Selection can be used for field access (operand.field). When {@code isTest} is true, the
 * expression is a presence test (has(operand.field)).
 *
 * @param operand the expression whose field is being selected
 * @param field the field name to select
 * @param isTest whether this represents a presence test rather than a value fetch
 */
public record Select(Expression operand, String field, boolean isTest) implements Expression {
  /**
   * Constructs a field selection expression.
   *
   * <p>Represents a field access operation (operand.field) in CEL expressions. When {@code isTest}
   * is true, the expression represents a presence test (has(operand.field)) rather than a value
   * fetch.
   *
   * @param operand the expression whose field is being selected
   * @param field the field name to select
   */
  public Select(final Expression operand, final String field) {
    this(operand, field, false);
  }

  /**
   * Accepts a visitor and dispatches the visit to the appropriate method based on the expression
   * type.
   *
   * <p>This method implements the visitor pattern by calling the visitSelect method on the provided
   * visitor, passing this Select expression instance to it.
   *
   * @param <T> the return type of the visitor's visit operations
   * @param visitor the visitor that will process this select expression
   * @return the result of the visitor's visitSelect method
   */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitSelect(this);
  }
}

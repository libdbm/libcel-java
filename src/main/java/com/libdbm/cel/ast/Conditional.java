package com.libdbm.cel.ast;

/**
 * Represents a conditional (ternary) expression in CEL.
 *
 * <p>Evaluates the condition; if true, yields then; otherwise yields otherwise.
 *
 * @param condition the boolean condition expression
 * @param then the expression evaluated when condition is true
 * @param otherwise the expression evaluated when condition is false
 */
public record Conditional(Expression condition, Expression then, Expression otherwise)
    implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitConditional(this);
  }
}

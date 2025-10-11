package com.libdbm.cel.ast;

/**
 * Represents a binary operation in CEL expressions.
 *
 * <p>Examples include arithmetic (+, -, *, /), comparisons (==, !=, &lt;, &lt;=, >, >=), logical
 * operations (&amp;&amp;, ||), modulo (%), and the membership operator (in).
 *
 * @param op the binary operator to apply
 * @param left the left-hand operand expression
 * @param right the right-hand operand expression
 */
public record Binary(BinaryOp op, Expression left, Expression right) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitBinary(this);
  }
}

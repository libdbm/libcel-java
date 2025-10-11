package com.libdbm.cel.ast;

/**
 * Represents a unary operation in CEL expressions.
 *
 * <p>Supported operations include logical NOT (!) and numeric negation (-).
 *
 * @param op the unary operator to apply
 * @param operand the operand expression the operator applies to
 */
public record Unary(UnaryOp op, Expression operand) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitUnary(this);
  }
}

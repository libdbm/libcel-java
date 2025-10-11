package com.libdbm.cel.ast;

/**
 * Represents an index access operation in CEL expressions.
 *
 * <p>Supports indexing into lists, maps, strings, and other indexable types.
 *
 * @param operand the indexed expression (e.g., list or map)
 * @param index the index/key expression
 */
public record Index(Expression operand, Expression index) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitIndex(this);
  }
}

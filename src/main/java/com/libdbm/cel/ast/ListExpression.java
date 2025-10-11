package com.libdbm.cel.ast;

import java.util.List;

/**
 * Represents a list literal in CEL expressions.
 *
 * @param elements the element expressions in list order
 */
public record ListExpression(List<Expression> elements) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitList(this);
  }
}

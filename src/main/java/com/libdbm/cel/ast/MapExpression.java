package com.libdbm.cel.ast;

import java.util.List;

/**
 * Represents a map literal in CEL expressions.
 *
 * @param entries the key/value entry expressions
 */
public record MapExpression(List<MapEntry> entries) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitMap(this);
  }
}

package com.libdbm.cel.ast;

/**
 * Represents an identifier reference in CEL expressions.
 *
 * <p>Identifiers typically refer to variables, fields in the evaluation context, or function names
 * when used as part of a call.
 *
 * @param name the identifier text as it appears in the source
 */
public record Identifier(String name) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitIdentifier(this);
  }
}

package com.libdbm.cel.ast;

/**
 * Represents a literal value in CEL expressions. Examples: null, true, 42, 3.14, "hello", b"bytes"
 *
 * @param value the literal runtime value (boxed as needed); may be null for NULL_VALUE
 * @param type the literal kind to disambiguate numeric/string/bytes/null forms
 */
public record Literal(Object value, LiteralType type) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitLiteral(this);
  }
}

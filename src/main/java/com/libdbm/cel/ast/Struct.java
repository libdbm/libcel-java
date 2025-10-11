package com.libdbm.cel.ast;

import java.util.List;

/**
 * Represents a struct construction in CEL expressions.
 *
 * @param type the fully qualified type name of the struct being constructed
 * @param fields the list of field initializers for the struct
 */
public record Struct(String type, List<FieldInitializer> fields) implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitStruct(this);
  }
}

package com.libdbm.cel.ast;

/**
 * Represents a field initialization in a struct constructor.
 *
 * @param field the field name being initialized
 * @param value the expression providing the field value
 */
public record FieldInitializer(String field, Expression value) {}

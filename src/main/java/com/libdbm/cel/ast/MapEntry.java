package com.libdbm.cel.ast;

/**
 * Represents a key-value pair in a map literal.
 *
 * @param key the key expression
 * @param value the value expression associated with the key
 */
public record MapEntry(Expression key, Expression value) {}

package com.libdbm.cel.ast;

/**
 * Represents a comprehension expression for generating lists or maps.
 *
 * <p>A comprehension has an iteration variable and range, an accumulator variable with its
 * initialization, a loop condition and step expression, and a final result expression. This
 * generalizes constructs like list and map comprehensions.
 *
 * @param variable the name of the iteration variable
 * @param range the expression producing the iterable range
 * @param accumulator the accumulator variable name
 * @param initializer the initial value of the accumulator
 * @param condition the loop continuation condition expression
 * @param step the expression that updates the accumulator per iteration
 * @param result the expression that produces the final result value
 */
public record Comprehension(
    String variable,
    Expression range,
    String accumulator,
    Expression initializer,
    Expression condition,
    Expression step,
    Expression result)
    implements Expression {
  /** {@inheritDoc} */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitComprehension(this);
  }
}

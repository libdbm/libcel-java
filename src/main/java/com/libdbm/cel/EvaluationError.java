package com.libdbm.cel;

/** Exception thrown during CEL expression evaluation. */
public class EvaluationError extends RuntimeException {
  /**
   * Creates an evaluation error with the given message.
   *
   * @param message the detail message describing the error
   */
  public EvaluationError(final String message) {
    super(message);
  }
}

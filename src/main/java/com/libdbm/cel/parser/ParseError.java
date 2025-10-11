package com.libdbm.cel.parser;

/**
 * Exception thrown when parsing CEL expressions fails.
 *
 * <p>This unchecked exception is raised by the parser and lexer when they encounter invalid syntax
 * or characters. The error message includes the source location, and the {@link #line()} and {@link
 * #column()} accessors expose the exact 1-based line and column where the problem was detected.
 */
public class ParseError extends RuntimeException {
  /** The 1-based line number where the parsing error occurred. */
  private final int line;

  /** The 1-based column number where the parsing error occurred. */
  private final int column;

  /**
   * Constructs a new parse error with the specified detail message and source location.
   *
   * @param message a human-readable description of the parsing issue
   * @param line the 1-based line number at which the error occurred
   * @param column the 1-based column number at which the error occurred
   */
  public ParseError(final String message, final int line, final int column) {
    super(message + " at line " + line + ", column " + column);
    this.line = line;
    this.column = column;
  }

  /**
   * Returns the 1-based line number where the error occurred.
   *
   * @return the line number
   */
  public int line() {
    return line;
  }

  /**
   * Returns the 1-based column number where the error occurred.
   *
   * @return the column number
   */
  public int column() {
    return column;
  }
}

package com.libdbm.cel;

import com.libdbm.cel.ast.Expression;
import java.util.HashMap;
import java.util.Map;

/**
 * A compiled CEL program that can be evaluated multiple times.
 *
 * <p>A {@link Program} represents a parsed CEL expression that can be efficiently evaluated with
 * different sets of variables. This is more efficient than parsing the expression each time it
 * needs to be evaluated.
 *
 * <p>Programs are created using {@link CEL#compile} and should be reused when the same expression
 * needs to be evaluated multiple times.
 */
public class Program {
  private final Expression ast;
  private final Functions functions;

  /**
   * Creates a new compiled program.
   *
   * <p>This constructor is typically called by {@link CEL#compile} and should not be used directly.
   *
   * @param ast The abstract syntax tree of the compiled expression
   * @param functions The function library to use for evaluation
   */
  Program(final Expression ast, final Functions functions) {
    this.ast = ast;
    this.functions = functions;
  }

  /**
   * Evaluates the compiled program with the given variables.
   *
   * @param variables A map of variable names to their values
   * @return The result of evaluating the expression
   * @throws EvaluationError if an error occurs during evaluation, such as undefined variables or
   *     type mismatches
   *     <p>Example:
   *     <pre>{@code
   * final Object result = program.evaluate(Map.of(
   *     "x", 10,
   *     "y", 20,
   *     "items", List.of(1, 2, 3)
   * ));
   *
   * }</pre>
   */
  public Object evaluate(final Map<String, Object> variables) {
    final var interpreter = new Interpreter(new HashMap<>(variables), functions);
    return interpreter.evaluate(ast);
  }
}

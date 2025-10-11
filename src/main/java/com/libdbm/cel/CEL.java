package com.libdbm.cel;

import com.libdbm.cel.parser.ParseError;
import com.libdbm.cel.parser.Parser;
import java.util.Map;

/**
 * The main entry point for evaluating CEL expressions.
 *
 * <p>The {@link CEL} class provides methods to compile and evaluate CEL expressions. It supports
 * all standard CEL operators, functions, and macros.
 *
 * <p>Example:
 *
 * <pre>{@code
 * final CEL cel = new CEL();
 * final Object result = cel.eval("x * 2 + y", Map.of("x", 10, "y", 5));
 * System.out.println(result); // 25
 * }</pre>
 */
public class CEL {
  private final Functions functions;

  /** Creates a new CEL evaluator with the standard function library. */
  public CEL() {
    this(null);
  }

  /**
   * Creates a new CEL evaluator.
   *
   * @param functions Optional custom function library. If not provided, the standard CEL function
   *     library will be used.
   */
  public CEL(final Functions functions) {
    this.functions = functions != null ? functions : new StandardFunctions();
  }

  /**
   * Compiles a CEL expression using the provided function library.
   *
   * <p>This static convenience method avoids the need to instantiate {@link CEL} when callers just
   * need to compile once.
   *
   * @param expression The CEL expression to compile
   * @param functions The function library to use when compiling the program
   * @return A compiled {@link Program} using the provided functions
   * @throws ParseError if the expression is invalid
   */
  public static Program compile(final String expression, final Functions functions) {
    final var parser = new Parser(expression);

    return new Program(parser.parse(), functions);
  }

  /**
   * Evaluates a CEL expression with the given variables using the provided function library.
   *
   * <p>This static convenience method avoids the need to instantiate {@link CEL} when callers just
   * need to evaluate an expression once.
   *
   * @param expression The CEL expression to evaluate
   * @param functions The function library to use when evaluating the expression
   * @param variables A map of variable names to their values
   * @return The result of evaluating the expression
   * @throws ParseError if the expression is invalid
   * @throws EvaluationError if an error occurs during evaluation
   */
  public static Object eval(
      final String expression, final Functions functions, final Map<String, Object> variables) {
    final var program = compile(expression, functions);
    return program.evaluate(variables);
  }

  /**
   * Compiles a CEL expression into a reusable program.
   *
   * <p>This method parses the expression and returns a {@link Program} that can be evaluated
   * multiple times with different variables. This is more efficient than calling {@link #eval}
   * repeatedly with the same expression.
   *
   * @param expression The CEL expression to compile
   * @return A compiled program
   * @throws ParseError if the expression is invalid
   *     <p>Example:
   *     <pre>{@code
   * final Program program = cel.compile("price * quantity");
   * final Object result1 = program.evaluate(Map.of("price", 10, "quantity", 5));
   * final Object result2 = program.evaluate(Map.of("price", 20, "quantity", 3));
   *
   * }</pre>
   */
  public Program compile(final String expression) {
    return compile(expression, functions);
  }

  /**
   * Evaluates a CEL expression with the given variables.
   *
   * <p>This is a convenience method that compiles and evaluates the expression in one step. For
   * better performance when evaluating the same expression multiple times, use {@link #compile} to
   * create a reusable {@link Program}.
   *
   * @param expression The CEL expression to evaluate
   * @param variables A map of variable names to their values
   * @return The result of evaluating the expression
   * @throws ParseError if the expression is invalid
   * @throws EvaluationError if an error occurs during evaluation
   *     <p>Example:
   *     <pre>{@code
   * final Object result = cel.eval("user.age >= 18", Map.of(
   *     "user", Map.of("name", "Alice", "age", 25)
   * ));
   *
   * }</pre>
   */
  public Object eval(final String expression, final Map<String, Object> variables) {
    return eval(expression, functions, variables);
  }
}

package com.libdbm.cel.ast;

import java.util.List;

/**
 * Represents a function call or method invocation in CEL expressions.
 *
 * <p>Calls can be in either function style (fn(arg1, arg2)) or receiver/method style
 * (receiver.fn(arg1, arg2)).
 *
 * @param target the optional receiver expression; may be null for global function calls
 * @param function the function or method name
 * @param args the argument expressions in call order
 * @param isMacro whether this call represents a macro invocation that should be expanded during
 *     parsing rather than evaluated at runtime
 */
public record Call(Expression target, String function, List<Expression> args, boolean isMacro)
    implements Expression {
  /**
   * Constructs a function call or method invocation expression.
   *
   * <p>This constructor creates a call expression with the specified target, function name, and
   * arguments, marking it as a regular function call rather than a macro invocation.
   *
   * @param target the optional receiver expression; may be null for global function calls
   * @param function the function or method name
   * @param args the argument expressions in call order
   */
  public Call(final Expression target, final String function, final List<Expression> args) {
    this(target, function, args, false);
  }

  /**
   * Accepts a visitor and dispatches the visit operation to the appropriate visitor method.
   *
   * <p>This method implements the visitor pattern for expression traversal, allowing external
   * visitors to process call expressions in the abstract syntax tree.
   *
   * @param <T> the return type of the visitor's visit operations
   * @param visitor the visitor instance that will process this call expression
   * @return the result of the visitor's visitCall method, type T
   */
  @Override
  public <T> T accept(final Visitor<T> visitor) {
    return visitor.visitCall(this);
  }
}

package com.libdbm.cel.ast;

/**
 * Base class for all CEL expression nodes in the Abstract Syntax Tree.
 *
 * <p>Uses the Visitor pattern to enable different operations on expressions such as evaluation,
 * type checking, or code generation.
 */
public sealed interface Expression
    permits Literal,
        Identifier,
        Select,
        Call,
        ListExpression,
        MapExpression,
        Struct,
        Comprehension,
        Unary,
        Binary,
        Conditional,
        Index {

  /**
   * Accepts a visitor to perform operations on this expression node.
   *
   * <p>Invokes the appropriate visit method on the provided visitor, dispatching the visit
   * operation to the visitor implementation that corresponds to the type of this expression node.
   *
   * @param <T> the return type of the visitor's visit operations
   * @param visitor the visitor that will operate on this expression node
   * @return the result of the visitor's visit operation
   */
  <T> T accept(final Visitor<T> visitor);

  /**
   * Visitor interface for traversing and operating on CEL expression nodes.
   *
   * <p>Defines methods for visiting each type of expression node in the Abstract Syntax Tree,
   * enabling implementation of operations such as evaluation, type checking, or code generation
   * through the Visitor pattern.
   *
   * @param <T> the return type of the visit operations
   */
  interface Visitor<T> {
    /**
     * Visits a literal expression node in the AST.
     *
     * <p>This method is called by the accept method of a Literal expression to dispatch the visit
     * operation to the appropriate visitor implementation.
     *
     * @param expr the literal expression node to visit
     * @return the result of the visit operation, type T
     */
    T visitLiteral(final Literal expr);

    /**
     * Visits an identifier expression node in the AST.
     *
     * <p>This method is called by the accept method of an Identifier expression node to dispatch
     * the visit to the appropriate visitor implementation.
     *
     * @param expr the identifier expression node to visit
     * @return the result of the visit operation, type T
     */
    T visitIdentifier(final Identifier expr);

    /**
     * Visits a select expression node in the AST.
     *
     * <p>Processes a field selection operation, which can represent either a value fetch
     * (operand.field) or a presence test (has(operand.field)) depending on the isTest flag.
     *
     * @param expr the select expression node to visit
     * @return the result of processing the select expression
     */
    T visitSelect(final Select expr);

    /**
     * Visits a function call or method invocation expression node.
     *
     * <p>This method is called by the visitor pattern implementation when processing a Call
     * expression node in the abstract syntax tree. It allows for custom handling of function calls
     * and method invocations during traversal of the expression tree.
     *
     * @param expr the call expression node to visit, containing the target expression, function
     *     name, arguments, and macro flag
     * @return the result of processing the call expression, type depends on the specific visitor
     *     implementation
     */
    T visitCall(final Call expr);

    /**
     * Visits a list expression node in the AST.
     *
     * <p>This method is called by the accept method of ListExpression to dispatch the visit
     * operation to the appropriate visitor implementation.
     *
     * @param expr the list expression node to visit
     * @return the result of the visit operation, type T
     */
    T visitList(final ListExpression expr);

    /**
     * Visits a map expression node in the AST.
     *
     * <p>Invoked by the accept method of a MapExpression to dispatch the visit to the appropriate
     * visitor implementation.
     *
     * @param expr the map expression node to visit
     * @return the result of the visit operation as defined by the visitor implementation
     */
    T visitMap(final MapExpression expr);

    /**
     * Visits a struct expression node in the AST.
     *
     * <p>This method is called by the accept method of a Struct expression node to dispatch the
     * visit operation to the appropriate visitor implementation.
     *
     * @param expr the struct expression node to visit
     * @return the result of the visit operation, type T
     */
    T visitStruct(final Struct expr);

    /**
     * Visits a comprehension expression node in the AST.
     *
     * <p>Processes a comprehension expression which represents operations that generate lists or
     * maps through iteration and accumulation. The comprehension includes an iteration variable and
     * range, an accumulator variable with its initialization, a loop condition and step expression,
     * and a final result expression.
     *
     * @param expr the comprehension expression node to visit
     * @return the result of processing the comprehension expression
     */
    T visitComprehension(final Comprehension expr);

    /**
     * Visits a unary expression node in the AST.
     *
     * <p>Processes a unary operation such as logical NOT (!) or numeric negation (-) by delegating
     * to the appropriate visitor method.
     *
     * @param expr the unary expression node to visit
     * @return the result of the visit operation, type T
     */
    T visitUnary(final Unary expr);

    /**
     * Visits a binary expression node in the AST.
     *
     * <p>Processes a binary operation by delegating to the appropriate visitor method based on the
     * operator and operands.
     *
     * @param expr the binary expression node to visit
     * @return the result of the visit operation, type T
     */
    T visitBinary(final Binary expr);

    /**
     * Visits a conditional expression node in the AST.
     *
     * <p>Processes a conditional expression that evaluates a condition and returns one of two
     * possible values based on the result. The conditional expression follows the form "condition ?
     * then : otherwise".
     *
     * @param expr the conditional expression to visit
     * @return the result of the visit operation
     */
    T visitConditional(final Conditional expr);

    /**
     * Visits an index expression node in the AST.
     *
     * <p>Processes an index operation that accesses elements from collections such as lists, maps,
     * or strings using the specified operand and index expressions.
     *
     * @param expr the index expression to visit, containing an operand and an index
     * @return the result of processing the index expression, type determined by the visitor
     *     implementation
     */
    T visitIndex(final Index expr);
  }
}

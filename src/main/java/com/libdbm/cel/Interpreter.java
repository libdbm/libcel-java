package com.libdbm.cel;

import com.libdbm.cel.ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreter for evaluating CEL expressions.
 *
 * <p>Implements the Visitor pattern to traverse and evaluate the AST produced by the parser.
 * Supports all CEL operations including macros, type conversions, and complex expressions.
 */
public class Interpreter implements Expression.Visitor<Object> {
  private final Map<String, Object> variables;
  private final Functions functions;

  /**
   * Constructs an interpreter with the specified variables and functions.
   *
   * @param variables A map of variable names to their values. If null, an empty map is used.
   * @param functions An instance of Functions to handle function calls. If null, StandardFunctions
   *     is used.
   */
  public Interpreter(final Map<String, Object> variables, final Functions functions) {
    this.variables = variables != null ? variables : new HashMap<>();
    this.functions = functions != null ? functions : new StandardFunctions();
  }

  /**
   * Creates a new interpreter with default empty variable and function mappings.
   *
   * <p>This constructor initializes the interpreter with empty collections for variables and
   * functions, using the standard function library.
   */
  public Interpreter() {
    this(null, null);
  }

  /**
   * Evaluates a CEL expression and returns its result.
   *
   * @param expr The expression to evaluate
   * @return The result of the evaluation
   * @throws EvaluationError if evaluation fails
   */
  public Object evaluate(final Expression expr) {
    return expr.accept(this);
  }

  @Override
  public Object visitLiteral(final Literal expr) {
    // In CEL, byte literals are just strings marked as bytes, they are not base64 encoded
    return expr.value();
  }

  @Override
  public Object visitIdentifier(final Identifier expr) {
    if (!variables.containsKey(expr.name())) {
      throw new EvaluationError("Undefined variable: " + expr.name());
    }
    return variables.get(expr.name());
  }

  @Override
  public Object visitSelect(final Select expr) {
    final var target = expr.operand() != null ? evaluate(expr.operand()) : variables;

    if (target == null) {
      if (expr.isTest()) {
        return false;
      }
      throw new EvaluationError("Cannot select field " + expr.field() + " from null");
    }

    if (target instanceof Map<?, ?> map) {
      if (expr.isTest()) {
        return map.containsKey(expr.field());
      }
      if (!map.containsKey(expr.field())) {
        throw new EvaluationError("Field " + expr.field() + " not found");
      }
      return map.get(expr.field());
    }

    throw new EvaluationError("Cannot select field from non-map type");
  }

  @Override
  public Object visitCall(final Call expr) {
    // For macro calls, we need special handling
    if (expr.isMacro() && expr.target() != null) {
      final var target = evaluate(expr.target());

      // For macros, we pass the AST expressions, not evaluated values
      // The first argument should be an identifier (variable name)
      if (expr.args().isEmpty()) {
        throw new EvaluationError("Macro " + expr.function() + " requires arguments");
      }

      // Extract variable name from first argument
      final var expression = expr.args().get(0);
      if (!(expression instanceof Identifier)) {
        throw new EvaluationError(
            "First argument to macro " + expr.function() + " must be a variable name");
      }
      final var name = ((Identifier) expression).name();

      // Second argument is the expression (kept as AST)
      if (expr.args().size() < 2) {
        throw new EvaluationError("Macro " + expr.function() + " requires an expression argument");
      }
      final var macro = expr.args().get(1);

      // Handle each macro function
      return evaluateMacro(target, expr.function(), name, macro);
    }

    // Regular function call - evaluate all arguments
    final var args = new ArrayList<>();
    for (final Expression arg : expr.args()) {
      args.add(evaluate(arg));
    }

    if (expr.target() != null) {
      final var target = evaluate(expr.target());
      return functions.callMethod(target, expr.function(), args);
    } else {
      return functions.callFunction(expr.function(), args);
    }
  }

  private Object evaluateMacro(
      final Object target, final String function, final String name, final Expression expr) {
    if (!(target instanceof List<?> list)) {
      throw new EvaluationError("Macro " + function + " requires a list target");
    }

    // Save the current value of the variable (if any)
    final var saved = variables.get(name);
    final var had = variables.containsKey(name);

    try {
      switch (function) {
        case "map" -> {
          final var results = new ArrayList<>();
          for (final Object item : list) {
            variables.put(name, item);
            results.add(evaluate(expr));
          }
          return results;
        }
        case "filter" -> {
          final var results = new ArrayList<>();
          for (final Object item : list) {
            variables.put(name, item);
            final Object condition = evaluate(expr);
            if (Boolean.TRUE.equals(condition)) {
              results.add(item);
            }
          }
          return results;
        }
        case "all" -> {
          for (final Object item : list) {
            variables.put(name, item);
            final var condition = evaluate(expr);
            if (!Boolean.TRUE.equals(condition)) {
              return false;
            }
          }
          return true;
        }
        case "exists" -> {
          for (final Object item : list) {
            variables.put(name, item);
            final var condition = evaluate(expr);
            if (Boolean.TRUE.equals(condition)) {
              return true;
            }
          }
          return false;
        }
        case "existsOne" -> {
          var count = 0;
          for (final Object item : list) {
            variables.put(name, item);
            final var condition = evaluate(expr);
            if (Boolean.TRUE.equals(condition)) {
              count++;
              if (count > 1) {
                return false;
              }
            }
          }
          return count == 1;
        }
        default -> throw new EvaluationError("Unknown macro function: " + function);
      }
    } finally {
      // Restore the original value of the variable
      if (had) {
        variables.put(name, saved);
      } else {
        variables.remove(name);
      }
    }
  }

  @Override
  public Object visitList(final ListExpression expr) {
    final var result = new ArrayList<>();
    for (final Expression element : expr.elements()) {
      result.add(evaluate(element));
    }
    return result;
  }

  @Override
  public Object visitMap(final MapExpression expr) {
    final var map = new HashMap<>();
    for (final MapEntry entry : expr.entries()) {
      final var key = evaluate(entry.key());
      final var value = evaluate(entry.value());
      map.put(key, value);
    }
    return map;
  }

  @Override
  public Object visitStruct(final Struct expr) {
    final var map = new HashMap<>();
    for (final FieldInitializer field : expr.fields()) {
      map.put(field.field(), evaluate(field.value()));
    }
    return map;
  }

  @Override
  public Object visitComprehension(final Comprehension expr) {
    final var range = evaluate(expr.range());
    if (!(range instanceof List<?> list)) {
      throw new EvaluationError("Comprehension range must be a list");
    }

    final var iterator = variables.get(expr.variable());
    final var saved = variables.get(expr.accumulator());
    final var hadIterator = variables.containsKey(expr.variable());
    final var hadAccumulator = variables.containsKey(expr.accumulator());

    try {
      var accumulator = evaluate(expr.initializer());
      variables.put(expr.accumulator(), accumulator);

      for (final Object item : list) {
        variables.put(expr.variable(), item);

        final var condition = evaluate(expr.condition());
        if (!Boolean.TRUE.equals(condition)) {
          continue;
        }

        accumulator = evaluate(expr.step());
        variables.put(expr.accumulator(), accumulator);
      }

      return evaluate(expr.result());
    } finally {
      if (hadIterator) {
        variables.put(expr.variable(), iterator);
      } else {
        variables.remove(expr.variable());
      }
      if (hadAccumulator) {
        variables.put(expr.accumulator(), saved);
      } else {
        variables.remove(expr.accumulator());
      }
    }
  }

  @Override
  public Object visitUnary(final Unary expr) {
    final var operand = evaluate(expr.operand());

    return switch (expr.op()) {
      case NOT -> {
        if (!(operand instanceof Boolean)) {
          throw new EvaluationError("NOT operator requires boolean operand");
        }
        yield !(Boolean) operand;
      }
      case NEGATE -> {
        if (operand instanceof Long l) {
          yield -l;
        } else if (operand instanceof Integer i) {
          yield -(long) i;
        } else if (operand instanceof Double d) {
          yield -d;
        } else if (operand instanceof Float f) {
          yield -(double) f;
        }
        throw new EvaluationError("Negation requires numeric operand");
      }
    };
  }

  @Override
  public Object visitBinary(final Binary expr) {
    // Short-circuit evaluation for logical operators
    if (expr.op() == BinaryOp.LOGICAL_AND) {
      final var left = evaluate(expr.left());
      if (!Boolean.TRUE.equals(left)) {
        return false;
      }
      return Boolean.TRUE.equals(evaluate(expr.right()));
    } else if (expr.op() == BinaryOp.LOGICAL_OR) {
      final var left = evaluate(expr.left());
      if (Boolean.TRUE.equals(left)) {
        return true;
      }
      return Boolean.TRUE.equals(evaluate(expr.right()));
    }

    final var left = evaluate(expr.left());
    final var right = evaluate(expr.right());

    return switch (expr.op()) {
      case ADD -> {
        // String concatenation
        if (left instanceof String || right instanceof String) {
          yield String.valueOf(left) + right;
        }
        // List concatenation
        if (left instanceof List<?> l && right instanceof List<?> r) {
          final var result = new ArrayList<Object>(l);
          result.addAll(r);
          yield result;
        }
        // Numeric addition
        if (left instanceof Number && right instanceof Number) {
          yield addNumbers((Number) left, (Number) right);
        }
        throw new EvaluationError("Invalid operands for addition");
      }
      case SUBTRACT -> {
        if (left instanceof Number && right instanceof Number) {
          yield subtractNumbers((Number) left, (Number) right);
        }
        throw new EvaluationError("Subtraction requires numeric operands");
      }
      case MULTIPLY -> {
        if (left instanceof Number && right instanceof Number) {
          yield multiplyNumbers((Number) left, (Number) right);
        }
        // String repetition
        if (left instanceof String str && right instanceof Number num) {
          yield str.repeat(num.intValue());
        }
        // List repetition
        if (left instanceof List<?> list && right instanceof Number num) {
          final var result = new ArrayList<>();
          final var count = num.intValue();
          for (int i = 0; i < count; i++) {
            result.addAll(list);
          }
          yield result;
        }
        throw new EvaluationError("Invalid operands for multiplication");
      }
      case DIVIDE -> {
        if (left instanceof Number && right instanceof Number) {
          final var value = ((Number) right).doubleValue();
          if (value == 0.0) {
            throw new EvaluationError("Division by zero");
          }
          // Division always returns double
          yield ((Number) left).doubleValue() / value;
        }
        throw new EvaluationError("Division requires numeric operands");
      }
      case MODULO -> {
        if (left instanceof Long l && right instanceof Long r) {
          if (r == 0L) {
            throw new EvaluationError("Modulo by zero");
          }
          yield l % r;
        }
        if (left instanceof Integer l && right instanceof Integer r) {
          if (r == 0) {
            throw new EvaluationError("Modulo by zero");
          }
          yield (long) (l % r);
        }
        // Try to convert to long
        if (left instanceof Number && right instanceof Number) {
          final var l = ((Number) left).longValue();
          final var r = ((Number) right).longValue();
          if (r == 0L) {
            throw new EvaluationError("Modulo by zero");
          }
          yield l % r;
        }
        throw new EvaluationError("Modulo requires integer operands");
      }
      case EQUAL -> equals(left, right);
      case NOT_EQUAL -> !equals(left, right);
      case LESS -> compare(left, right) < 0;
      case LESS_EQUAL -> compare(left, right) <= 0;
      case GREATER -> compare(left, right) > 0;
      case GREATER_EQUAL -> compare(left, right) >= 0;
      case IN -> {
        if (right instanceof List<?> list) {
          yield containsInList(list, left);
        } else if (right instanceof Map<?, ?> map) {
          yield map.containsKey(left);
        } else if (right instanceof String str && left instanceof String substr) {
          yield str.contains(substr);
        }
        throw new EvaluationError("IN operator requires list, map, or string on right side");
      }
      default -> throw new EvaluationError("Unknown binary operator: " + expr.op());
    };
  }

  @Override
  public Object visitConditional(final Conditional expr) {
    final var condition = evaluate(expr.condition());
    if (Boolean.TRUE.equals(condition)) {
      return evaluate(expr.then());
    } else {
      return evaluate(expr.otherwise());
    }
  }

  @Override
  public Object visitIndex(final Index expr) {
    final var operand = evaluate(expr.operand());
    final var index = evaluate(expr.index());

    if (operand == null) {
      throw new EvaluationError("Cannot index null value");
    }

    if (operand instanceof List<?> list) {
      if (!(index instanceof Number)) {
        throw new EvaluationError("List index must be an integer");
      }
      final int idx = ((Number) index).intValue();
      if (idx < 0 || idx >= list.size()) {
        throw new EvaluationError("List index out of bounds: " + idx);
      }
      return list.get(idx);
    } else if (operand instanceof Map<?, ?> map) {
      if (!map.containsKey(index)) {
        throw new EvaluationError("Map key not found: " + index);
      }
      return map.get(index);
    } else if (operand instanceof String str) {
      if (!(index instanceof Number)) {
        throw new EvaluationError("String index must be an integer");
      }
      final int idx = ((Number) index).intValue();
      if (idx < 0 || idx >= str.length()) {
        throw new EvaluationError("String index out of bounds: " + idx);
      }
      return String.valueOf(str.charAt(idx));
    }

    throw new EvaluationError("Cannot index type: " + operand.getClass().getName());
  }

  // Helper methods for numeric operations
  private Number addNumbers(final Number left, final Number right) {
    if (left instanceof Double
        || right instanceof Double
        || left instanceof Float
        || right instanceof Float) {
      return left.doubleValue() + right.doubleValue();
    }
    return left.longValue() + right.longValue();
  }

  private Number subtractNumbers(final Number left, final Number right) {
    if (left instanceof Double
        || right instanceof Double
        || left instanceof Float
        || right instanceof Float) {
      return left.doubleValue() - right.doubleValue();
    }
    return left.longValue() - right.longValue();
  }

  private Number multiplyNumbers(final Number left, final Number right) {
    if (left instanceof Double
        || right instanceof Double
        || left instanceof Float
        || right instanceof Float) {
      return left.doubleValue() * right.doubleValue();
    }
    return left.longValue() * right.longValue();
  }

  // Deep equality checking
  private boolean equals(final Object left, final Object right) {
    if (left == null || right == null) {
      return left == right;
    }

    if (left instanceof List<?> l && right instanceof List<?> r) {
      if (l.size() != r.size()) {
        return false;
      }
      for (int i = 0; i < l.size(); i++) {
        if (!equals(l.get(i), r.get(i))) {
          return false;
        }
      }
      return true;
    }

    if (left instanceof Map<?, ?> l && right instanceof Map<?, ?> r) {
      if (l.size() != r.size()) {
        return false;
      }
      for (final var key : l.keySet()) {
        if (!r.containsKey(key)) {
          return false;
        }
        if (!equals(l.get(key), r.get(key))) {
          return false;
        }
      }
      return true;
    }

    // Numeric equality with type coercion
    if (left instanceof Number ln && right instanceof Number rn) {
      // If either is floating point, compare as doubles
      if (ln instanceof Double
          || ln instanceof Float
          || rn instanceof Double
          || rn instanceof Float) {
        return ln.doubleValue() == rn.doubleValue();
      }
      // Otherwise compare as longs
      return ln.longValue() == rn.longValue();
    }

    return left.equals(right);
  }

  // Helper for list contains with deep equality
  private boolean containsInList(final List<?> list, final Object value) {
    for (final var item : list) {
      if (equals(item, value)) {
        return true;
      }
    }
    return false;
  }

  // Lexicographic comparison
  private int compare(final Object left, final Object right) {
    if (left == null && right == null) {
      return 0;
    }
    if (left == null) {
      return -1;
    }
    if (right == null) {
      return 1;
    }

    if (left instanceof Number && right instanceof Number) {
      final var l = ((Number) left).doubleValue();
      final var r = ((Number) right).doubleValue();
      return Double.compare(l, r);
    } else if (left instanceof String && right instanceof String) {
      return ((String) left).compareTo((String) right);
    } else if (left instanceof Boolean && right instanceof Boolean) {
      return Boolean.compare((Boolean) left, (Boolean) right);
    } else if (left instanceof List<?> l && right instanceof List<?> r) {
      final int size = Math.min(l.size(), r.size());
      for (var i = 0; i < size; i++) {
        final var cmp = compare(l.get(i), r.get(i));
        if (cmp != 0) {
          return cmp;
        }
      }
      return Integer.compare(l.size(), r.size());
    }

    throw new EvaluationError(
        "Cannot compare types: "
            + left.getClass().getName()
            + " and "
            + right.getClass().getName());
  }
}

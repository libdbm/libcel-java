package com.libdbm.cel;

import com.libdbm.cel.ast.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreter for evaluating CEL expressions.
 *
 * <p>Implements the Visitor pattern to traverse and evaluate the AST produced by the parser.
 * Supports all CEL operations including macros, type conversions, and complex expressions.
 *
 * <p><b>Note:</b> This class is NOT thread-safe. Each thread should use its own Interpreter
 * instance, or synchronize externally when sharing. The variables map is mutable and modified
 * during macro evaluation.
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

  private static boolean isDecimal(final Object value) {
    return value instanceof Double || value instanceof Float;
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
      // Bare type names resolve to type values unless a variable shadows them
      final var type = Type.of(expr.name());
      if (type != null) {
        return type;
      }
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

      // Remaining arguments are kept as unevaluated AST
      if (expr.args().size() < 2) {
        throw new EvaluationError("Macro " + expr.function() + " requires an expression argument");
      }
      final var steps = expr.args().subList(1, expr.args().size());

      // Handle each macro function
      return evaluateMacro(target, expr.function(), name, steps);
    }

    // Regular function call - evaluate all arguments
    final var args = new ArrayList<>();
    for (final Expression arg : expr.args()) {
      args.add(evaluate(arg));
    }

    if (expr.target() != null) {
      final var namespace = qualify(expr.target());
      if (namespace != null) {
        final var qualified = namespace + "." + expr.function();
        if (!variables.containsKey(root(namespace)) && functions.knows(qualified)) {
          return functions.callFunction(qualified, args);
        }
      }
      final var target = evaluate(expr.target());
      return functions.callMethod(target, expr.function(), args);
    } else {
      return functions.callFunction(expr.function(), args);
    }
  }

  // Flattens an identifier or a chain of selections over one into a dotted name, or null
  private String qualify(final Expression expr) {
    if (expr instanceof Identifier identifier) {
      return identifier.name();
    }
    if (expr instanceof Select select && select.operand() != null && !select.isTest()) {
      final var prefix = qualify(select.operand());
      return prefix == null ? null : prefix + "." + select.field();
    }
    return null;
  }

  private String root(final String name) {
    final var dot = name.indexOf('.');
    return dot < 0 ? name : name.substring(0, dot);
  }

  private Object evaluateMacro(
      final Object target, final String function, final String name, final List<Expression> steps) {
    final List<?> list;
    if (target instanceof List<?> values) {
      list = values;
    } else if (target instanceof Map<?, ?> map) {
      list = new ArrayList<>(map.keySet());
    } else {
      throw new EvaluationError("Macro " + function + " requires a list or map target");
    }

    // Save the current value of the variable (if any)
    final var saved = variables.get(name);
    final var had = variables.containsKey(name);

    try {
      switch (function) {
        case "map" -> {
          // Optional second expression makes the first a predicate: map(x, filter, transform)
          final var predicate = steps.size() > 1 ? steps.get(0) : null;
          final var transform = steps.size() > 1 ? steps.get(1) : steps.get(0);
          final var results = new ArrayList<>();
          for (final Object item : list) {
            variables.put(name, item);
            if (predicate != null && !test(predicate, function)) {
              continue;
            }
            results.add(evaluate(transform));
          }
          return results;
        }
        case "filter" -> {
          final var results = new ArrayList<>();
          for (final Object item : list) {
            variables.put(name, item);
            if (test(steps.get(0), function)) {
              results.add(item);
            }
          }
          return results;
        }
        case "all" -> {
          for (final Object item : list) {
            variables.put(name, item);
            if (!test(steps.get(0), function)) {
              return false;
            }
          }
          return true;
        }
        case "exists" -> {
          for (final Object item : list) {
            variables.put(name, item);
            if (test(steps.get(0), function)) {
              return true;
            }
          }
          return false;
        }
        case "existsOne" -> {
          var count = 0;
          for (final Object item : list) {
            variables.put(name, item);
            if (test(steps.get(0), function)) {
              count++;
              if (count > 1) {
                return false;
              }
            }
          }
          return count == 1;
        }
        case "sortBy" -> {
          final var keys = new ArrayList<Object>(list.size());
          for (final Object item : list) {
            variables.put(name, item);
            keys.add(evaluate(steps.get(0)));
          }
          final var order = new ArrayList<Integer>(list.size());
          for (var i = 0; i < list.size(); i++) {
            order.add(i);
          }
          order.sort((left, right) -> Utilities.compare(keys.get(left), keys.get(right)));
          final var results = new ArrayList<>(list.size());
          for (final var index : order) {
            results.add(list.get(index));
          }
          return results;
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

  // Evaluates a macro predicate, which the specification requires to yield a boolean
  private boolean test(final Expression expr, final String function) {
    final var result = evaluate(expr);
    if (!(result instanceof Boolean flag)) {
      throw new EvaluationError("Macro " + function + " requires a boolean predicate");
    }
    return flag;
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
          if (l == Long.MIN_VALUE) {
            throw new EvaluationError("Integer overflow");
          }
          yield -l;
        } else if (operand instanceof Integer i) {
          yield -(long) i;
        } else if (operand instanceof Double d) {
          yield -d;
        } else if (operand instanceof Float f) {
          yield -(double) f;
        } else if (operand instanceof Duration span) {
          yield span.negated();
        }
        throw new EvaluationError("Negation requires numeric operand");
      }
    };
  }

  @Override
  public Object visitBinary(final Binary expr) {
    // Logical operators short-circuit and absorb errors from the other operand
    if (expr.op() == BinaryOp.LOGICAL_AND) {
      return combine(expr, false);
    } else if (expr.op() == BinaryOp.LOGICAL_OR) {
      return combine(expr, true);
    }

    final var left = evaluate(expr.left());
    final var right = evaluate(expr.right());

    return switch (expr.op()) {
      case ADD -> {
        // Bytes concatenation
        if (left instanceof byte[] l && right instanceof byte[] r) {
          Utilities.limit((long) l.length + r.length, "Bytes concatenation");
          final var result = new byte[l.length + r.length];
          System.arraycopy(l, 0, result, 0, l.length);
          System.arraycopy(r, 0, result, l.length, r.length);
          yield result;
        }
        // Timestamp and duration arithmetic
        if (left instanceof Instant l && right instanceof Duration r) {
          yield l.plus(r);
        }
        if (left instanceof Duration l && right instanceof Instant r) {
          yield r.plus(l);
        }
        if (left instanceof Duration l && right instanceof Duration r) {
          yield l.plus(r);
        }
        // String concatenation
        if (left instanceof String || right instanceof String) {
          final var head = Utilities.asString(left);
          final var tail = Utilities.asString(right);
          Utilities.limit((long) head.length() + tail.length(), "String concatenation");
          yield head + tail;
        }
        // List concatenation
        if (left instanceof List<?> l && right instanceof List<?> r) {
          Utilities.limit((long) l.size() + r.size(), "List concatenation");
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
        if (left instanceof Instant l && right instanceof Instant r) {
          yield Duration.between(r, l);
        }
        if (left instanceof Instant l && right instanceof Duration r) {
          yield l.minus(r);
        }
        if (left instanceof Duration l && right instanceof Duration r) {
          yield l.minus(r);
        }
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
          final var count = repetitions(num, str.length());
          yield str.repeat(count);
        }
        // List repetition
        if (left instanceof List<?> list && right instanceof Number num) {
          final var count = repetitions(num, list.size());
          final var result = new ArrayList<>(count * list.size());
          for (var i = 0; i < count; i++) {
            result.addAll(list);
          }
          yield result;
        }
        throw new EvaluationError("Invalid operands for multiplication");
      }
      case DIVIDE -> {
        if (left instanceof Number && right instanceof Number) {
          if (isDecimal(left) || isDecimal(right)) {
            // Double division follows IEEE 754, so a zero divisor yields infinity or NaN
            yield ((Number) left).doubleValue() / ((Number) right).doubleValue();
          }
          final var divisor = ((Number) right).longValue();
          if (divisor == 0L) {
            throw new EvaluationError("Division by zero");
          }
          final var dividend = ((Number) left).longValue();
          if (dividend == Long.MIN_VALUE && divisor == -1L) {
            throw new EvaluationError("Integer overflow");
          }
          yield dividend / divisor;
        }
        throw new EvaluationError("Division requires numeric operands");
      }
      case MODULO -> {
        if (left instanceof Number
            && right instanceof Number
            && !isDecimal(left)
            && !isDecimal(right)) {
          final var divisor = ((Number) right).longValue();
          if (divisor == 0L) {
            throw new EvaluationError("Modulo by zero");
          }
          yield ((Number) left).longValue() % divisor;
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
          yield Utilities.contains(map, left);
        } else if (right instanceof String str && left instanceof String substr) {
          yield str.contains(substr);
        }
        throw new EvaluationError("IN operator requires list, map, or string on right side");
      }
      default -> throw new EvaluationError("Unknown binary operator: " + expr.op());
    };
  }

  // Evaluates a logical operator: the absorbing value wins even when the other operand errors
  private Object combine(final Binary expr, final boolean absorbing) {
    final var left = attempt(expr.left());
    if (left instanceof Boolean flag && flag == absorbing) {
      return absorbing;
    }

    final var right = attempt(expr.right());
    if (right instanceof Boolean flag && flag == absorbing) {
      return absorbing;
    }

    if (left instanceof RuntimeException error) {
      throw error;
    }
    if (right instanceof RuntimeException error) {
      throw error;
    }
    if (!(left instanceof Boolean) || !(right instanceof Boolean)) {
      throw new EvaluationError("Logical operator " + expr.op() + " requires boolean operands");
    }
    return !absorbing;
  }

  // Evaluates an operand, returning the error instead of throwing it
  private Object attempt(final Expression expr) {
    try {
      return evaluate(expr);
    } catch (final EvaluationError | IllegalArgumentException error) {
      return error;
    }
  }

  @Override
  public Object visitConditional(final Conditional expr) {
    final var condition = evaluate(expr.condition());
    if (!(condition instanceof Boolean flag)) {
      throw new EvaluationError("Conditional requires a boolean condition");
    }
    return flag ? evaluate(expr.then()) : evaluate(expr.otherwise());
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
      if (!Utilities.contains(map, index)) {
        throw new EvaluationError("Map key not found: " + index);
      }
      return Utilities.select(map, index);
    } else if (operand instanceof byte[] bytes) {
      if (!(index instanceof Number)) {
        throw new EvaluationError("Bytes index must be an integer");
      }
      final int at = ((Number) index).intValue();
      if (at < 0 || at >= bytes.length) {
        throw new EvaluationError("Bytes index out of bounds: " + at);
      }
      return (long) (bytes[at] & 0xFF);
    } else if (operand instanceof String str) {
      if (!(index instanceof Number)) {
        throw new EvaluationError("String index must be an integer");
      }
      // Indexed by code point, so a supplementary character is one position and is never split
      final int idx = ((Number) index).intValue();
      if (idx < 0 || idx >= str.codePointCount(0, str.length())) {
        throw new EvaluationError("String index out of bounds: " + idx);
      }
      return new String(Character.toChars(str.codePointAt(str.offsetByCodePoints(0, idx))));
    }

    throw new EvaluationError("Cannot index type: " + operand.getClass().getName());
  }

  // Validates a repetition count against the evaluation limit, before anything is allocated
  private int repetitions(final Number count, final int width) {
    final var repeats = count.longValue();
    if (repeats < 0) {
      throw new EvaluationError("Repetition count must not be negative: " + repeats);
    }
    // Bounding the count first keeps the product below the range of a long
    if (repeats > Utilities.LIMIT || repeats * width > Utilities.LIMIT) {
      throw new EvaluationError("Repetition exceeds the evaluation limit of " + Utilities.LIMIT);
    }
    return (int) repeats;
  }

  // Helper methods for numeric operations
  private Number addNumbers(final Number left, final Number right) {
    if (isDecimal(left) || isDecimal(right)) {
      return left.doubleValue() + right.doubleValue();
    }
    try {
      return Math.addExact(left.longValue(), right.longValue());
    } catch (final ArithmeticException e) {
      throw new EvaluationError("Integer overflow");
    }
  }

  private Number subtractNumbers(final Number left, final Number right) {
    if (isDecimal(left) || isDecimal(right)) {
      return left.doubleValue() - right.doubleValue();
    }
    try {
      return Math.subtractExact(left.longValue(), right.longValue());
    } catch (final ArithmeticException e) {
      throw new EvaluationError("Integer overflow");
    }
  }

  private Number multiplyNumbers(final Number left, final Number right) {
    if (isDecimal(left) || isDecimal(right)) {
      return left.doubleValue() * right.doubleValue();
    }
    try {
      return Math.multiplyExact(left.longValue(), right.longValue());
    } catch (final ArithmeticException e) {
      throw new EvaluationError("Integer overflow");
    }
  }

  // Deep equality checking
  private boolean equals(final Object left, final Object right) {
    return Utilities.equals(left, right);
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

    if (left instanceof Number l && right instanceof Number r) {
      return Utilities.order(l, r);
    } else if (left instanceof String && right instanceof String) {
      return ((String) left).compareTo((String) right);
    } else if (left instanceof Boolean && right instanceof Boolean) {
      return Boolean.compare((Boolean) left, (Boolean) right);
    } else if (left instanceof Instant l && right instanceof Instant r) {
      return l.compareTo(r);
    } else if (left instanceof Duration l && right instanceof Duration r) {
      return l.compareTo(r);
    } else if (left instanceof byte[] l && right instanceof byte[] r) {
      return Arrays.compare(l, r);
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

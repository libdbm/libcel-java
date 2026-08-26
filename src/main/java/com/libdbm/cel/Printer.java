package com.libdbm.cel;

import com.libdbm.cel.ast.Binary;
import com.libdbm.cel.ast.BinaryOp;
import com.libdbm.cel.ast.Call;
import com.libdbm.cel.ast.Comprehension;
import com.libdbm.cel.ast.Conditional;
import com.libdbm.cel.ast.Expression;
import com.libdbm.cel.ast.Identifier;
import com.libdbm.cel.ast.Index;
import com.libdbm.cel.ast.ListExpression;
import com.libdbm.cel.ast.Literal;
import com.libdbm.cel.ast.MapExpression;
import com.libdbm.cel.ast.Select;
import com.libdbm.cel.ast.Struct;
import com.libdbm.cel.ast.Unary;
import com.libdbm.cel.ast.UnaryOp;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders an expression tree back into CEL source text.
 *
 * <p>The compact form is a single line that parses back into an equivalent tree, with parentheses
 * only where operator precedence requires them. The pretty form breaks lists, maps, structs,
 * argument lists and logical chains across lines once they no longer fit within the configured
 * width.
 *
 * <p>Expressions built by the parser always round-trip. A {@link Comprehension}, which the parser
 * never produces, is rendered in a diagnostic form that is not valid CEL.
 *
 * <pre>{@code
 * final var expr = new Parser("a + b * c").parse();
 * Printer.print(expr);                          // "a + b * c"
 * Printer.print(expr, Printer.Options.pretty()); // same, it fits on one line
 * }</pre>
 */
public final class Printer implements Expression.Visitor<String> {
  private static final int CONDITIONAL = 1;
  private static final int MEMBER = 8;
  private static final int PRIMARY = 9;

  private final Options options;
  private int level;

  private Printer(final Options options) {
    this.options = options;
  }

  /**
   * Renders the given expression as a single line of CEL source.
   *
   * @param expr the expression to render
   * @return the rendered text
   */
  public static String print(final Expression expr) {
    return print(expr, Options.compact());
  }

  /**
   * Renders the given expression as CEL source using the given options.
   *
   * @param expr the expression to render
   * @param options the rendering options
   * @return the rendered text
   */
  public static String print(final Expression expr, final Options options) {
    return new Printer(options).text(expr);
  }

  private static int precedence(final Expression expr) {
    if (expr instanceof Conditional) {
      return CONDITIONAL;
    }
    if (expr instanceof Binary binary) {
      return switch (binary.op()) {
        case LOGICAL_OR -> 2;
        case LOGICAL_AND -> 3;
        case EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, IN -> 4;
        case ADD, SUBTRACT -> 5;
        case MULTIPLY, DIVIDE, MODULO -> 6;
      };
    }
    if (expr instanceof Unary) {
      return 7;
    }
    return PRIMARY;
  }

  private static String spelling(final BinaryOp op) {
    return switch (op) {
      case ADD -> "+";
      case SUBTRACT -> "-";
      case MULTIPLY -> "*";
      case DIVIDE -> "/";
      case MODULO -> "%";
      case EQUAL -> "==";
      case NOT_EQUAL -> "!=";
      case LESS -> "<";
      case LESS_EQUAL -> "<=";
      case GREATER -> ">";
      case GREATER_EQUAL -> ">=";
      case LOGICAL_AND -> "&&";
      case LOGICAL_OR -> "||";
      case IN -> "in";
    };
  }

  private static String decimal(final double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException("Cannot render " + value + " as a CEL literal");
    }
    return Double.toString(value);
  }

  // Renders a node without any enclosing parentheses
  private String text(final Expression expr) {
    if (options.wrap()) {
      final var compact = print(expr, Options.compact());
      if (level * options.indent() + compact.length() <= options.width()) {
        return compact;
      }
    }
    return expr.accept(this);
  }

  // Renders a node, adding parentheses when its precedence is below the required level
  private String render(final Expression expr, final int least) {
    final var body = text(expr);
    return precedence(expr) < least ? "(" + body + ")" : body;
  }

  @Override
  public String visitLiteral(final Literal expr) {
    final var value = expr.value();
    return switch (expr.type()) {
      case NULL_VALUE -> "null";
      case BOOL, INT -> String.valueOf(value);
      case UINT -> value + "u";
      case DOUBLE -> decimal(Utilities.asDouble(value));
      case STRING -> Strings.quote(Utilities.asString(value));
      case BYTES -> "b\"" + Strings.escape(Utilities.asBytes(value)) + "\"";
    };
  }

  @Override
  public String visitIdentifier(final Identifier expr) {
    return expr.name();
  }

  @Override
  public String visitSelect(final Select expr) {
    final var operand = expr.operand() == null ? "" : render(expr.operand(), MEMBER);
    final var selection = operand + "." + expr.field();
    return expr.isTest() ? "has(" + selection + ")" : selection;
  }

  @Override
  public String visitCall(final Call expr) {
    final var target = expr.target() == null ? "" : render(expr.target(), MEMBER) + ".";
    final var args = new ArrayList<String>(expr.args().size());
    final var broken = options.wrap();

    level += broken ? 1 : 0;
    for (final var arg : expr.args()) {
      args.add(text(arg));
    }
    level -= broken ? 1 : 0;

    return target + expr.function() + group("(", args, ")");
  }

  @Override
  public String visitList(final ListExpression expr) {
    return group("[", parts(expr.elements()), "]");
  }

  @Override
  public String visitMap(final MapExpression expr) {
    final var entries = new ArrayList<String>(expr.entries().size());
    final var broken = options.wrap();

    level += broken ? 1 : 0;
    for (final var entry : expr.entries()) {
      entries.add(text(entry.key()) + ": " + text(entry.value()));
    }
    level -= broken ? 1 : 0;

    return group("{", entries, "}");
  }

  @Override
  public String visitStruct(final Struct expr) {
    final var fields = new ArrayList<String>(expr.fields().size());
    final var broken = options.wrap();

    level += broken ? 1 : 0;
    for (final var field : expr.fields()) {
      fields.add(field.field() + ": " + text(field.value()));
    }
    level -= broken ? 1 : 0;

    return expr.type() + group("{", fields, "}");
  }

  @Override
  public String visitComprehension(final Comprehension expr) {
    final var parts =
        List.of(
            expr.variable(),
            text(expr.range()),
            expr.accumulator(),
            text(expr.initializer()),
            text(expr.condition()),
            text(expr.step()),
            text(expr.result()));
    return "__comprehension__" + group("(", parts, ")");
  }

  @Override
  public String visitUnary(final Unary expr) {
    final var operator = expr.op() == UnaryOp.NOT ? "!" : "-";
    return operator + render(expr.operand(), 7);
  }

  @Override
  public String visitBinary(final Binary expr) {
    final var order = precedence(expr);
    final var left = render(expr.left(), order);
    final var right = render(expr.right(), order + 1);
    final var operator = spelling(expr.op());

    if (options.wrap()
        && (expr.op() == BinaryOp.LOGICAL_AND || expr.op() == BinaryOp.LOGICAL_OR)) {
      return left + "\n" + pad(level) + operator + " " + right;
    }
    return left + " " + operator + " " + right;
  }

  @Override
  public String visitConditional(final Conditional expr) {
    final var condition = render(expr.condition(), CONDITIONAL + 1);
    final var broken = options.wrap();

    level += broken ? 1 : 0;
    final var then = render(expr.then(), CONDITIONAL + 1);
    final var otherwise = render(expr.otherwise(), CONDITIONAL);
    level -= broken ? 1 : 0;

    if (broken) {
      final var inner = pad(level + 1);
      return condition + "\n" + inner + "? " + then + "\n" + inner + ": " + otherwise;
    }
    return condition + " ? " + then + " : " + otherwise;
  }

  @Override
  public String visitIndex(final Index expr) {
    return render(expr.operand(), MEMBER) + "[" + text(expr.index()) + "]";
  }

  private List<String> parts(final List<Expression> expressions) {
    final var result = new ArrayList<String>(expressions.size());
    final var broken = options.wrap();

    level += broken ? 1 : 0;
    for (final var expr : expressions) {
      result.add(text(expr));
    }
    level -= broken ? 1 : 0;

    return result;
  }

  private String group(final String open, final List<String> parts, final String close) {
    if (!options.wrap() || parts.isEmpty()) {
      return open + String.join(", ", parts) + close;
    }
    final var inner = pad(level + 1);
    return open + "\n" + inner + String.join(",\n" + inner, parts) + "\n" + pad(level) + close;
  }

  private String pad(final int depth) {
    return " ".repeat(depth * options.indent());
  }

  /**
   * Rendering options.
   *
   * @param wrap whether to break long constructs across lines
   * @param indent the number of spaces per indentation level
   * @param width the maximum line width before a construct is broken
   */
  public record Options(boolean wrap, int indent, int width) {
    /**
     * Returns options for a single line rendering.
     *
     * @return the compact options
     */
    public static Options compact() {
      return new Options(false, 0, Integer.MAX_VALUE);
    }

    /**
     * Returns options for a multi-line rendering indented by two spaces at a width of 100.
     *
     * @return the pretty options
     */
    public static Options pretty() {
      return new Options(true, 2, 100);
    }
  }
}

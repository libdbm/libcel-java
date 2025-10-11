package com.libdbm.cel.parser;

import com.libdbm.cel.ast.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Token types for CEL lexical analysis. */
enum TokenType {
  // Literals
  NULL,
  TRUE,
  FALSE,
  INT,
  UINT,
  DOUBLE,
  STRING,
  BYTES,
  IDENTIFIER,

  // Operators
  PLUS,
  MINUS,
  STAR,
  SLASH,
  PERCENT,
  EQ,
  NE,
  LT,
  LE,
  GT,
  GE,
  LOGICAL_AND,
  LOGICAL_OR,
  BANG,
  IN,

  // Delimiters
  LPAREN,
  RPAREN,
  LBRACKET,
  RBRACKET,
  LBRACE,
  RBRACE,
  DOT,
  COMMA,
  COLON,
  QUESTION,

  // Special
  EOF
}

/**
 * Recursive descent parser for CEL (Common Expression Language).
 *
 * <p>Parses CEL expressions into an Abstract Syntax Tree (AST) represented by Expression objects.
 * The parser handles all CEL literal types, operators, function calls, and complex structures.
 */
public class Parser {
  private static final Set<String> MACRO_METHODS =
      Set.of("map", "filter", "all", "exists", "existsOne");

  private final Lexer lexer;
  private Token current;

  /**
   * Constructs a new Parser with the specified input string.
   *
   * @param input the input string to be parsed
   */
  public Parser(final String input) {
    this.lexer = new Lexer(input);
    this.current = lexer.next();
  }

  /**
   * Parses a CEL expression from the input string.
   *
   * @return The parsed Expression AST node
   * @throws ParseError if the input contains syntax errors
   */
  public Expression parse() {
    final var e = parseExpr();
    if (current.type() != TokenType.EOF) {
      throw new ParseError(
          "Unexpected token after expression: " + current.value(),
          current.line(),
          current.column());
    }
    return e;
  }

  // expr = conditionalOr ( '?' conditionalOr ':' expr )?
  private Expression parseExpr() {
    final var condition = parseConditionalOr();

    if (match(TokenType.QUESTION)) {
      final var then = parseConditionalOr();
      expect(TokenType.COLON);
      final var otherwise = parseExpr();
      return new Conditional(condition, then, otherwise);
    }

    return condition;
  }

  // conditionalOr = conditionalAnd ( '||' conditionalAnd )*
  private Expression parseConditionalOr() {
    var left = parseConditionalAnd();

    while (match(TokenType.LOGICAL_OR)) {
      final var right = parseConditionalAnd();
      left = new Binary(BinaryOp.LOGICAL_OR, left, right);
    }

    return left;
  }

  // conditionalAnd = relation ( '&&' relation )*
  private Expression parseConditionalAnd() {
    var left = parseRelation();

    while (match(TokenType.LOGICAL_AND)) {
      final var right = parseRelation();
      left = new Binary(BinaryOp.LOGICAL_AND, left, right);
    }

    return left;
  }

  // relation = addition ( relop addition )*
  // relop = '<=' | '>=' | '!=' | '==' | '<' | '>' | 'in'
  private Expression parseRelation() {
    var left = parseAddition();

    while (isRelationalOp(current.type())) {
      final var op = current.type();
      advance();
      final var right = parseAddition();
      left = new Binary(toBinaryOp(op), left, right);
    }

    return left;
  }

  // addition = multiplication ( ('+' | '-') multiplication )*
  private Expression parseAddition() {
    var left = parseMultiplication();

    while (current.type() == TokenType.PLUS || current.type() == TokenType.MINUS) {
      final var op = current.type();
      advance();
      final var right = parseMultiplication();
      left = new Binary(op == TokenType.PLUS ? BinaryOp.ADD : BinaryOp.SUBTRACT, left, right);
    }

    return left;
  }

  // multiplication = unary ( ('*' | '/' | '%') unary )*
  private Expression parseMultiplication() {
    var left = parseUnary();

    while (current.type() == TokenType.STAR
        || current.type() == TokenType.SLASH
        || current.type() == TokenType.PERCENT) {
      final var op = current.type();
      advance();
      final var right = parseUnary();
      final var operator =
          switch (op) {
            case STAR -> BinaryOp.MULTIPLY;
            case SLASH -> BinaryOp.DIVIDE;
            case PERCENT -> BinaryOp.MODULO;
            default ->
                throw new ParseError(
                    "Unexpected operator: " + op, current.line(), current.column());
          };
      left = new Binary(operator, left, right);
    }

    return left;
  }

  // unary = '!'+ member | '-'+ member | member
  private Expression parseUnary() {
    if (current.type() == TokenType.BANG) {
      advance();
      return new Unary(UnaryOp.NOT, parseUnary());
    } else if (current.type() == TokenType.MINUS) {
      advance();
      return new Unary(UnaryOp.NEGATE, parseUnary());
    }

    return parseMember();
  }

  // member = primary ( selector | index | fieldCall )*
  private Expression parseMember() {
    var expr = parsePrimary();

    while (true) {
      if (current.type() == TokenType.DOT) {
        advance();
        final String field = expectIdentifier();

        // Check if it's a method call or a field selection
        if (current.type() == TokenType.LPAREN) {
          advance();
          final var args = parseExprList();
          expect(TokenType.RPAREN);
          final var isMacro = MACRO_METHODS.contains(field);
          expr = new Call(expr, field, args, isMacro);
        } else {
          expr = new Select(expr, field);
        }
      } else if (current.type() == TokenType.LBRACKET) {
        advance();
        final Expression index = parseExpr();
        expect(TokenType.RBRACKET);
        expr = new Index(expr, index);
      } else {
        break;
      }
    }

    return expr;
  }

  // primary = literal
  //         | ident callArgs?
  //         | listLiteral
  //         | mapLiteral
  //         | structLiteral
  //         | '(' expr ')'
  //         | '.' ident callArgs?
  private Expression parsePrimary() {
    // Check for literals
    if (isLiteralToken(current.type())) {
      return parseLiteral();
    }

    // List literal
    if (current.type() == TokenType.LBRACKET) {
      return parseListLiteral();
    }

    // Map or struct literal
    if (current.type() == TokenType.LBRACE) {
      return parseMapOrStructLiteral(null);
    }

    // Parenthesized expression
    if (current.type() == TokenType.LPAREN) {
      advance();
      final var expr = parseExpr();
      expect(TokenType.RPAREN);
      return expr;
    }

    // Leading dot (e.g., .identifier or .method())
    if (current.type() == TokenType.DOT) {
      advance();
      final var field = expectIdentifier();

      if (current.type() == TokenType.LPAREN) {
        advance();
        final var args = parseExprList();
        expect(TokenType.RPAREN);
        return new Call(null, field, args);
      }

      return new Select(null, field);
    }

    // Identifier (possibly followed by call args or struct literal)
    if (current.type() == TokenType.IDENTIFIER) {
      final var name = current.value();
      advance();

      // Function call
      if (current.type() == TokenType.LPAREN) {
        advance();
        final var args = parseExprList();
        expect(TokenType.RPAREN);
        return new Call(null, name, args);
      }

      // Check for qualified identifier followed by struct literal
      // We need to look ahead to see if this is Type{...} or obj.field
      if (current.type() == TokenType.DOT && isQualifiedStructLiteral()) {
        final var qualified = parseQualifiedIdent(name);
        return parseMapOrStructLiteral(qualified);
      }

      // Check for struct literal with simple type name
      if (current.type() == TokenType.LBRACE) {
        return parseMapOrStructLiteral(name);
      }

      return new Identifier(name);
    }

    throw new ParseError("Unexpected token: " + current.value(), current.line(), current.column());
  }

  // listLiteral = '[' exprList? ','? ']'
  private Expression parseListLiteral() {
    expect(TokenType.LBRACKET);

    final var elements = new ArrayList<Expression>();
    if (current.type() != TokenType.RBRACKET) {
      elements.addAll(parseExprList());
      // Optional trailing comma
      if (current.type() == TokenType.COMMA) {
        advance();
      }
    }

    expect(TokenType.RBRACKET);
    return new ListExpression(elements);
  }

  // mapLiteral or structLiteral
  private Expression parseMapOrStructLiteral(final String type) {
    expect(TokenType.LBRACE);

    if (current.type() == TokenType.RBRACE) {
      advance();
      if (type != null) {
        return new Struct(type, List.of());
      }
      return new MapExpression(List.of());
    }

    // Try to determine if this is a struct (field: expr) or map (expr: expr)
    // We need to look ahead to see if the first item is identifier:expr or expr:expr
    final var isStruct =
        current.type() == TokenType.IDENTIFIER && peekAhead(1).type() == TokenType.COLON;

    if (isStruct || type != null) {
      final var fields = parseFieldInits();
      if (current.type() == TokenType.COMMA) {
        advance();
      }
      expect(TokenType.RBRACE);
      return new Struct(type, fields);
    } else {
      final var entries = parseMapInits();
      if (current.type() == TokenType.COMMA) {
        advance();
      }
      expect(TokenType.RBRACE);
      return new MapExpression(entries);
    }
  }

  // exprList = expr ( ',' expr )*
  private List<Expression> parseExprList() {
    final var expressions = new ArrayList<Expression>();

    if (current.type() == TokenType.RPAREN || current.type() == TokenType.RBRACKET) {
      return expressions;
    }

    expressions.add(parseExpr());

    while (current.type() == TokenType.COMMA) {
      advance();
      if (current.type() == TokenType.RPAREN || current.type() == TokenType.RBRACKET) {
        break;
      }
      expressions.add(parseExpr());
    }

    return expressions;
  }

  // mapInits = mapInit ( ',' mapInit )*
  private List<MapEntry> parseMapInits() {
    final var entries = new ArrayList<MapEntry>();

    entries.add(parseMapInit());
    while (current.type() == TokenType.COMMA) {
      advance();
      if (current.type() == TokenType.RBRACE) {
        break;
      }
      entries.add(parseMapInit());
    }

    return entries;
  }

  // mapInit = expr ':' expr
  private MapEntry parseMapInit() {
    final var key = parseExpr();
    expect(TokenType.COLON);
    final var value = parseExpr();
    return new MapEntry(key, value);
  }

  // fieldInits = fieldInit ( ',' fieldInit )*
  private List<FieldInitializer> parseFieldInits() {
    final var fields = new ArrayList<FieldInitializer>();

    fields.add(parseFieldInit());
    while (current.type() == TokenType.COMMA) {
      advance();
      if (current.type() == TokenType.RBRACE) {
        break;
      }
      fields.add(parseFieldInit());
    }

    return fields;
  }

  // fieldInit = ident ':' expr
  private FieldInitializer parseFieldInit() {
    final var field = expectIdentifier();
    expect(TokenType.COLON);
    final var value = parseExpr();
    return new FieldInitializer(field, value);
  }

  // qualifiedIdent = ident ( '.' ident )*
  private String parseQualifiedIdent(final String first) {
    final var sb = new StringBuilder(first);

    while (current.type() == TokenType.DOT) {
      advance();
      sb.append('.');
      sb.append(expectIdentifier());
    }

    return sb.toString();
  }

  private Expression parseLiteral() {
    final var token = current;
    advance();

    return switch (token.type()) {
      case NULL -> new Literal(null, LiteralType.NULL_VALUE);
      case TRUE -> new Literal(true, LiteralType.BOOL);
      case FALSE -> new Literal(false, LiteralType.BOOL);
      case INT -> new Literal(parseIntLiteral(token.value()), LiteralType.INT);
      case UINT -> new Literal(parseUintLiteral(token.value()), LiteralType.UINT);
      case DOUBLE -> new Literal(Double.parseDouble(token.value()), LiteralType.DOUBLE);
      case STRING -> new Literal(parseStringLiteral(token.value()), LiteralType.STRING);
      case BYTES -> new Literal(parseBytesLiteral(token.value()), LiteralType.BYTES);
      default ->
          throw new ParseError("Not a literal: " + token.value(), token.line(), token.column());
    };
  }

  private long parseIntLiteral(final String value) {
    if (value.startsWith("-0x") || value.startsWith("-0X")) {
      return -Long.parseLong(value.substring(3), 16);
    } else if (value.startsWith("0x") || value.startsWith("0X")) {
      return Long.parseLong(value.substring(2), 16);
    } else {
      return Long.parseLong(value);
    }
  }

  private long parseUintLiteral(final String value) {
    // Remove 'u' or 'U' suffix
    final var number = value.substring(0, value.length() - 1);
    if (number.startsWith("0x") || number.startsWith("0X")) {
      return Long.parseLong(number.substring(2), 16);
    } else {
      return Long.parseLong(number);
    }
  }

  private String parseStringLiteral(final String value) {
    var isRaw = value.startsWith("r") || value.startsWith("R");
    String content;

    if (isRaw
        && (value.substring(1).startsWith("\"\"\"") || value.substring(1).startsWith("'''"))) {
      // Raw triple-quoted string: r"""...""" or r'''...'''
      content = value.substring(4, value.length() - 3);
    } else if (value.startsWith("\"\"\"") || value.startsWith("'''")) {
      // Triple-quoted string: """...""" or '''...'''
      content = value.substring(3, value.length() - 3);
      content = unescapeString(content);
    } else if (isRaw) {
      // Raw string: r"..." or r'...'
      content = value.substring(2, value.length() - 1);
    } else {
      // Regular string: "..." or '...'
      content = value.substring(1, value.length() - 1);
      content = unescapeString(content);
    }

    return content;
  }

  private String parseBytesLiteral(final String value) {
    // Remove b"..." or B'...' wrapper and process escapes
    final var content = value.substring(2, value.length() - 1);
    return unescapeString(content);
  }

  private String unescapeString(final String value) {
    final var result = new StringBuilder();
    int i = 0;

    while (i < value.length()) {
      if (value.charAt(i) == '\\' && i + 1 < value.length()) {
        final var next = value.charAt(i + 1);
        switch (next) {
          case '\\' -> {
            result.append('\\');
            i += 2;
          }
          case '"' -> {
            result.append('"');
            i += 2;
          }
          case '\'' -> {
            result.append('\'');
            i += 2;
          }
          case '`' -> {
            result.append('`');
            i += 2;
          }
          case '?' -> {
            result.append('?');
            i += 2;
          }
          case 'a' -> {
            result.append('\u0007');
            i += 2;
          }
          case 'b' -> {
            result.append('\b');
            i += 2;
          }
          case 'f' -> {
            result.append('\f');
            i += 2;
          }
          case 'n' -> {
            result.append('\n');
            i += 2;
          }
          case 'r' -> {
            result.append('\r');
            i += 2;
          }
          case 't' -> {
            result.append('\t');
            i += 2;
          }
          case 'v' -> {
            result.append('\u000B');
            i += 2;
          }
          case 'x' -> {
            // Hexadecimal escape: backslash-x-HH
            if (i + 3 < value.length()) {
              final var hex = value.substring(i + 2, i + 4);
              result.append((char) Integer.parseInt(hex, 16));
              i += 4;
            } else {
              result.append(value.charAt(i));
              i++;
            }
          }
          case 'u' -> {
            // Unicode escape: backslash-u-HHHH
            if (i + 5 < value.length()) {
              final var hex = value.substring(i + 2, i + 6);
              result.append((char) Integer.parseInt(hex, 16));
              i += 6;
            } else {
              result.append(value.charAt(i));
              i++;
            }
          }
          case 'U' -> {
            // Unicode escape: backslash-U-HHHHHHHH
            if (i + 9 < value.length()) {
              final var hex = value.substring(i + 2, i + 10);
              final var codePoint = Integer.parseInt(hex, 16);
              result.appendCodePoint(codePoint);
              i += 10;
            } else {
              result.append(value.charAt(i));
              i++;
            }
          }
          default -> {
            // Check for octal escape: backslash-[0-3][0-7][0-7]
            if (i + 3 < value.length()
                && next >= '0'
                && next <= '3'
                && value.charAt(i + 2) >= '0'
                && value.charAt(i + 2) <= '7'
                && value.charAt(i + 3) >= '0'
                && value.charAt(i + 3) <= '7') {
              final String octal = value.substring(i + 1, i + 4);
              result.append((char) Integer.parseInt(octal, 8));
              i += 4;
            } else {
              result.append(value.charAt(i));
              i++;
            }
          }
        }
      } else {
        result.append(value.charAt(i));
        i++;
      }
    }

    return result.toString();
  }

  private boolean isLiteralToken(final TokenType type) {
    return type == TokenType.NULL
        || type == TokenType.TRUE
        || type == TokenType.FALSE
        || type == TokenType.INT
        || type == TokenType.UINT
        || type == TokenType.DOUBLE
        || type == TokenType.STRING
        || type == TokenType.BYTES;
  }

  private boolean isRelationalOp(final TokenType type) {
    return type == TokenType.LT
        || type == TokenType.LE
        || type == TokenType.GT
        || type == TokenType.GE
        || type == TokenType.EQ
        || type == TokenType.NE
        || type == TokenType.IN;
  }

  private BinaryOp toBinaryOp(final TokenType type) {
    return switch (type) {
      case LT -> BinaryOp.LESS;
      case LE -> BinaryOp.LESS_EQUAL;
      case GT -> BinaryOp.GREATER;
      case GE -> BinaryOp.GREATER_EQUAL;
      case EQ -> BinaryOp.EQUAL;
      case NE -> BinaryOp.NOT_EQUAL;
      case IN -> BinaryOp.IN;
      default ->
          throw new ParseError(
              "Unknown relational operator: " + type, current.line(), current.column());
    };
  }

  private boolean match(final TokenType type) {
    if (current.type() == type) {
      advance();
      return true;
    }
    return false;
  }

  private void expect(final TokenType type) {
    if (current.type() != type) {
      throw new ParseError(
          "Expected " + type + " but found " + current.type(), current.line(), current.column());
    }
    advance();
  }

  private String expectIdentifier() {
    if (current.type() != TokenType.IDENTIFIER) {
      throw new ParseError(
          "Expected identifier but found " + current.value(), current.line(), current.column());
    }
    final var name = current.value();
    advance();
    return name;
  }

  private void advance() {
    current = lexer.next();
  }

  private Token peekAhead(final int count) {
    return lexer.peek(count);
  }

  // Check if we have a pattern like: . ident ( . ident )* {
  // This distinguishes Type.Name{...} from obj.field
  private boolean isQualifiedStructLiteral() {
    var lookahead = 1;
    Token token = peekAhead(lookahead);

    // Must start with identifier after dot
    if (token.type() != TokenType.IDENTIFIER) {
      return false;
    }

    lookahead++;
    token = peekAhead(lookahead);

    // Skip additional .ident sequences
    while (token.type() == TokenType.DOT) {
      lookahead++;
      token = peekAhead(lookahead);
      if (token.type() != TokenType.IDENTIFIER) {
        return false;
      }
      lookahead++;
      token = peekAhead(lookahead);
    }

    // Must end with {
    return token.type() == TokenType.LBRACE;
  }
}

/** Lexer for tokenizing CEL expressions. */
class Lexer {
  private final String input;
  private final List<Token> lookahead;
  private int position;
  private int line;
  private int column;

  public Lexer(final String input) {
    this.input = input;
    this.position = 0;
    this.line = 1;
    this.column = 1;
    this.lookahead = new ArrayList<>();
  }

  public Token next() {
    if (!lookahead.isEmpty()) {
      return lookahead.remove(0);
    }
    return token();
  }

  public Token peek(final int count) {
    while (lookahead.size() < count) {
      lookahead.add(token());
    }
    return lookahead.get(count - 1);
  }

  private void step() {
    if (position >= input.length()) return;
    var ch = input.charAt(position);
    position++;
    if (ch == '\r') {
      // Handle CRLF as a single newline
      if (position < input.length() && input.charAt(position) == '\n') {
        position++;
      }
      line++;
      column = 1;
    } else if (ch == '\n') {
      line++;
      column = 1;
    } else {
      column++;
    }
  }

  private void forward(int n) {
    for (int i = 0; i < n; i++) {
      step();
    }
  }

  private Token token() {
    whitespace();

    if (position >= input.length()) {
      return new Token(TokenType.EOF, "", line, column);
    }

    final var start = position;
    final var line = this.line;
    final var column = this.column;
    final var ch = input.charAt(position);

    // Single character tokens
    switch (ch) {
      case '(':
        step();
        return new Token(TokenType.LPAREN, "(", line, column);
      case ')':
        step();
        return new Token(TokenType.RPAREN, ")", line, column);
      case '[':
        step();
        return new Token(TokenType.LBRACKET, "[", line, column);
      case ']':
        step();
        return new Token(TokenType.RBRACKET, "]", line, column);
      case '{':
        step();
        return new Token(TokenType.LBRACE, "{", line, column);
      case '}':
        step();
        return new Token(TokenType.RBRACE, "}", line, column);
      case ',':
        step();
        return new Token(TokenType.COMMA, ",", line, column);
      case '.':
        step();
        return new Token(TokenType.DOT, ".", line, column);
      case ':':
        step();
        return new Token(TokenType.COLON, ":", line, column);
      case '?':
        step();
        return new Token(TokenType.QUESTION, "?", line, column);
      case '+':
        step();
        return new Token(TokenType.PLUS, "+", line, column);
      case '*':
        step();
        return new Token(TokenType.STAR, "*", line, column);
      case '/':
        step();
        return new Token(TokenType.SLASH, "/", line, column);
      case '%':
        step();
        return new Token(TokenType.PERCENT, "%", line, column);
    }

    // Multi-character operators
    if (ch == '&' && peekchar() == '&') {
      forward(2);
      return new Token(TokenType.LOGICAL_AND, "&&", line, column);
    }
    if (ch == '|' && peekchar() == '|') {
      forward(2);
      return new Token(TokenType.LOGICAL_OR, "||", line, column);
    }
    if (ch == '=' && peekchar() == '=') {
      forward(2);
      return new Token(TokenType.EQ, "==", line, column);
    }
    if (ch == '!' && peekchar() == '=') {
      forward(2);
      return new Token(TokenType.NE, "!=", line, column);
    }
    if (ch == '<' && peekchar() == '=') {
      forward(2);
      return new Token(TokenType.LE, "<=", line, column);
    }
    if (ch == '>' && peekchar() == '=') {
      forward(2);
      return new Token(TokenType.GE, ">=", line, column);
    }
    if (ch == '<') {
      step();
      return new Token(TokenType.LT, "<", line, column);
    }
    if (ch == '>') {
      step();
      return new Token(TokenType.GT, ">", line, column);
    }
    if (ch == '!') {
      step();
      return new Token(TokenType.BANG, "!", line, column);
    }
    if (ch == '-') {
      step();
      return new Token(TokenType.MINUS, "-", line, column);
    }

    // String literals
    if (ch == '"' || ch == '\'') {
      return string(line, column, start);
    }

    // Raw strings or triple-quoted strings
    if ((ch == 'r' || ch == 'R') && position + 1 < input.length()) {
      final var next = input.charAt(position + 1);
      if (next == '"' || next == '\'') {
        return string(line, column, start);
      }
    }

    // Bytes literals
    if ((ch == 'b' || ch == 'B') && position + 1 < input.length()) {
      final var next = input.charAt(position + 1);
      if (next == '"' || next == '\'') {
        return bytes(line, column, start);
      }
    }

    // Numbers
    if (Character.isDigit(ch)) {
      return number(line, column, start);
    }

    // Identifiers and keywords
    if (Character.isLetter(ch) || ch == '_') {
      return identifier(line, column, start);
    }

    throw new ParseError("Unexpected character: " + ch, line, column);
  }

  private Token string(final int start, final int column, final int position) {
    var isRaw = false;

    // Check for raw prefix
    if (input.charAt(this.position) == 'r' || input.charAt(this.position) == 'R') {
      isRaw = true;
      step();
    }

    // Check for triple-quoted string
    if (this.position + 2 < input.length()) {
      final var prefix = input.substring(this.position, this.position + 3);
      if (prefix.equals("\"\"\"") || prefix.equals("'''")) {
        final char quote = input.charAt(this.position);
        forward(3);

        // Find closing triple quotes
        while (this.position + 2 < input.length()) {
          if (input.charAt(this.position) == quote
              && input.charAt(this.position + 1) == quote
              && input.charAt(this.position + 2) == quote) {
            forward(3);
            return new Token(
                TokenType.STRING, input.substring(position, this.position), start, column);
          }
          step();
        }
        throw new ParseError("Unterminated triple-quoted string", start, column);
      }
    }

    // Regular string
    final var quote = input.charAt(this.position);
    step(); // consume opening quote

    while (this.position < input.length()) {
      final var ch = input.charAt(this.position);
      if (ch == quote) {
        step();
        return new Token(TokenType.STRING, input.substring(position, this.position), start, column);
      }
      if (ch == '\\' && !isRaw && this.position + 1 < input.length()) {
        // Skip escape sequence as two chars
        forward(2);
      } else {
        step();
      }
    }

    throw new ParseError("Unterminated string", start, column);
  }

  private Token bytes(final int line, final int column, final int offset) {
    step(); // Skip 'b' or 'B'
    final var quote = input.charAt(position);
    step();

    while (position < input.length()) {
      final var ch = input.charAt(position);
      if (ch == quote) {
        step();
        return new Token(TokenType.BYTES, input.substring(offset, position), line, column);
      }
      if (ch == '\\' && position + 1 < input.length()) {
        forward(2); // Skip escape sequence
      } else {
        step();
      }
    }

    throw new ParseError("Unterminated bytes literal", line, column);
  }

  private Token number(final int line, final int column, final int offset) {
    // Check for hexadecimal
    if (input.charAt(position) == '0' && position + 1 < input.length()) {
      final var next = input.charAt(position + 1);
      if (next == 'x' || next == 'X') {
        forward(2);
        while (position < input.length() && isHexDigit(input.charAt(position))) {
          step();
        }

        // Check for uint suffix
        if (position < input.length()) {
          final var suffix = input.charAt(position);
          if (suffix == 'u' || suffix == 'U') {
            step();
            return new Token(TokenType.UINT, input.substring(offset, position), line, column);
          }
        }

        return new Token(TokenType.INT, input.substring(offset, position), line, column);
      }
    }

    // Scan digits
    while (position < input.length() && Character.isDigit(input.charAt(position))) {
      step();
    }

    // Check for decimal point or exponent
    var isDouble = false;
    if (position < input.length() && input.charAt(position) == '.') {
      isDouble = true;
      step();
      while (position < input.length() && Character.isDigit(input.charAt(position))) {
        step();
      }
    }

    if (position < input.length()) {
      final var c = input.charAt(position);
      if (c == 'e' || c == 'E') {
        isDouble = true;
        step();
        if (position < input.length()) {
          final var sign = input.charAt(position);
          if (sign == '+' || sign == '-') {
            step();
          }
        }
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
          step();
        }
      }
    }

    // Check for uint suffix
    if (!isDouble && position < input.length()) {
      final var suffix = input.charAt(position);
      if (suffix == 'u' || suffix == 'U') {
        step();
        return new Token(TokenType.UINT, input.substring(offset, position), line, column);
      }
    }

    final TokenType type = isDouble ? TokenType.DOUBLE : TokenType.INT;
    return new Token(type, input.substring(offset, position), line, column);
  }

  private Token identifier(final int line, final int column, final int offset) {
    while (position < input.length()) {
      final var c = input.charAt(position);
      if (!Character.isLetterOrDigit(c) && c != '_') {
        break;
      }
      step();
    }

    final var value = input.substring(offset, position);
    final var type = typeOf(value);

    return new Token(type, value, line, column);
  }

  private TokenType typeOf(final String value) {
    return switch (value) {
      case "null" -> TokenType.NULL;
      case "true" -> TokenType.TRUE;
      case "false" -> TokenType.FALSE;
      case "in" -> TokenType.IN;
      default -> TokenType.IDENTIFIER;
    };
  }

  private void whitespace() {
    while (position < input.length()) {
      final var c = input.charAt(position);
      if (!Character.isWhitespace(c)) {
        break;
      }
      step();
    }
  }

  private char peekchar() {
    final var pos = position + 1;
    if (pos >= input.length()) {
      return '\0';
    }
    return input.charAt(pos);
  }

  private boolean isHexDigit(final char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }
}

/** Token representation for lexical analysis. */
record Token(TokenType type, String value, int line, int column) {

  @Override
  public String toString() {
    return type + "(" + value + ")";
  }
}

package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import com.libdbm.cel.ast.*;
import com.libdbm.cel.parser.ParseError;
import com.libdbm.cel.parser.Parser;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for the Parser class. */
class ParserTests {

  @Test
  void testNullLiteral() {
    final Parser parser = new Parser("null");
    final Expression expr = parser.parse();
    assertInstanceOf(Literal.class, expr);
    final Literal lit = (Literal) expr;
    assertNull(lit.value());
    assertEquals(LiteralType.NULL_VALUE, lit.type());
  }

  @Test
  void testBooleanLiterals() {
    final Parser parser1 = new Parser("true");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Literal.class, expr1);
    assertEquals(true, ((Literal) expr1).value());
    assertEquals(LiteralType.BOOL, ((Literal) expr1).type());

    final Parser parser2 = new Parser("false");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Literal.class, expr2);
    assertEquals(false, ((Literal) expr2).value());
    assertEquals(LiteralType.BOOL, ((Literal) expr2).type());
  }

  @Test
  void testIntLiterals() {
    final Parser parser1 = new Parser("42");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Literal.class, expr1);
    assertEquals(42L, ((Literal) expr1).value());
    assertEquals(LiteralType.INT, ((Literal) expr1).type());

    // Hexadecimal
    final Parser parser2 = new Parser("0xFF");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Literal.class, expr2);
    assertEquals(255L, ((Literal) expr2).value());
    assertEquals(LiteralType.INT, ((Literal) expr2).type());

    // Negative hex
    final Parser parser3 = new Parser("-0x10");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(Unary.class, expr3);
    final Unary unary = (Unary) expr3;
    assertEquals(UnaryOp.NEGATE, unary.op());
    assertInstanceOf(Literal.class, unary.operand());
    assertEquals(16L, ((Literal) unary.operand()).value());
  }

  @Test
  void testUintLiterals() {
    final Parser parser1 = new Parser("42u");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Literal.class, expr1);
    assertEquals(42L, ((Literal) expr1).value());
    assertEquals(LiteralType.UINT, ((Literal) expr1).type());

    final Parser parser2 = new Parser("0xFFu");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Literal.class, expr2);
    assertEquals(255L, ((Literal) expr2).value());
    assertEquals(LiteralType.UINT, ((Literal) expr2).type());
  }

  @Test
  void testDoubleLiterals() {
    final Parser parser1 = new Parser("3.14");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Literal.class, expr1);
    assertEquals(3.14, ((Literal) expr1).value());
    assertEquals(LiteralType.DOUBLE, ((Literal) expr1).type());

    final Parser parser2 = new Parser("1e10");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Literal.class, expr2);
    assertEquals(1e10, ((Literal) expr2).value());
    assertEquals(LiteralType.DOUBLE, ((Literal) expr2).type());

    final Parser parser3 = new Parser("2.5e-3");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(Literal.class, expr3);
    assertEquals(2.5e-3, ((Literal) expr3).value());
    assertEquals(LiteralType.DOUBLE, ((Literal) expr3).type());
  }

  @Test
  void testStringLiterals() {
    final Parser parser1 = new Parser("\"hello\"");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Literal.class, expr1);
    assertEquals("hello", ((Literal) expr1).value());
    assertEquals(LiteralType.STRING, ((Literal) expr1).type());

    final Parser parser2 = new Parser("'world'");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Literal.class, expr2);
    assertEquals("world", ((Literal) expr2).value());
    assertEquals(LiteralType.STRING, ((Literal) expr2).type());
  }

  @Test
  void testStringEscapeSequences() {
    final Parser parser1 = new Parser("\"hello\\nworld\"");
    final Expression expr1 = parser1.parse();
    assertEquals("hello\nworld", ((Literal) expr1).value());

    final Parser parser2 = new Parser("\"tab\\there\"");
    final Expression expr2 = parser2.parse();
    assertEquals("tab\there", ((Literal) expr2).value());

    final Parser parser3 = new Parser("\"quote\\\"here\"");
    final Expression expr3 = parser3.parse();
    assertEquals("quote\"here", ((Literal) expr3).value());

    // Hex escape
    final Parser parser4 = new Parser("\"\\x41\\x42\"");
    final Expression expr4 = parser4.parse();
    assertEquals("AB", ((Literal) expr4).value());

    // Unicode escape
    final Parser parser5 = new Parser("\"\\u0041\\u0042\"");
    final Expression expr5 = parser5.parse();
    assertEquals("AB", ((Literal) expr5).value());
  }

  @Test
  void testRawStrings() {
    final Parser parser1 = new Parser("r\"hello\\nworld\"");
    final Expression expr1 = parser1.parse();
    assertEquals("hello\\nworld", ((Literal) expr1).value());

    final Parser parser2 = new Parser("R'test\\t123'");
    final Expression expr2 = parser2.parse();
    assertEquals("test\\t123", ((Literal) expr2).value());
  }

  @Test
  void testTripleQuotedStrings() {
    final Parser parser1 = new Parser("\"\"\"multi\nline\nstring\"\"\"");
    final Expression expr1 = parser1.parse();
    assertEquals("multi\nline\nstring", ((Literal) expr1).value());

    final Parser parser2 = new Parser("'''another\none'''");
    final Expression expr2 = parser2.parse();
    assertEquals("another\none", ((Literal) expr2).value());

    final Parser parser3 = new Parser("r\"\"\"raw\\nmulti\\nline\"\"\"");
    final Expression expr3 = parser3.parse();
    assertEquals("raw\\nmulti\\nline", ((Literal) expr3).value());
  }

  @Test
  void testBytesLiterals() {
    final Parser parser1 = new Parser("b\"bytes\"");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Literal.class, expr1);
    assertEquals("bytes", ((Literal) expr1).value());
    assertEquals(LiteralType.BYTES, ((Literal) expr1).type());

    final Parser parser2 = new Parser("B'test'");
    final Expression expr2 = parser2.parse();
    assertEquals("test", ((Literal) expr2).value());
    assertEquals(LiteralType.BYTES, ((Literal) expr2).type());
  }

  @Test
  void testIdentifier() {
    final Parser parser = new Parser("myVariable");
    final Expression expr = parser.parse();
    assertInstanceOf(Identifier.class, expr);
    assertEquals("myVariable", ((Identifier) expr).name());
  }

  @Test
  void testBinaryOperators() {
    final Parser parser1 = new Parser("1 + 2");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Binary.class, expr1);
    final Binary bin1 = (Binary) expr1;
    assertEquals(BinaryOp.ADD, bin1.op());
    assertEquals(1L, ((Literal) bin1.left()).value());
    assertEquals(2L, ((Literal) bin1.right()).value());

    final Parser parser2 = new Parser("x * y");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Binary.class, expr2);
    assertEquals(BinaryOp.MULTIPLY, ((Binary) expr2).op());

    final Parser parser3 = new Parser("a == b");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(Binary.class, expr3);
    assertEquals(BinaryOp.EQUAL, ((Binary) expr3).op());
  }

  @Test
  void testOperatorPrecedence() {
    final Parser parser = new Parser("1 + 2 * 3");
    final Expression expr = parser.parse();
    assertInstanceOf(Binary.class, expr);
    final Binary add = (Binary) expr;
    assertEquals(BinaryOp.ADD, add.op());
    assertInstanceOf(Binary.class, add.right());
    final Binary mul = (Binary) add.right();
    assertEquals(BinaryOp.MULTIPLY, mul.op());
  }

  @Test
  void testUnaryOperators() {
    final Parser parser1 = new Parser("!true");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Unary.class, expr1);
    assertEquals(UnaryOp.NOT, ((Unary) expr1).op());

    final Parser parser2 = new Parser("-42");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Unary.class, expr2);
    assertEquals(UnaryOp.NEGATE, ((Unary) expr2).op());

    final Parser parser3 = new Parser("!!value");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(Unary.class, expr3);
    final Unary outer = (Unary) expr3;
    assertInstanceOf(Unary.class, outer.operand());
  }

  @Test
  void testConditional() {
    final Parser parser = new Parser("x > 0 ? 1 : -1");
    final Expression expr = parser.parse();
    assertInstanceOf(Conditional.class, expr);
    final Conditional cond = (Conditional) expr;
    assertInstanceOf(Binary.class, cond.condition());
    assertInstanceOf(Literal.class, cond.then());
    assertInstanceOf(Unary.class, cond.otherwise());
  }

  @Test
  void testListLiteral() {
    final Parser parser1 = new Parser("[]");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(ListExpression.class, expr1);
    assertEquals(0, ((ListExpression) expr1).elements().size());

    final Parser parser2 = new Parser("[1, 2, 3]");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(ListExpression.class, expr2);
    final List<Expression> elements = ((ListExpression) expr2).elements();
    assertEquals(3, elements.size());
    assertEquals(1L, ((Literal) elements.get(0)).value());
    assertEquals(2L, ((Literal) elements.get(1)).value());
    assertEquals(3L, ((Literal) elements.get(2)).value());

    final Parser parser3 = new Parser("[1, 2, 3,]");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(ListExpression.class, expr3);
    assertEquals(3, ((ListExpression) expr3).elements().size());
  }

  @Test
  void testMapLiteral() {
    final Parser parser1 = new Parser("{}");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(MapExpression.class, expr1);
    assertEquals(0, ((MapExpression) expr1).entries().size());

    final Parser parser2 = new Parser("{1: \"one\", 2: \"two\"}");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(MapExpression.class, expr2);
    final List<MapEntry> entries = ((MapExpression) expr2).entries();
    assertEquals(2, entries.size());
    assertEquals(1L, ((Literal) entries.get(0).key()).value());
    assertEquals("one", ((Literal) entries.get(0).value()).value());
  }

  @Test
  void testStructLiteral() {
    final Parser parser1 = new Parser("{name: \"John\", age: 30}");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Struct.class, expr1);
    final Struct struct1 = (Struct) expr1;
    assertNull(struct1.type());
    assertEquals(2, struct1.fields().size());
    assertEquals("name", struct1.fields().get(0).field());
    assertEquals("John", ((Literal) struct1.fields().get(0).value()).value());

    final Parser parser2 = new Parser("Person{name: \"John\", age: 30}");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Struct.class, expr2);
    final Struct struct2 = (Struct) expr2;
    assertEquals("Person", struct2.type());
    assertEquals(2, struct2.fields().size());
  }

  @Test
  void testFieldSelection() {
    final Parser parser = new Parser("obj.field");
    final Expression expr = parser.parse();
    assertInstanceOf(Select.class, expr);
    final Select sel = (Select) expr;
    assertInstanceOf(Identifier.class, sel.operand());
    assertEquals("obj", ((Identifier) sel.operand()).name());
    assertEquals("field", sel.field());
  }

  @Test
  void testIndexing() {
    final Parser parser = new Parser("list[0]");
    final Expression expr = parser.parse();
    assertInstanceOf(Index.class, expr);
    final Index idx = (Index) expr;
    assertInstanceOf(Identifier.class, idx.operand());
    assertEquals("list", ((Identifier) idx.operand()).name());
    assertEquals(0L, ((Literal) idx.index()).value());
  }

  @Test
  void testFunctionCall() {
    final Parser parser1 = new Parser("size(list)");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Call.class, expr1);
    final Call call1 = (Call) expr1;
    assertNull(call1.target());
    assertEquals("size", call1.function());
    assertEquals(1, call1.args().size());
    assertFalse(call1.isMacro());

    final Parser parser2 = new Parser("func()");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Call.class, expr2);
    final Call call2 = (Call) expr2;
    assertEquals(0, call2.args().size());
  }

  @Test
  void testMethodCall() {
    final Parser parser = new Parser("obj.method(arg1, arg2)");
    final Expression expr = parser.parse();
    assertInstanceOf(Call.class, expr);
    final Call call = (Call) expr;
    assertInstanceOf(Identifier.class, call.target());
    assertEquals("obj", ((Identifier) call.target()).name());
    assertEquals("method", call.function());
    assertEquals(2, call.args().size());
  }

  @Test
  void testMacroCall() {
    final Parser parser1 = new Parser("list.map(x, x * 2)");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Call.class, expr1);
    final Call call1 = (Call) expr1;
    assertEquals("map", call1.function());
    assertTrue(call1.isMacro());

    final Parser parser2 = new Parser("list.filter(x, x > 0)");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Call.class, expr2);
    assertTrue(((Call) expr2).isMacro());

    final Parser parser3 = new Parser("list.all(x, x > 0)");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(Call.class, expr3);
    assertTrue(((Call) expr3).isMacro());
  }

  @Test
  void testInOperator() {
    final Parser parser = new Parser("x in list");
    final Expression expr = parser.parse();
    assertInstanceOf(Binary.class, expr);
    final Binary bin = (Binary) expr;
    assertEquals(BinaryOp.IN, bin.op());
    assertEquals("x", ((Identifier) bin.left()).name());
    assertEquals("list", ((Identifier) bin.right()).name());
  }

  @Test
  void testLogicalOperators() {
    final Parser parser1 = new Parser("a && b");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Binary.class, expr1);
    assertEquals(BinaryOp.LOGICAL_AND, ((Binary) expr1).op());

    final Parser parser2 = new Parser("x || y");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Binary.class, expr2);
    assertEquals(BinaryOp.LOGICAL_OR, ((Binary) expr2).op());

    final Parser parser3 = new Parser("a && b || c");
    final Expression expr3 = parser3.parse();
    assertInstanceOf(Binary.class, expr3);
    final Binary or = (Binary) expr3;
    assertEquals(BinaryOp.LOGICAL_OR, or.op());
    assertInstanceOf(Binary.class, or.left());
    assertEquals(BinaryOp.LOGICAL_AND, ((Binary) or.left()).op());
  }

  @Test
  void testParenthesizedExpression() {
    final Parser parser = new Parser("(1 + 2) * 3");
    final Expression expr = parser.parse();
    assertInstanceOf(Binary.class, expr);
    final Binary mul = (Binary) expr;
    assertEquals(BinaryOp.MULTIPLY, mul.op());
    assertInstanceOf(Binary.class, mul.left());
    final Binary add = (Binary) mul.left();
    assertEquals(BinaryOp.ADD, add.op());
  }

  @Test
  void testComplexExpression() {
    final Parser parser =
        new Parser("user.age > 18 && user.name == \"John\" ? \"adult\" : \"minor\"");
    final Expression expr = parser.parse();
    assertInstanceOf(Conditional.class, expr);
    final Conditional cond = (Conditional) expr;
    assertInstanceOf(Binary.class, cond.condition());
    assertEquals(BinaryOp.LOGICAL_AND, ((Binary) cond.condition()).op());
  }

  @Test
  void testLeadingDot() {
    final Parser parser1 = new Parser(".field");
    final Expression expr1 = parser1.parse();
    assertInstanceOf(Select.class, expr1);
    final Select sel = (Select) expr1;
    assertNull(sel.operand());
    assertEquals("field", sel.field());

    final Parser parser2 = new Parser(".method()");
    final Expression expr2 = parser2.parse();
    assertInstanceOf(Call.class, expr2);
    final Call call = (Call) expr2;
    assertNull(call.target());
    assertEquals("method", call.function());
  }

  @Test
  void testParseError() {
    assertThrows(
        ParseError.class,
        () -> {
          final Parser parser = new Parser("1 +");
          parser.parse();
        });

    assertThrows(
        ParseError.class,
        () -> {
          final Parser parser = new Parser("\"unterminated");
          parser.parse();
        });

    assertThrows(
        ParseError.class,
        () -> {
          final Parser parser = new Parser("@invalid");
          parser.parse();
        });
  }

  @Test
  void testQualifiedIdentifier() {
    final Parser parser = new Parser("com.example.Type{field: 1}");
    final Expression expr = parser.parse();
    assertInstanceOf(Struct.class, expr);
    final Struct struct = (Struct) expr;
    assertEquals("com.example.Type", struct.type());
    assertEquals(1, struct.fields().size());
  }

  @Test
  void testChainedMemberAccess() {
    final Parser parser = new Parser("obj.field1.field2.method()");
    final Expression expr = parser.parse();
    assertInstanceOf(Call.class, expr);
    final Call call = (Call) expr;
    assertInstanceOf(Select.class, call.target());
    final Select sel2 = (Select) call.target();
    assertEquals("field2", sel2.field());
    assertInstanceOf(Select.class, sel2.operand());
    final Select sel1 = (Select) sel2.operand();
    assertEquals("field1", sel1.field());
    assertInstanceOf(Identifier.class, sel1.operand());
    assertEquals("obj", ((Identifier) sel1.operand()).name());
  }

  @Test
  void testErrorReportsLineAndColumn() {
    // Unterminated string on line 3, column 4
    String src1 = "\n\n   \"unterminated";
    ParseError e1 = assertThrows(ParseError.class, () -> new Parser(src1).parse());
    assertTrue(e1.getMessage().contains("Unterminated string"));
    assertEquals(3, e1.line());
    assertEquals(4, e1.column());

    // Unexpected character after leading spaces on line 1, column 3
    String src2 = "  @oops";
    ParseError e2 = assertThrows(ParseError.class, () -> new Parser(src2).parse());
    assertTrue(e2.getMessage().contains("Unexpected character"));
    assertEquals(1, e2.line());
    assertEquals(3, e2.column());

    // CRLF should count as a single newline; error '@' at line 3, column 1
    String src3 = "\r\n\t\t1 +\r\n@";
    ParseError e3 = assertThrows(ParseError.class, () -> new Parser(src3).parse());
    assertTrue(e3.getMessage().contains("Unexpected character"));
    assertEquals(3, e3.line());
    assertEquals(1, e3.column());
  }
}

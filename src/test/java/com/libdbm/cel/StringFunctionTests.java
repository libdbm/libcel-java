package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for the string, regex and encoding functions. */
class StringFunctionTests {
  private CEL cel;

  @BeforeEach
  void setup() {
    cel = new CEL();
  }

  private Object eval(final String source) {
    return cel.eval(source, Map.of());
  }

  @Test
  void testCharAt() {
    assertEquals("e", eval("'hello'.charAt(1)"));
    assertEquals("", eval("'hello'.charAt(5)"));
    assertThrows(IllegalArgumentException.class, () -> eval("'hello'.charAt(6)"));
  }

  @Test
  void testIndexOf() {
    assertEquals(2L, eval("'hello'.indexOf('l')"));
    assertEquals(3L, eval("'hello'.indexOf('l', 3)"));
    assertEquals(-1L, eval("'hello'.indexOf('z')"));
    assertEquals(3L, eval("'hello'.lastIndexOf('l')"));
    assertEquals(2L, eval("'hello'.lastIndexOf('l', 2)"));
  }

  @Test
  void testIndexesCountCodePoints() {
    final var grin = "\uD83D\uDE00";
    assertEquals(grin, eval("'a\\U0001F600b'.charAt(1)"));
    assertEquals(2L, eval("'a\\U0001F600b'.indexOf('b')"));
    assertEquals(2L, eval("'a\\U0001F600b'.lastIndexOf('b')"));
    assertEquals(grin, eval("'a\\U0001F600b'.substring(1, 2)"));
    assertEquals(2L, eval("size('a\\U0001F600')"));
  }

  @Test
  void testCaseConversion() {
    assertEquals("HELLO", eval("'hello'.upperAscii()"));
    assertEquals("hello", eval("'HELLO'.lowerAscii()"));
    assertEquals("cafÉ", eval("'CAFÉ'.lowerAscii()"), "lowerAscii leaves non-ASCII untouched");
  }

  @Test
  void testSubstringAndReverse() {
    assertEquals("ell", eval("'hello'.substring(1, 4)"));
    assertEquals("ello", eval("'hello'.substring(1)"));
    assertEquals("olleh", eval("'hello'.reverse()"));
    assertThrows(IllegalArgumentException.class, () -> eval("'hello'.substring(3, 1)"));
  }

  @Test
  void testReplaceWithLimit() {
    assertEquals("bbb", eval("'aaa'.replace('a', 'b')"));
    assertEquals("bba", eval("'aaa'.replace('a', 'b', 2)"));
    assertEquals("aaa", eval("'aaa'.replace('a', 'b', 0)"));
  }

  @Test
  void testSplitWithLimit() {
    assertEquals(List.of("a", "b", "c"), eval("'a,b,c'.split(',')"));
    assertEquals(List.of("a", "b,c"), eval("'a,b,c'.split(',', 2)"));
    assertEquals(List.of(), eval("'a,b,c'.split(',', 0)"));
  }

  @Test
  void testJoin() {
    assertEquals("abc", eval("['a', 'b', 'c'].join()"));
    assertEquals("a-b-c", eval("['a', 'b', 'c'].join('-')"));
    assertEquals("", eval("[].join(',')"));
  }

  @Test
  void testFormat() {
    assertEquals("3 apples", eval("'%d apples'.format([3])"));
    assertEquals("50% of 2.50", eval("'%d%% of %.2f'.format([50, 2.5])"));
    assertEquals("a=x b=true", eval("'a=%s b=%s'.format(['x', true])"));
    assertEquals("ff FF 777 1010", eval("'%x %X %o %b'.format([255, 255, 511, 10])"));
    assertEquals("1.500000e+03", eval("'%e'.format([1500.0])"));
    assertThrows(IllegalArgumentException.class, () -> eval("'%q'.format(['x'])"));
    assertThrows(IllegalArgumentException.class, () -> eval("'%d'.format([1, 2])"));
  }

  @Test
  void testQuote() {
    assertEquals("\"a\\nb\"", eval("strings.quote('a\\nb')"));
    assertEquals("\"plain\"", eval("strings.quote('plain')"));
  }

  @Test
  void testMatchesUsesSearchSemantics() {
    assertEquals(true, eval("'hello world'.matches('o w')"));
    assertEquals(true, eval("matches('hello world', 'o w')"));
    assertEquals(false, eval("'hello'.matches('^ello')"));
  }

  @Test
  void testRegexReplace() {
    assertEquals("a-b-c", eval("regex.replace('a b c', ' ', '-')"));
    assertEquals("a-b c", eval("regex.replace('a b c', ' ', '-', 1)"));
    assertEquals("[a][b]", eval("regex.replace('ab', '(\\\\w)', '[\\\\1]')"));
    assertThrows(
        IllegalArgumentException.class, () -> eval("regex.replace('ab', '(\\\\w)', '\\\\2')"));
  }

  @Test
  void testRegexExtract() {
    assertEquals("123", eval("regex.extract('id 123 x', '[0-9]+')"));
    assertEquals("123", eval("regex.extract('id=123', '=([0-9]+)')"));
    assertNull(eval("regex.extract('none', '[0-9]+')"));
    assertEquals(List.of("1", "2"), eval("regex.extractAll('a1b2', '[0-9]')"));
    assertEquals(List.of(), eval("regex.extractAll('abc', '[0-9]')"));
  }

  @Test
  void testBase64() {
    assertEquals("aGVsbG8=", eval("base64.encode(b'hello')"));
    assertEquals("aGVsbG8=", eval("base64.encode('hello')"));
    assertEquals("hello", eval("string(base64.decode('aGVsbG8='))"));
    assertThrows(IllegalArgumentException.class, () -> eval("base64.decode('!!!')"));
  }

  @Test
  void testBytes() {
    assertEquals(5L, eval("size(b'hello')"));
    assertEquals(Type.BYTES, eval("type(b'hello')"));
    assertEquals(true, eval("b'ab' == b'ab'"));
    assertEquals(false, eval("b'ab' == b'ba'"));
    assertEquals("abcd", eval("string(b'ab' + b'cd')"));
    assertEquals(97L, eval("b'ab'[0]"));
    assertEquals("aGk=", eval("base64.encode(bytes('hi'))"));
  }

  @Test
  void testNamespaceIsShadowedByVariables() {
    assertEquals(2L, cel.eval("math.greatest(1, 2)", Map.of()));
    assertEquals(
        "shadowed", cel.eval("math.greatest", Map.of("math", Map.of("greatest", "shadowed"))));
  }
}

package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import com.libdbm.cel.parser.ParseError;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Conformance tests organized after the sections of the google/cel-spec test corpus.
 *
 * <p>Every case evaluates through the public {@link CEL} boundary. The final group records the
 * places where this implementation deliberately differs from the specification.
 */
class ConformanceTests {

  private static Object eval(final String source) {
    return new CEL().eval(source, Map.of());
  }

  private static Object eval(final String source, final Map<String, Object> variables) {
    return new CEL().eval(source, variables);
  }

  @Nested
  class Basic {
    @Test
    void testSelfEvaluatingLiterals() {
      assertNull(eval("null"));
      assertEquals(true, eval("true"));
      assertEquals(false, eval("false"));
      assertEquals(42L, eval("42"));
      assertEquals(3.14, eval("3.14"));
      assertEquals("hello", eval("'hello'"));
      assertEquals(List.of(), eval("[]"));
      assertEquals(Map.of(), eval("{}"));
    }

    @Test
    void testParenthesesAndPrecedence() {
      assertEquals(14L, eval("2 + 3 * 4"));
      assertEquals(20L, eval("(2 + 3) * 4"));
      assertEquals(true, eval("1 < 2 == true"));
    }
  }

  @Nested
  class Variables {
    @Test
    void testResolvesBoundNames() {
      assertEquals(7L, eval("x + 2", Map.of("x", 5L)));
      assertEquals("ab", eval("a + b", Map.of("a", "a", "b", "b")));
    }

    @Test
    void testUnboundNameIsAnError() {
      assertThrows(EvaluationError.class, () -> eval("missing"));
    }

    @Test
    void testSelectsNestedFields() {
      final Map<String, Object> data = Map.of("a", Map.of("b", Map.of("c", 1L)));
      assertEquals(1L, eval("a.b.c", data));
      assertEquals(1L, eval("a['b']['c']", data));
    }
  }

  @Nested
  class Comparisons {
    @Test
    void testOrdering() {
      assertEquals(true, eval("1 < 2"));
      assertEquals(true, eval("2 <= 2"));
      assertEquals(true, eval("3 > 2"));
      assertEquals(true, eval("3 >= 3"));
      assertEquals(true, eval("'a' < 'b'"));
    }

    @Test
    void testEqualityAcrossNumericTypes() {
      assertEquals(true, eval("1 == 1.0"));
      assertEquals(true, eval("1 != 2"));
      assertEquals(true, eval("2.0 > 1"));
    }

    @Test
    void testMixedNumbersCompareExactly() {
      // Converting both to a double would call these equal
      assertEquals(false, eval("9223372036854775807 == 9223372036854775808.0"));
      assertEquals(true, eval("9223372036854775807 < 9223372036854775808.0"));
      assertEquals(false, eval("9007199254740993 == 9007199254740992.0"));
      assertEquals(true, eval("9007199254740993 > 9007199254740992.0"));
      assertEquals(true, eval("9007199254740992 == 9007199254740992.0"));
      assertEquals(true, eval("1 == 1.0"));
      assertEquals(true, eval("1 < 1.5"));
      assertEquals(false, eval("9223372036854775807 in [9223372036854775808.0]"));
    }

    @Test
    void testNotANumberEqualsNothing() {
      assertEquals(false, eval("0.0 / 0.0 == 0.0 / 0.0"));
      assertEquals(true, eval("0.0 / 0.0 != 0.0 / 0.0"));
    }

    @Test
    void testDeepEquality() {
      assertEquals(true, eval("[1, [2, 3]] == [1, [2, 3]]"));
      assertEquals(true, eval("{'a': [1]} == {'a': [1]}"));
      assertEquals(false, eval("[1, 2] == [2, 1]"));
      assertEquals(true, eval("b'ab' == b'ab'"));
    }

    @Test
    void testSignedZeroIsOneValueEverywhere() {
      assertEquals(true, eval("-0.0 == 0.0"));
      assertEquals(true, eval("[[-0.0]] == [[0.0]]"));
      assertEquals(true, eval("x == {0.0: 'z'}", Map.of("x", Map.of(-0.0, "z"))));
      // The hash-backed operations must agree with equality, not with Double.hashCode
      assertEquals(1, ((List<?>) eval("[-0.0, 0.0].distinct()")).size());
      assertEquals(true, eval("sets.contains([-0.0], [0.0])"));
      assertEquals(true, eval("sets.equivalent([-0.0], [0.0])"));
      assertEquals(true, eval("0.0 in [-0.0]"));
    }

    @Test
    void testNullComparison() {
      assertEquals(true, eval("null == null"));
      assertEquals(true, eval("null != 1"));
    }
  }

  @Nested
  class Arithmetic {
    @Test
    void testIntegerDivisionTruncates() {
      assertEquals(0L, eval("1 / 2"));
      assertEquals(2L, eval("5 / 2"));
      assertEquals(-2L, eval("-5 / 2"));
      assertEquals(5L, eval("15 / 3"));
    }

    @Test
    void testIntegerDivisionByZeroIsAnError() {
      assertThrows(EvaluationError.class, () -> eval("1 / 0"));
      assertThrows(EvaluationError.class, () -> eval("1 % 0"));
    }

    @Test
    void testDoubleDivisionYieldsInfinity() {
      assertEquals(Double.POSITIVE_INFINITY, eval("1.0 / 0.0"));
      assertEquals(Double.NEGATIVE_INFINITY, eval("-1.0 / 0.0"));
      assertEquals(true, eval("math.isNaN(0.0 / 0.0)"));
      assertEquals(0.5, eval("1.0 / 2"));
    }

    @Test
    void testModuloRequiresIntegers() {
      assertEquals(1L, eval("5 % 2"));
      assertEquals(2L, eval("17 % 5"));
      assertThrows(EvaluationError.class, () -> eval("5.0 % 2"));
      assertThrows(EvaluationError.class, () -> eval("5 % 2.0"));
    }

    @Test
    void testIntegerOverflowIsAnError() {
      assertThrows(EvaluationError.class, () -> eval("9223372036854775807 + 1"));
      assertThrows(EvaluationError.class, () -> eval("-9223372036854775807 - 2"));
      assertThrows(EvaluationError.class, () -> eval("9223372036854775807 * 2"));
      assertThrows(EvaluationError.class, () -> eval("(-9223372036854775807 - 1) / -1"));
    }

    @Test
    void testAbsoluteValueOfTheMinimumIntegerIsAnError() {
      assertEquals(5L, eval("math.abs(-5)"));
      assertEquals(5.5, eval("math.abs(-5.5)"));
      assertThrows(EvaluationError.class, () -> eval("math.abs(-9223372036854775807 - 1)"));
    }

    @Test
    void testDoubleArithmeticDoesNotOverflow() {
      assertEquals(true, eval("math.isInf(1.7976931348623157e308 * 2.0)"));
    }

    @Test
    void testNegation() {
      assertEquals(-5L, eval("-5"));
      assertEquals(5L, eval("--5"));
      assertEquals(-2.5, eval("-2.5"));
    }
  }

  @Nested
  class Logic {
    @Test
    void testShortCircuits() {
      assertEquals(false, eval("false && missing"));
      assertEquals(true, eval("true || missing"));
    }

    @Test
    void testAbsorbsErrorsFromEitherSide() {
      assertEquals(false, eval("missing && false"));
      assertEquals(true, eval("missing || true"));
    }

    @Test
    void testPropagatesUnabsorbedErrors() {
      assertThrows(EvaluationError.class, () -> eval("missing && true"));
      assertThrows(EvaluationError.class, () -> eval("missing || false"));
    }

    @Test
    void testRequiresBooleanOperands() {
      assertThrows(EvaluationError.class, () -> eval("1 && true"));
      assertThrows(EvaluationError.class, () -> eval("false || 'x'"));
      // A short-circuiting operand still wins over a non-boolean on the other side
      assertEquals(true, eval("true || 'x'"));
      assertEquals(false, eval("false && 1"));
      assertThrows(EvaluationError.class, () -> eval("!1"));
    }

    @Test
    void testConditionalRequiresABooleanCondition() {
      assertEquals(1L, eval("true ? 1 : 2"));
      assertEquals(2L, eval("false ? 1 : 2"));
      assertThrows(EvaluationError.class, () -> eval("1 ? 1 : 2"));
      assertThrows(EvaluationError.class, () -> eval("'x' ? 1 : 2"));
    }

    @Test
    void testConditionalEvaluatesOneBranchOnly() {
      assertEquals(1L, eval("true ? 1 : missing"));
      assertEquals(2L, eval("false ? missing : 2"));
    }
  }

  @Nested
  class Conversions {
    @Test
    void testNumericConversions() {
      assertEquals(42L, eval("int('42')"));
      assertEquals(1L, eval("int(1.9)"));
      assertEquals(42.0, eval("double(42)"));
      assertEquals("42", eval("string(42)"));
      assertEquals(1L, eval("uint(1)"));
    }

    @Test
    void testStringAndBytesConversions() {
      assertEquals("hello", eval("string(b'hello')"));
      assertEquals(true, eval("bytes('hello') == b'hello'"));
      assertEquals(true, eval("bool('x')"));
    }

    @Test
    void testTimestampConversions() {
      assertEquals(Instant.ofEpochSecond(1), eval("timestamp(1)"));
      assertEquals(1L, eval("int(timestamp(1))"));
      assertEquals(
          Instant.parse("2024-01-01T00:00:00Z"), eval("timestamp('2024-01-01T00:00:00Z')"));
      assertEquals(
          Instant.parse("2024-01-01T00:00:00Z"), eval("timestamp('2024-01-01T01:00:00+01:00')"));
      assertEquals("2024-01-01T00:00:00Z", eval("string(timestamp('2024-01-01T00:00:00Z'))"));
    }

    @Test
    void testDurationConversions() {
      assertEquals(Duration.ofHours(1), eval("duration('1h')"));
      assertEquals("3600s", eval("string(duration('1h'))"));
      assertEquals("1.5s", eval("string(duration('1.5s'))"));
      assertEquals("-90s", eval("string(duration('-1m30s'))"));
      assertEquals("0s", eval("string(duration('0s'))"));
    }

    @Test
    void testLargeDurationsKeepTheirValue() {
      // Accumulating through a double would silently clamp this to 9223372036 seconds
      assertEquals(Duration.ofHours(10000000), eval("duration('10000000h')"));
      assertEquals("36000000000s", eval("string(duration('10000000h'))"));
      assertEquals(Duration.ofNanos(1), eval("duration('0.000000001s')"));
      assertEquals("0.000000001s", eval("string(duration('0.000000001s'))"));
    }

    @Test
    void testOutOfRangeTimesAreRejected() {
      assertThrows(IllegalArgumentException.class, () -> eval("duration('100000000h')"));
      assertThrows(IllegalArgumentException.class, () -> eval("timestamp(9223372036854775807)"));
    }

    @Test
    void testTypeValues() {
      assertEquals(true, eval("type(null) == null_type"));
      assertEquals(true, eval("type(true) == bool"));
      assertEquals(true, eval("type(1) == int"));
      assertEquals(true, eval("type(1.0) == double"));
      assertEquals(true, eval("type('a') == string"));
      assertEquals(true, eval("type(b'a') == bytes"));
      assertEquals(true, eval("type([1]) == list"));
      assertEquals(true, eval("type({'a': 1}) == map"));
      assertEquals(true, eval("type(timestamp(0)) == timestamp"));
      assertEquals(true, eval("type(duration('1s')) == duration"));
      assertEquals(true, eval("type(type(1)) == type"));
      assertEquals(false, eval("type(1) == double"));
      assertEquals("int", eval("string(type(1))"));
    }

    @Test
    void testTypeNamesAreShadowedByVariables() {
      assertEquals(7L, eval("int", Map.of("int", 7L)));
    }
  }

  @Nested
  class Lists {
    @Test
    void testIndexingAndSize() {
      assertEquals(1L, eval("[1, 2, 3][0]"));
      assertEquals(3L, eval("size([1, 2, 3])"));
      assertThrows(EvaluationError.class, () -> eval("[1][5]"));
      assertThrows(EvaluationError.class, () -> eval("[1][-1]"));
    }

    @Test
    void testConcatenation() {
      assertEquals(List.of(1L, 2L, 3L), eval("[1] + [2, 3]"));
      assertEquals(List.of(1L), eval("[1] + []"));
    }

    @Test
    void testMembership() {
      assertEquals(true, eval("2 in [1, 2, 3]"));
      assertEquals(false, eval("4 in [1, 2, 3]"));
      assertEquals(true, eval("[1] in [[1], [2]]"));
    }
  }

  @Nested
  class Maps {
    @Test
    void testAccessAndSize() {
      assertEquals(1L, eval("{'a': 1}['a']"));
      assertEquals(1L, eval("{'a': 1}.a"));
      assertEquals(2L, eval("size({'a': 1, 'b': 2})"));
      assertThrows(EvaluationError.class, () -> eval("{'a': 1}['b']"));
    }

    @Test
    void testMembershipMatchesKeysNumerically() {
      assertEquals(true, eval("'a' in {'a': 1}"));
      assertEquals(false, eval("'b' in {'a': 1}"));
      assertEquals(true, eval("1 in x", Map.of("x", Map.of(1, "one"))));
    }

    @Test
    void testMembershipIndexingAndPresenceAgree() {
      // The key is an Integer while the expression probes with a Long
      final Map<String, Object> data = Map.of("x", Map.of(1, "one"));
      assertEquals(true, eval("1 in x", data));
      assertEquals("one", eval("x[1]", data));
      assertEquals(true, eval("has(x, 1)", data));
      assertEquals("one", eval("1 in x ? x[1] : 'fallback'", data));
      assertEquals("fallback", eval("2 in x ? x[2] : 'fallback'", data));
      assertThrows(EvaluationError.class, () -> eval("x[2]", data));
    }

    @Test
    void testEqualityMatchesKeysAcrossNumberTypes() {
      // The bound map holds an Integer key while the literal map holds a Long
      final Map<String, Object> data = Map.of("x", Map.of(1, "one"));
      assertEquals(true, eval("x == {1: 'one'}", data));
      assertEquals(false, eval("x == {1: 'two'}", data));
      assertEquals(false, eval("x == {2: 'one'}", data));
      assertEquals(true, eval("[x] == [{1: 'one'}]", data));
    }

    @Test
    void testNullKeyProbeIsAnOrdinaryMiss() {
      // An immutable map rejects a null probe, which must not surface as a NullPointerException
      final Map<String, Object> data = Map.of("x", Map.of("a", 1L));
      assertEquals(false, eval("null in x", data));
      assertEquals(false, eval("has(x, null)", data));
      assertThrows(EvaluationError.class, () -> eval("x[null]", data));
    }
  }

  @Nested
  class Macros {
    @Test
    void testHasTestsPresenceWithoutErroring() {
      final Map<String, Object> data = Map.of("a", Map.of("b", 1L));
      assertEquals(true, eval("has(a.b)", data));
      assertEquals(false, eval("has(a.c)", data));
      assertEquals(true, eval("has(a.b) && a.b == 1", data));
    }

    @Test
    void testHasRequiresAFieldSelection() {
      assertThrows(ParseError.class, () -> eval("has(1)", Map.of("a", Map.of())));
    }

    @Test
    void testComprehensions() {
      assertEquals(List.of(2L, 4L), eval("[1, 2].map(x, x * 2)"));
      assertEquals(List.of(2L), eval("[1, 2].filter(x, x % 2 == 0)"));
      assertEquals(true, eval("[1, 2].all(x, x > 0)"));
      assertEquals(true, eval("[1, 2].exists(x, x > 1)"));
      assertEquals(true, eval("[1, 2].existsOne(x, x > 1)"));
    }

    @Test
    void testComprehensionPredicatesMustBeBoolean() {
      assertThrows(EvaluationError.class, () -> eval("[1, 2].filter(x, x)"));
      assertThrows(EvaluationError.class, () -> eval("[1, 2].all(x, x)"));
    }
  }

  @Nested
  class StringFunctions {
    @Test
    void testStandardMethods() {
      assertEquals(true, eval("'hello'.contains('ell')"));
      assertEquals(true, eval("'hello'.startsWith('he')"));
      assertEquals(true, eval("'hello'.endsWith('lo')"));
      assertEquals(5L, eval("size('hello')"));
      assertEquals("HELLO", eval("'hello'.upperAscii()"));
    }

    @Test
    void testMatchesIsAPartialMatch() {
      assertEquals(true, eval("'hello'.matches('ell')"));
      assertEquals(true, eval("matches('hello', 'ell')"));
      assertEquals(false, eval("'hello'.matches('^ell')"));
    }

    @Test
    void testConcatenation() {
      assertEquals("ab", eval("'a' + 'b'"));
      assertEquals(true, eval("b'a' + b'b' == b'ab'"));
    }
  }

  @Nested
  class Timestamps {
    @Test
    void testAccessorsDefaultToUtc() {
      assertEquals(2024L, eval("timestamp('2024-03-05T14:30:45Z').getFullYear()"));
      assertEquals(14L, eval("timestamp('2024-03-05T14:30:45Z').getHours()"));
      assertEquals(9L, eval("timestamp('2024-03-05T14:30:45Z').getHours('America/New_York')"));
    }

    @Test
    void testArithmeticAndOrdering() {
      assertEquals(
          Instant.parse("2024-01-01T01:00:00Z"),
          eval("timestamp('2024-01-01T00:00:00Z') + duration('1h')"));
      assertEquals(
          Duration.ofHours(1),
          eval("timestamp('2024-01-01T01:00:00Z') - timestamp('2024-01-01T00:00:00Z')"));
      assertEquals(true, eval("timestamp(0) < timestamp(1)"));
      assertEquals(true, eval("duration('1s') < duration('1m')"));
    }
  }

  /** Positions and sizes are counted in Unicode code points, never UTF-16 code units. */
  @Nested
  class Unicode {
    private static final String GRIN = "\uD83D\uDE00";

    @Test
    void testSizeCountsCodePoints() {
      assertEquals(1L, eval("size('\\U0001F600')"));
      assertEquals(2L, eval("size('\\U0001F600b')"));
      assertEquals(4L, eval("size('café')"));
    }

    @Test
    void testCharAtAndIndexingReturnWholeCharacters() {
      assertEquals(GRIN, eval("'\\U0001F600b'.charAt(0)"));
      assertEquals("b", eval("'\\U0001F600b'.charAt(1)"));
      assertEquals(GRIN, eval("'\\U0001F600b'[0]"));
      assertEquals("b", eval("'\\U0001F600b'[1]"));
      assertEquals("", eval("'\\U0001F600b'.charAt(2)"));
      assertThrows(IllegalArgumentException.class, () -> eval("'\\U0001F600b'.charAt(3)"));
      assertThrows(EvaluationError.class, () -> eval("'\\U0001F600b'[2]"));
    }

    @Test
    void testSubstringDoesNotSplitACharacter() {
      assertEquals(GRIN, eval("'a\\U0001F600b'.substring(1, 2)"));
      assertEquals(GRIN + "b", eval("'a\\U0001F600b'.substring(1)"));
      assertEquals("b", eval("'a\\U0001F600b'.substring(2)"));
      assertEquals("", eval("'a\\U0001F600b'.substring(1, 1)"));
    }

    @Test
    void testSearchPositionsAreCodePoints() {
      assertEquals(2L, eval("'a\\U0001F600b'.indexOf('b')"));
      assertEquals(2L, eval("'a\\U0001F600b'.lastIndexOf('b')"));
      assertEquals(1L, eval("'a\\U0001F600b'.indexOf('\\U0001F600')"));
      assertEquals(-1L, eval("'a\\U0001F600b'.indexOf('z')"));
      assertEquals(2L, eval("'a\\U0001F600b'.indexOf('b', 1)"));
    }

    @Test
    void testSurvivesTheOtherStringFunctions() {
      assertEquals(GRIN, eval("'\\U0001F600'.reverse()"));
      assertEquals("b" + GRIN + "a", eval("'a\\U0001F600b'.reverse()"));
      assertEquals(GRIN, eval("'\\U0001F600'.upperAscii()"));
      assertEquals(GRIN, eval("string(bytes('\\U0001F600'))"));
    }
  }

  /** Guards that keep an untrusted expression from exhausting the heap. */
  @Nested
  class Limits {
    @Test
    void testRangeIsBounded() {
      assertEquals(List.of(0L, 1L, 2L), eval("lists.range(3)"));
      assertEquals(1000000L, eval("size(lists.range(1000000))"));
      assertThrows(EvaluationError.class, () -> eval("lists.range(9223372036854775807)"));
      assertThrows(EvaluationError.class, () -> eval("lists.range(1000001)"));
    }

    @Test
    void testStringRepetitionIsBounded() {
      assertEquals("ababab", eval("'ab' * 3"));
      assertThrows(EvaluationError.class, () -> eval("'ab' * 9223372036854775807"));
      assertThrows(EvaluationError.class, () -> eval("'ab' * 500001"));
      assertThrows(EvaluationError.class, () -> eval("'' * 9223372036854775807"));
    }

    @Test
    void testListRepetitionIsBounded() {
      assertEquals(List.of(1L, 1L), eval("[1] * 2"));
      assertThrows(EvaluationError.class, () -> eval("[1, 2] * 9223372036854775807"));
      assertThrows(EvaluationError.class, () -> eval("[1, 2] * 500001"));
      assertThrows(EvaluationError.class, () -> eval("[] * 9223372036854775807"));
    }

    @Test
    void testDeduplicationIsNotQuadratic() {
      // A linear scan over the result would need tens of billions of comparisons here
      assertTimeoutPreemptively(
          Duration.ofSeconds(10),
          () -> assertEquals(200000L, eval("size(lists.range(200000).distinct())")));
    }

    @Test
    void testSetOperationsAreNotQuadratic() {
      assertTimeoutPreemptively(
          Duration.ofSeconds(10),
          () -> {
            assertEquals(
                true, eval("sets.contains(lists.range(200000), lists.range(200000))"));
            assertEquals(true, eval("sets.intersects(lists.range(200000), [199999])"));
            assertEquals(true, eval("sets.equivalent(lists.range(200000), lists.range(200000))"));
          });
    }

    @Test
    void testFormatPrecisionIsBounded() {
      assertEquals("1.500", eval("'%.3f'.format([1.5])"));
      assertThrows(EvaluationError.class, () -> eval("'%.2000000f'.format([1.5])"));
      assertThrows(EvaluationError.class, () -> eval("'%.1000000f'.format([1.5])"));
      // A precision too large to hold in an int must be rejected, not fail to parse
      assertThrows(EvaluationError.class, () -> eval("'%.99999999999999999999f'.format([1.5])"));
    }

    @Test
    void testCombiningOperationsCannotAmplify() {
      // Every input here is individually legal; the combined output would not be
      assertThrows(
          EvaluationError.class,
          () -> eval("lists.range(1000000).map(x, 'a').join('x' * 1000000)"));
      assertThrows(EvaluationError.class, () -> eval("('x' * 1000000) + ('x' * 1000000)"));
      assertThrows(
          EvaluationError.class, () -> eval("lists.range(1000000) + lists.range(1000000)"));
      assertThrows(EvaluationError.class, () -> eval("('x' * 1000000).replace('x', 'yy')"));
      assertThrows(
          EvaluationError.class, () -> eval("regex.replace('x' * 1000000, 'x', 'yy')"));
      assertThrows(EvaluationError.class, () -> eval("base64.encode(bytes('x' * 1000000))"));
      assertThrows(EvaluationError.class, () -> eval("b'ab' * 1 + b'cd' * 1000000"));
    }

    @Test
    void testOrdinaryCombinationsStillWork() {
      assertEquals("a-b", eval("['a', 'b'].join('-')"));
      assertEquals("xy", eval("'x' + 'y'"));
      assertEquals(List.of(1L, 2L), eval("[1] + [2]"));
      assertEquals("yy", eval("'xx'.replace('x', 'y')"));
    }

    @Test
    void testNegativeRepetitionIsRejected() {
      assertThrows(EvaluationError.class, () -> eval("'ab' * -1"));
      assertThrows(EvaluationError.class, () -> eval("[1] * -1"));
    }
  }

  /** Behavior this implementation adds on purpose, beyond what the specification defines. */
  @Nested
  class Extensions {
    @Test
    void testRepetitionOperators() {
      assertEquals("ababab", eval("'ab' * 3"));
      assertEquals(List.of(1L, 1L), eval("[1] * 2"));
    }

    @Test
    void testSubstringMembership() {
      assertEquals(true, eval("'ell' in 'hello'"));
    }

    @Test
    void testTwoArgumentHasFunction() {
      assertEquals(true, eval("has(a, 'b')", Map.of("a", Map.of("b", 1L))));
      assertEquals(false, eval("has(a, 'c')", Map.of("a", Map.of("b", 1L))));
    }

    @Test
    void testUnsignedIsNotADistinctType() {
      assertEquals(true, eval("type(1u) == int"));
      assertEquals(true, eval("1u == 1"));
    }
  }
}

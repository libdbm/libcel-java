package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for the timestamp and duration functions. */
class TimeFunctionTests {
  private static final String MOMENT = "2024-03-05T14:30:45.250Z";

  private CEL cel;

  @BeforeEach
  void setup() {
    cel = new CEL();
  }

  private Object eval(final String source) {
    return cel.eval(source, Map.of("t", Instant.parse(MOMENT)));
  }

  @Test
  void testParsesDurations() {
    assertEquals(Duration.ofSeconds(5), eval("duration('5s')"));
    assertEquals(Duration.ofMinutes(90), eval("duration('1h30m')"));
    assertEquals(Duration.ofMillis(1500), eval("duration('1.5s')"));
    assertEquals(Duration.ofMillis(100), eval("duration('100ms')"));
    assertEquals(Duration.ofNanos(10), eval("duration('10ns')"));
    assertEquals(Duration.ofHours(-2), eval("duration('-2h')"));
    assertThrows(IllegalArgumentException.class, () -> eval("duration('5x')"));
    // Exact accumulation: a double would clamp this to 9223372036 seconds
    assertEquals(Duration.ofHours(10000000), eval("duration('10000000h')"));
    assertThrows(IllegalArgumentException.class, () -> eval("duration('100000000h')"));
  }

  @Test
  void testAccessorsDefaultToUtc() {
    assertEquals(2024L, eval("getFullYear(t)"));
    assertEquals(2L, eval("getMonth(t)"));
    assertEquals(5L, eval("getDate(t)"));
    assertEquals(14L, eval("getHours(t)"));
    assertEquals(30L, eval("getMinutes(t)"));
    assertEquals(45L, eval("getSeconds(t)"));
    assertEquals(250L, eval("getMilliseconds(t)"));
    assertEquals(2L, eval("getDayOfWeek(t)"));
    assertEquals(64L, eval("getDayOfYear(t)"));
  }

  @Test
  void testAccessorsAcceptATimeZone() {
    assertEquals(9L, eval("getHours(t, 'America/New_York')"));
    assertEquals(23L, eval("getHours(t, 'Asia/Tokyo')"));
    assertEquals(5L, eval("getDate(t, 'Asia/Tokyo')"));
    assertEquals(6L, eval("getDate(timestamp('2024-03-05T23:30:00Z'), 'Asia/Tokyo')"));
    assertEquals(5L, eval("getDate(timestamp('2024-03-05T23:30:00Z'))"));
    assertThrows(IllegalArgumentException.class, () -> eval("getHours(t, 'Mars/Olympus')"));
  }

  @Test
  void testAccessorsAsMethods() {
    assertEquals(2024L, eval("t.getFullYear()"));
    assertEquals(14L, eval("t.getHours()"));
    assertEquals(9L, eval("t.getHours('America/New_York')"));
  }

  @Test
  void testDurationAccessors() {
    assertEquals(1L, eval("duration('1h30m').getHours()"));
    assertEquals(90L, eval("duration('1h30m').getMinutes()"));
    assertEquals(5400L, eval("duration('1h30m').getSeconds()"));
    assertEquals(1500L, eval("duration('1.5s').getMilliseconds()"));
  }

  @Test
  void testArithmetic() {
    assertEquals(Instant.parse("2024-03-05T15:30:45.250Z"), eval("t + duration('1h')"));
    assertEquals(Instant.parse("2024-03-05T15:30:45.250Z"), eval("duration('1h') + t"));
    assertEquals(Instant.parse("2024-03-05T13:30:45.250Z"), eval("t - duration('1h')"));
    assertEquals(Duration.ofHours(1), eval("(t + duration('1h')) - t"));
    assertEquals(Duration.ofMinutes(90), eval("duration('1h') + duration('30m')"));
    assertEquals(Duration.ofMinutes(30), eval("duration('1h') - duration('30m')"));
    assertEquals(Duration.ofHours(-1), eval("-duration('1h')"));
  }

  @Test
  void testComparison() {
    assertEquals(true, eval("t < t + duration('1s')"));
    assertEquals(true, eval("duration('1h') > duration('30m')"));
    assertEquals(true, eval("t == timestamp('" + MOMENT + "')"));
  }

  @Test
  void testTypeNames() {
    assertEquals(Type.TIMESTAMP, eval("type(t)"));
    assertEquals(Type.DURATION, eval("type(duration('1s'))"));
  }
}

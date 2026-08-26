package com.libdbm.cel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for the list, set and macro functions. */
class ListFunctionTests {
  private CEL cel;

  @BeforeEach
  void setup() {
    cel = new CEL();
  }

  private Object eval(final String source) {
    return cel.eval(source, Map.of());
  }

  @Test
  void testDistinct() {
    assertEquals(List.of(1L, 2L, 3L), eval("[1, 2, 2, 3, 1].distinct()"));
    assertEquals(List.of(), eval("[].distinct()"));
    assertEquals(List.of(List.of(1L)), eval("[[1], [1]].distinct()"));
  }

  @Test
  void testDistinctUsesDeepEquality() {
    // Duplicates are recognised through the same equality the language uses elsewhere
    assertEquals(List.of(1L, 2L), eval("[1, 1.0, 2].distinct()"));
    assertEquals(List.of(Map.of("a", 1L)), eval("[{'a': 1}, {'a': 1}].distinct()"));
    assertEquals(1, ((List<?>) eval("[b'ab', b'ab'].distinct()")).size());
    assertEquals(2, ((List<?>) eval("[b'ab', b'ba'].distinct()")).size());
  }

  @Test
  void testFlatten() {
    assertEquals(List.of(1L, 2L, 3L), eval("[[1, 2], [3]].flatten()"));
    assertEquals(List.of(1L, List.of(2L)), eval("[[1, [2]]].flatten()"));
    assertEquals(List.of(1L, 2L), eval("[[1, [2]]].flatten(2)"));
    assertThrows(IllegalArgumentException.class, () -> eval("[[1]].flatten(0)"));
  }

  @Test
  void testReverseAndSlice() {
    assertEquals(List.of(3L, 2L, 1L), eval("[1, 2, 3].reverse()"));
    assertEquals(List.of(2L, 3L), eval("[1, 2, 3, 4].slice(1, 3)"));
    assertEquals(List.of(), eval("[1, 2, 3].slice(1, 1)"));
    assertThrows(IllegalArgumentException.class, () -> eval("[1, 2].slice(0, 5)"));
  }

  @Test
  void testSortAndAccessors() {
    assertEquals(List.of(1L, 2L, 3L), eval("[3, 1, 2].sort()"));
    assertEquals(List.of("a", "b"), eval("['b', 'a'].sort()"));
    assertEquals(1L, eval("[1, 2].first()"));
    assertEquals(2L, eval("[1, 2].last()"));
    assertThrows(IllegalArgumentException.class, () -> eval("[].first()"));
  }

  @Test
  void testRange() {
    assertEquals(List.of(0L, 1L, 2L), eval("lists.range(3)"));
    assertEquals(List.of(), eval("lists.range(0)"));
  }

  @Test
  void testSets() {
    assertEquals(true, eval("sets.contains([1, 2, 3], [1, 3])"));
    assertEquals(false, eval("sets.contains([1, 2], [3])"));
    assertEquals(true, eval("sets.equivalent([1, 2, 2], [2, 1])"));
    assertEquals(false, eval("sets.equivalent([1], [1, 2])"));
    assertEquals(true, eval("sets.intersects([1, 2], [2, 3])"));
    assertEquals(false, eval("sets.intersects([1], [2])"));
  }

  @Test
  void testSortByMacro() {
    final Map<String, Object> users =
        Map.of(
            "users",
            List.of(
                Map.of("name", "Charlie", "age", 30),
                Map.of("name", "Alice", "age", 25),
                Map.of("name", "Bob", "age", 35)));

    assertEquals(
        List.of("Alice", "Charlie", "Bob"),
        cel.eval("users.sortBy(u, u.age).map(u, u.name)", users));
  }

  @Test
  void testMacrosOverMaps() {
    final Map<String, Object> scores = Map.of("scores", Map.of("a", 1, "b", 2));

    assertEquals(List.of("a", "b"), Lists.sort((List<?>) cel.eval("scores.map(k, k)", scores)));
    assertEquals(true, cel.eval("scores.exists(k, k == 'a')", scores));
    assertEquals(true, cel.eval("scores.all(k, size(k) == 1)", scores));
    assertEquals(List.of("b"), cel.eval("scores.filter(k, k == 'b')", scores));
  }

  @Test
  void testThreeArgumentMap() {
    assertEquals(List.of(4L, 8L), eval("[1, 2, 3, 4].map(x, x % 2 == 0, x * 2)"));
    assertEquals(List.of(), eval("[1, 3].map(x, x % 2 == 0, x * 2)"));
  }
}

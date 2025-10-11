package com.libdbm.cel;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract interface for providing functions to CEL expressions.
 *
 * <p>Implement this interface to provide custom functions that can be called from CEL expressions.
 * The {@link StandardFunctions} class provides all standard CEL functions and can be extended for
 * custom functionality.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class MyFunctions extends StandardFunctions {
 *   @Override
 *   public Object call(final String name, final List<Object> args) {
 *     if (name.equals("customFunc")) {
 *       return myCustomImplementation(args);
 *     }
 *     return super.call(name, args);
 *   }
 * }
 * }</pre>
 */
public interface Functions {
  /**
   * Calls a global function by name.
   *
   * @param name The name of the function to call.
   * @param args The arguments to pass to the function.
   * @return The result of the function call.
   * @throws IllegalArgumentException if the function is not found or if the arguments are invalid.
   */
  Object callFunction(final String name, final List<Object> args);

  /**
   * Calls a method on a target object.
   *
   * @param target The object to call the method on.
   * @param method The name of the method to call.
   * @param args The arguments to pass to the method.
   * @return The result of the method call.
   * @throws IllegalArgumentException if the method is not found or if the arguments are invalid.
   */
  Object callMethod(final Object target, final String method, final List<Object> args);
}

/** Exception thrown during CEL expression evaluation. */
class EvaluationError extends RuntimeException {
  public EvaluationError(final String message) {
    super(message);
  }
}

/**
 * Standard CEL function library implementation.
 *
 * <p>Provides all built-in CEL functions including:
 *
 * <ul>
 *   <li>Type conversions: int(), double(), string(), bool()
 *   <li>Type checking: type()
 *   <li>Collection operations: size(), has()
 *   <li>String operations: contains(), startsWith(), endsWith(), matches()
 *   <li>Date/time: timestamp(), duration()
 *   <li>Math operations: Math functions when called on numbers
 * </ul>
 *
 * <p>This class can be extended to add custom functions while retaining all standard CEL
 * functionality.
 */
class StandardFunctions implements Functions {
  @Override
  public Object callFunction(final String name, final List<Object> args) {
    return switch (name) {
      case "size" -> size(args.get(0));
      case "int" -> toInt(args.get(0));
      case "uint" -> toUint(args.get(0));
      case "double" -> toDouble(args.get(0));
      case "string" -> asString(args.get(0));
      case "bool" -> toBool(args.get(0));
      case "type" -> getType(args.get(0));
      case "has" -> {
        if (args.size() != 2) {
          throw new IllegalArgumentException("has() requires 2 arguments");
        }
        yield has(args.get(0), args.get(1));
      }
      case "matches" -> {
        if (args.size() != 2) {
          throw new IllegalArgumentException("matches() requires 2 arguments");
        }
        yield matches((String) args.get(0), (String) args.get(1));
      }
      case "timestamp" -> timestamp(!args.isEmpty() ? args.get(0) : null);
      case "duration" -> duration((String) args.get(0));
      case "getDate" -> getDate(args.get(0));
      case "getMonth" -> getMonth(args.get(0));
      case "getFullYear" -> getFullYear(args.get(0));
      case "getHours" -> getHours(args.get(0));
      case "getMinutes" -> getMinutes(args.get(0));
      case "getSeconds" -> getSeconds(args.get(0));
      case "max" -> max(args);
      case "min" -> min(args);
      default -> throw new IllegalArgumentException("Unknown function: " + name);
    };
  }

  @Override
  public Object callMethod(final Object target, final String method, final List<Object> args) {
    if (target == null) {
      throw new IllegalArgumentException("Cannot call method on null");
    }

    return switch (method) {
      case "contains" -> {
        if (target instanceof String str && args.size() == 1 && args.get(0) instanceof String arg) {
          yield str.contains(arg);
        } else if (target instanceof List<?> list && args.size() == 1) {
          yield list.contains(args.get(0));
        }
        throw new IllegalArgumentException("Invalid arguments for contains()");
      }
      case "startsWith" -> {
        if (target instanceof String str && args.size() == 1 && args.get(0) instanceof String arg) {
          yield str.startsWith(arg);
        }
        throw new IllegalArgumentException("startsWith() requires string target and argument");
      }
      case "endsWith" -> {
        if (target instanceof String str && args.size() == 1 && args.get(0) instanceof String arg) {
          yield str.endsWith(arg);
        }
        throw new IllegalArgumentException("endsWith() requires string target and argument");
      }
      case "toLowerCase" -> {
        if (target instanceof String str && args.isEmpty()) {
          yield str.toLowerCase();
        }
        throw new IllegalArgumentException("toLowerCase() requires string target");
      }
      case "toUpperCase" -> {
        if (target instanceof String str && args.isEmpty()) {
          yield str.toUpperCase();
        }
        throw new IllegalArgumentException("toUpperCase() requires string target");
      }
      case "trim" -> {
        if (target instanceof String str && args.isEmpty()) {
          yield str.trim();
        }
        throw new IllegalArgumentException("trim() requires string target");
      }
      case "replace" -> {
        if (target instanceof String str
            && args.size() == 2
            && args.get(0) instanceof String from
            && args.get(1) instanceof String to) {
          yield str.replace(from, to);
        }
        throw new IllegalArgumentException(
            "replace() requires string target and 2 string arguments");
      }
      case "split" -> {
        if (target instanceof String str
            && args.size() == 1
            && args.get(0) instanceof String separator) {
          yield List.of(str.split(Pattern.quote(separator)));
        }
        throw new IllegalArgumentException("split() requires string target and separator");
      }
      case "size" -> size(target);
      // Macro functions are handled in the interpreter with special logic
      case "map", "filter", "all", "exists", "existsOne" ->
          throw new EvaluationError(
              "Macro function " + method + " was not properly handled by the interpreter");
      default -> throw new IllegalArgumentException("Unknown method: " + method);
    };
  }

  private int size(final Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof String str) {
      return str.length();
    }
    if (value instanceof List<?> list) {
      return list.size();
    }
    if (value instanceof Map<?, ?> map) {
      return map.size();
    }
    throw new IllegalArgumentException(
        "size() not supported for type: " + value.getClass().getName());
  }

  private long toInt(final Object value) {
    if (value instanceof Long l) {
      return l;
    }
    if (value instanceof Integer i) {
      return i.longValue();
    }
    if (value instanceof Double d) {
      return d.longValue();
    }
    if (value instanceof Float f) {
      return f.longValue();
    }
    if (value instanceof String str) {
      return Long.parseLong(str);
    }
    if (value instanceof Boolean b) {
      return b ? 1L : 0L;
    }
    throw new IllegalArgumentException("Cannot convert to int: " + value);
  }

  private long toUint(final Object value) {
    final long result = toInt(value);
    if (result < 0) {
      throw new IllegalArgumentException("Cannot convert negative value to uint: " + value);
    }
    return result;
  }

  private double toDouble(final Object value) {
    if (value instanceof Double d) {
      return d;
    }
    if (value instanceof Float f) {
      return f.doubleValue();
    }
    if (value instanceof Long l) {
      return l.doubleValue();
    }
    if (value instanceof Integer i) {
      return i.doubleValue();
    }
    if (value instanceof String str) {
      return Double.parseDouble(str);
    }
    throw new IllegalArgumentException("Cannot convert to double: " + value);
  }

  private String asString(final Object value) {
    if (value == null) {
      return "null";
    }
    return value.toString();
  }

  private boolean toBool(final Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof Long l) {
      return l != 0L;
    }
    if (value instanceof Integer i) {
      return i != 0;
    }
    if (value instanceof Double d) {
      return d != 0.0;
    }
    if (value instanceof Float f) {
      return f != 0.0f;
    }
    if (value instanceof String str) {
      return !str.isEmpty();
    }
    if (value instanceof List<?> list) {
      return !list.isEmpty();
    }
    if (value instanceof Map<?, ?> map) {
      return !map.isEmpty();
    }
    return value != null;
  }

  private String getType(final Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Boolean) {
      return "bool";
    }
    if (value instanceof Long || value instanceof Integer) {
      return "int";
    }
    if (value instanceof Double || value instanceof Float) {
      return "double";
    }
    if (value instanceof String) {
      return "string";
    }
    if (value instanceof List) {
      return "list";
    }
    if (value instanceof Map) {
      return "map";
    }
    return "unknown";
  }

  private boolean has(final Object target, final Object field) {
    if (target instanceof Map<?, ?> map && field instanceof String key) {
      return map.containsKey(key);
    }
    return false;
  }

  private boolean matches(final String text, final String pattern) {
    final var regex = Pattern.compile(pattern);
    return regex.matcher(text).find();
  }

  private Instant timestamp(final Object value) {
    if (value == null) {
      return Instant.now();
    }
    if (value instanceof String str) {
      return Instant.parse(str);
    }
    if (value instanceof Long millis) {
      return Instant.ofEpochMilli(millis);
    }
    if (value instanceof Integer millis) {
      return Instant.ofEpochMilli(millis.longValue());
    }
    throw new IllegalArgumentException("Invalid timestamp value: " + value);
  }

  private Duration duration(final String value) {
    final var regex = Pattern.compile("^(\\d+)([hms])$");
    final var matcher = regex.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid duration format: " + value);
    }

    final int amount = Integer.parseInt(matcher.group(1));
    final String unit = matcher.group(2);

    return switch (unit) {
      case "h" -> Duration.ofHours(amount);
      case "m" -> Duration.ofMinutes(amount);
      case "s" -> Duration.ofSeconds(amount);
      default -> throw new IllegalArgumentException("Invalid duration unit: " + unit);
    };
  }

  private int getDate(final Object value) {
    final var instant = value instanceof Instant i ? i : timestamp(value);
    final var date = instant.atZone(ZoneId.systemDefault());
    return date.getDayOfMonth();
  }

  private int getMonth(final Object value) {
    final var instant = value instanceof Instant i ? i : timestamp(value);
    final var date = instant.atZone(ZoneId.systemDefault());
    return date.getMonthValue() - 1;
  }

  private int getFullYear(final Object value) {
    final var instant = value instanceof Instant i ? i : timestamp(value);
    final var date = instant.atZone(ZoneId.systemDefault());
    return date.getYear();
  }

  private int getHours(final Object value) {
    final var instant = value instanceof Instant i ? i : timestamp(value);
    final var date = instant.atZone(ZoneId.systemDefault());
    return date.getHour();
  }

  private int getMinutes(final Object value) {
    final var instant = value instanceof Instant i ? i : timestamp(value);
    final var date = instant.atZone(ZoneId.systemDefault());
    return date.getMinute();
  }

  private int getSeconds(final Object value) {
    final var instant = value instanceof Instant i ? i : timestamp(value);
    final var date = instant.atZone(ZoneId.systemDefault());
    return date.getSecond();
  }

  private Object max(final List<Object> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("max() requires at least one argument");
    }

    var result = values.get(0);
    for (int i = 1; i < values.size(); i++) {
      if (compare(values.get(i), result) > 0) {
        result = values.get(i);
      }
    }
    return result;
  }

  private Object min(final List<Object> values) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("min() requires at least one argument");
    }

    var result = values.get(0);
    for (int i = 1; i < values.size(); i++) {
      if (compare(values.get(i), result) < 0) {
        result = values.get(i);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private int compare(final Object a, final Object b) {
    if (a instanceof Number na && b instanceof Number nb) {
      return Double.compare(na.doubleValue(), nb.doubleValue());
    }
    if (a instanceof String sa && b instanceof String sb) {
      return sa.compareTo(sb);
    }
    if (a instanceof Instant ia && b instanceof Instant ib) {
      return ia.compareTo(ib);
    }
    if (a instanceof Comparable<?> ca && b instanceof Comparable<?> cb) {
      try {
        return ((Comparable<Object>) ca).compareTo(cb);
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Cannot compare values of different types");
      }
    }
    throw new IllegalArgumentException("Cannot compare values of different types");
  }
}

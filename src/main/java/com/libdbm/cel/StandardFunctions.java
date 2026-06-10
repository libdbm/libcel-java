package com.libdbm.cel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

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
public class StandardFunctions implements Functions {
  /** Creates a standard function library with all built-in CEL functions. */
  public StandardFunctions() {}

  private static boolean isAssignable(final Class<?> paramType, final Class<?> argType) {
    if (paramType.isAssignableFrom(argType)) return true;
    // Handle primitive to wrapper compatibility
    if (paramType.isPrimitive()) {
      return (paramType == boolean.class && argType == Boolean.class)
          || (paramType == byte.class && (argType == Byte.class))
          || (paramType == short.class && (argType == Short.class || argType == Byte.class))
          || (paramType == char.class && argType == Character.class)
          || (paramType == int.class
              && (argType == Integer.class
                  || argType == Short.class
                  || argType == Byte.class
                  || argType == Character.class))
          || (paramType == long.class
              && (argType == Long.class
                  || argType == Integer.class
                  || argType == Short.class
                  || argType == Byte.class
                  || argType == Character.class))
          || (paramType == float.class
              && (argType == Float.class
                  || argType == Long.class
                  || argType == Integer.class
                  || argType == Short.class
                  || argType == Byte.class
                  || argType == Character.class))
          || (paramType == double.class
              && (argType == Double.class
                  || argType == Float.class
                  || argType == Long.class
                  || argType == Integer.class
                  || argType == Short.class
                  || argType == Byte.class
                  || argType == Character.class));
    }
    // Allow wrapper param accepting primitive arg type (should be covered by assignableFrom
    // normally)
    return false;
  }

  @Override
  public Object callFunction(final String name, final List<Object> args) {
    return switch (name) {
      case "size" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("size() requires 1 argument");
        }
        yield Utilities.sizeOf(args.get(0));
      }
      case "int" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("int() requires 1 argument");
        }
        yield Utilities.asInt(args.get(0));
      }
      case "uint" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("uint() requires 1 argument");
        }
        yield Utilities.asUInt(args.get(0));
      }
      case "double" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("double() requires 1 argument");
        }
        yield Utilities.asDouble(args.get(0));
      }
      case "string" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("string() requires 1 argument");
        }
        yield Utilities.asString(args.get(0));
      }
      case "bool" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("bool() requires 1 argument");
        }
        yield Utilities.asBool(args.get(0));
      }
      case "type" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("type() requires 1 argument");
        }
        yield Utilities.typeOf(args.get(0));
      }
      case "has" -> {
        if (args.size() != 2) {
          throw new IllegalArgumentException("has() requires 2 arguments");
        }
        yield Utilities.has(args.get(0), args.get(1));
      }
      case "matches" -> {
        if (args.size() != 2) {
          throw new IllegalArgumentException("matches() requires 2 arguments");
        }
        yield Utilities.matches((String) args.get(0), (String) args.get(1));
      }
      case "timestamp" -> Utilities.timestamp(!args.isEmpty() ? args.get(0) : null);
      case "duration" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("duration() requires 1 argument");
        }
        yield Utilities.duration((String) args.get(0));
      }
      case "getDate" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("getDate() requires 1 argument");
        }
        yield Utilities.dateOf(args.get(0));
      }
      case "getMonth" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("getMonth() requires 1 argument");
        }
        yield Utilities.monthOf(args.get(0));
      }
      case "getFullYear" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("getFullYear() requires 1 argument");
        }
        yield Utilities.yearOf(args.get(0));
      }
      case "getHours" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("getHours() requires 1 argument");
        }
        yield Utilities.hoursOf(args.get(0));
      }
      case "getMinutes" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("getMinutes() requires 1 argument");
        }
        yield Utilities.minutesOf(args.get(0));
      }
      case "getSeconds" -> {
        if (args.isEmpty()) {
          throw new IllegalArgumentException("getSeconds() requires 1 argument");
        }
        yield Utilities.secondsOf(args.get(0));
      }
      case "max" -> Utilities.max(args);
      case "min" -> Utilities.min(args);
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
      case "size" -> Utilities.sizeOf(target);
      // Macro functions are handled in the interpreter with special logic
      case "map", "filter", "all", "exists", "existsOne" ->
          throw new EvaluationError(
              "Macro function " + method + " was not properly handled by the interpreter");
      default -> callJavaMethod(target, method, args);
    };
  }

  private Object callJavaMethod(
      final Object target, final String name, final List<Object> parameters) {
    try {
      final var clazz = target.getClass();
      final var args = parameters.toArray();
      Method method = null;
      search:
      for (final var m : clazz.getMethods()) {
        if (!m.getName().equals(name)) continue;
        final var types = m.getParameterTypes();
        if (types.length != args.length) continue;
        for (int i = 0; i < types.length; i++) {
          final Object a = args[i];
          if (a == null) {
            if (types[i].isPrimitive()) {
              continue search;
            }
          } else if (!isAssignable(types[i], a.getClass())) {
            continue search;
          }
        }
        method = m;
        break;
      }

      if (method == null) {
        throw new IllegalArgumentException(
            "No such method '"
                + name
                + "' on type "
                + clazz.getName()
                + " with "
                + parameters.size()
                + " argument(s)");
      }

      method.setAccessible(true);
      return method.invoke(target, args);
    } catch (final IllegalArgumentException e) {
      throw e;
    } catch (final IllegalAccessException e) {
      throw new EvaluationError("Cannot access method '" + name + "': " + e.getMessage());
    } catch (final InvocationTargetException e) {
      final var cause = e.getCause();
      throw new EvaluationError(
          "Method '" + name + "' threw: " + (cause != null ? cause.getMessage() : e.getMessage()));
    } catch (final Exception e) {
      throw new EvaluationError("Failed to invoke method '" + name + "': " + e.getMessage());
    }
  }
}

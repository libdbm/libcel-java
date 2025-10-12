package com.libdbm.cel;

import java.lang.reflect.Method;
import java.util.List;
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
      case "size" -> Utilities.sizeOf(args.get(0));
      case "int" -> Utilities.asInt(args.get(0));
      case "uint" -> Utilities.asUInt(args.get(0));
      case "double" -> Utilities.asDouble(args.get(0));
      case "string" -> Utilities.asString(args.get(0));
      case "bool" -> Utilities.asBool(args.get(0));
      case "type" -> Utilities.typeOf(args.get(0));
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
      case "duration" -> Utilities.duration((String) args.get(0));
      case "getDate" -> Utilities.dateOf(args.get(0));
      case "getMonth" -> Utilities.monthOf(args.get(0));
      case "getFullYear" -> Utilities.yearOf(args.get(0));
      case "getHours" -> Utilities.hoursOf(args.get(0));
      case "getMinutes" -> Utilities.minutesOf(args.get(0));
      case "getSeconds" -> Utilities.secondsOf(args.get(0));
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
      try {
        method.setAccessible(true);
        return method.invoke(target, args);
      } catch (Throwable e) {
        throw new EvaluationError(
            "Invocation of method '"
                + name
                + "' on type "
                + clazz.getName()
                + " failed: "
                + e.getMessage());
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Throwable t) {
      throw new EvaluationError("Invocation of method '" + name + "' failed: " + t.getMessage());
    }
  }
}

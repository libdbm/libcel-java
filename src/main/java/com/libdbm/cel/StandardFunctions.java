package com.libdbm.cel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

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
  private static final Set<String> QUALIFIED =
      Set.of(
          "math.greatest",
          "math.least",
          "math.abs",
          "math.ceil",
          "math.floor",
          "math.round",
          "math.trunc",
          "math.sign",
          "math.sqrt",
          "math.isNaN",
          "math.isInf",
          "math.isFinite",
          "math.bitAnd",
          "math.bitOr",
          "math.bitXor",
          "math.bitNot",
          "math.bitShiftLeft",
          "math.bitShiftRight",
          "sets.contains",
          "sets.equivalent",
          "sets.intersects",
          "base64.encode",
          "base64.decode",
          "lists.range",
          "strings.quote",
          "regex.replace",
          "regex.extract",
          "regex.extractAll");

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

  private static void require(
      final String name, final List<Object> args, final int least, final int most) {
    if (args.size() < least || args.size() > most) {
      throw new IllegalArgumentException(
          name + "() requires " + (least == most ? least : least + " to " + most) + " argument(s)");
    }
  }

  private static Object zone(final List<Object> args) {
    return args.size() > 1 ? args.get(1) : null;
  }

  private static String text(final Object value, final String name) {
    if (value instanceof String result) {
      return result;
    }
    throw new IllegalArgumentException(name + "() requires string arguments");
  }

  private static List<?> list(final Object value, final String name) {
    if (value instanceof List<?> result) {
      return result;
    }
    throw new IllegalArgumentException(name + "() requires a list");
  }

  @Override
  public boolean knows(final String name) {
    return QUALIFIED.contains(name);
  }

  @Override
  public Object callFunction(final String name, final List<Object> args) {
    if (name.indexOf('.') > 0) {
      return callQualified(name, args);
    }

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
        require(name, args, 1, 1);
        yield Utilities.asDuration(args.get(0));
      }
      case "getDate" -> {
        require(name, args, 1, 2);
        yield (long) Utilities.dateOf(args.get(0), zone(args));
      }
      case "getMonth" -> {
        require(name, args, 1, 2);
        yield (long) Utilities.monthOf(args.get(0), zone(args));
      }
      case "getFullYear" -> {
        require(name, args, 1, 2);
        yield (long) Utilities.yearOf(args.get(0), zone(args));
      }
      case "getHours" -> {
        require(name, args, 1, 2);
        yield Utilities.hoursOf(args.get(0), zone(args));
      }
      case "getMinutes" -> {
        require(name, args, 1, 2);
        yield Utilities.minutesOf(args.get(0), zone(args));
      }
      case "getSeconds" -> {
        require(name, args, 1, 2);
        yield Utilities.secondsOf(args.get(0), zone(args));
      }
      case "getDayOfWeek" -> {
        require(name, args, 1, 2);
        yield (long) Utilities.weekdayOf(args.get(0), zone(args));
      }
      case "getDayOfYear" -> {
        require(name, args, 1, 2);
        yield (long) Utilities.ordinalOf(args.get(0), zone(args));
      }
      case "getMilliseconds" -> {
        require(name, args, 1, 2);
        yield Utilities.millisecondsOf(args.get(0), zone(args));
      }
      case "bytes" -> {
        require(name, args, 1, 1);
        yield Utilities.asBytes(args.get(0));
      }
      case "dyn" -> {
        require(name, args, 1, 1);
        yield args.get(0);
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
          yield Lists.contains(list, args.get(0));
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
        require(method, args, 2, 3);
        yield Strings.replace(
            text(target, method),
            text(args.get(0), method),
            text(args.get(1), method),
            args.size() > 2 ? Utilities.asInt(args.get(2)) : -1L);
      }
      case "split" -> {
        require(method, args, 1, 2);
        yield Strings.split(
            text(target, method),
            text(args.get(0), method),
            args.size() > 1 ? Utilities.asInt(args.get(1)) : -1L);
      }
      case "charAt" -> {
        require(method, args, 1, 1);
        yield Strings.charAt(text(target, method), Utilities.asInt(args.get(0)));
      }
      case "indexOf" -> {
        require(method, args, 1, 2);
        yield Strings.indexOf(
            text(target, method),
            text(args.get(0), method),
            args.size() > 1 ? Utilities.asInt(args.get(1)) : 0L);
      }
      case "lastIndexOf" -> {
        require(method, args, 1, 2);
        yield args.size() > 1
            ? Strings.lastIndexOf(
                text(target, method), text(args.get(0), method), Utilities.asInt(args.get(1)))
            : Strings.lastIndexOf(text(target, method), text(args.get(0), method));
      }
      case "lowerAscii" -> {
        require(method, args, 0, 0);
        yield Strings.lower(text(target, method));
      }
      case "upperAscii" -> {
        require(method, args, 0, 0);
        yield Strings.upper(text(target, method));
      }
      case "substring" -> {
        require(method, args, 1, 2);
        yield args.size() > 1
            ? Strings.substring(
                text(target, method), Utilities.asInt(args.get(0)), Utilities.asInt(args.get(1)))
            : Strings.substring(text(target, method), Utilities.asInt(args.get(0)));
      }
      case "join" -> {
        require(method, args, 0, 1);
        yield Strings.join(list(target, method), args.isEmpty() ? "" : text(args.get(0), method));
      }
      case "format" -> {
        require(method, args, 1, 1);
        yield Strings.format(text(target, method), list(args.get(0), method));
      }
      case "matches" -> {
        require(method, args, 1, 1);
        yield Utilities.matches(text(target, method), text(args.get(0), method));
      }
      case "reverse" -> {
        require(method, args, 0, 0);
        yield target instanceof List<?> values
            ? Lists.reverse(values)
            : Strings.reverse(text(target, method));
      }
      case "distinct" -> {
        require(method, args, 0, 0);
        yield Lists.distinct(list(target, method));
      }
      case "flatten" -> {
        require(method, args, 0, 1);
        yield Lists.flatten(
            list(target, method), args.isEmpty() ? 1L : Utilities.asInt(args.get(0)));
      }
      case "slice" -> {
        require(method, args, 2, 2);
        yield Lists.slice(
            list(target, method), Utilities.asInt(args.get(0)), Utilities.asInt(args.get(1)));
      }
      case "sort" -> {
        require(method, args, 0, 0);
        yield Lists.sort(list(target, method));
      }
      case "first" -> {
        require(method, args, 0, 0);
        yield Lists.first(list(target, method));
      }
      case "last" -> {
        require(method, args, 0, 0);
        yield Lists.last(list(target, method));
      }
      case "getDate",
          "getMonth",
          "getFullYear",
          "getHours",
          "getMinutes",
          "getSeconds",
          "getMilliseconds",
          "getDayOfWeek",
          "getDayOfYear" -> {
        require(method, args, 0, 1);
        final var parameters = new ArrayList<Object>();
        parameters.add(target);
        parameters.addAll(args);
        yield callFunction(method, parameters);
      }
      case "size" -> Utilities.sizeOf(target);
      // Macro functions are handled in the interpreter with special logic
      case "map", "filter", "all", "exists", "existsOne", "sortBy" ->
          throw new EvaluationError(
              "Macro function " + method + " was not properly handled by the interpreter");
      default -> callJavaMethod(target, method, args);
    };
  }

  private Object callQualified(final String name, final List<Object> args) {
    return switch (name) {
      case "math.greatest" -> Maths.greatest(args);
      case "math.least" -> Maths.least(args);
      case "math.abs" -> single(name, args, Maths::abs);
      case "math.ceil" -> single(name, args, Maths::ceil);
      case "math.floor" -> single(name, args, Maths::floor);
      case "math.round" -> single(name, args, Maths::round);
      case "math.trunc" -> single(name, args, Maths::trunc);
      case "math.sign" -> single(name, args, Maths::sign);
      case "math.sqrt" -> single(name, args, Maths::sqrt);
      case "math.isNaN" -> single(name, args, Maths::isNaN);
      case "math.isInf" -> single(name, args, Maths::isInf);
      case "math.isFinite" -> single(name, args, Maths::isFinite);
      case "math.bitAnd" -> pair(name, args, Maths::and);
      case "math.bitOr" -> pair(name, args, Maths::or);
      case "math.bitXor" -> pair(name, args, Maths::xor);
      case "math.bitNot" -> single(name, args, Maths::not);
      case "math.bitShiftLeft" -> pair(name, args, Maths::left);
      case "math.bitShiftRight" -> pair(name, args, Maths::right);
      case "sets.contains" -> {
        require(name, args, 2, 2);
        yield Sets.contains(list(args.get(0), name), list(args.get(1), name));
      }
      case "sets.equivalent" -> {
        require(name, args, 2, 2);
        yield Sets.equivalent(list(args.get(0), name), list(args.get(1), name));
      }
      case "sets.intersects" -> {
        require(name, args, 2, 2);
        yield Sets.intersects(list(args.get(0), name), list(args.get(1), name));
      }
      case "base64.encode" -> {
        require(name, args, 1, 1);
        yield Codecs.encode(args.get(0));
      }
      case "base64.decode" -> {
        require(name, args, 1, 1);
        yield Codecs.decode(text(args.get(0), name));
      }
      case "lists.range" -> {
        require(name, args, 1, 1);
        yield Lists.range(Utilities.asInt(args.get(0)));
      }
      case "strings.quote" -> {
        require(name, args, 1, 1);
        yield Strings.quote(text(args.get(0), name));
      }
      case "regex.replace" -> {
        require(name, args, 3, 4);
        yield Regexes.replace(
            text(args.get(0), name),
            text(args.get(1), name),
            text(args.get(2), name),
            args.size() > 3 ? Utilities.asInt(args.get(3)) : -1L);
      }
      case "regex.extract" -> {
        require(name, args, 2, 2);
        yield Regexes.extract(text(args.get(0), name), text(args.get(1), name));
      }
      case "regex.extractAll" -> {
        require(name, args, 2, 2);
        yield Regexes.extractAll(text(args.get(0), name), text(args.get(1), name));
      }
      default -> throw new IllegalArgumentException("Unknown function: " + name);
    };
  }

  private Object single(
      final String name, final List<Object> args, final UnaryOperator<Object> operation) {
    require(name, args, 1, 1);
    return operation.apply(args.get(0));
  }

  private Object pair(
      final String name, final List<Object> args, final BinaryOperator<Object> operation) {
    require(name, args, 2, 2);
    return operation.apply(args.get(0), args.get(1));
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

package com.libdbm.cel;

import java.lang.reflect.InvocationTargetException;
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


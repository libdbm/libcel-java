package com.libdbm.cel;

import java.util.List;

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
 *   public Object callFunction(final String name, final List<Object> args) {
 *     if (name.equals("customFunc")) {
 *       return myCustomImplementation(args);
 *     }
 *     return super.callFunction(name, args);
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

  /**
   * Reports whether this library provides a qualified global function with the given name.
   *
   * <p>The interpreter uses this to resolve namespaced calls such as {@code math.greatest(1, 2)},
   * which would otherwise be parsed as a field selection followed by a method call. Only names that
   * are not shadowed by a bound variable are ever tested.
   *
   * @param name the qualified function name, for example "math.greatest"
   * @return true if the name should be dispatched to callFunction
   */
  default boolean knows(final String name) {
    return false;
  }
}

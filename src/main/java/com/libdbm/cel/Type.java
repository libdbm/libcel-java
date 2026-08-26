package com.libdbm.cel;

/**
 * A CEL type value, as returned by the {@code type()} function.
 *
 * <p>Type values compare equal by name and are bound as identifiers, so the specification form
 * {@code type(42) == int} holds. They render as their bare name, so {@code string(type(42))} is
 * {@code "int"}.
 *
 * @param name the type name, for example "int", "list" or "timestamp"
 */
public record Type(String name) {
  /** The type of a null value. */
  public static final Type NULL = new Type("null_type");

  /** The boolean type. */
  public static final Type BOOL = new Type("bool");

  /** The signed integer type. */
  public static final Type INT = new Type("int");

  /** The unsigned integer type. */
  public static final Type UINT = new Type("uint");

  /** The double precision floating point type. */
  public static final Type DOUBLE = new Type("double");

  /** The string type. */
  public static final Type STRING = new Type("string");

  /** The bytes type. */
  public static final Type BYTES = new Type("bytes");

  /** The list type. */
  public static final Type LIST = new Type("list");

  /** The map type. */
  public static final Type MAP = new Type("map");

  /** The timestamp type. */
  public static final Type TIMESTAMP = new Type("timestamp");

  /** The duration type. */
  public static final Type DURATION = new Type("duration");

  /** The type of a type value. */
  public static final Type TYPE = new Type("type");

  /** The type of a value this library does not recognise. */
  public static final Type UNKNOWN = new Type("unknown");

  /**
   * Returns the type with the given name, or null when the name does not denote a CEL type.
   *
   * <p>Used to resolve bare type names such as {@code int} when they appear as identifiers.
   *
   * @param name the candidate type name
   * @return the matching type, or null
   */
  public static Type of(final String name) {
    return switch (name) {
      case "null_type" -> NULL;
      case "bool" -> BOOL;
      case "int" -> INT;
      case "uint" -> UINT;
      case "double" -> DOUBLE;
      case "string" -> STRING;
      case "bytes" -> BYTES;
      case "list" -> LIST;
      case "map" -> MAP;
      case "timestamp" -> TIMESTAMP;
      case "duration" -> DURATION;
      case "type" -> TYPE;
      default -> null;
    };
  }

  @Override
  public String toString() {
    return name;
  }
}

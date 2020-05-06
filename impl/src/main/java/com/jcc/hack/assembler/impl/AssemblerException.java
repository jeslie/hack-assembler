package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A {@link RuntimeException} related to the Hack Assembler.
 */
@SuppressWarnings({
                      "PMD.MissingSerialVersionUID",
                      "UncheckedExceptionClass",
                      "serial"
                  })
class AssemblerException extends RuntimeException {

  /**
   * Default SerialVersionUID.
   */
  @SuppressWarnings({
                        "PMD.FieldNamingConventions",
                        "PMD.VariableNamingConventions",
                        "unused"
                    })
  private static final long SerialVersionUID = 1L;

  /**
   * Constructor.
   *
   * @param message error message as invariant text
   */
  @SuppressWarnings("PMD.CommentDefaultAccessModifier")
  AssemblerException(final String message) {
    super(Objects.requireNonNullElse(message, ""));
  }

  /**
   * Constructor.
   *
   * @param format formatting specification as per {@link
   *     String#format(String, Object...)}
   * @param args argument(s) as per {@link String#format(String,
   *     Object...)}
   */
  @SuppressFBWarnings("FORMAT_STRING_MANIPULATION")
  @SuppressWarnings({
                        "PMD.CommentDefaultAccessModifier",
                        "OverloadedVarargsMethod"
                    })
  AssemblerException(final String format, final Object... args) {
    this(args == null || args.length == 0
             ? format : String.format(format, args));
  }

  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  @SuppressWarnings("PMD.LawOfDemeter")
  @Override
  public final @NonNull String getMessage() {
    return Objects.requireNonNullElse(super.getMessage(), "");
  }

}

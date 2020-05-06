package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An {@link AssemblerException} related to the Hack Assembler command line
 * use.
 */
@SuppressWarnings({
                      "PMD.MissingSerialVersionUID",
                      "SameParameterValue",
                      "UncheckedExceptionClass",
                      "serial"
                  })
final class CommandLineException extends AssemblerException {

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
   * @param format formatting specification as per {@link
   *     String#format(String, Object...)}; treated as simple text (not as a
   *     format) if <em>args</em> are missing or an empty array
   * @param args argument(s) for <em>format</em>
   */
  @SuppressFBWarnings("FORMAT_STRING_MANIPULATION")
  @SuppressWarnings("PMD.CommentDefaultAccessModifier")
  CommandLineException(final String format, final Object... args) {
    super(args == null || args.length == 0
              ? format : String.format(format, args));
  }

}

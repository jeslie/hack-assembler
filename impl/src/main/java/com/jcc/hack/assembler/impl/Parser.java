package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hack assembly (source) code parser.
 */
@SuppressWarnings({
                      "PMD.BeanMembersShouldSerialize",
                      "PMD.CommentDefaultAccessModifier",
                      "PMD.DefaultPackage",
                      "PMD.LawOfDemeter"
                  })
final class Parser implements Closeable {

  /**
   * Class logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

  /**
   * Source file reader.
   */
  private final LineNumberReader reader;

  /**
   * {@code true} iff generate a listing of the source code read.
   */
  private final boolean echoSource;

  /**
   * Current source line; empty before first {@link #hasMoreCommands()}
   * and after EOF detected.
   */
  @SuppressWarnings("PMD.RedundantFieldInitializer")
  private String nextLine = "";

  /**
   * Classification of the current source line.
   */
  private LineType lineType = LineType.COMMENT_ONLY;

  /**
   * Character index of the '=' in the current assembly source; non-negative iff
   * found '=' outside of comment. Only valid for a C-command.
   */
  @SuppressWarnings("InstanceVariableMayNotBeInitialized")
  private int equalsAt;

  /**
   * Character index of the ';' in the current assembly source; non-negative iff
   * found ';' outside of comment. Only valid for a C-command.
   */
  @SuppressWarnings("InstanceVariableMayNotBeInitialized")
  private int semiAt;

  /**
   * Constructor.
   *
   * @param showPath {@code true} iff show assembly file pathname
   * @param input assembly input file
   * @param showSource {@code true} iff list source code as encountered
   * @throws IOException iff problem accessing <em>input</em> as a text file
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  Parser(final boolean showPath, final File input, final boolean showSource)
      throws IOException {
    reader = new LineNumberReader(Files.newBufferedReader(input.toPath(),
        StandardCharsets.UTF_8));
    if (showPath) {
      Parser.LOG.info("asm-file:   {}", input.getCanonicalFile());
    }

    echoSource = showSource;
    if (echoSource && Parser.LOG.isInfoEnabled()) {
      Parser.LOG.info("line#:cmd|        source");
      Parser.LOG.info("-----:---+-------------------------");
    }
  }

  @SuppressWarnings("PublicMethodWithoutLogging")
  @Override
  public void close()
      throws IOException {
    reader.close();
  }

  /**
   * Determine if there is another source code line to parse.
   *
   * @return {@code true} iff there is more source code
   * @throws IOException iff I/O problem
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  @SuppressWarnings("PMD.NullAssignment")
  boolean hasMoreCommands()
      throws IOException {
    final @Nullable String sourceLine = reader.readLine();

    if (sourceLine == null) {
      nextLine = "";
      if (echoSource) {
        Parser.LOG.info("<<EOF>>");
      }
    } else {
      @SuppressWarnings("HardcodedFileSeparator")
      final int commentAt = sourceLine.indexOf("//");

      // strip EOL-style comment and any leading/trailing spaces after that
      nextLine =
          (commentAt >= 0
               ? sourceLine.substring(0, commentAt) : sourceLine).trim();

      // determine assembly line type
      lineType = analyzeLine();

      if (echoSource && Parser.LOG.isInfoEnabled()) {
        Parser.LOG.info(String.format("%1$5s: %2$s |%3$s",
            Integer.toUnsignedString(reader.getLineNumber()),
            lineType, sourceLine));
      }
    }

    return sourceLine != null;
  }

  /**
   * Analyze the current source code line.
   *
   * @return type of the source code line
   * @throws AssemblerException iff unrecognized assembly source line type
   */
  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")//NOPMD
  @SuppressWarnings({
                        "PMD.AvoidLiteralsInIfCondition",
                        "PMD.OnlyOneReturn",
                        "ImplicitNumericConversion",
                        "MagicCharacter",
                        "MethodWithMultipleReturnPoints"
                    })
  private LineType analyzeLine() {
    //noinspection IfStatementWithTooManyBranches
    if (nextLine.isEmpty()) {
      // (could also have been an empty line)
      return LineType.COMMENT_ONLY;
    } else if (nextLine.charAt(0) == '@') {
      return LineType.A_COMMAND;
    } else if (nextLine.charAt(0) == '(') {
      return LineType.L_COMMAND;
    } else {
      equalsAt = nextLine.indexOf('=');
      semiAt = nextLine.indexOf(';');

      return LineType.C_COMMAND;
    }
  }

  /**
   * Determine the type of the current source code line.
   *
   * @return type of the current source code line
   */
  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  LineType commandType() {
    return lineType;
  }

  /**
   * Extract the symbol from an A-command or label in the source code.
   *
   * @return symbol embedded in the current source code line
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @SuppressWarnings("ImplicitNumericConversion")
  String symbol() {
    //noinspection MagicCharacter
    return nextLine.charAt(0) == '@'
               ? nextLine.substring(1)
               : nextLine.substring(1, nextLine.length() - 1);
  }

  /**
   * Extract the "dest" field from a C-command in the source code, trimming any
   * whitespace in the field.
   *
   * @return "dest" field in the current source code line
   */
  @SuppressFBWarnings({
                          "NP_NONNULL_RETURN_VIOLATION",
                          "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
                      })
  String dest() {
    return equalsAt < 0 ? "" : nextLine.substring(0, equalsAt).stripTrailing();
  }

  /**
   * Extract the "comp" field from a C-command in the source code, trimming any
   * whitespace in the field.
   *
   * @return "comp" field in the current source code line
   */
  @SuppressFBWarnings({
                          "NP_NONNULL_RETURN_VIOLATION",
                          "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
                      })
  String comp() {
    return semiAt < 0
               ? nextLine.substring(equalsAt + 1).stripLeading()
               : nextLine.substring(equalsAt + 1, semiAt).trim();
  }

  /**
   * Extract the "jump" field from a C-command in the source code, trimming any
   * whitespace in the field.
   *
   * @return "jump" field in the current source code line
   */
  String jump() {
    return semiAt > 0 ? nextLine.substring(semiAt + 1).stripLeading() : "";
  }

  /**
   * Determine the current line number being processed in the source code.
   *
   * @return current line number being processed in the source code
   */
  int getLineNumber() {
    return reader.getLineNumber();
  }

}

package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Analyze the Hack Assembler's command line.
 */
@SuppressWarnings({
                      "PMD.BeanMembersShouldSerialize",
                      "PMD.CommentDefaultAccessModifier",
                      "PMD.DefaultPackage",
                      "PMD.ModifiedCyclomaticComplexity",
                      "InstanceVariableMayNotBeInitialized",
                      "SuspiciousGetterSetter"
                  })
final class CommandArgs {

  /**
   * Self-help text.
   */
  private static final String[] SELF_HELP = {
      "Usage: Assembler [-[options]] asm-file [output-directory]",
      "  Generate a Hack binary file from the Hack assembly code file.",
      "  The Hack assembly code (asm-file) must end with extension '.asm'.",
      "  The generated Hack binary (hack-file) name will be the same as",
      "   asm-file but with extension '.hack'.",
      "  The hack-file will be placed in the output-directory.",
      "  The output-directory defaults to the asm-file's directory.",
      "\toptions may be any combination of the following characters:",
      "\t  1 -- list the asm-file in pass 1",
      "\t  2 -- list the asm-file in pass 2",
      "\t  c -- list the generated code of the hack-file",
      "\t  h -- give this self-help",
      "\t  p -- show the pathnames of the asm-file and hack-file",
      "\t  s -- dump system-defined symbols (implies 'u' option)",
      "\t  u -- dump user-defined symbols",
      "\tNote: -h implied when the assembler is invoked without any arguments."
  };

  /**
   * User-specified Hack assembly code source file.
   */
  private final File sourceFile;

  /**
   * User-specified Hack machine code binary file.
   */
  private final File binaryFile;

  /**
   * {@code true} iff command line option to generate machine code listing is
   * specified.
   */
  private boolean codeListing; // default {@code false}

  /**
   * {@code true} iff command line option to dump the reserved (system) symbols
   * is specified.
   */
  private boolean dumpSystemSymbols; // default {@code false}

  /**
   * {@code true} iff command line option to dump the user symbols is
   * specified.
   */
  private boolean dumpUserSymbols; // default {@code false}

  /**
   * {@code true} iff command line option to generate assembly code listing in
   * pass 1 is specified.
   */
  private boolean pass1Listing; // default {@code false}

  /**
   * {@code true} iff command line option to generate assembly code listing in
   * pass 2 is specified.
   */
  private boolean pass2Listing; // default {@code false}

  /**
   * {@code true} iff command line option for self-help (implicitly) specified.
   */
  @SuppressWarnings("InstanceVariableMayNotBeInitialized")
  private boolean selfHelp; // default {@code false}

  /**
   * {@code true} iff command line option to show assembly and machine code
   * filenames is specified.
   */
  @SuppressWarnings("InstanceVariableMayNotBeInitialized")
  private boolean showFilePaths; // default {@code false}

  /**
   * Constructor.
   *
   * @param args command line arguments, if any
   * @throws CommandLineException iff unable to initialize
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
                      "PMD.CyclomaticComplexity",
                      "PMD.StdCyclomaticComplexity",
                      "MagicCharacter"
  })
  public CommandArgs(final String... args) {
    //noinspection ImplicitNumericConversion,OverlyComplexBooleanExpression
    if (args == null || args.length <= 0
            || args[0].charAt(0) == '-' && args[0].contains("h")) {
      selfHelp = true; // implicit (or explicit) "-h"
      sourceFile = new File("dummySource");
      binaryFile = new File("dummyBinary");
    } else {
      int argIndex = 0;

      //noinspection ImplicitNumericConversion
      if (args[argIndex].charAt(0) == '-') {
        // check each single character option, skipping initial "-"
        for (final char option : args[0].substring(1).toCharArray()) {
          switch (option) {
            case 'c':
              codeListing = true;
              continue;
            case 'h':
              selfHelp = true;
              continue;
            case 'p':
              showFilePaths = true;
              continue;
            case 's':
              dumpSystemSymbols = true;
              continue;
            case 'u':
              dumpUserSymbols = true;
              continue;
            case '1':
              pass1Listing = true;
              continue;
            case '2':
              pass2Listing = true;
              continue;
            default:
              //noinspection AutoBoxing
              throw new CommandLineException("unrecognized option ('%1$c')",
                  option);
          }
        }
        dumpUserSymbols |= dumpSystemSymbols; // "-s" implies "-u" parseOptions(args[argIndex++]);
        ++argIndex; // consume the "options" argument
      }

      if (args.length <= argIndex) {
        throw new CommandLineException("missing asm-file");
      }

      sourceFile = CommandArgs.verifySourceFile(args[argIndex++]);

      final @Nullable File outputDir =
          args.length <= argIndex
              ? sourceFile.getParentFile() : new File(args[argIndex]);

      if (outputDir == null) {
        final String detail = String.format(
            "source file has no containing directory (%1$s)",
            sourceFile.getAbsolutePath());

        throw new CommandLineException(detail);
      }

      binaryFile = CommandArgs.verifyBinaryFile(sourceFile, outputDir);
    }
  }

  /**
   * Verify the source file.
   *
   * @param asmFileName command line argument for Hack assembly  file
   * @return existent file with proper file name extension
   * @throws CommandLineException iff unreadable file -or- invalid file name
   *     extension detected
   */
  @SuppressFBWarnings({
                          "PATH_TRAVERSAL_IN",
                          "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
                      })
  private static @NonNull File verifySourceFile(final String asmFileName) {
    if (!asmFileName.endsWith(".asm")) {
      throw new CommandLineException(
          "asm-file (%1$s) must have a '.asm' filename extension",
          asmFileName);
    }

    final File sourceFile = new File(asmFileName);

    if (!sourceFile.canRead()) {
      //noinspection HardcodedFileSeparator
      throw new CommandLineException("unable to find/read asm-file (%1$s)",
          asmFileName);
    }

    return sourceFile;
  }

  /**
   * Verify the binary file.
   *
   * @param sourceFile verified Hack assembly source file
   * @param outputDir proposed location for the binary file
   * @return file to use for binary file
   * @throws CommandLineException iff invalid <em>outputDir</em> specified
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  private static File verifyBinaryFile(final File sourceFile,
                                       final File outputDir) {
    if (!outputDir.isDirectory()) {
      throw new CommandLineException(
          "output-directory (%1$s) is not a directory",
          outputDir);
    }

    final String inputName = sourceFile.getName();
    @SuppressWarnings(
        { "PMD.LawOfDemeter", "StringConcatenation" }) final String outputName =
        inputName.substring(0, inputName.length() - 3) + "hack";

    return new File(outputDir, outputName);
  }

  /**
   * Determine if the code listing option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  boolean isCodeListingWanted() {
    return codeListing;
  }

  /**
   * Determine if the self-help option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  public boolean isSelfHelpWanted() {
    return selfHelp;
  }

  /**
   * Determine if the code listing option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  boolean isSystemSymbolDumpWanted() {
    return dumpSystemSymbols;
  }

  /**
   * Determine if the code listing option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  boolean isUserSymbolsDumpWanted() {
    return dumpUserSymbols;
  }

  /**
   * Determine if the code listing option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  boolean isPass1ListingWanted() {
    return pass1Listing;
  }

  /**
   * Determine if the code listing option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  boolean isPass2ListingWanted() {
    return pass2Listing;
  }

  /**
   * Determine if the code listing option was specified.
   *
   * @return {@code true} iff option appeared on command line
   */
  public boolean isFilePathOutputWanted() {
    return showFilePaths;
  }

  /**
   * Determine the input Hack assembly file.
   *
   * @return Hack assembly code source file
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  public File getSourceFile() {
    return Objects.requireNonNull(sourceFile);
  }

  /**
   * Determine the output Hack binary file.
   *
   * @return Hack assembly code binary file
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  public File getBinaryFile() {
    return Objects.requireNonNull(binaryFile);
  }

  /**
   * Give permissible forms of arguments for assembler usage.
   *
   * @return description of command line arguments (usage)
   */
  public static List<String> usage() {

    return new ArrayList<>(Arrays.asList(CommandArgs.SELF_HELP));
  }

}

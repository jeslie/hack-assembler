package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;

/**
 * The Hack Assembler.
 */
@SuppressWarnings({
                      "PMD.BeanMembersShouldSerialize",
                      "PMD.CommentSize",
                      "PMD.LawOfDemeter",
                      "PMD.ShortClassName"
                      , "unused"
                  })
public final class HackAssembler {

  /**
   * Largest possible 15-bit unsigned integer.
   */
  private static final int MAX_INT15 = (0x01 << 15) - 1;

  /**
   * Definition of a valid Hack assembler constant.
   */
  private static final @Regex Pattern CONSTANT_REGEX =
      Pattern.compile("[0-9]+");

  /**
   * Definition of a valid Hack assembler symbol.
   */
  private static final @Regex Pattern SYMBOL_REGEX =
      Pattern.compile("[a-zA-Z][a-zA-Z0-9_.$:]*");

  /** Parsed "command line arguments". */
  private final CommandArgs parsedArgs;

  /**
   * Symbol table.
   */
  private final SymbolTable symtab = new SymbolTable();

  /** The last error from a {@link #process()} invocation. */
  private @Nullable Exception exception;

  /**
   * Constructor.
   *
   * @param args "command line" arguments for the Hack assembler
   */
  @SuppressWarnings({
                        "PMD.AvoidCatchingGenericException",
                        "PMD.NullAssignment"
                    })
  public HackAssembler(final String... args)  {
    CommandArgs commandArgs;

    try {
      commandArgs = new CommandArgs(args); // might fail
      exception = null;
    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Exception ex) {
      commandArgs = new CommandArgs(); // can't fail
      exception = ex;
    }
    parsedArgs = commandArgs;
  }

  /**
   * Determine the Hack assembly code file used as input
   *
   * @return Hack assembly code file
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE") //NOPMD
  public File getSourceFile() {
    return parsedArgs.getSourceFile();
  }

  /**
   * Determine the Hack binary code file used as output
   *
   * @return Hack binary code file
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @SuppressWarnings("unused")
  public File getBinaryFile() {
    return parsedArgs.getBinaryFile();
  }

  /**
   * Determine if some text is a (decimal) constant.
   *
   * @param text possible decimal constant
   * @return {@code true} iff <em>text</em> is a decimal constant
   */
  private static boolean isConstant(final String text) {
    return HackAssembler.CONSTANT_REGEX.matcher(text).matches();
  }

  /**
   * Determine if some text is a (Hack) symbol.
   *
   * @param text possible Hack symbol
   * @return {@code true} iff <em>text</em> is a Hack symbol
   */
  private static boolean isSymbol(final String text) {
    return HackAssembler.SYMBOL_REGEX.matcher(text).matches();
  }

  /**
   * Pass 1 logic for an A-command statement.
   *
   * @param constant address symbol/constant
   */
  private void pass1Constant(final String constant) {
    if (HackAssembler.isConstant(constant)) {
      final int integer = Integer.parseInt(constant);

      if (integer > HackAssembler.MAX_INT15) {
        throw new AssemblerException("integer too large: %1$d (0x%1$s)",
            Integer.toHexString(integer));
      }
    } else if (HackAssembler.isSymbol(constant)) {
      symtab.addMemorylessSymbol(constant);
    } else {
      //noinspection HardcodedFileSeparator
      throw new AssemblerException("invalid symbol/constant: %1$s", constant);
    }
  }

  /**
   * Pass 1 logic for a label statement.
   *
   * @param label address symbol
   * @param instructionCounter current calculated location in ROM
   */
  private void pass1Label(final String label,
                          @SuppressWarnings("PMD.LongVariable")
                          final @NonNegative int instructionCounter) {
    if (!HackAssembler.isSymbol(label)) {
      throw new AssemblerException("invalid label: '%1$s'", label);
    }

    symtab.addLabelSymbol(label, instructionCounter);
  }

  /**
   * In pass 1 of the assembler, all integer constants are checked for size and
   * all symbols/labels defined.
   *
   * @param showSource {@code true} iff list source code as encountered
   * @param sourceFile Hack assembly code source file
   * @return label symbols as a map (ROM Address -&gt; label)
   * @throws AssemblerException iff problem encountered with
   *     constants/symbols
   * @throws IOException iff I/O problem
   */
  @SuppressFBWarnings({
                          "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
                          "SF_SWITCH_FALLTHROUGH"
                      })
  @SuppressWarnings({
                        "PMD.AssignmentInOperand",
                        "PMD.CyclomaticComplexity",
                        "PMD.DataflowAnomalyAnalysis",
                        "PMD.UnnecessaryModifier"
                    })
  private Map<@NonNull Integer, @NonNull String> pass1(
      final boolean showSource, final File sourceFile)
      throws IOException {
    int currentLine = 0;

    try (final Parser parser =
             new Parser(parsedArgs.isFilePathOutputWanted(), sourceFile,
                 showSource)) {
      @NonNegative int romAddress = 0; // instructions start at location 0

      //noinspection MethodCallInLoopCondition
      while (parser.hasMoreCommands()) {
        currentLine = parser.getLineNumber();
        switch (parser.commandType()) {
          case A_COMMAND: // declare all symbols
            pass1Constant(parser.symbol());
            // fallthru
          case C_COMMAND:
            // check for ROM overflow; RAM done in pass 2
            if (++romAddress > HackAssembler.MAX_INT15 + 1) {
              //noinspection ThrowCaughtLocally
              throw new AssemblerException("ROM capacity exceeded");
            }
            continue;
          case COMMENT_ONLY: // nothing to do
            continue;
          case L_COMMAND: // define all labels
            pass1Label(parser.symbol(), romAddress);
            continue;
          default:
            //noinspection ThrowCaughtLocally
            throw new AssemblerException("unrecognized assembly line type");
        }
      }

      return symtab.resolveUserSymbols();
    } catch (final AssemblerException ex) {
      final AssemblerException chainedEx =
          new AssemblerException("line %1$s: %2$s",
              Integer.toString(currentLine), ex.getMessage());

      chainedEx.initCause(ex);
      throw chainedEx;
    }
  }

  /**
   * In pass 2 of the assembler, all commands are validated and appropriate code
   * generated.
   *
   * @param showSource {@code true} iff list source code as encountered
   * @param showBinary {@code true} iff list binary code as generated
   * @param sourceFile Hack assembly code source file
   * @param binaryFile Hack assembly code binary file
   * @param labelTable reverse symbol table for labels
   * @throws AssemblerException iff problem encountered with code
   *     generation
   * @throws IOException iff I/O problem
   */
  @SuppressWarnings({
                        "PMD.DataflowAnomalyAnalysis",
                        "PMD.UnnecessaryModifier"
                    })
  private void pass2(final boolean showSource,
                     final boolean showBinary,
                     final File sourceFile,
                     final File binaryFile,
                     final Map<@NonNull Integer, @NonNull String> labelTable)
      throws IOException {
    int currentLine = 0;

    try (final Parser parser = new Parser(false, sourceFile, showSource);
         final CodeGen codeGen =
             new CodeGen(parsedArgs.isFilePathOutputWanted(), binaryFile,
                 labelTable, showBinary)) {
      //noinspection MethodCallInLoopCondition
      while (parser.hasMoreCommands()) {
        currentLine = parser.getLineNumber();
        switch (parser.commandType()) {
          case A_COMMAND:
            final String address = parser.symbol();

            if (HackAssembler.isConstant(address)) {
              codeGen.generateA(Integer.parseInt(address), null);
            } else {
              codeGen.generateA(symtab.getAddress(address), address);
            }
            continue;
          case C_COMMAND:
            codeGen.generateC(parser.comp(), parser.dest(), parser.jump());
            continue;
          default:
            // all other line types have been previously handled
        }
      }
    } catch (final AssemblerException ex) {
      final AssemblerException chainedEx =
          new AssemblerException("line %1$s: %2$s",
              Integer.toString(currentLine), ex.getMessage());

      chainedEx.initCause(ex);
      throw chainedEx;
    }
  }

  /**
   * Execute (run) the Hack assembler processing.
   *
   * @return this assembler (for fluent-style invocations)
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @SuppressWarnings("PMD.NullAssignment")
  public HackAssembler process() {
    if (!hasFailed()) { // don't attempt after failure
      if (parsedArgs.isSelfHelpWanted()) {
        CommandArgs.usage().forEach(System.out::println);
      } else {
        try {
          final Map<@NonNull Integer, @NonNull String> labelTable
              = pass1(parsedArgs.isPass1ListingWanted(), parsedArgs.getSourceFile());

          if (parsedArgs.isUserSymbolsDumpWanted()) {
            symtab.dump(parsedArgs.isSystemSymbolDumpWanted());
          }

          pass2(parsedArgs.isPass2ListingWanted(),
              parsedArgs.isCodeListingWanted(), parsedArgs.getSourceFile(),
              parsedArgs.getBinaryFile(), labelTable);
        } catch (final CommandLineException ex) {
          //noinspection StringConcatenation
          exception = new Exception("ERROR: " + ex.getMessage());
        } catch (final AssemblerException ex) {
          exception = new Exception(ex.getMessage()); // already formatted
        } catch (final IOException ex) {
          exception = ex;
        }
      }
    }

    //noinspection ReturnOfThis
    return this;
  }

  /**
   * Determine the error (an {@link Exception}) that resulted from the last
   * {@link #process()} invocation.
   *
   * @return   {@link Exception} iff assembler failed in some manner, otherwise
   * {@code null}
   */
  @SuppressWarnings("SuspiciousGetterSetter")
  public @Nullable Exception getError() {
    return exception;
  }

  /**
   * Determine if there was an error in the last {@link #process()} invocation.
   *
   * @return {@code true} iff assembler completed <em>UN</em>successfully
   */
  public boolean hasFailed() {
    return exception != null;
  }
}


package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hack machine language code generator.
 */
@SuppressWarnings({
                      "PMD.BeanMembersShouldSerialize",
                      "PMD.CommentSize",
                      "PMD.LawOfDemeter",
                      "AutoBoxing",
                      "DuplicateStringLiteralInspection",
                      "MethodWithMultipleReturnPoints",
                      "StringConcatenation",
                      "StringConcatenationMissingWhitespace"
                  })
final class CodeGen implements Closeable {

  /**
   * Class logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(CodeGen.class);

  /**
   * Predefined (system) symbols.
   */
  @SuppressWarnings({
                        "PMD.UseConcurrentHashMap",
                        "CollectionWithoutInitialCapacity"
                    })
  private static final Map<@NonNull String, String> COMP_CODES =
      new HashMap<>();

  static {
    //       comp  a   c[1-6]
    CodeGen.COMP_CODES.put("0", "0" + "101010");
    CodeGen.COMP_CODES.put("1", "0" + "111111");
    CodeGen.COMP_CODES.put("-1", "0" + "111010");
    CodeGen.COMP_CODES.put("D", "0" + "001100");
    CodeGen.COMP_CODES.put("A", "0" + "110000");
    CodeGen.COMP_CODES.put("!D", "0" + "001101");
    CodeGen.COMP_CODES.put("!A", "0" + "110001");
    CodeGen.COMP_CODES.put("-D", "0" + "001111");
    CodeGen.COMP_CODES.put("-A", "0" + "110011");
    CodeGen.COMP_CODES.put("D+1", "0" + "011111");
    CodeGen.COMP_CODES.put("A+1", "0" + "110111");
    CodeGen.COMP_CODES.put("D-1", "0" + "001110");
    CodeGen.COMP_CODES.put("A-1", "0" + "110010");
    CodeGen.COMP_CODES.put("D+A", "0" + "000010");
    CodeGen.COMP_CODES.put("D-A", "0" + "010011");
    CodeGen.COMP_CODES.put("A-D", "0" + "000111");
    CodeGen.COMP_CODES.put("D&A", "0" + "000000");
    CodeGen.COMP_CODES.put("D|A", "0" + "010101");
    CodeGen.COMP_CODES.put("M", "1" + "110000");
    CodeGen.COMP_CODES.put("!M", "1" + "110001");
    CodeGen.COMP_CODES.put("-M", "1" + "110011");
    CodeGen.COMP_CODES.put("M+1", "1" + "110111");
    CodeGen.COMP_CODES.put("M-1", "1" + "110010");
    CodeGen.COMP_CODES.put("D+M", "1" + "000010");
    CodeGen.COMP_CODES.put("D-M", "1" + "010011");
    CodeGen.COMP_CODES.put("M-D", "1" + "000111");
    CodeGen.COMP_CODES.put("D&M", "1" + "000000");
    CodeGen.COMP_CODES.put("D|M", "1" + "010101");
  }

  /**
   * Binary file writer: only text {@code 0}s, {@code 1}s and newlines are
   * output to this file.
   */
  private final PrintWriter writer;

  /**
   * Table of all labels and their ROM address. If multiple labels refer to the
   * same location, only one will be preserved in this table.
   */
  @SuppressWarnings({
                        "PMD.UseConcurrentHashMap",
                        "CollectionWithoutInitialCapacity"
                    })
  private final Map<@NonNull Integer, @NonNull String> labelTable =
      new HashMap<>();

  /**
   * ROM address for next binary word.
   */
  @SuppressWarnings("PMD.RedundantFieldInitializer")
  private @NonNegative int romAddress = 0; // default

  /**
   * {@code true} iff want a formatted listing of the generated binary code (in
   * addition to the file).
   */
  private final boolean echoCode;

  /**
   * Constructor.
   *
   * @param showPath {@code true} iff show binary file pathname
   * @param hackFile hack machine code (aka, binary) file to be generated
   * @param labelMap reverse symbol table for labels
   * @param showCode {@code true} iff list machine code as generated
   * @throws IOException iff I/O problem
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS") //NOPMD
  @SuppressWarnings("PMD.CommentDefaultAccessModifier")
  CodeGen(final boolean showPath,
          final File hackFile,
          final Map<@NonNull Integer, String> labelMap,
          final boolean showCode)
      throws IOException {
    if (showPath) {
      CodeGen.LOG.info("hack-file:  {}", hackFile.getCanonicalFile());
    }

    writer = new PrintWriter(Files.newBufferedWriter(hackFile.toPath(),
        StandardCharsets.UTF_8));
    labelTable.putAll(labelMap);
    echoCode = showCode;

    if (echoCode) {
      CodeGen.LOG.info(
          " ROM =   machine code   |     details");
      CodeGen.LOG.info(
          "-----=------------------+------------------------------");
    }
  }

  @SuppressWarnings("PublicMethodWithoutLogging")
  @Override
  public void close() {
    writer.close();
  }

  /**
   * Convert the "comp" portion of a C-command to its bit pattern.
   *
   * @param mnemonic "comp" portion of a C-command assembly instruction
   * @return corresponding bit pattern of a C-command machine instruction
   * @throws AssemblerException iff <em>mnemonic</em> is unrecognized
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  private static String comp(final String mnemonic) {
    final @Nullable String result = CodeGen.COMP_CODES.get(mnemonic);

    if (result == null) {
      throw new AssemblerException("unrecognized comp mnemonic: %1$s",
          mnemonic);
    }

    return result;
  }

  /**
   * Convert the "dest" portion of a C-command to its bit pattern.
   *
   * @param mnemonic "dest" portion of a C-command assembly instruction
   * @return corresponding bit pattern of a C-command machine instruction
   * @throws AssemblerException iff <em>mnemonic</em> is unrecognized
   */
  @SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.OnlyOneReturn" })
  private static String dest(final String mnemonic) {
    switch (mnemonic) {
      case "":
        return "000";
      case "M":
        return "001";
      case "D":
        return "010";
      case "MD":
        return "011";
      case "A":
        return "100";
      case "AM":
        return "101";
      case "AD":
        return "110";
      case "AMD":
        return "111";
      default:
        throw new AssemblerException("unrecognized dest mnemonic: %1$s",
            mnemonic);
    }
  }

  /**
   * Convert the "jump" portion of a C-command to its bit pattern.
   *
   * @param mnemonic "jump" portion of a C-command assembly instruction
   * @return corresponding bit pattern of a C-command machine instruction
   * @throws AssemblerException iff <em>mnemonic</em> is unrecognized
   */
  @SuppressWarnings({
                        "PMD.CyclomaticComplexity",
                        "PMD.OnlyOneReturn",
                        "MethodWithMultipleReturnPoints"
                    })
  private static String jump(final String mnemonic) {
    switch (mnemonic) {
      case "":
        return "000";
      case "JGT":
        return "001";
      case "JEQ":
        return "010";
      case "JGE":
        return "011";
      case "JLT":
        return "100";
      case "JNE":
        return "101";
      case "JLE":
        return "110";
      case "JMP":
        return "111";
      default:
        throw new AssemblerException("unrecognized jmp mnemonic: %1$s",
            mnemonic);
    }
  }

  /**
   * Show a label in the listing if one is associated with the current ROM
   * address.
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  private void showLabel() {
    if (echoCode) {
      final @Nullable String label = labelTable.get(romAddress);

      if (label != null) {
        final String output =
            String.format("%1$5d=%2$16s  | label[%3$s]",
                romAddress, "", label);

        CodeGen.LOG.info(output);
      }
    }
  }

  /**
   * Generate a machine language A-instruction; generate corresponding machine
   * language listing if appropriate.
   *
   * @param address value to embed in the A-instruction
   * @param symbol symbol associated with the <em>address</em>; {@code null}
   *     if none
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  @SuppressWarnings({
                        "PMD.CommentDefaultAccessModifier",
                        "PMD.DefaultPackage"
                    })
  void generateA(final int address, final @Nullable String symbol) {
    final StringBuilder buffer = new StringBuilder("0");

    for (int mask = 0x4000; mask != 0; mask >>= 1) {
      //noinspection MagicCharacter
      buffer.append((address & mask) == 0 ? '0' : '1');
    }

    final String code = buffer.toString();

    if (echoCode) {
      showLabel();
      final String output =
          String.format("%1$5d=%2$16s  | address[%3$5d=0x%3$04x]",
              romAddress++, code, address);

      if (symbol == null) {
        CodeGen.LOG.info("{}", output);
      } else {
        CodeGen.LOG.info("{} @{}", output, symbol);
      }
    }

    writer.println(code);
  }

  /**
   * Generate a machine language C-instruction; generate corresponding machine
   * language listing if appropriate.
   *
   * @param compMnemonic mnemonic of the "comp" portion
   * @param destMnemonic mnemonic of the "dest" portion
   * @param jumpMnemonic mnemonic of the "jump" portion
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  @SuppressWarnings({
                        "PMD.CommentDefaultAccessModifier",
                        "PMD.DefaultPackage"
                    })
  void generateC(final String compMnemonic,
                 final String destMnemonic,
                 final String jumpMnemonic) {
    final String code = "111"
                            + CodeGen.comp(compMnemonic)
                            + CodeGen.dest(destMnemonic)
                            + CodeGen.jump(jumpMnemonic);

    showLabel();
    if (echoCode) {
      final StringBuilder output =
          new StringBuilder(String.format("%1$5d=%2$s  | comp[%3$3s] ",
              romAddress++, code, compMnemonic));

      if (destMnemonic.isEmpty()) {
        output.append("         ");
      } else {
        output.append(String.format("dest[%1$3s]", destMnemonic));
      }
      if (!jumpMnemonic.isEmpty()) {
        output.append(String.format(" jump[%1$3s]", jumpMnemonic));
      }

      CodeGen.LOG.info(output.toString());
    }

    writer.println(code);
  }

}

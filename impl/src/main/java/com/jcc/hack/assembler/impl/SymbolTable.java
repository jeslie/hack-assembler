package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hack assembler symbol table.
 * <p>
 * Note: RAM addresses are stored as positive integers, ROM address are stored
 * as negative integers offset by {@code 1}. For example, if {@code @foo} refers
 * to ROM address 12, it is placed in the symbol table as {@code -13}, but if it
 * refers to a RAM address it is placed in the symbol table as {@code 12}.
 * </p>
 */
@SuppressWarnings({
                      "PMD.BeanMembersShouldSerialize",
                      "PMD.CommentDefaultAccessModifier",
                      "PMD.CommentSize",
                      "PMD.DefaultPackage",
                      "PMD.LawOfDemeter",
                      "AutoBoxing",
                      "AutoUnboxing"
                  })
final class SymbolTable {

  /**
   * Logger.
   */
  private static final Logger LOG =
      LoggerFactory.getLogger(SymbolTable.class);

  /**
   * Predefined (system) symbols. (sorted for listing by {@link #dump(boolean)}).
   */
  @SuppressWarnings("PMD.UseConcurrentHashMap")
  private static final SortedMap<@NonNull String, Integer> PREDEFINED_SYMTAB =
      new TreeMap<>();

  static {
    SymbolTable.PREDEFINED_SYMTAB.put("SP", 0);
    SymbolTable.PREDEFINED_SYMTAB.put("LCL", 1);
    SymbolTable.PREDEFINED_SYMTAB.put("ARG", 2);
    SymbolTable.PREDEFINED_SYMTAB.put("THIS", 3);
    SymbolTable.PREDEFINED_SYMTAB.put("THAT", 4);
    SymbolTable.PREDEFINED_SYMTAB.put("SCREEN", 0x4000);
    SymbolTable.PREDEFINED_SYMTAB.put("KBD", 0x6000);
    for (int reg = 0; reg < 16; ++reg) {
      SymbolTable.PREDEFINED_SYMTAB.put(String.format("R%d", reg), reg);
    }
  }

  /**
   * Initial value for an undefined symbol in the symbol table.
   */
  private static final int UNDEFINED_SYMBOL = Integer.MIN_VALUE;

  /**
   * First address available for user data in RAM (after dedicated memory for
   * registers).
   */
  @SuppressWarnings("PMD.LongVariable")
  private static final int FIRST_AVAILABLE_RAM_ADDRESS = 16;

  /**
   * Last address available for user data in RAM (before dedicated memory for
   * the screen).
   */
  @SuppressWarnings("PMD.LongVariable")
  private static final int LAST_AVAILABLE_RAM_ADDRESS = 0x4000 - 1;

  /**
   * Section marker in the symbol table (in {@link #dump(boolean)}.
   */
  private static final String SECTION_SEPARATOR = "========";

  /**
   * Mapping of symbols to their corresponding ROM/RAM address (sorted for
   * listing by  {@link #dump(boolean)}).
   */
  private final SortedMap<@NonNull String, Integer> symbolsTable =
      new TreeMap<>();

  /**
   * Constructor.
   */
  SymbolTable() {
    // ensure all predefined symbols exist in user symbol table
    symbolsTable.putAll(SymbolTable.PREDEFINED_SYMTAB);
  }

  /**
   * Declares an entry in the symbol table.
   *
   * @param symbol name of a (possibly previously undeclared) symbol
   */
  @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
  void addMemorylessSymbol(final String symbol) {
    symbolsTable.putIfAbsent(symbol, SymbolTable.UNDEFINED_SYMBOL);
  }

  private boolean isUndefinedSymbol(final String symbol) {
    return !symbolsTable.containsKey(symbol)
               || SymbolTable.UNDEFINED_SYMBOL == symbolsTable.get(symbol);
  }

  /**
   * Adds the pair (symbol, address) to the symbol table.
   *
   * @param symbol name of a (possibly previously undeclared) symbol
   * @param address location in memory to be associated with
   *     <em>symbol</em>
   * @throws AssemblerException iff <em>symbol</em> already defined
   */
  @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
  void addLabelSymbol(final @NonNull String symbol,
                      final @NonNegative int address) {
    if (symbolsTable.containsKey(symbol)) {
      final @Nullable Integer priorAddress =
          symbolsTable.put(symbol, -1 - address); // mapped ROM address

      //noinspection UnnecessaryUnboxing
      if (priorAddress != null
              && priorAddress.intValue() != SymbolTable.UNDEFINED_SYMBOL) {
        throw new AssemblerException("duplicate symbol: %1$s", symbol);
      }
    } else {
      symbolsTable.put(symbol, -1 - address); // mapped ROM address
    }
  }

  /**
   * Determine if a defined symbol refers to RAM or ROM; this is only valid in
   * pass 2.
   *
   * @param symbol name of a symbol
   * @return {@code true} iff <em>symbol</em> is defined and refers to ROM
   */
  private boolean isInRom(final String symbol) {
    final @Nullable Integer address = symbolsTable.get(symbol);

    return address != null && address < 0;
  }

  /**
   * Determine the address associated with a symbol.
   * <p>
   * Note: Whether the symbol refers to RAM or ROM, the value returned is always
   * that used in the original definition (via {@link #addLabelSymbol(String,
   * int)} or through {@link #resolveUserSymbols()}).
   * </p>
   *
   * @param symbol name of a previously declared symbol
   * @return address associated with the <em>symbol</em>
   * @throws AssemblerException iff <em>symbol</em> not previously defined
   */
  @NonNegative int getAddress(final String symbol) {
    if (isUndefinedSymbol(symbol)) {
      throw new AssemblerException("undefined symbol: %1$s", symbol);
    }

    final Integer rawAddress = symbolsTable.get(symbol);

    if (rawAddress == null) {
      throw new AssemblerException("unreachable");
    }

    @SuppressWarnings("UnnecessaryUnboxing")
    final int address = rawAddress.intValue();

    return address < 0 ? -(address + 1) : address; // ROM addresses are < 0
  }

  /**
   * Resolve (define) all RAM addresses in symbol table.
   *
   * @return reverse symbol table for labels
   * @throws AssemblerException iff insufficient RAM for all user variables
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @SuppressWarnings({ "PMD.UseConcurrentHashMap", "ForeachStatement" })
  Map<@NonNull Integer, @NonNull String> resolveUserSymbols() {
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    final Map<@NonNull Integer, @NonNull String> invertedSymTab =
        new HashMap<>(symbolsTable.size());
    @NonNegative int ramAddress = SymbolTable.FIRST_AVAILABLE_RAM_ADDRESS;

    for (final Map.Entry<@NonNull String, @NonNull Integer> entry
        : symbolsTable.entrySet()) {
      final int currentAddress = entry.getValue();

      if (currentAddress == SymbolTable.UNDEFINED_SYMBOL) {
        entry.setValue(ramAddress++);
      } else if (currentAddress < 0) {
        // NOTE: label reverse symtab keeps at most 1 symbol per address
        invertedSymTab.put(-(currentAddress + 1), entry.getKey());
      }
    }

    // RAM address refers to next available address
    if (ramAddress - 1 > SymbolTable.LAST_AVAILABLE_RAM_ADDRESS) {
      throw new AssemblerException("RAM capacity exceeded");
    }

    return invertedSymTab;
  }

  /**
   * Dump/list a named symbol table entry.
   *
   * @param name symbol table entry name
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  private void dumpNamedSymbol(final String name) {
    final String output =
        String.format("%1$40s: %2$3s 0x%3$04x (%3$5d)",
            name, isInRom(name) ? "ROM" : "RAM", getAddress(name));

    SymbolTable.LOG.info(output);
  }

  /**
   * Dump/list the symbol table (predefined) constants.
   */
  private void dumpConstants() {
    SymbolTable.LOG.info("CONSTANTS:");
    SymbolTable.PREDEFINED_SYMTAB.keySet()
        .forEach(this::dumpNamedSymbol);
  }

  /**
   * Dump/list the symbol table (user) variables/labels.
   *
   * @param wantLabels {@code true} iff dump the user labels (in ROM) instead
   *     of the user variables (in RAM)
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  private void dumpSymbols(final boolean wantLabels) {
    SymbolTable.LOG.info(wantLabels ? "LABELS:" : "DATA:");
    symbolsTable.keySet()
        .forEach(symbol -> {
          if (isInRom(symbol) == wantLabels
                  && !SymbolTable.PREDEFINED_SYMTAB.containsKey(symbol)) {
            dumpNamedSymbol(symbol);
          }
        });
  }

  /**
   * Dump/list the symbol table.
   *
   * @param constantsToo {@code true} iff dump the (constant) system symbols
   *     along with the user symbols
   */
  void dump(final boolean constantsToo) {
    if (constantsToo) {
      dumpConstants();
      SymbolTable.LOG.info(SymbolTable.SECTION_SEPARATOR);
    }

    dumpSymbols(false);
    SymbolTable.LOG.info(SymbolTable.SECTION_SEPARATOR);
    dumpSymbols(true);
  }

}

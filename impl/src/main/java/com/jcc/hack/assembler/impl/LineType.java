package com.jcc.hack.assembler.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An enum for classifying source code line type.
 */
@SuppressWarnings("EnumClass")
enum LineType {

  /**
   * An A-type command assembly line.
   */
  A_COMMAND('A'),
  /**
   * A C-type command assembly line.
   */
  C_COMMAND('C'),
  /**
   * A label-defining assembly line.
   */
  L_COMMAND(':'),
  /**
   * A comment-only/blank assembly line.
   */
  COMMENT_ONLY('#');

  /**
   * Single character representation of a line of the indicated type.
   */
  private final String key;

  /**
   * Constructor.
   *
   * @param mnemonic single-character symbol associated with this type of
   *     assembly line
   */
  LineType(final char mnemonic) {
    key = Character.toString(mnemonic);
  }

  // inherit javadoc
  @SuppressFBWarnings("NP_TOSTRING_COULD_RETURN_NULL")
  @SuppressWarnings("PublicMethodWithoutLogging")
  @Override
  public String toString() {
    return key;
  }

}

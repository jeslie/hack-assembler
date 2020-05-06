package com.jcc.hack.assembler.main;

import com.jcc.hack.assembler.impl.HackAssembler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The Hack Assembler.
 */
@SuppressWarnings({
                      "PMD.CommentSize",
                      "PMD.LawOfDemeter",
                      "PMD.ShortClassName",
                      "ClassIndependentOfModule",
                      "ClassNamePrefixedWithPackageName",
                      "ClassOnlyUsedInOneModule",
                      "ClassOnlyUsedInOnePackage",
                      "ClassUnconnectedToPackage",
                      "UtilityClassCanBeEnum",
                      "WeakerAccess"
                  })
public final class Main {

  /**
   * Constructor.
   */
  private Main() {
    // no nothing extra
  }

  /**
   * Program entry point; non-zero exit code if an error is detected.
   *
   * @param args command line arguments, if any
   */
  @SuppressFBWarnings("INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE")
  @SuppressWarnings({
                        "PMD.DoNotCallSystemExit",
                        "PMD.SystemPrintln",
                        "CallToSystemExit"
                    })
  public static void main(final String... args) {
    final HackAssembler assembler = new HackAssembler(args);

    // check for either command line failure or assembler failure
    if (assembler.hasFailed() || assembler.process().hasFailed()) {
      final @Nullable Exception exception = assembler.getError();
      final @Nullable String detail =
          exception == null ? null : exception.getMessage();

      System.err.println(detail == null || detail.isBlank()
          ? "???no information on failure???" : detail);
      if (exception instanceof IOException) {
        exception.printStackTrace(System.err); // extra details for failed I/O
      }

      System.exit(1);
    }
  }

}

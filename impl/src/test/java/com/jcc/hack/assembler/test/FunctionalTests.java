package com.jcc.hack.assembler.test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.After;
import org.junit.Test;

@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
@SuppressWarnings({
                      "PMD.AtLeastOneConstructor",
                      "PMD.BeanMembersShouldSerialize",
                      "PMD.CommentRequired",
                      "PMD.LawOfDemeter",
                      "JUnitTestNG",
                      "NonBooleanMethodNameMayNotStartWithQuestion",
                      "StringConcatenation"
                  })
public final class FunctionalTests {

  @SuppressWarnings("PMD.LongVariable")
  private static final int MAX_RUNTIME_SECONDS = 5; // per assembler attempt

  @SuppressWarnings("AccessOfSystemProperties")
  private static final @NonNull File OUTPUT_DIR =
      new File(System.getProperty("java.io.tmpdir"));

  private static final FilenameFilter ASM_FILTER =
      (dir, name) -> name.endsWith(".asm");

  private final SoftAssertions softly;

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public FunctionalTests() {
    softly = new SoftAssertions();
  }

  @After
  public void tearDown() {
    softly.assertAll();
  }

  private static File[] getResourceFiles(final String folder)
      throws ClassNotFoundException {
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    if (loader == null) {
      throw new ClassNotFoundException("unable to locate class loader");
    }

    final @Nullable URL url = loader.getResource(folder);

    if (url == null) {
      throw new ClassNotFoundException("unable to locate test resources");
    }

    final File [] resourceFiles =
        new File(url.getPath()).listFiles(FunctionalTests.ASM_FILTER);

    if (resourceFiles == null) {
      throw new ClassNotFoundException("unable to locate test resources");
    }

    return resourceFiles;
  }

  private static @NonNull String getSourceBasename(final File sourceFile) {
    final @NonNull String basename = sourceFile.getName();

    return basename.substring(0, basename.length() - 3); // strip "asm" extension
  }

  private static @NonNull File getResultFile(final File sourceFile) {
    final String basename = FunctionalTests.getSourceBasename(sourceFile);
    final String resultName = basename + "asm";

    return new File(FunctionalTests.OUTPUT_DIR, resultName);
  }

  private static @NonNull File getSolutionFile(final File sourceFile) {
    final String basename = FunctionalTests.getSourceBasename(sourceFile);
    final String solutionName = basename + "expected";

    return new File(sourceFile.getParent(), solutionName);
  }

  private static void runAssembler(final File sourceFile)
      throws IOException {
    final Runner runner =
        new Runner("1cpu", sourceFile, FunctionalTests.OUTPUT_DIR);

    runner.setTimeoutSeconds(FunctionalTests.MAX_RUNTIME_SECONDS);
    try {
      if (!runner.run()) {
        // FAILED RUN
        @SuppressWarnings("HardcodedLineSeparator")
        final String output =
            runner.getOutput().collect(Collectors.joining("\n"));
        final String detail =
            "assembler failure: "
                + (output.isEmpty() ? "<<no output>>" : output);

        throw new IOException(detail);
      }
    } catch (final InterruptedException ex) { // recast timeout as I/O issue
      final String detail =
          "timeout after " + runner.getTimeoutSeconds() + " seconds";

      throw new IOException(detail, ex);
    }
  }

  @SuppressWarnings("OverlyBroadThrowsClause")
  private static @NonNull LineNumberReader getReader(final File file)
  throws IOException {
    if (!file.exists()) {
      throw new NoSuchFileException(file.getAbsolutePath());
    }

    return new LineNumberReader(Files.newBufferedReader(file.toPath(),
        StandardCharsets.UTF_8));
  }

  @SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UnnecessaryModifier"})
  private void checkForCorrectness(final File sourceFile)
      throws IOException {
    FunctionalTests.runAssembler(sourceFile);

    final File actualFile = FunctionalTests.getResultFile(sourceFile);
    final File expectedFile = FunctionalTests.getSolutionFile(sourceFile);

    try (final var actual = FunctionalTests.getReader(actualFile);
         final var expected = FunctionalTests.getReader(expectedFile)) {
      softly.assertThat(actualFile.length()).isEqualTo(expectedFile.length())
          .describedAs("file lengths don't match (%1$s)",
              actualFile.getAbsolutePath());

      for  (int line = 0; true; ++line) {
        final @Nullable String actualLine = actual.readLine();
        final @Nullable String expectedLine = actual.readLine();

        if (actualLine == null) {
          //noinspection VariableNotUsedInsideIf
          if (expectedLine != null) {
            softly.fail("generated file too short (%1$s)",
                actualFile.getName());
            //noinspection BreakStatement
            break;
          }
        } else {
          if (expectedLine == null) {
            softly.fail("generated file too long (%1$s)",
                actualFile.getName());
            //noinspection BreakStatement
            break;
          }
          //noinspection UnnecessaryBoxing
          softly.assertThat(actualLine).isEqualTo(expectedLine)
              .describedAs("mismatch at line %1$d: '%2$s' vs '%3$s'",
                  Integer.valueOf(line), actualLine, expectedLine);
        }
      }
    }
  }

  @SuppressWarnings({
                        "PMD.AvoidInstantiatingObjectsInLoops",
                        "ConstantConditions",
                        "MethodWithMultipleReturnPoints"
                    })
  private void runCatagoryTests(final String category)
      throws ClassNotFoundException {
    final File[] patternTests = FunctionalTests.getResourceFiles(category);

    if (patternTests == null) {
      return;
    }

    for (final @Nullable File patternFile : patternTests) {
      try {
        checkForCorrectness(patternFile);
      } catch (final NoSuchFileException ex) {
        final @Nullable String filename = ex.getFile();

        if (filename == null) {
          softly.fail("?missing file lacks name?");
        } else {
          @SuppressWarnings("ObjectAllocationInLoop")
          final @Nullable File fileDirectory =
              new File(filename).getParentFile();

          if (FunctionalTests.OUTPUT_DIR.equals(fileDirectory)) {
            @SuppressWarnings({ "ImplicitNumericConversion", "MagicCharacter" })
            final int dotAt = filename.lastIndexOf('.');
            final String fileType =
                dotAt < 0 ? "temporary" : filename.substring(dotAt);

            softly.fail("%1$s test (%2$s): generated %3$s file missing",
                category, patternFile.getName(), fileType, ex);
          } else {
            softly.fail("%1$s test (%2$s): %3$s",
                category, patternFile.getName(), ex.toString(), ex);
          }
        }
      } catch (final IOException ex) {
        softly.fail("%1$s test (%2$s): %3$s",
            category, patternFile.getName(), ex.toString(), ex);
      }
    }
  }

  @SuppressWarnings({
                        "PMD.AvoidPrintStackTrace",
                        "PMD.JUnitTestsShouldIncludeAssert",
                        "JUnitTestMethodWithNoAssertions"
                    })
  @Test
  public void checkPatterns() {
    try {
      runCatagoryTests("patterns");
    } catch (final ClassNotFoundException ex) {
      ex.printStackTrace();
    }
  }

  @SuppressWarnings({
                        "PMD.AvoidPrintStackTrace",
                        "PMD.JUnitTestsShouldIncludeAssert",
                        "JUnitTestMethodWithNoAssertions"
                    })
  @Test
  public void checkExamples() {
    try {
      runCatagoryTests("programs");
    } catch (final ClassNotFoundException ex) {
      ex.printStackTrace();
    }
  }
}

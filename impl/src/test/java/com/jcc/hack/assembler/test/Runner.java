package com.jcc.hack.assembler.test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * API for invoking the Hack Assembler programmatically instead of from the
 * command line.
 */
@SuppressWarnings({
                      "PMD.BeanMembersShouldSerialize",
                      "StringConcatenation",
                      "UseOfProcessBuilder",
                      "WeakerAccess"
                  })
public final class Runner {

  /**
   * Certain characters are prohibited in "options" to preclude potential
   * command injection.
   */
  @SuppressWarnings("PMD.LongVariable")
  private static final String FORBIDDEN_OPTION_CHARS = "$({[<;>])}";

  /** Placeholder for a nonexistent parent directory. */
  private static final File NO_PARENT = new File("nonesuch");

  /** Maximum time, in seconds, Hack assembler will be allowed to run. */
  @SuppressWarnings("PMD.LongVariable")
  private static final int MAX_RUNTIME_SECONDS = 15; // per assembler attempt

  /**
   * Fully qualified pathname to the {@code java} program.
   */
  @SuppressWarnings("AccessOfSystemProperties")
  private static final String JAVA =
      Path.of(System.getProperty("java.home"), "bin", "java").toString();

  /** Location of the directory to house temporary files. */
  @SuppressWarnings("AccessOfSystemProperties")
  private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

  /** Name of the self-contained JAR-file with a Hack assembler. */
  private static final String ASSEMBLER_JAR =
      "Assembler-jar-with-dependencies.jar";

  /** Model of the Hack assembler command line. */
  private final ProcessBuilder builder;

  /**
   * All output from Hack assembler after last {@link #run()} call. Deleted on
   * normal termination of the JVM and prior to calls to {@link #run()}.
   */
  private final File outputFile;

  /** Current limit, in seconds, for an execution of the Hack assembler. */
  private int timeoutSeconds = Runner.MAX_RUNTIME_SECONDS;

  /**
   * Constructor.
   *
   * @param options command line options -or- empty if none
   * @throws IOException iff I/O problem
   */
  public Runner(final String options)
      throws IOException {
    this(new String[] { options });
  }

  /**
   * Constructor.
   *
   * @param options command line options -or- empty if none
   * @param sourceFile Hack assembly code file
   * @throws IOException iff I/O problem
   */
  public Runner(final String options, final File sourceFile)
      throws IOException {
    this(options,
        sourceFile,
        sourceFile.getParentFile() == null
            ? Runner.NO_PARENT : sourceFile.getParentFile());
  }

  /**
   * Constructor.
   *
   * @param options command line options -or- empty if none
   * @param sourceFile Hack assembly code file
   * @param outputDirectory directory to contain the generated Hack binary file
   * @throws IOException iff I/O problem
   */
  public Runner(final String options,
                final File sourceFile,
                final File outputDirectory)
      throws IOException {
    this(options,
        sourceFile.getAbsolutePath(),
        outputDirectory == Runner.NO_PARENT
            ? "" : outputDirectory.getAbsolutePath());
  }

  /**
   * Attempt to determine the actual location of {@link #ASSEMBLER_JAR}.
   *
   * @return fully qualified pathname of the JAR-file
   * @throws IOException iff unable to locate the necessary JAR-file
   */
  @SuppressFBWarnings({ "NP_NONNULL_RETURN_VIOLATION", "PATH_TRAVERSAL_IN" }) //NOPMD
  @SuppressWarnings({
                        "PMD.LawOfDemeter",
                        "PMD.OnlyOneReturn",
                        "MethodWithMultipleReturnPoints"
                    })
  private static String findAssemblerJarfile()
      throws IOException {
    try {
      final @Nullable CodeSource code =
          Runner.class.getProtectionDomain().getCodeSource();

      if (code == null) {
        throw new IOException("unable to locate code of Assembler");
      }

      final URL jarUrl = code.getLocation();
      final File jarfileDirectory = new File(jarUrl.toURI().getPath());
      final File jarfile =
          new File(jarfileDirectory, Runner.ASSEMBLER_JAR);

      if (jarfile.canRead()) {
        return jarfile.getAbsolutePath();
      }

      final File jarfileDirectory2 =
          jarfileDirectory.getParentFile();
      final File jarfile2 =
          new File(jarfileDirectory2, Runner.ASSEMBLER_JAR);

      if (jarfile2.canRead()) {
        return jarfile2.getAbsolutePath();
      }

      //noinspection HardcodedFileSeparator
      throw new IOException("unable to locate/read Assembler JAR");
    } catch (final URISyntaxException ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Add the options, if any, to the command line.
   *
   * @param command command line to build
   * @param options command line options, -or- empty
   * @throws IOException iff prohibited characters found in <em>options</em>
   */
  private static void augmentWithOptions(final List<? super String> command,
                                         final String options)
      throws IOException {
    if (!options.isEmpty()) {
      if (options.contains(Runner.FORBIDDEN_OPTION_CHARS)) {
        throw new IOException("prohibited characters in options: "  + options);
      }
      command.add("-" + options);
    }
  }

  /**
   * Add the source file, if any, to the command line.
   *
   * @param command command line to build
   * @param source pathname of source file, -or- empty
   * @throws IOException iff unable to read <em>source</em>
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  private static void augmentWithSourceFile(final List<? super String> command,
                                            final String source)
      throws IOException {
    if (new File(source).canRead()) {
      command.add(source);
    } else {
      //noinspection HardcodedFileSeparator
      throw new IOException(
          "unable to read/locate source file: " + source);
    }
  }

  /**
   * Add the target directory, if any, to the command line.
   *
   * @param command command line to build
   * @param target destination directory, -or- empty
   * @throws IOException iff to determine or read <em>target</em>
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  private static void augmentWithTargetDirectory(final List<? super String> command,
                                                 final String target)
      throws IOException {
    if (target.isEmpty()) {
      throw new IOException(
          "no parent directory for source: " + target);
    } else {
      if (new File(target).canRead()) {
        command.add(target);
      } else {
        //noinspection HardcodedFileSeparator
        throw new IOException(
            "unable to read/locate directory: " + target);
      }
    }
  }

  /**
   * Build the actual Hack assembler command.
   *
   * @param args command line arguments
   * @throws IOException iff I/O problem
   */
  @SuppressFBWarnings({ "COMMAND_INJECTION", "PATH_TRAVERSAL_IN" })
  @SuppressWarnings("OverloadedVarargsMethod")
  private Runner(final String... args)
      throws IOException {
    // base command
    if (!new File(Runner.JAVA).canExecute()) {
      //noinspection HardcodedFileSeparator
      throw new IOException("unable to locate/execute JVM: "  + Runner.JAVA);
    }

    final List<String> commandLine = new ArrayList<>(6);

    commandLine.add(Runner.JAVA);
    commandLine.add("-jar");
    commandLine.add(Runner.findAssemblerJarfile());

    Runner.augmentWithOptions(commandLine, args[0]);
    Runner.augmentWithSourceFile(commandLine, args[1]);
    Runner.augmentWithTargetDirectory(commandLine, args[2]);

    outputFile =
        File.createTempFile(Runner.TEMP_DIR, "stdout");
    outputFile.deleteOnExit(); // ensure deletion when JVM terminates normally
    builder = new ProcessBuilder(commandLine)
                  .redirectOutput(outputFile)
                  .redirectErrorStream(true);
  }

  /**
   * Define the timeout limit, in seconds, for {@link #run()} with en enforced
   * upper limit of {@value #MAX_RUNTIME_SECONDS}.
   *
   * @param seconds limit, in seconds; if negative or zero the upper is used
   */
  public void setTimeoutSeconds(final int seconds) {
    timeoutSeconds =
        seconds <= 0
            ? Runner.MAX_RUNTIME_SECONDS
            : Math.max(seconds, Runner.MAX_RUNTIME_SECONDS);
  }

  /**
   * Determine the time limit, in seconds, for the next {@link #run()}.
   *
   * @return maximum time (in seconds) program will be allowed to run
   */
  public @NonNegative int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Run the Hack Assembler as a process with the current parameters;
   * multiple runs are permitted.
   *
   * @return {@code true} iff Hack assembler had an exit code of {@code 0}
   * @throws IOException iff there was a problem running the process
   * @throws InterruptedException iff time limit exceeded
   */
  @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
  public boolean run()
      throws IOException, InterruptedException {

    // flush any prior content
    Files.delete(Path.of(outputFile.getAbsolutePath()));

    final Process process = builder.start();

    try {
      //noinspection ImplicitNumericConversion
      process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

      return process.exitValue() == 0;
    } finally {
      process.destroyForcibly();
    }
  }

  /**
   * Get the Hack Assembler output after a {@link #run()}.
   *
   * @return all the text written to {@link System#out} by the Hack assembler
   *     since the last {@link #run()}
   * @throws IOException iff I/O problem
   */
  @SuppressFBWarnings({ "NP_NONNULL_RETURN_VIOLATION", "PATH_TRAVERSAL_IN" })
  public Stream<String> getOutput()
      throws IOException {
    final Path stdoutPath = Paths.get(outputFile.getAbsolutePath());

    return Files.exists(stdoutPath)
            ? Files.lines(stdoutPath, StandardCharsets.UTF_8)
            : Stream.empty();
  }

}

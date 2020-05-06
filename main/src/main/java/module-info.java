@SuppressWarnings("PMD.CommentSize")
module com.jcc.hack.assembler.main {
  // Java 11+
  //noinspection Java9RedundantRequiresStatement
  requires ch.qos.logback.classic; // want fat-jar to use Logback logging
  requires com.jcc.hack.assembler.impl;

  // annotations
  requires static com.github.spotbugs.annotations;
  requires static org.checkerframework.checker.qual;

  // nothing to export -- program
}
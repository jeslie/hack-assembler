@SuppressWarnings("PMD.CommentSize")
module com.jcc.hack.assembler.impl {
  // Java 11+
  requires org.slf4j;

  // annotations
  requires static com.github.spotbugs.annotations;
  requires static org.checkerframework.checker.qual;

  exports com.jcc.hack.assembler.impl; // HackAssembler
}
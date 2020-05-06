<h2>Hack Assembler</h2>

<p>
This is an enhanced implementation of the two pass Hack Assembler originally
described at the Nand2Tetris <a href="www.nand2tetris.com">website</a>. 
This implementation supports the following additional
features:
</p>
<ul>
  <li>options (including source listing, object listing, symbol table dump, filename resolution display)</li>
  <li>self-help</li>
  <li>specification of the location/name of the generated Hack machine language file</li>
  <li>allowing embedded whitespace between C-command fields and their delimiters</li>
  <li>extensive error checks</li>
</ul>

<p>
The files contained here also have the following features:
</p>
<ul>
  <li>presumes use of Java 11 and Maven toolsets</li>
  <li>unit tests, functional/integration tests and a stress test</li>
  <li>example use of Java 9+ modules</li>
  <li>Maven support (pom.xml) files for all build steps</li>
  <li>simple (UNIX) shell script to invoke the assembler</li>
  <li>wrappers (<code>mvnw</code> for UNIX-like systems and <code>mvnw.bat</code> for Windows-like systems) to invoke <code>mvn</code></li>
</ul>

<p>
The bulk of this work is under the MIT License (LICENSE.txt), but portions
are under the more restrictive Creative Commons License (NOTICE.txt).
</p>
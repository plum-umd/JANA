## Examples

* `java_http_server` - A simple http server written as a minimal 2
  short java files.

* `pldi09.{c,java,pseudocode}` - The examples from Eric's 2009 PLDI
  paper written in C, java, and the original pseudocode.

* `red_complexity.java` - Small examples of generating false positives
  by hiding vital information via java api functions, methods from
  `java.io.socket` in this case. The same can be done to hide
  information flows against an analysis that "under-approximates" the
  flows through the opaque java api.

* `red_sampling.java` - Small examples that hide information flows
  from testing/sampling/machine learning approaches.

* `red_jit.java` - A small example that hides an information flow from
  analyses that fail to take into account just-in-time-compilation.

## Glossary of useful terms

* openJDK - A collection of the below, source code available.
  * hotspot - JVM runtime, separate from JDK.
  * JDK - Includes java api, minus a lot of native stuff.

* jikes - A JVM, plus java api mostly written in java itself with minimal native code.

## Useful tidbits or tools.

* [grepcode.com](http://www.grepcode.com) - Search over java code found over the internets.

* vmmap - Shows mapped memory regions of a process.

* To observe JIT events when running a java program, invoke java with `-XX:+PrintCompilation` .

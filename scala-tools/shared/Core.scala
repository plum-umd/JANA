import Annotation._

object Core {
  case class NotImplemented      (s:String) extends Exception(s)
  case class InterpException     (s:String) extends Exception(s)
  case class AbstractionException(s:String) extends Exception(s)
  case class LogicException      (s:String) extends Exception(s)

  case class AssertException(s: String) extends Exception(s)
  // For failed "assert" annotations.

  type VarIndex = WALA.RegIndex // frame's local variable
  type DimIndex = Long // PPL dimension

  var interpCleanPhis: Boolean = true // false

  var debug: Boolean    = false
  var verbose: Boolean  = false
  var progress: Boolean = false

  def debugString(f: => String): String = {
    if (debug) f else ""
  }
  def verboseString(f: => String): String = {
    if (verbose) f else ""
  }

  def ifDebug(f: => Unit): Unit = {
    if (debug) f
  }
  def ifVerbose(f: => Unit): Unit = {
    if (verbose) f
  }
  def ifProgress(f: => Unit): Unit = {
    if (progress) f
  }

  def memStats: String = {
    // memory info
    val mb = 1024*1024
    val runtime = Runtime.getRuntime
    "Used Memory: " + (runtime.totalMemory - runtime.freeMemory) / mb + "MB, " +
      "Free Memory:  " + runtime.freeMemory / mb + "MB, " +
      "Total Memory: " + runtime.totalMemory / mb + "MB, " +
      "Max Memory:   " + runtime.maxMemory / mb + "MB"
  }

  case class EvalContext(
    val regs: Map[Int, Value],
    val heap: Map[Reference, Value],
    val annots: Annots
  ) {
    def this() = this(Map[Int,Value](), Map[Reference,Value](), emptyAnnots)
  }

  case class InterpContext(val eval: EvalContext, val wala: WALA.WALAContext) {

    def this(
      pa: edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder,
      cgnode: WALA.CGNode
    ) = this(new EvalContext(), new WALA.WALAContext(pa, cgnode))

    def this(
      heap: WALA.HeapGraph,
      cg: WALA.CG,
      cgnode: WALA.CGNode
    ) = this(new EvalContext(), new WALA.WALAContext(heap, cg, cgnode))

  }

  //type FieldIndex  = String
  //type ObjectIndex = Long
  //type ArrayIndex  = Long
  //type IndexIndex  = Long
}

// remember that 
// type N = CGNode // call graph nodes
// type PutI = SSAPutInstruction
// type LocalP = LocalPointerKey

import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector
import com.ibm.wala.util.graph.traverse.DFS

import com.ibm.wala.ssa.IR

import com.ibm.wala.shrikeBT.IBinaryOpInstruction
import com.ibm.wala.shrikeBT.IShiftInstruction
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction

import com.ibm.wala.types.TypeReference

import edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder
import edu.illinois.wala.ipa.callgraph.propagation.P

// convenience object that activates all implicit converters
import edu.illinois.wala.Facade._

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map, HashMap, Stack, ListBuffer, Set}
import scala.collection.immutable.{List}
import scala.sys.exit

object Cost {
  //given a CFG and an entry point in the 
  //CFG, compute the cost of walking from that 
  //entry point to the set of exit points 

  def computecost(c:CFG, start:I, end:Set[I]) : Long = {
    var costs : Map[I, Option[Long]] = new HashMap[I, Option[Long]]()

    //start at 'start'

    //the set of nodes we'll be interested in are just the nodes that 
    //are dominated by start and don't go beyond end

    //compute the distance between start and every point in end

    //return the largest value
    return 0
  }

  def main(argv : scala.Array[String]) : Unit = {

    //load a a list of IR
    val contexts = CommandLine.loadFromCommandline(
      argv,
      "tests",
      "tests"
      //"TestCost.foo.*",
      //"java/awt/.* javax/.* com/sun/.* com/apple/.* sun/.* apple/.* org/eclipse/.* apache/.*"
    )
    val irs = contexts.map(_.wala.ir)

    //there should only be one function matching
    val ir = irs(0)
    println(ir)

    val cfg = ir.getControlFlowGraph()
    //starting instruction
    val i1 = cfg.getInstructions()(2)
    //return 1
    val i2 = cfg.getInstructions()(13)
    //return 2
    val i3 = cfg.getInstructions()(20)
    //return 3
    val i4 = cfg.getInstructions()(22)

    //pass to computecost
    val v = computecost(cfg, i1, Set(i2,i3,i4))

    //print output
    println("computed cost is "+v)
    return
  }
}

import DFA._
import com.ibm.wala.ssa.SSACFG._

object DFAMetaInterpreter {

  type CFG      = com.ibm.wala.ssa.SSACFG
  type CFGNode  = com.ibm.wala.ssa.SSACFG#BasicBlock
  type CFGEdge  = (CFGNode, CFGNode)

  class DFAMIState(
    val edgesToConsume:List[CFGEdge],
    val q:Int
  ) {
    //def this(e:List[CFGEdge],qq:Int) { }
    override def toString() :String = { return "dfaimstate" }
  }

  class DFAMetaInterpreter(
    val aut:DFA[Int,List[CFGEdge]],
    val mq:DFAMIState
  ) {
    def this(a:DFA[Int,List[CFGEdge]]) {
      this(a,new DFAMIState(List[CFGEdge](),0))
    }

    def getStartState() : DFAMIState = {
      return new DFAMIState(List[CFGEdge](),0)
    }

    def getEdges(q:DFAMIState) : Map[CFGEdge,List[DFAMIState]] = { 
      return Map[CFGEdge,List[DFAMIState]]();
    }

  }
}

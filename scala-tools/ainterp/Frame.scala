import Core._
import Util._
import WALA._
import Label._
import Annotation._

import scalaz._
import Scalaz._

import scala.collection.{Map}
import scala.collection.Map._
import scala.collection.immutable.{Map=>IMap, Set, HashMap}

case class Frame(
  /* based on Andrew's concrete interpreter */
  val pc: WALA.IAddr,
  val hasReturned: Boolean,
  val annots: MethodAnnots,
  val depth: Int
) extends IFrame {
  
  def id = (depth, pc.method.hashCode())
  def numParameters = pc.method.getNumberOfParameters() + 1

  def localLabel(idx: VarIndex) = if (idx < numParameters) {
    parLabel(idx)
  } else {
    regLabel(idx)
  }
  def regLabel(idx: VarIndex) = FrameRegister(id, idx)

  def parLabel(idx: VarIndex) = FrameParameter(id, idx)
  def retLabel = FrameReturn(id)

  def parHeapLabel(pk:WALA.PK) = 
    if(ConfigManager.globalHeapLabel) {
      FrameHeapParameter((-1, -1), pk)
    } else {
      FrameHeapParameter(id, pk)
    }
    
  def regHeapLabel(pk:WALA.PK) = 
    if(ConfigManager.globalHeapLabel) {
      FrameHeapRegister((-1, -1), pk)
    } else {
      FrameHeapRegister(id, pk)
    }
   
  //temporary heap label for implicit field/array content read due to summary invocation
  def tmpHeapLabel(pk:WALA.PK) = 
    if(ConfigManager.globalHeapLabel) {
      FrameHeapTemporary((-1, -1), pk)
    } else {
      FrameHeapTemporary(id, pk)
    }
    
  def tmpLabel(i: Int) = FrameTemporary(id, i)
  
  def accessPathLabel(pk:WALA.PK, field:WALA.Field) = FrameAccessPath(id, pk, field)
  
  def arrayLengthAccessPathLabel(pk:WALA.PK) = FrameArrayLengthAccessPath(id, pk)
  
  def ghostLabel(label: Label) = GhostLabel(label)
  
  def staticFuncLabel(sig: String) = StaticUnmodeledFunction(id, sig)
  def dispatchFuncLabel(index: VarIndex, sig: String) = DispatchUnmodeledFunction(id, index, sig)

  def evalDeferred(e: Exp.Open): Exp.Open = {
    e.mapTerms{
      case Term.Variable(FrameDeferredReturn())      => Term.Variable(FrameReturn(this.id))
      case Term.Variable(FrameDeferredRegister(vid)) => Term.Variable(this.localLabel(vid))
      case t => t
    }
  }

  def this(
    pc: WALA.IAddr
  ) = this(
    pc,
    false,
    annots = emptyMethodAnnots,
    0
  )

  def this(ir: WALA.IR) = this(
    pc = new WALA.IAddr(
      method = ir,
      block = ir.getControlFlowGraph().entry()
    )
  )

  def block: WALA.CFGNode = pc.block

  def goto(idx: Int): Frame =
    this.copy(
      pc = new WALA.IAddr(
        method = this.pc.method,
        index = idx
      )
    )
  
  def goto(n: WALA.CFGNode): Frame = {
    this.copy(
      pc = new WALA.IAddr(
        method = this.pc.method,
        block = n,
        index = n.getFirstInstructionIndex()
      )
    )
  }

  def doReturn: Frame = {
    this.copy(
      pc = this.pc.doReturn,
      hasReturned = true
    )
  }

  def next: Frame = this.copy(pc = pc.next.get)

  override def toString = {
    s"Frame:\tblock: $block\n" + tab(s"pc: $pc"
    )
  }
}

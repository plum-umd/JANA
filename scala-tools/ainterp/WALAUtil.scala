import Core._
import Label._

import collection.JavaConversions._

object WALAUtil {
  type Machine = AbstractMachine
  val Machine = AbstractMachine

  def argsOfI(i: WALA.I): List[Int] = {
    (0 to i.getNumberOfUses() - 1).toList.map(i.getUse)
  }

  import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.{IOperator => BOperator}
  import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator._
  import com.ibm.wala.shrikeBT.IBinaryOpInstruction.{IOperator => AOperator}
  import com.ibm.wala.shrikeBT.IUnaryOpInstruction.{IOperator => AUOperator}
  import com.ibm.wala.shrikeBT.IBinaryOpInstruction.Operator._
  import com.ibm.wala.shrikeBT.IUnaryOpInstruction.Operator._
  import edu.illinois.wala.Facade._

  def binopOfBranch(i: BranchI): Operator.NumericRelation = {
    i.getOperator().asInstanceOf[BOperator] match {
      case EQ => Operator.NumericRelation.==
      case NE => Operator.NumericRelation.≠
      case LT => Operator.NumericRelation.<
      case LE => Operator.NumericRelation.≤
      case GT => Operator.NumericRelation.>
      case GE => Operator.NumericRelation.≥
    }
  }
  def binopOfBinop(i: BinopI): Option[Operator.NumericBinop] = {
    i.getOperator().asInstanceOf[AOperator] match {
      case ADD => Some(Operator.NumericBinop.+)
      case SUB => Some(Operator.NumericBinop.-)
      case MUL => Some(Operator.NumericBinop.*)
      case DIV => Some(Operator.NumericBinop./)
      case REM => Some(Operator.NumericBinop.%)
      case _ => None
    }
  }
  
  def unopOfUnop(i: com.ibm.wala.ssa.SSAUnaryOpInstruction): Option[Operator.NumericUnop] = {
    i.getOpcode.asInstanceOf[AUOperator] match {
      case NEG => Some(Operator.NumericUnop.-)
      case _ => None
    }
  }

  def isSymbol(l: Label)(implicit ir: WALA.IR): Boolean = {
    val st = ir.getSymbolTable()
    l match {
      case FrameRegister(_, vid) => st.isConstant(vid)
      case _ => false
    }
  }

  def populateSymbols(m: Machine, wcontext: WALA.WALAContext): Machine = {
    val st = wcontext.ir.getSymbolTable()
    WALA.varsOfSymbolTable(wcontext.ir).foldLeft(m){case (m, vid) =>
      if (st.isIntegerConstant(vid)) {
        m.bind(m.topFrame.localLabel(vid), Term.Constant(CIV(st.getIntValue(vid))))
      } else if(st.isLongConstant(vid) | st.isBooleanConstant(vid)) {
        val v = st.getValue(vid)
        ifDebug {
          Log.println(s"WARNING: the value of v$vid in symbol table (is $v) not properly handled")
        }
        m.alloc(m.topFrame.localLabel(vid))
      } else {
        m
      }
    }
  }

  def initMachine(context: InterpContext): Machine = {
    val InterpContext(econtext, wcontext) = context
    val ir = wcontext.ir
    val method = wcontext.method
    val methodName   = method.getSelector()
    val m1 = new Machine(wcontext.cgnode, econtext.annots).newFrame
        
    //val m2 = populateArgs(m1, context)
    val (vars, m2) = populateVars(m1, context)
    
    val aps = //access path labels to be pre-allocated
      if(ConfigManager.hasAccessPath) {
        populateAPs(m2, context)
      } else {
        List[Label]()
      }
    
    val objs = //summarized object labels to be pre-allocated
      if(ConfigManager.hasSummarizedDimensions) {
        populateObjs(m2, context)
      } else {
        List[Label]()
      }
    
    val m3  = 
      if(ConfigManager.isTopDown){
        m2.alloc(vars++aps).alloc(objs)(true)
      } else {
        //TODO: BU analysis (pre-allocate summarized objects or not?)
//      m2.alloc(vars++aps)
        m2.alloc(vars++aps++objs)
    }
    
    populateSymbols(m3, wcontext)
    
  }

  def populateArgs(
    initm: Machine,
    context: InterpContext
  ): Machine = {
    val InterpContext(econtext, wcontext) = context

    val ir = wcontext.ir
    val method = wcontext.method
    val m = initm
    val frame = m.topFrame
    val regs = econtext.regs

    val num_args = method.getNumberOfParameters()

    // seems like register numbers in wala ir already take this into
    // account, on static methods there simply is no v0

    import WALA.TypeType._

    val finalm: Machine = (0 until num_args).foldLeft(m){
      case (m, idx) =>
        val aidx = idx + 1
        val reg = frame.localLabel(aidx)
        regs.get(aidx) match {
          case Some(v) => m.writevalue(
            reg,
            Term.Constant(v)
          )
          case None =>
            val t = method.getParameterType(idx)
            WALA.getTypeType(t) match {
              case TypeTypeIntegral => m.alloc(reg)
              case TypeTypeBoolean  => m.alloc(reg)
              case TypeTypeChar  => m.alloc(reg)
              case _ => m
            }
        }
    }

    finalm
  }
  
  def populateVars(
    initm: Machine,
    context: InterpContext
  ): (List[Label], Machine) = {
    val InterpContext(econtext, wcontext) = context
    
    val m = initm
    val frame = m.topFrame
    val regs = econtext.regs

    val variables = wcontext.variables
    import WALA.TypeType._
    var labels = List[Label]()
    val m1: Machine = variables.foldLeft(m){
      case (m, idx) =>
        val aidx = idx
        val reg = frame.localLabel(aidx)
        regs.get(aidx) match {
          case Some(v) => m.writevalue(
            reg,
            Term.Constant(v)
          )
          case None =>
            labels = reg :: labels
            m
        }
    }

    labels = frame.retLabel :: labels

    (labels, m1)
  }
  
  def populateAPs(
    initm: Machine,
    context: InterpContext
  ): List[Label] = {
    val InterpContext(econtext, wcontext) = context
    
    val m = initm
    val frame = m.topFrame
    val aps = wcontext.accesspaths

    aps.foldLeft(List[Label]()){
      case (list, (pk, field)) =>
        if(field == null) {
          val l = frame.arrayLengthAccessPathLabel(pk)
          if(!list.contains(l))
            l :: list
          else
            list
        } else {
          val l = frame.accessPathLabel(pk, field)
          if(!list.contains(l))
            l :: list
          else
            list
        }
    }

  }
  
  def populateObjs(
    initm: Machine,
    context: InterpContext
  ): List[Label] = {
    val InterpContext(econtext, wcontext) = context
    
    val m = initm
    val frame = m.topFrame
    val objs = WALA.heapMap.get(wcontext.cgnode).get

    objs.foldLeft(List[Label]()){
      case (list, pk) =>
          val l = frame.regHeapLabel(pk)
          l :: list
    }

  }

}

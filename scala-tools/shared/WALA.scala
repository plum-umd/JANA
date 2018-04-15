import collection.JavaConversions._

import com.ibm.wala.shrikeBT.IBinaryOpInstruction
import com.ibm.wala.shrikeBT.IShiftInstruction
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction
import com.ibm.wala.ssa.SSAInstruction

import Label._

import edu.illinois.wala.Facade._

import scala.collection.mutable.{Set=>MSet}

import Core._

object WALA {
  type IIndex   = Int
  type BIndex   = Int
  type RegIndex = Int

  type IR = com.ibm.wala.ssa.IR

  type TYPE   = com.ibm.wala.types.TypeReference
  type METHOD = com.ibm.wala.classLoader.IMethod

  type I = com.ibm.wala.ssa.SSAInstruction

  type CFG      = com.ibm.wala.ssa.SSACFG
  type CFGNode  = com.ibm.wala.ssa.SSACFG#BasicBlock
  type ICFGNode = com.ibm.wala.cfg.IBasicBlock[I]
  type Edge     = (CFGNode, CFGNode)
  type AIEdge   = (ICFGNode, ICFGNode)

  type CG       = com.ibm.wala.ipa.callgraph.CallGraph
  type CGNode   = com.ibm.wala.ipa.callgraph.CGNode

  //types for heap analysis
  type HeapGraph = com.ibm.wala.analysis.pointers.HeapGraph[_]
  type PK = com.ibm.wala.ipa.callgraph.propagation.PointerKey
  type FieldReference = com.ibm.wala.types.FieldReference
  type Field = com.ibm.wala.classLoader.IField
  type IK = com.ibm.wala.ipa.callgraph.propagation.InstanceKey
  
  var heapMap = Map[CGNode, List[WALA.PK]]()
  
  var allHeap = List[WALA.PK]()

  case class WALAContext(
    val heap: WALA.HeapGraph,
    val cg: WALA.CG,
    val cgnode: WALA.CGNode,
    val cfg: WALA.CFG,
    val ir: WALA.IR,
    val method: WALA.METHOD,
    val symbols: List[VarIndex],
    val reachingDefs: Map[WALA.CFGNode, Set[VarIndex]],
    val variables: List[VarIndex],
    val accesspaths: List[(WALA.PK, WALA.Field)],
    val summaryobjs: List[WALA.PK]
  ) {
    def this(
      pa: edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder,
      cgnode: WALA.CGNode
    ) =  {
        this(
        pa.heap, pa.cg, cgnode, if(cgnode.getIR != null) cgnode.getIR().getControlFlowGraph() else null, if(cgnode.getIR != null) cgnode.getIR() else null, cgnode.getMethod(),
        if(cgnode.getIR != null) varsOfSymbolTable(cgnode.getIR()) else null,
        null,//if(cgnode.getIR != null) reachingDefinitions(cgnode.getIR()) else null,
        if(cgnode.getIR != null) varsOfMethod(cgnode.getIR()) else null,
        if(cgnode.getIR != null && ConfigManager.hasAccessPath) apOfMethod(cgnode) else null,
        if(cgnode.getIR != null && ConfigManager.hasSummarizedDimensions) objsOfMethod(cgnode) else null
        )
    }
      

    def this(
      heap: WALA.HeapGraph,
      cg: WALA.CG,
      cgnode: WALA.CGNode
    ) = this(
      heap, cg, cgnode, cgnode.getIR().getControlFlowGraph(), cgnode.getIR(), cgnode.getMethod(),
      symbols = varsOfSymbolTable(cgnode.getIR()),
      reachingDefs = null,//reachingDefinitions(cgnode.getIR()),
      variables = varsOfMethod(cgnode.getIR()),
      accesspaths = apOfMethod(cgnode),
      summaryobjs = objsOfMethod(cgnode)
    )
  }

  case class IAddr(
    val method: IR,
    val block: CFGNode,
    val index: IIndex
  ) {
    def this(method: IR, index: IIndex) = this(
      method = method,
      block = method.getControlFlowGraph().getBlockForInstruction(index),
      index
    )
    def this(method: IR, block: CFGNode) = this(
      method = method,
      block = block,
      index = block.getFirstInstructionIndex()
    )
    def this(method: IR) = this(
      method = method,
      block = method.getControlFlowGraph().entry(),
      index = method.getControlFlowGraph().entry().getFirstInstructionIndex()
    )

    lazy val methodName = method.getMethod().getSelector()

    val cfg = method.getControlFlowGraph()
    val ir  = method
    val instr = if (index >= 0) method.getInstructions()(index) else null

//    val (instr, index): (I, IIndex) = getInstruction(fakeindex, method)
//    val block: WALA.CFGNode = cfg.getBlockForInstruction(index)

    lazy val doReturn: IAddr = new IAddr(this.method, this.cfg.exit())

    def next: Option[IAddr] = {
      if (instr == null || instr.isFallThrough()) {
        Some(IAddr(
          method,
          cfg.getBlockForInstruction(index+1),
          index+1
        ))
      } else {
        None
      }
    }
    override def toString: String = {
      index.toString() + ":" +
      (if (instr == null) {
        if (cfg.entry() == block)
          "entry"
        else if (cfg.exit() == block) {
          "exit"
        } else {
          "null?"
        } 
      } else { index.toString + " [" + instr.toString + "]" })
    }
  }

  def dataFlow[A](cfg: CFG)
    (init: CFGNode => A)(combine: CFGNode => List[A] => List[A] => A)
      : Map[CFGNode, A] = {
    // Perform some data flow analysis on the given control flow graph
    // 'cfg'. 'init' specifies how to initialize results and 'combine'
    // specifies how to combine results for a given node, predecessor
    // results, and successor results, respectively. Computes until
    // convergence.

    val nodes = cfg.entry() :: cfg.exit() :: cfg.iterator().toList.asInstanceOf[List[CFGNode]]

    var results = nodes.foldLeft(Map[CFGNode, A]()){ case (acc, node) =>
      acc + (node -> init(node))
    }

    var work = nodes.toSet

    while (0 != work.size) {
      val node = work.head
      work = work.tail

      val old_results = results(node)

      val preds = cfg.getPredNodes(node).toList.asInstanceOf[List[CFGNode]]
      val succs = cfg.getSuccNodes(node).toList.asInstanceOf[List[CFGNode]]
      val preds_results = preds.map{results(_)}
      val succs_results = succs.map{results(_)}

      val new_results = combine(node)(preds_results)(succs_results)

      if (old_results != new_results) {
        (preds ++ succs).foreach{n => work = work + n}
      }

      results = results + (node -> new_results)
    }

    results
  }

  def reachingDefinitions(ir: IR): Map[CFGNode, Set[VarIndex]] = {
    val default = varsOfSymbolTable(ir) ++ varsOfEntry(ir)
    val allVars = varsOfMethod(ir).sorted

    dataFlow(ir.getControlFlowGraph()){
      node => allVars.toSet
    }{
      node: CFGNode => preds_defs => _ =>
      varsOfBlock(node)._1.toSet ++
      Util.setsIntersection(preds_defs)(default=default.toSet)
    }
  }

  def varsOfMethod(ir: WALA.IR): List[VarIndex] = {
    val cfg = ir.getControlFlowGraph()
    val typeInference = com.ibm.wala.analysis.typeInference.TypeInference.make(ir, true); 

    (defsOfMethod(ir, typeInference) ++
      varsOfEntry(ir) ++
      varsOfSymbolTable(ir)
    ).toSet.toList
  }
  
  def apOfMethod(cgnode: WALA.CGNode): List[(WALA.PK, WALA.Field)] = {
    apsOfMethod(cgnode)
  }
    
  def varsOfMethodNoSymbol(ir: WALA.IR): List[VarIndex] = {
    val typeInference = com.ibm.wala.analysis.typeInference.TypeInference.make(ir, true); 
    (defsOfMethod(ir, typeInference) ++ varsOfEntry(ir)).toSet.toList
  }

  def varsOfEntry(ir: WALA.IR): List[VarIndex] =
    (0 until ir.getNumberOfParameters() + (if (ir.getMethod().isStatic()) 1 else 1)).toList

  def varsOfSymbolTable(ir: WALA.IR): List[VarIndex] = {
    val st      = ir.getSymbolTable()
    val maxpars = st.getMaxValueNumber()
    (0 to maxpars).toList.filter(st.isConstant(_))    
  }
  
    def varsOfBlock(b: WALA.CFGNode): (List[VarIndex], List[VarIndex]) = {
    // Given a block, return a list of variables defined in that block
    // and a list of variables used in that block, respectively.

    (b.getAllInstructions() ++ b.iteratePhis().toList).foldLeft(List[VarIndex](), List[VarIndex]()){
      case ((acc_defines: List[VarIndex], acc_uses: List[VarIndex]), ins) =>
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        val uses = (0 until ins.getNumberOfUses()).map{ins.getUse(_)}.toList
        (defs ++ acc_defines, uses ++ acc_uses)
    }
  }    

  def defsOfBlock(b: WALA.CFGNode, inference: com.ibm.wala.analysis.typeInference.TypeInference): (List[VarIndex]) = {

    (b.getAllInstructions() ++ b.iteratePhis().toList).foldLeft(List[VarIndex]()){
      case (acc_defines: List[VarIndex], ins) =>
        ins match {
        case i: InvokeI =>
          WALA.getTypeType(i.getDeclaredResultType) match {
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
              val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
              defs ++ acc_defines
            case _ => acc_defines
          }
      case i: ArrayLengthI =>     
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        defs ++ acc_defines
      case i: ArrayLoadI => 
          WALA.getTypeType(i.getElementType) match {
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
              val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
              defs ++ acc_defines
            case _ => acc_defines
          }
//      case i: ReturnI => 
//        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
//        defs ++ acc_defines
      case i: GetI =>
        WALA.getTypeType(i.getDeclaredFieldType) match {
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
              val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
              defs ++ acc_defines
            case _ => acc_defines
          }
      case i: BinopI =>     
        val t1 = inference.getType(i.getUse(0)).getTypeReference //the type of the first operand
        val t2 = inference.getType(i.getUse(1)).getTypeReference //the type of the second operand            
        (WALA.getTypeType(t1), WALA.getTypeType(t2))  match {    
        case (WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>   
          val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
          defs ++ acc_defines
        case (_, _) =>
          acc_defines
        }
        
      case i: com.ibm.wala.ssa.SSAPhiInstruction =>
       val t = inference.getType(i.getDef).getTypeReference
        
        WALA.getTypeType(t) match {       
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
            defs ++ acc_defines
          case _ => acc_defines
       }
      case i: com.ibm.wala.ssa.SSAComparisonInstruction =>
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        defs ++ acc_defines
      case i: com.ibm.wala.ssa.SSAUnaryOpInstruction =>
        val t = inference.getType(i.getDef).getTypeReference // the type of the lhs variable            
        WALA.getTypeType(t) match {
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
            defs ++ acc_defines
          case _ => 
            acc_defines
        }
        
      case i: com.ibm.wala.ssa.SSAConversionInstruction =>
        val from = i.getFromType() //the type of the rhs variable
        val to   = i.getToType() //the type of the lhs variable
        (WALA.getTypeType(from), WALA.getTypeType(to)) match {
          case (_, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>
            val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
            defs ++ acc_defines    
          case (_, _) => acc_defines
        }
      case i: com.ibm.wala.ssa.SSAInstanceofInstruction =>
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        defs ++ acc_defines
      case _ =>
        acc_defines
        }
    }
  }
  
  def defsOfMethod(ir: IR, inference: com.ibm.wala.analysis.typeInference.TypeInference): (List[VarIndex]) = {

    (ir.iterateAllInstructions().toList ++ ir.iteratePhis().toList).foldLeft(List[VarIndex]()){
      case (acc_defines: List[VarIndex], ins) =>
        ins match {
        case i: InvokeI =>
          WALA.getTypeType(i.getDeclaredResultType) match {
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
              val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
              defs ++ acc_defines
            case _ => acc_defines
          }
      case i: ArrayLengthI =>     
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        defs ++ acc_defines
      case i: ArrayLoadI => 
          WALA.getTypeType(i.getElementType) match {
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
              val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
              defs ++ acc_defines
            case _ => acc_defines
          }
//      case i: ReturnI => 
//        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
//        defs ++ acc_defines
      case i: GetI =>
        WALA.getTypeType(i.getDeclaredFieldType) match {
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
              val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
              defs ++ acc_defines
            case _ => acc_defines
          }
      case i: BinopI =>     
        val t1 = inference.getType(i.getUse(0)).getTypeReference //the type of the first operand
        val t2 = inference.getType(i.getUse(1)).getTypeReference //the type of the second operand            
        (WALA.getTypeType(t1), WALA.getTypeType(t2))  match {    
        case (WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>   
          val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
          defs ++ acc_defines
        case (_, _) =>
          acc_defines
        }
        
      case i: com.ibm.wala.ssa.SSAPhiInstruction =>
       val t = inference.getType(i.getDef).getTypeReference
        
        WALA.getTypeType(t) match {       
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
            defs ++ acc_defines
          case _ => acc_defines
       }
      case i: com.ibm.wala.ssa.SSAComparisonInstruction =>
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        defs ++ acc_defines
      case i: com.ibm.wala.ssa.SSAUnaryOpInstruction =>
        val t = inference.getType(i.getDef).getTypeReference // the type of the lhs variable            
        WALA.getTypeType(t) match {
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
            defs ++ acc_defines
          case _ => 
            acc_defines
        }
        
      case i: com.ibm.wala.ssa.SSAConversionInstruction =>
        val from = i.getFromType() //the type of the rhs variable
        val to   = i.getToType() //the type of the lhs variable
        (WALA.getTypeType(from), WALA.getTypeType(to)) match {
          case (_, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>
            val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
            defs ++ acc_defines    
          case (_, _) => acc_defines
        }
      case i: com.ibm.wala.ssa.SSAInstanceofInstruction =>
        val defs = (0 until ins.getNumberOfDefs()).map{ins.getDef(_)}.toList
        defs ++ acc_defines
      case _ =>
        acc_defines
        }
    }
  }
  
  def apsOfBlock(b: WALA.CFGNode, cgnode:WALA.CGNode): (List[(WALA.PK, WALA.Field)]) = {

    b.getAllInstructions().toList.foldLeft(List[(WALA.PK, WALA.Field)]()){
      case (acc_aps: List[(WALA.PK, WALA.Field)], ins) =>
        ins match {
          case i: NewI => 
            (WALA.getTypeType(i.getConcreteType), i.getNumberOfUses) match {
              case (_, 1) => 
                val pk = GlobalHeap.getPointerKey(cgnode,i.getDef)
                (pk, null) :: acc_aps
              case (_, _) => acc_aps
            }          
          case i: ArrayLengthI =>
            val pk = GlobalHeap.getPointerKey(cgnode,i.getArrayRef())
            (pk, null) :: acc_aps
          case i: GetI =>
            val field = GlobalHeap.getField(i.getDeclaredField())
            if(field != null && !i.isStatic()) {      
              WALA.getTypeType(field.getFieldTypeReference) match {
                case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                           
                  val pk = GlobalHeap.getPointerKey(cgnode,i.getRef())
                  (pk, field) :: acc_aps
                case _ => acc_aps
              }
            } else {
              acc_aps
            }
          case i: PutI =>
            val field = GlobalHeap.getField(i.getDeclaredField()) //field reference    
            WALA.getTypeType(field.getFieldTypeReference) match {
              case WALA.TypeType.TypeTypeIntegral| WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                if(!i.isStatic) {
                  val pk = GlobalHeap.getPointerKey(cgnode,i.getRef())
                  (pk, field) :: acc_aps
                } else {
                  acc_aps
                }
              case _ => acc_aps
              }
          case _ =>
            acc_aps
        }
    }
  }
  
  def apsOfMethod(cgnode:WALA.CGNode): (List[(WALA.PK, WALA.Field)]) = {

    cgnode.getIR.iterateAllInstructions().toList.foldLeft(List[(WALA.PK, WALA.Field)]()){
      case (acc_aps: List[(WALA.PK, WALA.Field)], ins) =>
        ins match {
          case i: NewI => 
            (WALA.getTypeType(i.getConcreteType), i.getNumberOfUses) match {
              case (_, 1) => 
                val pk = GlobalHeap.getPointerKey(cgnode,i.getDef)
                (pk, null) :: acc_aps
              case (_, _) => acc_aps
            }          
          case i: ArrayLengthI =>
            val pk = GlobalHeap.getPointerKey(cgnode,i.getArrayRef())
            (pk, null) :: acc_aps
          case i: GetI =>
            val field = GlobalHeap.getField(i.getDeclaredField())
            if(field != null && !i.isStatic()) {      
              WALA.getTypeType(field.getFieldTypeReference) match {
                case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                           
                  val pk = GlobalHeap.getPointerKey(cgnode,i.getRef())
                  (pk, field) :: acc_aps
                case _ => acc_aps
              }
            } else {
              acc_aps
            }
          case i: PutI =>
            val field = GlobalHeap.getField(i.getDeclaredField()) //field reference    
            WALA.getTypeType(field.getFieldTypeReference) match {
              case WALA.TypeType.TypeTypeIntegral| WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                if(!i.isStatic) {
                  val pk = GlobalHeap.getPointerKey(cgnode,i.getRef())
                  (pk, field) :: acc_aps
                } else {
                  acc_aps
                }
              case _ => acc_aps
              }
          case _ =>
            acc_aps
        }
    }
  }
  
  def objsOfMethod(cgnode:WALA.CGNode): (List[WALA.PK]) = {

    cgnode.getIR.iterateAllInstructions().toList.foldLeft(List[WALA.PK]()){
      case (acc_objs: List[WALA.PK], ins) =>
        ins match {
          case i: NewI => 
            (WALA.getTypeType(i.getConcreteType), i.getNumberOfUses) match {
              case (_, 1) => 
                val pts = GlobalHeap.getPointsToSet(cgnode, i.getDef)
                pts.foldLeft(acc_objs) {
                  case (l, ik) =>
                    val lkey = GlobalHeap.getLengthPK(ik)
                    val akey = GlobalHeap.getArrayKey(ik)
                    val l_1 =
                      if(l.contains(lkey))
                        l
                      else
                        lkey :: l
                    if(l_1.contains(akey))
                      l_1
                    else
                      akey :: l_1
                }
              case (_, _) => acc_objs
            }          
          case i: ArrayLengthI =>
            val pts = GlobalHeap.getPointsToSet(cgnode, i)
            pts.foldLeft(acc_objs) {
              case (l, ik) =>
                val lkey = GlobalHeap.getLengthPK(ik)
                  if(l.contains(lkey))
                    l
                  else
                    lkey :: l
            }
          case i: GetI =>
            val field = GlobalHeap.getField(i.getDeclaredField())
            if(field != null && !i.isStatic()) {      
              WALA.getTypeType(field.getFieldTypeReference) match {
                case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                           
                  val pts = GlobalHeap.getPointsToSet(cgnode, i)
                  pts.foldLeft(acc_objs) {
                    case (l, ik) =>
                      val fkey = GlobalHeap.getFieldPK(ik, field)
                      if(l.contains(fkey))
                        l
                      else
                        fkey :: l
                  }
                case _ => acc_objs
              }
            } else if(field != null && i.isStatic()) {  
              WALA.getTypeType(field.getFieldTypeReference) match {
                case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                           
                  val pk = GlobalHeap.getPointerKeyForStaticField(field)                   
                  if(acc_objs.contains(pk))
                    acc_objs
                  else
                    pk :: acc_objs
                case _ => acc_objs
              }
            } else {
              acc_objs
            }
          case i: PutI =>
            val field = GlobalHeap.getField(i.getDeclaredField()) //field reference    
            WALA.getTypeType(field.getFieldTypeReference) match {
              case WALA.TypeType.TypeTypeIntegral| WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                if(!i.isStatic) {
                  val fkeys = GlobalHeap.getFieldPKs(cgnode, i)
                  fkeys.foldLeft(acc_objs) {
                    case (l, fkey) =>
                      if(l.contains(fkey))
                        l
                      else
                        fkey :: l
                  }
                } else {
                  val pk = GlobalHeap.getPointerKeyForStaticField(field)
                  if(acc_objs.contains(pk))
                    acc_objs
                  else
                    pk :: acc_objs
                }
              case _ => acc_objs
              }
          case i: ArrayStoreI =>
            WALA.getTypeType(i.getElementType) match {
              case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                val akeys = GlobalHeap.getArrayKeys(cgnode, i)
                akeys.foldLeft(acc_objs) {
                    case (l, akey) =>
                      if(l.contains(akey))
                        l
                      else
                        akey :: l
                  }
              case _ => acc_objs
            }
          case i: ArrayLoadI =>
            WALA.getTypeType(i.getElementType) match {
              case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                   
                val pts = GlobalHeap.getPointsToSet(cgnode, i)
                pts.foldLeft(acc_objs) {
                    case (l, ik) =>
                      val akey = GlobalHeap.getArrayKey(ik)
                      if(l.contains(akey))
                        l
                      else
                        akey :: l
                  }
              case _ => acc_objs
            }
          case _ =>
            acc_objs
            
        }
    }
  }
  
  def createHeapMap(cg:WALA.CG): Unit = {
    cg.iterator().toList.foreach { node =>
      if(node.getIR != null ) { 
        var list = List[WALA.PK]()
        var visited = List[WALA.CGNode]()
        def dfs(
          n : WALA.CGNode
        ) : Unit = {
          val callees = GlobalHeap.cg.getSuccNodes(n).toList
          callees.reverse.foreach { callee =>
            if(!visited.contains(callee) && callee.getIR != null) {
              heapMap.get(callee) match {
                case None => 
                  visited = callee :: visited
                  dfs(callee)
                case Some(l) =>
                  visited = callee :: visited
                  list = l ++ list
              }              
            }
          }
          list = objsOfMethod(n) ++ list
        }
        dfs(node)
        heapMap += (node -> list.distinct)
      }      
    }    
  }
  
   def findAllHeap(cg:WALA.CG): Unit = {
     var list = List[WALA.PK]()
    cg.iterator().toList.foreach { node =>
      if(node.getIR != null ) { 
        
        list = objsOfMethod(node) ++ list
        
      }      
    }   
     allHeap = list.distinct
  }


//    class NoOpI(target:Int) extends I(target) {
//      def copyForSSA(
//        x$1: com.ibm.wala.ssa.SSAInstructionFactory,
//        x$2: scala.Array[Int],
//        x$3: scala.Array[Int]): com.ibm.wala.ssa.SSAInstruction = throw NotImplemented("")
//        def isFallThrough(): Boolean = throw NotImplemented("")
//      def toString(
//        x$1: com.ibm.wala.ssa.SymbolTable): String = throw NotImplemented("")
//      def visit(x$1: com.ibm.wala.ssa.SSAInstruction.IVisitor): Unit = throw NotImplemented("")
//    }


  def getSourceVarNames(ir:IR, n:CFGNode): List[String] = {
    val instructions = n.iterator().toList
    instructions.foldLeft(List[String]()) {
      case (l, inst) =>
        val l_1 = (0 to (inst.getNumberOfDefs-1)).foldLeft(l) {
          case (l, vn) =>
            val iindex = if (inst.iindex >= 0) inst.iindex else 0
            val names = ir.getLocalNames(iindex, inst.getDef(vn))
            if(names == null)
              l
            else {
              names.foldLeft(l) {
                case (l, name) =>
                  if(!l.contains(name))
                    name :: l
                  else l
              }
            }
                        
        }
        (0 to (inst.getNumberOfUses-1)).foldLeft(l_1) {
          case (l, vn) =>
            val iindex = if (inst.iindex >= 0) inst.iindex else 0
            val names = ir.getLocalNames(iindex, inst.getUse(vn))
            if(names == null)
              l
            else {
              names.foldLeft(l) {
                case (l, name) =>
                  if(!l.contains(name))
                    name :: l
                  else l
              }
            }            
        }
    }
  }
  
  def getEntryEdges(cfg: WALA.CFG): Set[(WALA.ICFGNode, WALA.ICFGNode)] = {
    val entry = cfg.entry()
    cfg.getSuccNodes(entry).toList.map{p => (entry,p)}.toSet
  }

  def getExitEdges(cfg: WALA.CFG): Set[(WALA.ICFGNode, WALA.ICFGNode)] = {
    val exit = cfg.exit()
    cfg.getPredNodes(exit).toList.map{p => (p,exit)}.toSet
  }

  def getInstruction(index:IIndex, ir:IR) : (I,Int) = {
    val cfg = ir.getControlFlowGraph()
    val instructions = ir.getInstructions().toList
    val mini = cfg.entry().getFirstInstructionIndex()
    //println("instructions = " + instructions.toString())
    if (index >= instructions.length) {
      throw InterpException("instruction index out of range: " + index.toString)
    }
    if (index >= 0) 
      (instructions(index),index)
    else
      (null, index)
  }

  def getNonNullInstruction(index:IIndex, ir:IR) : (I,Int) = {
    val cfg = ir.getControlFlowGraph()
    val instructions = ir.getInstructions().toList
    val mini = cfg.entry().getFirstInstructionIndex()
    if (index >= instructions.length) {
      throw InterpException("instruction index out of range: " + index.toString)
    }
    if (index >= 0) {
      var i = index
      while (instructions(i) == null) { i = i + 1 }
      (instructions(i), i)
    } else (null, index)
  }

  def getBackEdges(ir: WALA.IR): Set[Edge] = {
    var visited = Set[WALA.CFGNode]()
    var backEdges = Set[Edge]()

    val cfg = ir.getControlFlowGraph()

    def r(n: WALA.CFGNode, preds: Set[WALA.CFGNode]): Unit = {
      visited += n
      cfg.getSuccNodes(n).foreach{m =>
        if (preds.contains(m)) {
          backEdges += ((n, m.asInstanceOf[WALA.CFGNode]))
        }
        if (! visited.contains(m)) {
          r(m.asInstanceOf[WALA.CFGNode], preds + m.asInstanceOf[WALA.CFGNode])
        }
      }
    }

    r(cfg.entry(), Set[WALA.CFGNode]())

    backEdges.toSet
  }

  // find natural loops, adapted from muchnick97, section 7.4
  def getNaturalLoop(
    cfg : WALA.CFG, m : WALA.CFGNode, n : WALA.CFGNode
  ) : Set[WALA.CFGNode] = {
    var loop = Set[WALA.CFGNode]()
    loop += m
    loop += n
    def r(p : WALA.CFGNode) : Unit = {
      cfg.getPredNodes(p).foreach{ pred =>
        val q = pred.asInstanceOf[WALA.CFGNode]
        if (! loop.contains(q)) {
          loop += q
          r(q)
        }
      }
    }
    r(m)
    return loop
  }

  class LoopStructure(
      /** the loop header */
      val header: WALA.CFGNode,

      /** the set of nodes comprising the loop, including the header */
      val nodeset: Set[WALA.CFGNode],

      /** backedges to loop header */
      val backedges: Set[(WALA.CFGNode, WALA.CFGNode)],

      /** in edges to loop header */
      val inedges: Set[AIEdge],

      /** exit edges from loop header */
      val exitedges: Set[AIEdge],

      /** loop exits and backedges */
      val transexits: Set[AIEdge],

      /** inner loop backedges */
      val innerloopexits: Set[AIEdge],

      /** vars on lhs of all instructions */
      val loopvars: Set[VarIndex],

      /** phi nodes lhs, rhs mappings */
      val primevars: Set[(VarIndex, VarIndex)]
    ) {
  }

  /***
   * For each loop header in a given method, find its nodeset,
   * relevant edges, and loop variables.
   */
  def getLoopStructure(wcontext: WALAContext) : Map[WALA.CFGNode, LoopStructure] = {

    val allbackedges = WALA.getBackEdges(wcontext.ir)
    ifDebug {Log.println("allbackedges " + allbackedges)}
    val heads = allbackedges.map{ case (_, head) => head }
    ifDebug {Log.println("heads " + heads)}

    val cfg = wcontext.cfg

    // find loop information, nodeset, edges, variables, etc
    heads.asInstanceOf[Set[WALA.CFGNode]].map{ head =>
      ifDebug {Log.println("processing loop head " + head)}

      val backedges = allbackedges.filter{ case(_, to) => to == head }
      ifDebug {Log.println("backedges " + backedges)}

      // union natural loop sets for all backedges, treating them as a
      // single loop
      var nodeset = Set[WALA.CFGNode]()
      backedges.foreach{ case(m, n) =>
        nodeset = nodeset ++ WALA.getNaturalLoop(cfg, m, n)
      }
      ifDebug {Log.println("nodeset " + nodeset)}

      // // find all body edges, i.e., those from the loop header to
      // // a loop node
      // var bodyedges = Set[AIEdge]()
      // cfg.getSuccNodes(head).foreach{ succ =>
      //   val s = succ.asInstanceOf[WALA.CFGNode]
      //   if (nodeset.contains(s)) {
      //     bodyedges += (head, s).asInstanceOf[AIEdge]
      //   }
      // }
      // ifDebug {println("bodyedges " + bodyedges)}

      // find all exit edges, i.e. those from a loop node to a
      // non-loop node
      var exitedges = Set[AIEdge]()
      def find_exits(
        p : WALA.CFGNode,
        path : List[WALA.CFGNode],
        visited : MSet[WALA.CFGNode]
      ) : Unit = {
        cfg.getSuccNodes(p).foreach{ succ =>
          val q = succ.asInstanceOf[WALA.CFGNode]
          if (! visited.contains(q)) {
            if (! nodeset.contains(q)) {
              exitedges = exitedges + (p, q).asInstanceOf[AIEdge]
            } else {
              find_exits(q, path ++ List(q), visited + q)
            }
          }
        }
      }
      find_exits(head, List(head), MSet(head))
      ifDebug {Log.println("exitedges " + exitedges)}

      // find inedges.  inedges are all predecessors of head that are
      // not in loopSet
      val inedges = cfg.getPredNodes(head).filter{ pred =>
        val p = pred.asInstanceOf[WALA.CFGNode]
        ! nodeset.contains(p)
      }.map{ p => (p, head).asInstanceOf[AIEdge] }.toSet
      ifDebug {Log.println("inedges " + inedges)}

      // find loopvars, those which are the lhs of operations, and
      // primevar pairs, those which are the lhs of a phi node paired
      // with the rhs from an edge within the loop (maybe in the
      // future will only be from backedges)
      // TODO test with nested loops
      var loopvars = Set[VarIndex]()
      var primevars = Set[(VarIndex, VarIndex)]()
      val visited = MSet[WALA.CFGNode]()
      def find_loopvars(
        n: WALA.CFGNode
      ) : Unit = {
        visited += n
        // println(n)

        // add lhs of phis
        val preds = cfg.getPredNodes(n).toList
        n.iteratePhis().foreach{phi =>
          val lhs = phi.getDef().asInstanceOf[VarIndex]
          for (idx <- 0 to (phi.getNumberOfUses() - 1)) {
            if (nodeset.contains(preds.get(idx))) {
              val rhs = phi.getUse(idx).asInstanceOf[VarIndex]
              primevars = primevars + ((lhs, rhs))
              loopvars += lhs
            }
          }
        }

        // add lhs of instructions
        for (idx <- n.getFirstInstructionIndex() to n.getLastInstructionIndex()) {
          val (inst, _) = WALA.getInstruction(idx, wcontext.ir)
          // println(inst)
          inst match {
            case i: BinopI =>
              loopvars += i.getDef()
            case i: ArrayLengthI =>
              loopvars += i.getDef()
            case i: GetI =>
              // TODO may need special handling for field
              loopvars += i.getDef()
            case i: ArrayLoadI =>
              // TODO may need special handling for index var
              loopvars += i.getDef()
            case i => // do nothing
          }
        }

        cfg.getSuccNodes(n).foreach{m =>
          if (! visited.contains(m) && nodeset.contains(m)) {
            find_loopvars(m.asInstanceOf[WALA.CFGNode])
          }
        }
      }
      find_loopvars(head)
      ifDebug {Log.println("loopvars " + loopvars)}
      ifDebug {Log.println("primevars " + primevars)}

      // ifDebug {println("\ninitial machine\n" + loopmachine)}

      // stop ainterping at exit and backedges to get one iteration of
      // the loop
      val transexits = (backedges ++ exitedges).asInstanceOf[Set[AIEdge]]

      // // collect inner loop backedges to stop ainterp from widening inner loops
      // var innerloopexits = allbackedges.map{ case(_, to) => to }.toSet.filter{ n => nodeset.contains(n) }
      // var innerloopexits = allbackedges
      var innerloopexits = allbackedges.filter{ case(from, to) => nodeset.contains(to) }.asInstanceOf[Set[AIEdge]]

      val loopinfo = new LoopStructure(
        head,
        nodeset,
        backedges,
        inedges,
        exitedges,
        transexits,
        innerloopexits,
        loopvars,
        primevars
      )

      (head, loopinfo)
    }.toMap
  }

  object TypeType extends Enumeration {
    type t = Value
    val
      TypeTypeIntegral,
      TypeTypeFloating,
      TypeTypeChar,
      TypeTypeBoolean,
      TypeTypeArray,
      TypeTypeUnhandled
    = Value
  }

  import TypeType._

  def getTypeType(t: TYPE): t = t match {
    case com.ibm.wala.types.TypeReference.Byte
       | com.ibm.wala.types.TypeReference.Short
       | com.ibm.wala.types.TypeReference.Int
       | com.ibm.wala.types.TypeReference.Long    => TypeTypeIntegral
    case com.ibm.wala.types.TypeReference.Float
       | com.ibm.wala.types.TypeReference.Double  => TypeTypeFloating
    case com.ibm.wala.types.TypeReference.Char    => TypeTypeChar
    case com.ibm.wala.types.TypeReference.Boolean => TypeTypeBoolean
    case com.ibm.wala.types.TypeReference.ByteArray
       | com.ibm.wala.types.TypeReference.ShortArray
       | com.ibm.wala.types.TypeReference.LongArray
       | com.ibm.wala.types.TypeReference.IntArray
       | com.ibm.wala.types.TypeReference.CharArray
       | com.ibm.wala.types.TypeReference.BooleanArray => TypeTypeArray       
    case _ => TypeTypeUnhandled
  }
}

import Util._
import Core._
import PPL._
import PPL.LExp._
import WALAUtil._
import Label._
import Expression._
import AbstractState._
import StateTypes._
import Annotation._
import Config._

import collection.JavaConversions._
import edu.illinois.wala.Facade._
import play.api.libs.json._
import parma_polyhedra_library._

import scalaz._
import Scalaz._
import com.ibm.wala.analysis.typeInference.TypeInference

import java.io._

object AbstractMachine {
  type Machine = AbstractMachine
  val Machine = AbstractMachine

  def showStack(s: List[Frame]) = {
    "Stack with:\n" + tab(
      s.map{_.toString}.mkString("\n")
    )
  }

  def alignX
    (m1: Machine, m2: Machine)
    (implicit init: Boolean = false)
      : (Machine, Machine) = {
    // The first machine's mapping of label to dimension might be
    // reordered, but the second can only be expanded.

    val (s1, s2) = m1.state.alignX(m2.state)(init)
    (
      m1.copy(state = s1),
      m2.copy(state = s2)
    )
  }

  def alignToX
    (m1_in: Machine, ms: Iterable[Machine])
    (implicit init: Boolean = true)
      : (Machine, Iterable[Machine]) = {

    // Align ms to the first machine with the first machine preserving
    // the mapping of its variables.
    ms.foldRight[(Machine, List[Machine])]((m1_in, List[Machine]())){
      case (m, (m1:Machine, acc: List[Machine])) =>
        val (m1a: Machine, ma) = AbstractMachine.alignX(m1, m)(init)
        (m1a, ma :: acc)
    }
  }

  def join(m1: Machine, m2: Machine): Machine = m1.join(m2)
  def join(hs: Iterable[Machine]): Machine = join(hs.toList)
  def join(hs: List[Machine]): Machine = hs match {
    case h :: rest => (h.join(rest)).asInstanceOf[Machine]
    case _ => throw LogicException("cannot join empty list of machines")
  }
  
  def alignManyX(ms: List[Machine]): Iterable[Machine] = {
    val temp = ms.size match {
      case 0 => List[Machine]()
      case 1 => List[Machine](ms.head)
      case 2 =>
        val (s1, s2) = AbstractMachine.alignX(ms.head, ms.tail.head)
        List[Machine](s1, s2)
      case _ =>
        val first_m = ms.head
        val tail_ms = ms.tail
        val (first_ma, tail_msa)   = AbstractMachine.alignToX(first_m, tail_ms)
        val (first_maa, tail_msaa) = AbstractMachine.alignToX(first_ma, tail_msa)
        // Do this twice for reasons.
        first_maa :: tail_msaa.toList
    }

    temp
  }
}

case class AbstractMachine(
  /* based on Andrew's concrete interpreter */
    
  val cgnode: WALA.CGNode,    
  val ir: WALA.IR,
  //val heap: Heap,
  val state: AbstractState,
  val stack: List[Frame],
  val countMachines: BigInt,    // to track how many different machines were joined into this one
  val name:    String,          // for adding notes to keep track of it
  val prePhi:  AbstractMachine,
  val annots:  Annots,
  val allocs: List[WALA.IK],
  val calleeHeapMachine: AbstractMachine,
  val typeInference: TypeInference,
  val backedges: Set[WALA.Edge]
) extends IMachine[AbstractMachine] { // Binding[Label, AbstractValue] {
  import AbstractMachine.{Machine}

  def this(cgnode: WALA.CGNode, annots: Annots) = this(
    cgnode = cgnode,
    ir     = cgnode.getIR(),
    state = new AbstractState(cgnode.getIR()),
    stack = List[Frame](),
    countMachines = 1,
    name    = "",
    prePhi  = null,
    annots  = annots,
    allocs = List[WALA.IK](),
    calleeHeapMachine = null,
    typeInference = com.ibm.wala.analysis.typeInference.TypeInference.make(cgnode.getIR(), true),
    WALA.getBackEdges(cgnode.getIR())
  )

  def this(cgnode: WALA.CGNode) = this(
    cgnode = cgnode,
    ir     = cgnode.getIR(),
    //heap = new Heap(),
    state = new AbstractState(cgnode.getIR()),
    stack = List[Frame](),
    countMachines = 1,
    name    = "",
    prePhi  = null,
    annots  = emptyAnnots,
    allocs = List[WALA.IK](),
    calleeHeapMachine = null,
    typeInference = com.ibm.wala.analysis.typeInference.TypeInference.make(cgnode.getIR(), true),
    backedges = WALA.getBackEdges(cgnode.getIR())
  )

  val cfg: WALA.CFG = ir.getControlFlowGraph()  

  def mapState(f: AbstractState => AbstractState): AbstractMachine = {
    this.copy(state = state.mapState(f))
  }

  def flatMap(f: AbstractState => Iterable[AbstractState]): AbstractMachine = {
    this.copy(state = state.flatMap(f))
  }

  def doReturn: AbstractMachine = {
    mapTop(_.doReturn)
  }

  def readvalue(l: Label): List[(AbstractState, Option[Term.Closed])] = {
    List((state, state.readvalue(l)))
  }

  /*
   * IBinding interface, and related
   */ 
  type Binder = AbstractMachine
  
  def alloc(k: Label): AbstractMachine = {
    val new_m = this
      .mapState{s => s.alloc(k)}
    new_m.copy(state = new_m.state.joinAllStates())
  }
  
  def alloc(k: List[Label])(implicit init: Boolean = false): AbstractMachine = {
    this.mapState{s => s.alloc(k)(init)}
  }


  def bind(k: Label, v: Term.Closed): Machine = {
    val new_m = mapState{s => s.bind(k,v)}
    new_m.copy(state = new_m.state.joinAllStates())
  }
  def unbind(k: Label): Machine = this.mapState{s => s.unbind(k)}
  def unbind(l: List[Label]): Machine = this.mapState{s => s.unbind(l)}
  def unbind(f: Label => Boolean): Machine = this.mapState{s => s.unbind(f)}
  
  
  def bindEval(vid: Label, in_exp: Exp.Closed): AbstractMachine = {
    mapState{s => s.bindEval(vid, in_exp)}
  }

  def bindCopy(l1: Label, l2: Label): AbstractMachine = {
    mapState{s => s.bindCopy(l1,l2)}
  }

  def filter(f2: Label => Boolean): Machine = {
    mapState{s => s.filter(f2)}
  }
  
  def filter(l: List[Label], f: Label => Boolean): Machine = {
    mapState{s => s.filter(l, f)}
  }
  
  def duplicateVars(): Machine = {
    mapState { s => s.duplicateVars }
  }
  
  def diff(m: AbstractMachine): List[FrameHeapParameterCopy] = {
    this.state.dispatch.keySet.foldLeft(List[FrameHeapParameterCopy]()) {
    case (l, key) =>
      if(m.state.dispatch.keySet.contains(key))
        l
      else {
        key match {
          case fhpc: FrameHeapParameterCopy => fhpc :: l
          case _ => l
        }
      }
    }
  }

  /*
   * ILattice interface
   */
  def isTop: Boolean = false
  def isBottom = state.isBottom
  def assert(in_exp: Exp.Open): AbstractMachine = {
    this.copy(state = this.state.assert(in_exp))
  }
  def assume(in_exp: Exp.Open): AbstractMachine = {
    flatMap{s => s.assume(Exp.close(in_exp, s.dispatch))}
  }

  def joinAllStates(): AbstractMachine = {
    this.copy(state = this.state.joinAllStates())
  }
  def join(m2_in: AbstractMachine): AbstractMachine = {
    val (m1, m2) = this.align(m2_in)

    type S[R] = State[LinearContext,R]
    type Stack = List[Frame]

    val s12 = m1.state.join(m2.state)
    val temp = m1.copy(
      state = s12,
      countMachines = m1.countMachines + m2.countMachines
    )

    ifDebug{
      Log.println("joining:\n" + tab(this) +
        "\nwith:\n" + tab(m2_in) + "\nresult:\n" + tab(temp))
    }

    temp
  }
  def widen(m2: AbstractMachine): AbstractMachine = {
    this.copy(
      state = this.state.widen(m2.state),
      countMachines = this.countMachines + m2.countMachines
    )
  }
  def leq(m2: AbstractMachine): Boolean = {
    this.state.leq(m2.state)
  }
  
  def widen_leq(m2: AbstractMachine): Boolean = {
    this.state.widen_leq(m2.state)
  }
  
  def concat(m2: AbstractMachine): AbstractMachine = {
    this.copy(state = this.state.concat(m2.state))
  }

  def align(m2: AbstractMachine): (AbstractMachine, AbstractMachine) = {
    val m1 = this
    val (s1, s2) = m1.state.align(m2.state)
    (
      m1.copy(state = s1),
      m2.copy(state = s2)
    )
  }
  
  def alignX(m2: AbstractMachine): (AbstractMachine, AbstractMachine) = {
    val m1 = this
    val (s1, s2) = m1.state.alignX(m2.state)
    (
      m1.copy(state = s1),
      m2.copy(state = s2)
    )
  }

  def topFrame: Frame = this.stack match {
    case f :: _ => f
    case _ => throw InterpException("no frames on the stack")
  }
  def mapTop(f: Frame => Frame): AbstractMachine = {
    val (frame, stack) = this.stack match {
      case frame :: reststack => (frame, reststack)
      case _ => throw InterpException("no frames on the stack")
    }
    this.copy(stack = f(frame) :: stack)
  }

  def newFrame: AbstractMachine = {
    val mname = ir.getMethod().getSignature().toString()
    val f = new Frame(ir)
      .copy(annots = annots.getOrElse(mname, default=emptyMethodAnnots))
    ifDebug{Log.println(s"newframe mname=$mname, annots = " + f.annots)}
    this.copy(stack = f :: this.stack)
  }
  
  def newFrame(ir: WALA.IR): AbstractMachine = {
    val mname = ir.getMethod().getSignature().toString()
    val f = new Frame(ir)
      .copy(annots = annots.getOrElse(mname, default=emptyMethodAnnots))
    ifDebug{Log.println(s"newframe mname=$mname, annots = " + f.annots)}
    this.copy(stack = f :: this.stack)
  }
  
  def popFrame: AbstractMachine = {
    stack match {
      case f :: rest => this.copy(stack = rest)
    }
  }

  def gotoTop(n: WALA.CFGNode): AbstractMachine = this.mapTop{f => f.goto(n)}
  def gotoTop(idx: Int): AbstractMachine = this.mapTop{f => f.goto(idx)}
  def nextTop: AbstractMachine = this.mapTop{_.next}
  def hasTopReturned: Boolean = topFrame.hasReturned//state.isBound(this.topFrame.retLabel)
  def blockTop: WALA.CFGNode = topFrame.block

  def writevalue(label:Label, v:Term.Closed): AbstractMachine = {
    mapState{_.writevalue(label, v)}
  }

  def invokeViaStack(i: InvokeI, node:WALA.CGNode)(implicit conf: AInterpConfig): AbstractMachine = {
    ifVerbose {
      Log.println("entering " + node.getMethod.getSignature)
    }
            
    val rec = this.stack.foldLeft(false) {
      case (b, s) =>
        if(s.pc.method.equals(node.getIR)) {
          true
        } else {
          if(b) true
          else false
        }
    }
        
    val nullIR =
      if(node.getIR!=null)
        false
      else
        true
    
    (rec, nullIR) match {
      case (false, false) =>
        
        val caller_wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, this.cgnode)
        val caller_context = InterpContext(new EvalContext(), caller_wcontext)
        
        val caller_objs = 
          if(ConfigManager.hasSummarizedDimensions)
            WALAUtil.populateObjs(this, caller_context)
          else 
            List[Label]()
        
        val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)      
        val context = InterpContext(new EvalContext(), wcontext)
        val caller_frame = this.topFrame
                                
        //initialize a new callee's machine that copies the caller's labels 
        val m1 = this.copy(cgnode = node, ir = node.getIR, typeInference = com.ibm.wala.analysis.typeInference.TypeInference.make(node.getIR(), true), backedges = WALA.getBackEdges(node.getIR()))//.filter( { l => isLocalOrHeapReachable(l,this.topFrame.id, EscapeAnalysis.reachableHeapMap.get(this.cgnode).get) }, { l => isLocalOrHeapReachableL(l,this.topFrame.id, EscapeAnalysis.reachableHeapMap.get(this.cgnode).get) })
        val m2 = m1.newFrame
        val callee_frame = m2.topFrame
        val callee_method = node.getMethod
        
        val (vars, m_vars_before) = populateVars(m2, context)

        val aps =
        if(ConfigManager.hasAccessPath)
          WALAUtil.populateAPs(m_vars_before, context)
        else 
          List[Label]()
          
        val objs = 
          if(ConfigManager.hasSummarizedDimensions)
            WALAUtil.populateObjs(m_vars_before, context)
          else 
            List[Label]()
          
        val m_vars_after = m_vars_before.alloc(vars++aps)
                
        //link the formal and actual parameters as well as the access paths (if inline option is on)
        val m3 = m_vars_after.mapState { s => 
          (0 to (i.getNumberOfParameters()-1)).foldLeft(s) {
            case (s_tmp, j) =>
              val parType = callee_method.getParameterType(j)
              WALA.getTypeType(parType) match {
                case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                  val callerPar = caller_frame.localLabel(i.getUse(j))
                  val s_1 = s_tmp.promoteOrAlloc(callerPar) //promote to enable parameter passing
                  val calleePar = callee_frame.localLabel(j+1)
                  val s_2 = s_1.alloc(calleePar)
                  val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerPar))
                  val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleePar))
                  s_2.copy(linear = s_2.linear.assumeSingle(exp2 == exp1))     
                case _ => 
                  if(ConfigManager.inline) {
                    val fields = GlobalHeap.getFields(parType)
                    val pk = GlobalHeap.getPointerKey(this.cgnode, i.getUse(j))
                  
                    val s_new = fields.foldLeft(s_tmp) {
                      case (s_tmp_1, f) =>
                        val callerAP = caller_frame.accessPathLabel(pk, f)
                        val calleePK = GlobalHeap.getPointerKey(node, j+1)
                        val calleeAP = callee_frame.accessPathLabel(calleePK, f)
                        (s_tmp_1.readvalue(callerAP), s_tmp_1.readvalue(calleeAP)) match {
                          case (None, _) | (_, None) => s_tmp_1
                          case _ =>
//                            val callerAP = caller_frame.accessPathLabel(pk, f)
//                            val s_1 = s_tmp_1.promoteOrAlloc(callerAP)
//                            val calleePK = GlobalHeap.getPointerKey(node, j+1)
//                            val calleeAP = callee_frame.accessPathLabel(calleePK, f)
//                            val s_2 = s_1.alloc(calleeAP)
                            val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerAP))
                            val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeAP))
                            s_tmp_1.copy(linear = s_tmp_1.linear.assumeSingle(exp2 == exp1))
                        }
                    }
                    val callerAP = caller_frame.arrayLengthAccessPathLabel(pk)                   
                    val calleePK = GlobalHeap.getPointerKey(node, j+1)
                    val calleeAP = callee_frame.arrayLengthAccessPathLabel(calleePK)
                    (s_new.readvalue(callerAP), s_new.readvalue(calleeAP)) match {
                      case (None, _) | (_, None) => 
                        s_new
                      case _ =>
//                        val callerAP = caller_frame.arrayLengthAccessPathLabel(pk)
//                        val s_1 = s_new.promoteOrAlloc(callerAP)
//                        val calleePK = GlobalHeap.getPointerKey(node, j+1)
//                        val calleeAP = callee_frame.arrayLengthAccessPathLabel(calleePK)
//                        val s_2 = s_1.alloc(calleeAP)
                        val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerAP))
                        val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeAP))
                        s_new.copy(linear = s_new.linear.assumeSingle(exp2 == exp1))
                    }                    
                  } else {
                    s_tmp
                  }
              }
           }
        }
        
        val m4 = populateSymbols(m3, wcontext)
        
        //create m5 that contains only variables reachable from the callee
        val m5 = m4.filter (caller_objs.diff(objs), { l : Label => isLocalOrHeapReachableL(l,callee_frame.id, List[Label]()) })
                          
        val entry = m5.cfg.entry()
                
        val m6 = AInterp.cache.get(node) match {
          case None =>            
            AInterp.stepMethod(context, m5.gotoTop(entry))(conf) match {
              case Some(end_m) =>
                AInterp.cache += (node -> (m5, end_m))
                Some(end_m)
              case _ => None
            }
            
          case Some((input_m, ret_m)) =>           
            if(m5.leq(input_m)) {
              ifVerbose {
                Log.println("using cached results")
              }
              Some(ret_m.copy(stack = m5.stack))
            } else {
              val joined_m = m5.join(input_m).copy(stack = m5.stack)
              
              AInterp.stepMethod(context, joined_m.gotoTop(entry))(conf) match {
                case Some(end_m)=>
                  AInterp.cache += (node -> (joined_m, end_m))
                  Some(end_m)
                case _ => None
              }
              
            }
        }

        m6 match {
          case Some(tmp_m) =>
            val m7 = tmp_m
                                 
            //m_not_reachable stores labels not reachable from the callee
            val m_not_reachable = m4.unbind(objs).unbind(Label.isAccessPathL)
//              if(!ConfigManager.inline)                
//                m4.unbind(objs++aps)
//              else 
//                //TODO: inline access paths from callee to the caller
//                m4.unbind(objs++aps)
            
            //concatenate the callee's result with the unreachable part of the caller (disjoint labels)
            val m7_concat = m7.concat(m_not_reachable)
            
            //link the return and lhs of the call site
            val localret = caller_frame.localLabel(i.getReturnValue(0))
            val m8 = m7_concat.mapState { s => 
              s.readvalue(callee_frame.retLabel) match {
                case Some(Term.Constant(CIV(x))) =>
                  s.writevalue(localret, Term.Constant(CIV(x)))
                case Some(Term.Constant(CBV(x))) =>
                  s.writevalue(localret, Term.Constant(CBV(x)))
                case Some(Term.Linear(_)) =>
                  val retType = callee_method.getReturnType()
                  WALA.getTypeType(retType) match {
                    case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                      val s_promote = s.promoteOrAlloc(localret)
                      val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(callee_frame.retLabel))                     
                      s_promote.bindEval(localret, exp) //a strong update on the caller's rhs value
                    case _ => s//only deal with numeric types; reference types should have been reflected via heap analysis
                  }
                case Some(Term.Null()) => s;
                case None => //TODO: debug to check why this happens
                  val retType = callee_method.getReturnType()
                  WALA.getTypeType(retType) match {
                    case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                      s.promoteOrAlloc(localret)
                    case _ => s//only deal with numeric types; reference types should have been reflected via heap analysis
                  }
                case _ => throw InterpException("case not considered in invoke via stack")
              }
            }
            
            val m8_inline = if(ConfigManager.inline) {
              m8.mapState { s => 
                (0 to (i.getNumberOfParameters()-1)).foldLeft(s) {
                  case (s_tmp, j) =>
                    val parType = callee_method.getParameterType(j)
                    WALA.getTypeType(parType) match {
                      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                        s_tmp    
                      case _ => 
                       
                        val fields = GlobalHeap.getFields(parType)
                        val pk = GlobalHeap.getPointerKey(this.cgnode, i.getUse(j))
                      
                        val s_new = fields.foldLeft(s_tmp) {
                          case (s_tmp_1, f) =>
                            val callerAP = caller_frame.accessPathLabel(pk, f)
                            val calleePK = GlobalHeap.getPointerKey(node, j+1)
                            val calleeAP = callee_frame.accessPathLabel(calleePK, f)
                            (s_tmp_1.readvalue(callerAP), s_tmp_1.readvalue(calleeAP)) match {
                              case (None, _) | (_, None) => s_tmp_1
                              case _ =>                                
                                val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerAP))
                                val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeAP))
                                s_tmp_1.copy(linear = s_tmp_1.linear.assumeSingle(exp2 == exp1))
                            }
                        }
                        val callerAP = caller_frame.arrayLengthAccessPathLabel(pk)
                        val calleePK = GlobalHeap.getPointerKey(node, j+1)
                        val calleeAP = callee_frame.arrayLengthAccessPathLabel(calleePK)
                        (s_new.readvalue(callerAP), s_new.readvalue(calleeAP)) match {
                          case (None, _) | (_, None) => 
                            s_new
                          case _ =>                            
                            val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerAP))
                            val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeAP))
                            s_new.copy(linear = s_new.linear.assumeSingle(exp2 == exp1))
                        }                                            
                    }
                 }
              }
            } else {
              m8
            }
                                   
            //remove remaining callee's local labels
            val m9 = m8_inline.filter({ l => isNotLocalL(l,callee_frame.id) })
            
            //restore caller's machine
            val m10 = m9.popFrame.copy(cgnode = this.cgnode, ir = this.ir, typeInference = this.typeInference, backedges = this.backedges)
                    
            ifVerbose {
              Log.println("leaving " + node.getMethod.getSignature)
            }

            m10
          case None =>
            ifVerbose {
              Log.println("WARNING: skipping!")
            }
            WALA.getTypeType(i.getDeclaredResultType()) match {            
              case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                val m = this.mapState { s =>
                  s.unbind(this.topFrame.localLabel(i.getDef())).alloc(this.topFrame.localLabel(i.getDef()))
                }
                m.copy(state = m.state.joinAllStates())
              case _ => 
                this
            }
        }
        
              
      case (_, _) =>
        ifVerbose {
          Log.println("WARNING: skipping!")
        }
        WALA.getTypeType(i.getDeclaredResultType()) match {            
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            val m = this.mapState { s =>
              s.unbind(this.topFrame.localLabel(i.getDef())).alloc(this.topFrame.localLabel(i.getDef()))
            }
            m.copy(state = m.state.joinAllStates())
          case _ => 
            this
        }
    }    
  }
  
  def invokeViaClinitStack(node:WALA.CGNode)(implicit conf: AInterpConfig): AbstractMachine = {
    ifVerbose {
      Log.println("entering " + node.getMethod.getSignature)
    }
            
    val rec = this.stack.foldLeft(false) {
      case (b, s) =>
        if(s.pc.method.equals(node.getIR)) {
          true
        } else {
          if(b) true
          else false
        }
    }
        
    val nullIR =
      if(node.getIR!=null)
        false
      else
        true
    
    (rec, nullIR) match {
      case (false, false) =>
        
        val caller_wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, this.cgnode)
        val caller_context = InterpContext(new EvalContext(), caller_wcontext)
        
        val caller_objs = 
          if(ConfigManager.hasSummarizedDimensions)
            WALAUtil.populateObjs(this, caller_context)
          else 
            List[Label]()
        
        val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)      
        val context = InterpContext(new EvalContext(), wcontext)
        val caller_frame = this.topFrame
                                
        //initialize a new callee's machine that copies the caller's labels 
        val m1 = this.copy(cgnode = node, ir = node.getIR, typeInference = com.ibm.wala.analysis.typeInference.TypeInference.make(node.getIR(), true), backedges = WALA.getBackEdges(node.getIR()))//.filter( { l => isLocalOrHeapReachable(l,this.topFrame.id, EscapeAnalysis.reachableHeapMap.get(this.cgnode).get) }, { l => isLocalOrHeapReachableL(l,this.topFrame.id, EscapeAnalysis.reachableHeapMap.get(this.cgnode).get) })
        val m2 = m1.newFrame
        val callee_frame = m2.topFrame
        val callee_method = node.getMethod
        
        val (vars, m_vars_before) = populateVars(m2, context)

        val aps =
        if(ConfigManager.hasAccessPath)
          WALAUtil.populateAPs(m_vars_before, context)
        else 
          List[Label]()
          
        val objs = 
          if(ConfigManager.hasSummarizedDimensions)
            WALAUtil.populateObjs(m_vars_before, context)
          else 
            List[Label]()
          
        val m_vars_after = m_vars_before.alloc(vars++aps)//.alloc(objs)(true)
                
        //link the formal and actual parameters as well as the access paths (if inline option is on)
        val m3 = m_vars_after
        
        val m4 = populateSymbols(m3, wcontext)
        
        //create m5 that contains only variables reachable from the callee
        val m5 = m4.filter (caller_objs.diff(objs), { l : Label => isLocalOrHeapReachableL(l,callee_frame.id, List[Label]()) })
                          
        val entry = m5.cfg.entry()
                
        val m6 = AInterp.cache.get(node) match {
          case None => 
            
            AInterp.stepMethod(context, m5.gotoTop(entry))(conf) match {
              case Some(end_m) =>
                AInterp.cache += (node -> (m5, end_m))
                Some(end_m)
              case _ => None
            }
            
          case Some((input_m, ret_m)) =>           
            if(m5.leq(input_m)) {
              ifVerbose {
                Log.println("using cached results")
              }
              Some(ret_m.copy(stack = m5.stack))
            } else {
              val joined_m = m5.join(input_m).copy(stack = m5.stack)
              
              AInterp.stepMethod(context, joined_m.gotoTop(entry))(conf) match {
                case Some(end_m)=>
                  AInterp.cache += (node -> (joined_m, end_m))
                  Some(end_m)
                case _ => None
              }
              
            }
        }
        
        m6 match {
          case Some(tmp_m) =>
            val m7 = tmp_m
                                 
            //m_not_reachable stores labels not reachable from the callee
            val m_not_reachable = 
              if(!ConfigManager.inline)
                m4.unbind(objs++aps)
              else 
                m4.unbind(objs++aps)
           
            //concatenate the callee's result with the unreachable part of the caller (disjoint labels)
            val m8 = m7.concat(m_not_reachable)
                                   
            //remove remaining callee's local labels
            val m9 = m8.filter({ l => isNotLocalL(l,callee_frame.id) })
            
            //restore caller's machine
            val m10 = m9.popFrame.copy(cgnode = this.cgnode, ir = this.ir, typeInference = this.typeInference, backedges = this.backedges)
                    
            ifVerbose {
              Log.println("leaving " + node.getMethod.getSignature)
            }

            m10
          case None =>
            ifVerbose{
          Log.println("WARNING: skipping!")
        }
        this
        }
        
              
      case (_, _) =>
        ifVerbose{
          Log.println("WARNING: skipping!")
        }
        this
    }    
  }

  /* Take a machine and interpret an invoke expression pointed to be the
   * top frame by using the called method's summary machine.
   */
  def invokeViaSummary(summary_ms: List[AbstractMachine]): AbstractMachine = 
  {
    val instr = this.topFrame.pc.instr

    if (null == instr)
      throw InterpException("current instruction is null")
    
    instr match {
      case i: InvokeI =>
        val localret = this.topFrame.localLabel(i.getReturnValue(0))
        val ret = this.flatMap { s =>  
          var summarized_states = List[AbstractState]()
          summary_ms.foreach { summary_m => 
            val sum_s = summary_m.state
            val sum_s1 = sum_s.dispatch.keys.foldLeft(sum_s) {
              case(s, label) =>                  
                label match {
                  case fhr: FrameHeapRegister =>
                    fhr match {
                      case FrameHeapRegister(_, pk) =>                          
//                        if(!EscapeAnalysis.mayBeLive(pk, this.cgnode)) {
//                          s.filter(List(fhr), {l : Label => true})
//                        }else {
//                          s
//                        }
                        s
                    }
                  case _ => s
                }
            }
            val (sum_s2, notskip) = sum_s1.dispatch.keys.foldLeft((sum_s1, true)) {
              case((s, flag), label) =>                  
                label match {
                  case fhpc: FrameHeapParameterCopy =>
                    fhpc match {
                      case FrameHeapParameterCopy(FrameHeapParameter(_, pk), _) => 
                        if(pk.isInstanceOf[com.ibm.wala.ipa.modref.ArrayLengthKey]) {
                          if(!EscapeAnalysis.mayBeLive(pk, this.cgnode)) {
                            if(s.linear.constraints(fhpc)) {  
//                             (s.filter(List(fhpc), {l : Label => true}), false)
                              (s, false)
                            } else {
//                              (s.filter(List(fhpc), {l : Label => true}), flag)
                              (s, flag)
                            }
                          }else {
                            (s, flag)
                          }
                        } else {
                          (s, flag)
                        }
                    }
                  case _ => (s, flag)
                }
            }
             
            if(notskip) {
              
              val summary_s = sum_s2

//              var tmpLabels = List[Label]()

              val callee = summary_s.ir.getMethod()
              
              val s_copied =  s.copy(linear = s.linear.concat(summary_s.linear))
              
              //generate constraints: (actual parameters) == (formal parameters)
              val s_1 = (0 to (i.getNumberOfParameters()-1)).foldLeft(s_copied) {
                case (s_tmp, j) =>
                  val parType = callee.getParameterType(j)
                  WALA.getTypeType(parType) match {
                    case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                      val callerPar = this.topFrame.localLabel(i.getUse(j))
                      val s_promote = s_tmp.promoteOrAlloc(callerPar) //promote to enable parameter passing
                      val calleePar = summary_m.topFrame.localLabel(j+1)
                      val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerPar))
                      val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleePar))
//                      tmpLabels = calleePar :: tmpLabels
                      s_promote.copy(linear = s_promote.linear.assumeSingle(exp1 == exp2))
                      
                    case _ => s_tmp//only deal with numeric types; reference types should have been reflected via heap analysis
                  }
              }

              //strongly update on lhs of the call site with the return value in the summary
              val s_2 = summary_s.readvalue(summary_m.topFrame.retLabel) match {
                case Some(Term.Constant(CIV(x))) =>
                  s_1.writevalue(localret, Term.Constant(CIV(x)))
                case Some(Term.Constant(CBV(x))) =>
                  s_1.writevalue(localret, Term.Constant(CBV(x)))
                case Some(Term.Linear(_)) =>
                  val retType = callee.getReturnType()
                  WALA.getTypeType(retType) match {
                    case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                      val callerRVal = this.topFrame.localLabel(i.getReturnValue(0))
                      val s_promote = s_1.promoteOrAlloc(callerRVal)
                      val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(summary_m.topFrame.retLabel))                     
//                      tmpLabels = summary_m.topFrame.retLabel :: tmpLabels
                      s_promote.bindEval(callerRVal, exp) //a strong update on the caller's rhs value
                    case _ => s_1//only deal with numeric types; reference types should have been reflected via heap analysis
                  }
                case Some(Term.Null()) => s_1;
                case None => s_1
                case _ => throw InterpException("case not considered in invoke summary")
              }

              //"parameter passing" for heap dimensions in the summary
              var tmpCount = 0;                           
              val ss_3 = summary_s.dispatch.keys.foldLeft(List(s_2)) {
                case(list_1, label) =>  
                  val list = if(list_1.size > 8) {
                    ConfigManager.joinPolicy = None
                    AbstractState.join(list_1).toList
                  } else list_1
                  
                  label match {
                    case fhpc: FrameHeapParameterCopy =>
                      var new_list = List[AbstractState]()
                      tmpCount = tmpCount + 1
                      fhpc match {
                        case FrameHeapParameterCopy(FrameHeapParameter(_, pk), _) => 
                          if(EscapeAnalysis.mayBeLive(pk, this.cgnode)) {
                            list.foreach{s_l =>
                              s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                case None => 
                                  val s_init = s_l.initHeap(this.topFrame.parHeapLabel(pk)).copyBind(this.topFrame.tmpLabel(tmpCount), this.topFrame.parHeapLabel(pk))                        
                                  val callerHeapPar = this.topFrame.tmpLabel(tmpCount)
                                  val calleeHeapPar = fhpc
                                  val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeHeapPar))
                                  val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(callerHeapPar))
                              
//                                  if(!tmpLabels.contains(calleeHeapPar))
//                                    tmpLabels = calleeHeapPar :: tmpLabels
//                              
//                                  if(!tmpLabels.contains(callerHeapPar))
//                                    //also to remove the callerHeapPar because it is temporary (due to the implicit read)
//                                    tmpLabels = callerHeapPar :: tmpLabels     
                                
                                  new_list = s_init.copy(linear = s_init.linear.assumeSingle(exp1 == exp2)) :: new_list
                                case _ =>
                                  val s_promote = s_l.promoteOrAlloc(this.topFrame.regHeapLabel(pk))
                                  val s1 = s_promote.copyBindRemove(this.topFrame.tmpLabel(tmpCount), this.topFrame.regHeapLabel(pk))
                                  val s_init = s_l.initHeap(this.topFrame.parHeapLabel(pk))
                                  val s2 = s_init.copyBind(this.topFrame.tmpLabel(tmpCount), this.topFrame.parHeapLabel(pk))
                              
                                  val callerHeapPar = this.topFrame.tmpLabel(tmpCount)
                                  val calleeHeapPar = fhpc
                              
                                  val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeHeapPar))
                                  val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(callerHeapPar))
                              
//                                  if(!tmpLabels.contains(calleeHeapPar))
//                                    tmpLabels = calleeHeapPar :: tmpLabels
//                              
//                                  if(!tmpLabels.contains(callerHeapPar))
//                                    //also to remove the callerHeapPar because it is temporary (due to the implicit read)
//                                    tmpLabels = callerHeapPar :: tmpLabels     
                                                                  
                                  new_list = s1.copy(linear = s1.linear.assumeSingle(exp1 == exp2)) :: new_list
                                  new_list = s2.copy(linear = s2.linear.assumeSingle(exp1 == exp2)) :: new_list
                               }
                            }
                          } else { //the heap is not live from the caller
                            list.foreach{s_l =>
                              val calleeHeapPar = fhpc
                              val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeHeapPar))
                              val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
                          
//                              if(!tmpLabels.contains(calleeHeapPar))
//                                tmpLabels = calleeHeapPar :: tmpLabels
                                                      
                              new_list = s_l.copy(linear = s_l.linear.assumeSingle(exp1 == exp2)) :: new_list
                            }
                          }
                      }                   
                      new_list
                    case _ => list
                  }
              }

              //"side effects" of the summary
              val ss_4 = summary_s.dispatch.keys.foldLeft(ss_3) {
                case(list_1, label) =>   
                  val list = if(list_1.size > 8) {
                    ConfigManager.joinPolicy = None
                    AbstractState.join(list_1).toList
                  } else list_1
                  label match {
                    case fhr: FrameHeapRegister =>
                      fhr match {
                        case FrameHeapRegister(_, pk) =>                          
                          if(summary_s.hasUpdated(fhr)) {
//                              && EscapeAnalysis.mayBeLive(pk, this.cgnode)
                            summary_s.readvalue(fhr).get match {
                              case Term.Constant(CIV(x)) =>
                                var new_list = List[AbstractState]()
                                list.foreach{ s_l =>
                                  
                                  s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                    case None =>
                                      new_list = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CIV(x))) :: new_list
                                    case _ =>
                                      val s1 = s_l.writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CIV(x)))
                
                                      s_l.joinOrNot(s1) match {
                                        case None => 
                                          new_list = s_l :: new_list                                
                                          new_list = s1 :: new_list
                                        case Some(joined_s: AbstractState) =>
                                          new_list = joined_s :: new_list
                                      }
                                  
                                  }
                                }
                                new_list
                              case Term.Constant(CBV(x)) =>
                                var new_list = List[AbstractState]()
                                list.foreach{ s_l =>
                                  
                                  s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                    case None =>
                                      new_list = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CBV(x))) :: new_list
                                    case _ =>
                                      val s1 = s_l.writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CBV(x)))
                
                                      s_l.joinOrNot(s1) match {
                                        case None => 
                                          new_list = s_l :: new_list                                
                                          new_list = s1 :: new_list
                                        case Some(joined_s: AbstractState) =>
                                          new_list = joined_s :: new_list
                                      }
                                  
                                  }
                                }
                                new_list
                              case Term.Linear(_) =>
                                var new_list = List[AbstractState]()
                                list.foreach{ s_l =>
                                  s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                    case None =>
                                      val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(fhr))
                                  
//                                      if(!tmpLabels.contains(fhr))
//                                        tmpLabels = fhr :: tmpLabels
                                  
                                      new_list = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).bindEval(this.topFrame.regHeapLabel(pk), exp) :: new_list
                                    case _ =>
                                      val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(fhr))
                                      val s1 = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).bindEval(this.topFrame.regHeapLabel(pk), exp)
                
                                      s_l.joinOrNot(s1) match {
                                        case None => 
                                          new_list = s_l :: new_list                                
                                          new_list = s1 :: new_list
                                        case Some(joined_s: AbstractState) =>
                                          new_list = joined_s :: new_list
                                      }
                                  
//                                      if(!tmpLabels.contains(fhr))
//                                        tmpLabels = fhr :: tmpLabels
                                  }
                                }
                                new_list
                              }    
                          } else {
//                            if(!tmpLabels.contains(fhr))
//                              tmpLabels = fhr :: tmpLabels
                            list
                          }
                      }                      
                    case _ => list
                  }
              }     
              
              ss_4.foreach { s_4 =>    
                val s_4_filter = s_4.keepLabels ({ l : Label => isLocalOrHeapReachableL(l,this.topFrame.id, List[Label]()) })
                
                summarized_states = s_4_filter :: summarized_states
              }
                              
            }
          }
          AbstractState.join(summarized_states)
        }.joinAllStates
               
        //TODO:forget the access paths
        if(ConfigManager.inline)
          ret.unbind(Label.isAccessPathL)
        else
          ret.unbind(Label.isAccessPathL)
          
      case i => throw InterpException(s"current instruction is not invoke: $i")
    }
  }
  
  def invokeViaSummaryHybrid(summary_ms: List[AbstractMachine]): AbstractMachine = 
  {

    val instr = this.topFrame.pc.instr

    if (null == instr)
      throw InterpException("current instruction is null")
    
    instr match {
      case i: InvokeI =>
        val localret = this.topFrame.localLabel(i.getReturnValue(0))
        val ret = this.flatMap { s =>  
          var summarized_states = List[AbstractState]()
          summary_ms.foreach { summary_m => 

            List(summary_m.state).foreach { sum_s =>
              val sum_s1 = sum_s.dispatch.keys.foldLeft(sum_s) {
                case(s, label) =>                  
                  label match {
                    case fhr: FrameHeapRegister =>
                      fhr match {
                        case FrameHeapRegister(_, pk) =>                          
//                          if(!EscapeAnalysis.mayBeLive(pk, this.cgnode)) {
//                            s.filter({ x => !x.equals(fhr.toString()) }, { x => !x.equals(fhr) })
//                          }else {
//                            s
//                          }
                          s
                      }
                    case _ => s
                  }
              }

              val (sum_s2, notskip) = sum_s1.dispatch.keys.foldLeft((sum_s1, true)) {
                case((s, flag), label) =>                  
                  label match {
                    case fhpc: FrameHeapParameterCopy =>
                      fhpc match {
                        case FrameHeapParameterCopy(FrameHeapParameter(_, pk), _) => 
                          if(pk.isInstanceOf[com.ibm.wala.ipa.modref.ArrayLengthKey]) {
                            if(!EscapeAnalysis.mayBeLive(pk, this.cgnode)) {
                              if(s.linear.constraints(fhpc)) {                             
//                                (s.filter({ x => !x.equals(fhpc.toString()) }, { x => !x.equals(fhpc) }), false)
                                (s, false)
                              } else {
                                (s, flag)
//                                (s.filter({ x => !x.equals(fhpc.toString()) }, { x => !x.equals(fhpc) }), flag)
                              }
                            }else {
                              (s, flag)
                            }
                          } else {
                            (s, flag)
                          }
                      }
                    case _ => (s, flag)
                  }
              }
                             
              if(notskip) {
                                
                val summary_s = sum_s2

//                var tmpLabels = List[Label]()
  
                val callee = summary_s.ir.getMethod()

                val s_copied =  s.copy(linear = s.linear.concat(summary_s.linear))
                                
                //generate constraints: (actual parameters) == (formal parameters)
                //if handling the parameter type correctly, it shouldn't matter if the method is static
                val s_1 = (0 to (i.getNumberOfParameters()-1)).foldLeft(s_copied) {
                  case (s_tmp, j) =>
                    val parType = callee.getParameterType(j)
                    WALA.getTypeType(parType) match {
                      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                        val callerPar = this.topFrame.localLabel(i.getUse(j))
                        val s_promote = s_tmp.promoteOrAlloc(callerPar) //promote to enable parameter passing
                        val calleePar = summary_m.topFrame.localLabel(j+1)
                        val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(callerPar))
                        val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(calleePar))
//                        tmpLabels = calleePar :: tmpLabels
                        s_promote.copy(linear = s_promote.linear.assumeSingle(exp1 == exp2))
                      case _ => s_tmp//only deal with numeric types; reference types should have been reflected via heap analysis
                    }
                }
                
                //strongly update on lhs of the call site with the return value in the summary
                val s_2 = summary_s.readvalue(summary_m.topFrame.retLabel) match {
                  case Some(Term.Constant(CIV(x))) =>
                    s_1.writevalue(localret, Term.Constant(CIV(x)))
                  case Some(Term.Constant(CBV(x))) =>
                    s_1.writevalue(localret, Term.Constant(CBV(x)))
                  case Some(Term.Linear(_)) =>
                    val retType = callee.getReturnType()
                    WALA.getTypeType(retType) match {
                      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                        val callerRVal = this.topFrame.localLabel(i.getReturnValue(0))
                        val s_promote = s_1.promoteOrAlloc(callerRVal)
                        val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(summary_m.topFrame.retLabel))                     
//                        tmpLabels = summary_m.topFrame.retLabel :: tmpLabels
                        s_promote.bindEval(callerRVal, exp) //a strong update on the caller's rhs value
                      case _ => s_1//only deal with numeric types; reference types should have been reflected via heap analysis
                    }
                  case Some(Term.Null()) => s_1;
                  case None => s_1
                  case _ => throw InterpException("case not considered in invoke summary")
                }

                //"parameter passing" for heap dimensions in the summary
                var tmpCount = 0;                           
                val ss_3 = summary_s.dispatch.keys.foldLeft(List(s_2)) {
                  case(list_1, label) =>  
                    val list = if(list_1.size > 8) {
                      ConfigManager.joinPolicy = None
                      AbstractState.join(list_1).toList
                    } else list_1
                    
                    label match {
                      case fhpc: FrameHeapParameterCopy =>
                        var new_list = List[AbstractState]()
                        tmpCount = tmpCount + 1
                        fhpc match {
                          case FrameHeapParameterCopy(FrameHeapParameter(_, pk), _) => 
                            if(EscapeAnalysis.mayBeLive(pk, this.cgnode)) {
                              list.foreach{s_l =>
                                s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                  case None => 
//                                    val calleeHeapPar = fhpc
//                                    if(!tmpLabels.contains(calleeHeapPar))
//                                      tmpLabels = calleeHeapPar :: tmpLabels
                                      
                                  case _ =>
                                    val s_promote = s_l.promoteOrAlloc(this.topFrame.regHeapLabel(pk))
                                    val s1 = s_promote.copyBindRemove(this.topFrame.tmpLabel(tmpCount), this.topFrame.regHeapLabel(pk))
                                
                                    val callerHeapPar = this.topFrame.tmpLabel(tmpCount)
                                    val calleeHeapPar = fhpc
                                
                                    val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeHeapPar))
                                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Linear(callerHeapPar))
                                
//                                    if(!tmpLabels.contains(calleeHeapPar))
//                                      tmpLabels = calleeHeapPar :: tmpLabels
//                                
//                                    if(!tmpLabels.contains(callerHeapPar))
//                                      //also to remove the callerHeapPar because it is temporary (due to the implicit read)
//                                      tmpLabels = callerHeapPar :: tmpLabels     
                                                                    
                                    new_list = s1.copy(linear = s1.linear.assumeSingle(exp1 == exp2)) :: new_list
                                 }
                              }
                            } else { //the heap is not live from the caller
                              list.foreach{s_l =>
                                val calleeHeapPar = fhpc
                                val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(calleeHeapPar))
                                val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
                            
//                                if(!tmpLabels.contains(calleeHeapPar))
//                                  tmpLabels = calleeHeapPar :: tmpLabels
                                                        
                                new_list = s_l.copy(linear = s_l.linear.assumeSingle(exp1 == exp2)) :: new_list
                              }
                            }
                        }                   
                        new_list
                      case _ => list
                    }
                }

                //"side effects" of the summary
                val ss_4 = summary_s.dispatch.keys.foldLeft(ss_3) {
                  case(list_1, label) =>   
                    val list = if(list_1.size > 8) {
                      ConfigManager.joinPolicy = None
                      AbstractState.join(list_1).toList
                    } else list_1
                    label match {
                      case fhr: FrameHeapRegister =>
                        fhr match {
                          case FrameHeapRegister(_, pk) =>                          
                            if(summary_s.hasUpdated(fhr)) {
//                              && EscapeAnalysis.mayBeLive(pk, this.cgnode)
                              summary_s.readvalue(fhr).get match {
                                case Term.Constant(CIV(x)) =>
                                  var new_list = List[AbstractState]()
                                  list.foreach{ s_l =>
                                    
                                    s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                      case None =>
                                        new_list = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CIV(x))) :: new_list
                                      case _ =>
                                        val s1 = s_l.writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CIV(x)))
                  
                                        s_l.joinOrNot(s1) match {
                                          case None => 
                                            new_list = s_l :: new_list                                
                                            new_list = s1 :: new_list
                                          case Some(joined_s: AbstractState) =>
                                            new_list = joined_s :: new_list
                                        }
                                    
                                    }
                                  }
                                  new_list
                                case Term.Constant(CBV(x)) =>
                                  var new_list = List[AbstractState]()
                                  list.foreach{ s_l =>
                                    
                                    s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                      case None =>
                                        new_list = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CBV(x))) :: new_list
                                      case _ =>
                                        val s1 = s_l.writevalue(this.topFrame.regHeapLabel(pk), Term.Constant(CBV(x)))
                  
                                        s_l.joinOrNot(s1) match {
                                          case None => 
                                            new_list = s_l :: new_list                                
                                            new_list = s1 :: new_list
                                          case Some(joined_s: AbstractState) =>
                                            new_list = joined_s :: new_list
                                        }
                                    
                                    }
                                  }
                                  new_list
                                case Term.Linear(_) =>
                                  var new_list = List[AbstractState]()
                                  list.foreach{ s_l =>
                                    s_l.readvalue(this.topFrame.regHeapLabel(pk)) match {
                                      case None =>
                                        val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(fhr))
                                    
//                                        if(!tmpLabels.contains(fhr))
//                                          tmpLabels = fhr :: tmpLabels
                                    
                                        new_list = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).bindEval(this.topFrame.regHeapLabel(pk), exp) :: new_list
                                      case _ =>
                                        val exp:Expression[Term.Closed] = Expression.Term(Term.Linear(fhr))
                                        val s1 = s_l.updateHeaps(this.topFrame.regHeapLabel(pk)).bindEval(this.topFrame.regHeapLabel(pk), exp)
                  
                                        s_l.joinOrNot(s1) match {
                                          case None => 
                                            new_list = s_l :: new_list                                
                                            new_list = s1 :: new_list
                                          case Some(joined_s: AbstractState) =>
                                            new_list = joined_s :: new_list
                                        }
                                    
//                                        if(!tmpLabels.contains(fhr))
//                                          tmpLabels = fhr :: tmpLabels
                                    }
                                  }
                                  new_list
                                }    
                            } else {
//                              if(!tmpLabels.contains(fhr))
//                                tmpLabels = fhr :: tmpLabels
                              list
                            }
                        }                      
                      case _ => list
                    }
                }     
                
                ss_4.foreach { s_4 => 
                  val s_4_filter = s_4.keepLabels ({ l : Label => isLocalOrHeapReachableL(l,this.topFrame.id, List[Label]()) })
                  summarized_states = s_4_filter :: summarized_states
//                  summarized_states = s_4.filter (List[Label](), { l : Label => isLocalOrHeapReachableL(l,this.topFrame.id, List[Label]()) }) :: summarized_states

                }
                                
              }
            }
          }
          AbstractState.join(summarized_states)
        }.joinAllStates

        //TODO: forget the access paths
        if(ConfigManager.inline)
          ret.unbind(Label.isAccessPathL)
        else
          ret.unbind(Label.isAccessPathL)//.filter({l => !isAccessPathL(l)})
      case i => throw InterpException(s"current instruction is not invoke: $i")
    }
  }
  
//  def handleSpecialSigs(sigs: List[String], i:InvokeI): AbstractMachine = 
//  {
//    
//    val frame = this.topFrame
//    
//    this.flatMap { s =>  
//      var list = List[AbstractState]()
//      if(sigs.isEmpty) {
//        WALA.getTypeType(i.getDeclaredResultType()) match {            
//          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
//              list = s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) :: list
//          case _ =>
//            list = s :: list
//        }
//      }
//      sigs.foreach { sig =>
//        if(sig.equals(SpecialLibHandling.STRING_LENGTH) | sig.equals(SpecialLibHandling.BUFFER_LENGTH)  | sig.equals(SpecialLibHandling.LIST_SIZE) | sig.equals(SpecialLibHandling.BIG_INT_LENGTH)) {                    
//          val pts = GlobalHeap.getPointsToSet(cgnode, i.getUse(0))            
//          if(pts.length>0) {                                         
//            pts.foreach { ik =>               
//              val lkey = GlobalHeap.getLengthPK(ik) //array length key                        
//              val pk = GlobalHeap.getPointerKey(cgnode,i.getUse(0)) //pointer key                                                                      
//              s.readvalue(frame.arrayLengthAccessPathLabel(pk)) match {
//                case None =>  //the case access path pk.field does not exist
//                  s.readvalue(frame.regHeapLabel(lkey)) match {
//                    case None => //the case lkey does not exist as a regular heap label 
//                      val s_init = s.initHeap(frame.parHeapLabel(lkey)).copyBind(frame.arrayLengthAccessPathLabel(pk), frame.parHeapLabel(lkey))                        
//                      val exp = Expression.Term(s_init.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                      val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                      list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp) :: list                  
//                    case _ => //the case lkey already exists as a regular heap label
//                      val s1 = s.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
//                      val s_init = s.initHeap(frame.parHeapLabel(lkey))
//                      val s2 = s_init.copyBind(frame.arrayLengthAccessPathLabel(pk), frame.parHeapLabel(lkey))
//                      val exp1 = Expression.Term(s1.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                      val exp2 = Expression.Term(s2.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                      val exp3:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                      list = s1.copy(linear = s1.linear.assumeSingle(exp1 ≥ exp3)).bindEval(frame.localLabel(i.getDef()), exp1) :: list
//                      list = s2.copy(linear = s2.linear.assumeSingle(exp2 ≥ exp3)).bindEval(frame.localLabel(i.getDef()), exp2) :: list
//                  }
//                case _ => //the case array length access path exists
//                  val exp = Expression.Term(s.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                  list = s.bindEval(frame.localLabel(i.getDef()), exp) :: list
//              }
//            }
//          } else {
//            Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
//            list = s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) :: list
//          }                    
//        } else if(sig.equals(SpecialLibHandling.STRING_CHARAT) | sig.equals(SpecialLibHandling.READER_READ) | sig.equals(SpecialLibHandling.BUFFER_INDEXOF)) {
//
//          val pts = GlobalHeap.getPointsToSet(cgnode, i.getUse(0)) //points-to set of the array variable            
//            if(pts.length>0) {                             
//                pts.foreach { ik =>
//                  val akey = GlobalHeap.getArrayKey(ik)//array element keys           
//                  s.readvalue(frame.regHeapLabel(akey)) match {
//                    case None => //the case akey does not exist as a regular heap label         
//                      val s_init = s.initHeap(frame.parHeapLabel(akey))//if the parameter heap label does not exist, alloc the label                                    
//                      list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(akey)) :: list
//                    case _ => //the case akey already exists as a regular heap label
//                      if(s.hasUpdated(frame.regHeapLabel(akey))) {
//                        val s1 = s.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(akey))
//                        val s_init = s.initHeap(frame.parHeapLabel(akey))
//                        val s2 = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(akey))                    
//                        list = s1 :: list
//                        list = s2 :: list        
//                      } else {
//                        val s_init = s.initHeap(frame.parHeapLabel(akey))
//                      list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(akey)) :: list
//                      }
//                  }
//                }
//              } else {
//                Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
//                 list = s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) :: list
//              }                    
//        } else if(sig.equals(SpecialLibHandling.LIST_CONTAINS) | sig.equals(SpecialLibHandling.BIG_INT_TESTBIT)) {
//          val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(this.topFrame.localLabel(i.getDef)))
//          val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
//          val exp3:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(1)))
//
//          val tmp_s = s.alloc(this.topFrame.localLabel(i.getDef))
//
//          list = tmp_s.copy(linear = tmp_s.linear.assumeSingle(exp1 ≥ exp2)).copy(linear = tmp_s.linear.assumeSingle(exp1 ≤ exp3)) :: list
//        }
//        else {
//          WALA.getTypeType(i.getDeclaredResultType()) match {            
//              case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
//                  list = s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) :: list
//              case _ =>
//                list = s :: list
//            }
//        }
//        
//      }
//      list
//    }.joinAllStates()
//    
//  }

  /*
   * pretty printing
   */
  override def toString = {
    s"Machine($name) (imprecision factor = " + countMachines.toString() + ") " +
      (if (hasTopReturned) "returned" else "not returned") +
      (if (isBottom) " IS BOTTOM" else "") + " with:\n" +
      tab(state.toString) + "\n" +
      tab(topFrame.toString)
  }

  val complexity = 1//state.states.size
  val numConstraints = state.numConstraints
  val numLabels = state.numLabels

  def stepAnnotation(o: Order.t)(implicit i: WALA.IIndex = this.topFrame.pc.index): Machine = {
    val f = this.topFrame
    f.annots.get((i, o)) match {
      case Some(annots) =>
        annots.foldLeft(this) { (a : Machine, i : Annotation) =>
          i match {
            case Assume(e : Exp.Open) => a.assume(this.topFrame.evalDeferred(e))
            case Assert(e : Exp.Open) => a.assert(this.topFrame.evalDeferred(e))
          }
        }
      case None => this
    }
  }

  def stepPhis
    (context: InterpContext, priorn: WALA.CFGNode)
    (implicit cleanPhis: Boolean = true)
      : AbstractMachine = {
    
    val ir: WALA.IR = topFrame.pc.method
    val n = this.blockTop
    val phis = n.iteratePhis().toList

    
    if (0 == phis.length) {
      return this
    }
    val f = this.topFrame
    val preds = this.cfg.getPredNodes(n).toList
    val prednum = preds.indexOf(priorn)

    val final_m = phis.foldLeft(this){
      case (m, phi:com.ibm.wala.ssa.SSAInstruction) =>
        
        val t = typeInference.getType(phi.getDef).getTypeReference
        
        WALA.getTypeType(t) match {
        
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
        
            ifDebug{
              Log.println(tab(s"processing $phi"))
            }

            if (prednum < 0) {
              throw InterpException(s"could not find predecessor $priorn in list of predecessors: $preds")
            }
            
            val args = argsOfI(phi)
            val original_vid = args(prednum)
            val original_label = f.localLabel(original_vid)
            val def_label = f.localLabel(phi.getDef())
            
            val m2 = m.mapState{case s =>
              s.writevalue(
                def_label,
                s.readvalueOrFail(original_label)
              )}
            
            val out_m = m2
            ifDebug{Log.println(tab("out machine:\n" + tab(out_m.toString)))}

            out_m
          case _ => m
        }
    }
    ifDebug{Log.println(tab("machine after stepPhi:\n" + tab(final_m.toString)))}
    
    final_m.copy(state = final_m.state.joinAllStates())
  }

  def simpleMachine: Machine = this

  def evalConcrete(exp: Exp.Closed): Value = {
    exp match {
      case Expression.Term(Term.Constant(x)) => x
      case Expression.Binop(v1, r: Operator.NumericBinop, v2) =>
        val op = IValue.getExpArithmeticImp(r)
        op(evalConcrete(v1).asInstanceOf[IValue])(evalConcrete(v2).asInstanceOf[IValue])
      case Expression.Binop(v1, r: Operator.NumericRelation, v2) =>
        val op = IValue.getExpComparisonImp(r)
        op(evalConcrete(v1).asInstanceOf[IValue])(evalConcrete(v2).asInstanceOf[IValue])
      case _ => throw NotImplemented("other concrete expression eval cases")
    }
  }

  def promote(l: Label): AbstractMachine = {
    this.mapState{case s => s.promoteOrAlloc(l)}
  }

  def toJSON() : String = {
    val o = JsObject(Seq(
      "name" -> JsString(this.ir.getMethod().getSignature())
    ))

    o.toString()
  }
}

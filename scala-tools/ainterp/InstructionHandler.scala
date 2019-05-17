import edu.illinois.wala.Facade._

import Util._
import PPL._
import Core._
import AbstractMachine._
import Config._
import Exp._
import Label._

import com.ibm.wala.classLoader.ProgramCounter
import com.ibm.wala.ssa.SSAThrowInstruction
import com.ibm.wala.ssa.SSACheckCastInstruction
import com.ibm.wala.ssa.SSAInstanceofInstruction
import com.ibm.wala.ssa.SSALoadMetadataInstruction
import com.ibm.wala.ssa.SSAComparisonInstruction
import com.ibm.wala.ssa.SSAUnaryOpInstruction
import com.ibm.wala.ssa.SSAMonitorInstruction
import com.ibm.wala.ssa.SSASwitchInstruction

import collection.JavaConversions._

import scalaz._
import Scalaz._

import java.io._

object InstructionHandler {
  
  def stepInvokeI
    (i: InvokeI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = { 
    val frame = m.topFrame //top frame of the machine
    val cfg = m.ir.getControlFlowGraph() //control flow graph
    
    val new_m =
      if(ConfigManager.isInterProc) {    
        
          if(!ConfigManager.isTopDown) {//bottom-up inter-procedural analysis        
            stepInovkeBU(i, m)
          } else {//top-down inter-procedural analysis
            stepInovkeTD(i, m)
          }               
      } else { //intra-procedural analysis: ignore all the invocations   
        stepInovkeIntra(i, m)
      }
    
    //the next instruction after the invocation, possibly following an exceptional edge
    val successors = cfg.getSuccNodes(frame.block).toList
    var list = List[Machine]()
    successors.foreach { bb => 
      val index = bb.getFirstInstructionIndex
      if(index != -1) {
        list = new_m.gotoTop(index) :: list
      }
    }
    (None, list)
  }
  
  def stepInovkeTD(i: InvokeI, m: Machine)
    (implicit conf: AInterpConfig)
      : Machine = {
    
    val frame = m.topFrame //top frame of the machine    
    
    val targets = GlobalHeap.cg.getPossibleTargets(m.cgnode, i.getCallSite()).toList //get the callee nodes from the call graph
    
    if(!targets.isEmpty) {
      var list = List[Machine]()
      targets.foreach { target =>  
        if(ConfigManager.isHybrid) { //hybrid TD-BU
          val loader = target.getMethod.getDeclaringClass.getClassLoader.getName.toString()
          if(!loader.equals("Application")) {
            Hybrid.map.get(target) match {
              case Some(summary_m:Machine) =>
                val summary_ms = List(summary_m)
                try {
                  val m_1 = m.invokeViaSummaryHybrid(summary_ms)
                  list = m_1 :: list
                } catch {
                  case _: Throwable =>
                    WALA.getTypeType(i.getDeclaredResultType()) match {            
                      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                        list = m.mapState { s =>
                          s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
                        } :: list
                      case _ =>
                        list = m :: list
                    }
                    
                }
              case _ =>
                WALA.getTypeType(i.getDeclaredResultType()) match {            
                  case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
                    list = m.mapState { s =>
                      s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
                    } :: list
                  case _ =>
                    list = m :: list
                }
            }
          } else {
            val m_1 = m.invokeViaStack(i, target)(conf)
            list = m_1 :: list
            
          }
        } else { //TD analysis
          list = m.invokeViaStack(i, target)(conf) :: list
        }
      }

      val ret = AbstractMachine.join(list)
            
      ret
    } else {
      WALA.getTypeType(i.getDeclaredResultType()) match {            
        case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
          val newm = m.mapState { s =>
            s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
          }
          newm.copy(state = newm.state.joinAllStates())
        case _ =>
          m
      }
    }                   
  }
  
  def stepInovkeBU(i: InvokeI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Machine) = {
    
    val frame = m.topFrame //top frame of the machine
    
    val targets = GlobalHeap.cg.getPossibleTargets(m.cgnode, i.getCallSite()).toList //get the callee nodes from the call graph

    //construct summary_ms that contains all the summaries of the callees
    val summary_ms = targets.foldLeft((List[Machine]())){
      case(l, target) =>
        val map =
          if(ConfigManager.isHybrid)
            Hybrid.map
          else
            InterProc.map
        map.get(target) match {
          case Some(summary_m:Machine) =>
            summary_m :: l
          case _ =>
            l
        }
    }
  
    if(!summary_ms.isEmpty)           
      m.invokeViaSummary(summary_ms)
    else {
//      if(ConfigManager.useLibraryModel) {
//        val sigs = targets.foldLeft((List[String]())){
//          case(l, target) =>
//            target.getMethod.getSignature :: l
//        }
//        m.handleSpecialSigs(sigs, i)
//      } else {
        WALA.getTypeType(i.getDeclaredResultType()) match {            
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            val newm = m.mapState { s =>
              s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
            }
            newm.copy(state = newm.state.joinAllStates())
          case _ =>
            m
        }
//      }
    }
  }
  
  def stepInovkeIntra(i: InvokeI, m: Machine)
    (implicit conf: AInterpConfig)
      : Machine = {
    val frame = m.topFrame //top frame of the machine
    val cfg = m.ir.getControlFlowGraph() //control flow graph
    WALA.getTypeType(i.getDeclaredResultType()) match {            
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
        val newm = m.mapState { s =>
          s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
        }
        newm.copy(state = newm.state.joinAllStates())
      case _ => 
        m
    }
  }
  
  def stepArrayLengthI
    (i: ArrayLengthI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = { 
    val frame = m.topFrame //top frame of the machine
    
    if(!ConfigManager.hasAccessPath && !ConfigManager.hasSummarizedDimensions) {
      //TODO: implement stand-alone forget function to unconstraint a dimension (currently part of alloc)
      return (Some(m.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))), List())
    }
    
    if(!ConfigManager.isTopDown) { //a bottom-up analysis
      stepArrayLengthIBU(i, m)
    } else { //a top-down analysis
      stepArrayLengthITD(i, m)
    }
  }
  
  def stepArrayLengthITD
    (i: ArrayLengthI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = { 
    val frame = m.topFrame //top frame of the machine
    val pts = GlobalHeap.getPointsToSet(m.cgnode, i)     
    val newm = if(pts.length>0) {                           
      val ret  = m.flatMap { s =>
        var list = List[AbstractState]() 
        pts.foreach { ik =>              
          val lkey = GlobalHeap.getLengthPK(ik) //array length key                        
          val pk = GlobalHeap.getPointerKey(m.cgnode,i.getArrayRef()) //pointer key                                                                      
          s.readvalue(frame.arrayLengthAccessPathLabel(pk)) match {
            case None =>  //the case access path pk.field does not exist
              assert(!ConfigManager.hasAccessPath && ConfigManager.hasSummarizedDimensions)
              s.readvalue(frame.regHeapLabel(lkey)) match {
                case None => //the case lkey does not exist as a regular heap label
                  throw InterpException("TD analysis: both summarized objects and access paths do not exist")
//                  val s_init0 =
//                    if(ConfigManager.hasSummarizedDimensions) {
//                      s.alloc(frame.regHeapLabel(lkey))
//                    } else {
//                      s
//                    }
//                    
//                  val s_init = 
//                    if(ConfigManager.hasAccessPath) {
//                      if(ConfigManager.hasSummarizedDimensions) {
//                        s_init0.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
//                      } else {
//                        s.alloc(frame.arrayLengthAccessPathLabel(pk))
//                      }
//                    } else {
//                      s_init0
//                    }
//                  
//                  if(ConfigManager.hasAccessPath) {
//                    val exp = Expression.Term(s_init.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp) :: list
//                  } else {
//                    val exp = Expression.Term(s_init.readvalue(frame.regHeapLabel(lkey)).get)
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(lkey)) :: list
//                  }
                case _ => //the case lkey already exists as a regular heap label
                    val s_1 =
                      if(!AInterp.initialized_lengths.contains(frame.regHeapLabel(lkey))) {
                        s.unbind(frame.regHeapLabel(lkey))
                      } else {
                        s
                      }
                    val exp1 = Expression.Term(s.readvalue(frame.regHeapLabel(lkey)).get)
                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always > 0
                    list = s_1.copy(linear = s_1.linear.assumeSingle(exp1 ≥ exp2)).copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(lkey)) :: list

//                  if(ConfigManager.hasAccessPath) {
//                    val s1 = s.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
//                    val exp1 = Expression.Term(s1.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s1.copy(linear = s1.linear.assumeSingle(exp1 ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp1) :: list
//                  } else {
//                    val s1 = s
//                    val exp1 = Expression.Term(s1.readvalue(frame.regHeapLabel(lkey)).get)
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s1.copy(linear = s1.linear.assumeSingle(exp1 ≥ exp2)).copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(lkey)) :: list
//                  }
              }
            case _ => //the case array length access path exists
              assert(ConfigManager.hasAccessPath)
              if(s.linear.constraints(frame.arrayLengthAccessPathLabel(pk))) {
                val exp = Expression.Term(s.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
                list = s.bindEval(frame.localLabel(i.getDef()), exp) :: list
              } else {
                s.readvalue(frame.regHeapLabel(lkey)) match {
                  
                  case None => //the case lkey does not exist as a regular heap label
                    assert(!ConfigManager.hasSummarizedDimensions)
//                    val s_init0 =
//                      if(ConfigManager.hasSummarizedDimensions) {
//                        s.alloc(frame.regHeapLabel(lkey))
//                      } else {
//                        s
//                      }
//                      
//                    val s_init = 
//                      if(ConfigManager.hasSummarizedDimensions) {
//                        s_init0.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
//                      } else {
//                        s
//                      }
                    
                    val exp = Expression.Term(s.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
                    list = s.copy(linear = s.linear.assumeSingle(exp ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp) :: list
                  case _ => //the case lkey already exists as a regular heap label
                    val s_1 =
                      if(!AInterp.initialized_lengths.contains(frame.regHeapLabel(lkey))) {
                        s.unbind(frame.regHeapLabel(lkey))
                      } else {
                        s
                      }
                    val s1 = s_1.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
                    val exp1 = Expression.Term(s1.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
                    list = s1.copy(linear = s1.linear.assumeSingle(exp1 ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp1) :: list
                }
              }
              
          }
        }

        AbstractState.join(list)
      }
      ret
    } else {
      ifVerbose {
        Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
      }
      m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
    }
    (Some(newm.copy(state = newm.state.joinAllStates())), List())
    
  }
  
  def stepArrayLengthIBU
    (i: ArrayLengthI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = { 
    val frame = m.topFrame //top frame of the machine
    val pts = GlobalHeap.getPointsToSet(m.cgnode, i)            
    val newm = if(pts.length>0) {  
      val ret  = m.flatMap { s =>
        var list = List[AbstractState]() 
        pts.foreach { ik =>               
          val lkey = GlobalHeap.getLengthPK(ik) //array length key                        
          val pk = GlobalHeap.getPointerKey(m.cgnode,i.getArrayRef()) //pointer key  
          val aplabel = frame.arrayLengthAccessPathLabel(pk)
          s.readvalue(aplabel) match {
            case None =>  //the case access path pk.field does not exist
              assert(!ConfigManager.hasAccessPath && ConfigManager.hasSummarizedDimensions)
              s.readvalue(frame.regHeapLabel(lkey)) match {
                case None => //the case lkey does not exist as a regular heap label 
                  val s_init = s.initHeap(frame.parHeapLabel(lkey))
                  val exp = Expression.Term(s_init.readvalue(frame.parHeapLabel(lkey)).get)
                  val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always > 0
                  list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(lkey))  :: list
//                  val s_init0 = 
//                    if(ConfigManager.hasSummarizedDimensions) {
//                      s.initHeap(frame.parHeapLabel(lkey))
//                    } else {
//                      s
//                    }
//                  val s_init = 
//                    if(ConfigManager.hasAccessPath) {
//                      if(ConfigManager.hasSummarizedDimensions) {
//                        s_init0.copyBind(frame.arrayLengthAccessPathLabel(pk), frame.parHeapLabel(lkey))    
//                      } else {
//                        s_init0.alloc(frame.arrayLengthAccessPathLabel(pk))
//                      }
//                    } else {
//                      s_init0
//                    }
//                  if(ConfigManager.hasAccessPath) {
//                    val exp = Expression.Term(s_init.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp) :: list   
//                  } else {
//                    val exp = Expression.Term(s_init.readvalue(frame.parHeapLabel(lkey)).get)
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(lkey))  :: list
//                  }
                case _ => //the case lkey already exists as a regular heap label
                  val s_init = s.initHeap(frame.parHeapLabel(lkey))
                  val exp1 = Expression.Term(s.readvalue(frame.regHeapLabel(lkey)).get)
                  val exp2 = Expression.Term(s_init.readvalue(frame.parHeapLabel(lkey)).get)
                  val exp3:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always > 0
                  val s2 = s_init.copy(linear = s_init.linear.assumeSingle(exp2 ≥ exp3)).copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(lkey))
                  list = s2.copy(linear = s.linear.assumeSingle(exp1 ≥ exp3)).copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(lkey)) :: list
                  list = s2 :: list

//                  if(ConfigManager.hasAccessPath) {
//                    val s_init = s.initHeap(frame.parHeapLabel(lkey))
//                    val s2 = s_init.copyBind(frame.arrayLengthAccessPathLabel(pk), frame.parHeapLabel(lkey))
//                    val s1 = s2.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
//                    
//                    val exp1 = Expression.Term(s1.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                    val exp2 = Expression.Term(s2.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
//                    val exp3:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    list = s1.copy(linear = s1.linear.assumeSingle(exp1 ≥ exp3)).bindEval(frame.localLabel(i.getDef()), exp1) :: list
//                    list = s2.copy(linear = s2.linear.assumeSingle(exp2 ≥ exp3)).bindEval(frame.localLabel(i.getDef()), exp2) :: list
//                  } else {
//                    val s_init = s.initHeap(frame.parHeapLabel(lkey))
//                    val exp1 = Expression.Term(s.readvalue(frame.regHeapLabel(lkey)).get)
//                    val exp2 = Expression.Term(s_init.readvalue(frame.parHeapLabel(lkey)).get)
//                    val exp3:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always >=0
//                    val s2 = s_init.copy(linear = s_init.linear.assumeSingle(exp2 ≥ exp3)).copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(lkey))
//                    list = s2.copy(linear = s.linear.assumeSingle(exp1 ≥ exp3)).copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(lkey)) :: list
//                    list = s2 :: list
//                  }
              }
            case _ => //the case array length access path exists
              assert(ConfigManager.hasAccessPath == true)
              if(s.linear.constraints(aplabel)) {
                val exp = Expression.Term(s.readvalue(aplabel).get)
                list = s.bindEval(frame.localLabel(i.getDef()), exp) :: list
              } else {
                s.readvalue(frame.regHeapLabel(lkey)) match {
                  case None => //the case lkey does not exist as a regular heap label 
                    val s_init0 = 
                      if(ConfigManager.hasSummarizedDimensions) {
                        s.initHeap(frame.parHeapLabel(lkey))
                      } else {
                        s
                      }
                    val s_init = 
                      if(ConfigManager.hasSummarizedDimensions) {
                        s_init0.copyBind(frame.arrayLengthAccessPathLabel(pk), frame.parHeapLabel(lkey))    
                      } else {
                        s_init0
                      }

                    val exp = Expression.Term(s_init.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always > 0
                    list = s_init.copy(linear = s_init.linear.assumeSingle(exp ≥ exp2)).bindEval(frame.localLabel(i.getDef()), exp) :: list   
                  case _ => //the case lkey already exists as a regular heap label
                    val s_init = s.initHeap(frame.parHeapLabel(lkey))
                    val s1 = s_init.copyBindRemove(frame.arrayLengthAccessPathLabel(pk), frame.regHeapLabel(lkey))
                    val s2 = s_init.copyBind(frame.arrayLengthAccessPathLabel(pk), frame.parHeapLabel(lkey))                    
                    
                    val exp1 = Expression.Term(s1.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
                    val exp2 = Expression.Term(s2.readvalue(frame.arrayLengthAccessPathLabel(pk)).get)
                    val exp3:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0))) //array length should always > 0
                    list = s1.copy(linear = s1.linear.assumeSingle(exp1 ≥ exp3)).bindEval(frame.localLabel(i.getDef()), exp1) :: list
                    list = s2.copy(linear = s2.linear.assumeSingle(exp2 ≥ exp3)).bindEval(frame.localLabel(i.getDef()), exp2) :: list
                }
              }
          }
        }

        val new_list = list//AbstractState.alignManyX(list)
        AbstractState.join(new_list)
      }
      ret
    } else {
      ifDebug {
        Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
      }
      m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
    }
    (Some(newm.copy(state = newm.state.joinAllStates())), List())
  }
  
  //TODO: the initializations of dimensions (i.e., summarized objects and access paths) need further refactoring
  def stepNewI
    (i: NewI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    (WALA.getTypeType(i.getConcreteType), i.getNumberOfUses) match {
      case (_, 1) => 
        //TODO: implement weak updates using fold operation instead of join
        val m_weak_abstract = //(1) weak updates on abstract objects
          if(ConfigManager.hasSummarizedDimensions) {
            val pts = GlobalHeap.getPointsToSet(m.cgnode, i.getDef)
             
              if(pts.length > 0) {
                m.flatMap { s =>                
                  val (s1, s2) = pts.foldLeft(s, s){
                    case((s_1, s_2), ik) =>
                      val lkey = GlobalHeap.getLengthPK(ik)
                      val akey = GlobalHeap.getArrayKey(ik)
                      s_2.readvalue(frame.localLabel(i.getUse(0))) match {
                        case None => (s_1, s_2)
                        case Some(v) => 
                          val exp1 = Expression.Term(v)
                      
                      val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
                      (s_1.readvalue(frame.regHeapLabel(lkey)), s_1.readvalue(frame.regHeapLabel(akey))) match {
                        case (None, None) =>
                          if(ConfigManager.isTopDown) {
                            if(ConfigManager.isInterProc) {
                              (s_1.bindEval(frame.regHeapLabel(lkey), exp1).bindEval(frame.regHeapLabel(akey), exp2), s_2.bindEval(frame.regHeapLabel(lkey), exp1).bindEval(frame.regHeapLabel(akey), exp2))
                            } else {
                              (s_1.alloc(frame.regHeapLabel(lkey)), s_2.bindEval(frame.regHeapLabel(lkey), exp1).bindEval(frame.regHeapLabel(akey), exp2))
                            }
                          } else { //bottom-up analysis
                            (s_1.updateHeaps(frame.regHeapLabel(lkey)).bindEval(frame.regHeapLabel(lkey), exp1).updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp2), s_2.updateHeaps(frame.regHeapLabel(lkey)).bindEval(frame.regHeapLabel(lkey), exp1).updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp2))
                          }
                        case (_, _) => 
                          if(ConfigManager.isTopDown) {
                            if(!AInterp.initialized_lengths.contains(frame.regHeapLabel(lkey))) {
                              AInterp.initialized_lengths = frame.regHeapLabel(lkey) :: AInterp.initialized_lengths
                              (s_1.bindEval(frame.regHeapLabel(lkey), exp1), s_2.bindEval(frame.regHeapLabel(lkey), exp1).bindEval(frame.regHeapLabel(akey), exp2))
                            } else
                              (s_1, s_2.bindEval(frame.regHeapLabel(lkey), exp1).bindEval(frame.regHeapLabel(akey), exp2))
                          } else { //bottom-up analysis
                            if(s_1.hasUpdated(frame.regHeapLabel(lkey)) && s_1.hasUpdated(frame.regHeapLabel(akey))) {
                              (s_1, s_2.bindEval(frame.regHeapLabel(lkey), exp1).bindEval(frame.regHeapLabel(akey), exp2))
                            } else {
                              (s_1.updateHeaps(frame.regHeapLabel(lkey)).bindEval(frame.regHeapLabel(lkey), exp1).updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp2), s_2.updateHeaps(frame.regHeapLabel(lkey)).bindEval(frame.regHeapLabel(lkey), exp1).updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp2))
                            }
                          }
                      }                 
                  }
                  }
                  s1.joinOrNot(s2) match {
                    case None =>
                      List(s1, s2)
                    case Some(joined_s: AbstractState) =>
                      List(joined_s)
                  }
                }
                
              } else {
                m
              }
          } else {
            m
          }
        val m_strong_ap = 
          if(ConfigManager.hasAccessPath) {
            val pk = GlobalHeap.getPointerKey(m.cgnode,i.getDef)     
             m_weak_abstract.mapState { s => //(2) strong updates via access path
               s.readvalue(frame.localLabel(i.getUse(0))) match {
                 case None => s
                 case Some(v) =>
                   val exp = Expression.Term(v)
                   s.bindEval(frame.arrayLengthAccessPathLabel(pk), exp)
               }
              
            }
          } else {
            m_weak_abstract
          }
        (Some(m_strong_ap.copy(state = m_strong_ap.state.joinAllStates())), List())
      case (_, _) => (Some(m), List[Machine]())
    }
    
  }
  
  def stepArrayStoreI
    (i: ArrayStoreI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    if(ConfigManager.checkArrayBound) {
      Clients.checkArrayBound(i, m)
    }
    val frame = m.topFrame //top frame of the machine
    WALA.getTypeType(i.getElementType) match {
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>        
        if(ConfigManager.hasSummarizedDimensions) {
          val akeys = GlobalHeap.getArrayKeys(m.cgnode, i) //the array element keys
          if(akeys.length>0) {
              val newm = m.flatMap { s =>
                //TODO: replace with new weak update implementation
                val (s1, s2) = akeys.foldLeft(s, s){
                  case((s_1, s_2), akey) =>
                    val exp = Expression.Term(s_2.readvalue(frame.localLabel(i.getValue())).get)
                    s_1.readvalue(frame.regHeapLabel(akey)) match {
                      case None =>
                        if(ConfigManager.isTopDown) {
                          if(ConfigManager.isInterProc) {
                            val exp1:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
                            (s_1.bindEval(frame.regHeapLabel(akey), exp1), s_2.bindEval(frame.regHeapLabel(akey), exp))
                          } else {
                            (s_1.alloc(frame.regHeapLabel(akey)), s_2.bindEval(frame.regHeapLabel(akey), exp))
                          }
                        } else {
                          (s_1.updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp), s_2.updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp))
                        }
                      case _ => 
                        if(ConfigManager.isTopDown) {
                          (s_1, s_2.bindEval(frame.regHeapLabel(akey), exp))
                        } else { //bottom-up analysis
                          if(s_1.hasUpdated(frame.regHeapLabel(akey))) {
                            (s_1, s_2.bindEval(frame.regHeapLabel(akey), exp))
                          } else {
                            (s_1.updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp), s_2.updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp))
                          }
                        }
                    }                 
                }
                val s3 = s1.joinOrNot(s2) match {
                  case None => List(s1, s2)
                  case Some(joined_s: AbstractState) =>
                    List(joined_s)
                }
                s3
            }  
//            val newm = m.mapState { s =>
//                akeys.foldLeft(s){
//                  case(s_1, akey) =>
//                    var from = List(frame.localLabel(i.getValue()))
//                    val exp = Expression.Term(s_1.readvalue(frame.localLabel(i.getValue())).get)
//                    s_1.readvalue(frame.regHeapLabel(akey)) match {
//                      case None =>
//                        if(ConfigManager.isTopDown) {
//                          if(ConfigManager.isInterProc) {
//                            s_1.weakUpdate(frame.regHeapLabel(akey), from)
//                          } else {
//                            s_1.weakUpdate(frame.regHeapLabel(akey), from)
//                          }
//                        } else {
//                          s_1.updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp)
//                        }
//                      case _ => 
//                        if(ConfigManager.isTopDown) {
//                          s_1.weakUpdate(frame.regHeapLabel(akey), from)
//                        } else { //bottom-up analysis
//                          if(s_1.hasUpdated(frame.regHeapLabel(akey))) {
//                            s_1.weakUpdate(frame.regHeapLabel(akey), from)
//                          } else {
//                            s_1.updateHeaps(frame.regHeapLabel(akey)).bindEval(frame.regHeapLabel(akey), exp)
//                          }
//                        }
//                    }                 
//                }
//            }      
            (Some(newm.copy(state = newm.state.joinAllStates())), List())
          } else {
            ifVerbose{
              Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
            }
            (Some(m), List())
          }
        } else {
          (Some(m), List())
        }
      case _ => (Some(m), List()) //store an array element whose type is not numeric
    }
  }
  
  def stepArrayLoadI
    (i: ArrayLoadI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    if(ConfigManager.checkArrayBound) {
      Clients.checkArrayBound(i, m)
    }
    val frame = m.topFrame //top frame of the machine
    if(!ConfigManager.hasSummarizedDimensions) {
      WALA.getTypeType(i.getElementType) match {  
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                             
            return (Some(m.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))), List())
          case _ =>
            return (Some(m), List())
      }
    }
    if(!ConfigManager.isTopDown) { //bottom-up analysis
      stepArrayLoadIBU(i, m)
    } else { //top-down analysis
      stepArrayLoadITD(i, m)
    }
  }
  
  def stepArrayLoadITD
    (i: ArrayLoadI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    WALA.getTypeType(i.getElementType) match {
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                           
        
        val pts = GlobalHeap.getPointsToSet(m.cgnode, i) //points-to set of the array variable         
        val newm = if(pts.length>0) {                                        
            val ret  = m.flatMap { s =>
              var list = List[AbstractState]()                 
              
              pts.foreach { ik =>
                val akey = GlobalHeap.getArrayKey(ik)           
                s.readvalue(frame.regHeapLabel(akey)) match {
                  case None => //the case akey does not exist as a regular heap label 
                    throw InterpException("TD analysis: summarized objects do not exist")
//                    val s_init = s.initHeap(frame.regHeapLabel(akey))                                    
//                    list = s_init.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(akey)) :: list
                  case _ => //the case akey already exists as a regular heap label
                    val s1 = s.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(akey))
                    list = s1 :: list        
                }
              }
              AbstractState.join(list)
            }                           
            ret
        } else {
          ifDebug {
            Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
          }
          m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
        }
        (Some(newm.copy(state = newm.state.joinAllStates())), List())            
      case _ => 
        (Some(m), List()) //load an array element whose type is not numeric
    }   
  }
  
  def stepArrayLoadIBU
    (i: ArrayLoadI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    assert(ConfigManager.hasSummarizedDimensions)
    val frame = m.topFrame //top frame of the machine
    WALA.getTypeType(i.getElementType) match {
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                   
        val pts = GlobalHeap.getPointsToSet(m.cgnode, i) //points-to set of the array variable            
        val newm =
          if(pts.length>0) {                             
            val ret  = m.flatMap { s =>
              var list = List[AbstractState]()                    
              pts.foreach { ik =>
                val akey = GlobalHeap.getArrayKey(ik)//array element keys           
                s.readvalue(frame.regHeapLabel(akey)) match {
                  case None => //the case akey does not exist as a regular heap label         
                    val s_init = s.initHeap(frame.parHeapLabel(akey))//if the parameter heap label does not exist, alloc the label                                    
                    list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(akey)) :: list
                  case _ => //the case akey already exists as a regular heap label
                    if(s.hasUpdated(frame.regHeapLabel(akey))) {
                      val s_init = s.initHeap(frame.parHeapLabel(akey))
                      val s2 = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(akey)) 
                      val s1 = s_init.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(akey))
                                                                    
                      list = s1 :: list
                      list = s2 :: list        
                    } else {
                      val s_init = s.initHeap(frame.parHeapLabel(akey))
                      list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(akey)) :: list
                    }
                }
              }

              val new_list = list//AbstractState.alignManyX(list)
              AbstractState.join(new_list)
            }  
            ret
        } else {
          ifVerbose {
            Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
          }
          m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
        }
        (Some(newm.copy(state = newm.state.joinAllStates())), List())        
      case _ =>
        (Some(m), List()) //load an array element whose type is not numeric
    }
  }
  
  def stepBranchI
    (i: BranchI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    WALA.getTypeType(i.getType) match {
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>            
        val op = WALAUtil.binopOfBranch(i) //the numeric relational binary operator

        val exp = Expression.Binop(
          Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(0)))),
          op,
          Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(1))))
        )

        val mtrue = m.flatMap{s =>
          val cexp = exp.close(s.dispatch)
          s.assume(cexp)
        }.joinAllStates().gotoTop(i.getTarget())

        val mfalse = m.flatMap{s =>
          val cexp = exp.close(s.dispatch)
          s.assume(!cexp)
        }.joinAllStates().nextTop

        (None, List(mtrue,mfalse).filter(_.isFeasible))
      case _ => (None, List(m.gotoTop(i.getTarget()),m.nextTop).filter(_.isFeasible))
    }
  }
  
  def stepReturnI
    (i: ReturnI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val ret_m: Machine = m.mapState{s =>
      val retType = m.cgnode.getMethod.getReturnType()
      WALA.getTypeType(retType) match {
        case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
          if(i.getNumberOfUses() == 1) {
            s.bind(frame.retLabel, s.readvalueOrFail(frame.localLabel(i.getUse(0))))
          } else { //TODO: in what circumstances, the following case happens?
            s.bind(frame.retLabel, Term.Null())
          }
        case _ => s.bind(frame.retLabel, Term.Null()) //the return type being non-numeric
      }
    }.doReturn
    (None, List(ret_m))
  }
  
  def stepPutI
    (i: PutI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val field = GlobalHeap.getField(i.getDeclaredField()) //field reference    
    WALA.getTypeType(field.getFieldTypeReference) match {
      case WALA.TypeType.TypeTypeIntegral| WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
      if(!i.isStatic) { //non-static field write       
        val fkeys = GlobalHeap.getFieldPKs(m.cgnode, i) //field keys     
        
        //TODO: replace with new weak update implementation
        val m_weak_abstract = //(1) weak updates on abstract objects
          if(ConfigManager.hasSummarizedDimensions) {
            if(fkeys.length > 0) {
              m.flatMap { s =>                
                val (s1, s2) = fkeys.foldLeft(s, s){
                  case((s_1, s_2), fkey) =>
                    val exp = Expression.Term(s_2.readvalue(frame.localLabel(i.getVal())).get)
                    s_1.readvalue(frame.regHeapLabel(fkey)) match {
                      case None =>
                        if(ConfigManager.isTopDown) {
                          if(ConfigManager.isInterProc) {
                            val exp1:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
                            (s_1.bindEval(frame.regHeapLabel(fkey), exp1), s_2.bindEval(frame.regHeapLabel(fkey), exp))
                          } else {
                            (s_1.alloc(frame.regHeapLabel(fkey)), s_2.bindEval(frame.regHeapLabel(fkey), exp))
                          }
                        } else { //bottom-up analysis
                          (s_1.updateHeaps(frame.regHeapLabel(fkey)).bindEval(frame.regHeapLabel(fkey), exp), s_2.updateHeaps(frame.regHeapLabel(fkey)).bindEval(frame.regHeapLabel(fkey), exp))
                        }
                      case _ => 
                        if(ConfigManager.isTopDown) {
                          (s_1, s_2.bindEval(frame.regHeapLabel(fkey), exp))
                        } else { //bottom-up analysis
                          if(s_1.hasUpdated(frame.regHeapLabel(fkey))) {
                            (s_1, s_2.bindEval(frame.regHeapLabel(fkey), exp))
                          } else {
                            (s_1.updateHeaps(frame.regHeapLabel(fkey)).bindEval(frame.regHeapLabel(fkey), exp), s_2.updateHeaps(frame.regHeapLabel(fkey)).bindEval(frame.regHeapLabel(fkey), exp))
                          }
                        }
                    }                 
                }
                s1.joinOrNot(s2) match {
                  case None =>
                    List(s1, s2)
                  case Some(joined_s: AbstractState) =>
                    List(joined_s)
                }
              }  
//              m.mapState { s =>                
//                fkeys.foldLeft(s){
//                  case(s_1, fkey) =>
//                    val from = List(frame.localLabel(i.getVal()))
//                    val exp = Expression.Term(s_1.readvalue(frame.localLabel(i.getVal())).get)
//                    s_1.readvalue(frame.regHeapLabel(fkey)) match {
//                      case None =>
//                        if(ConfigManager.isTopDown) {
//                          if(ConfigManager.isInterProc) {
//                            s_1.weakUpdate(frame.regHeapLabel(fkey), from)
//                          } else {
//                            s_1.weakUpdate(frame.regHeapLabel(fkey), from)
//                          }
//                        } else { //bottom-up analysis
//                          s_1.updateHeaps(frame.regHeapLabel(fkey)).bindEval(frame.regHeapLabel(fkey), exp)
//                        }
//                      case _ => 
//                        if(ConfigManager.isTopDown) {
//                          s_1.weakUpdate(frame.regHeapLabel(fkey), from)
//                        } else { //bottom-up analysis
//                          if(s_1.hasUpdated(frame.regHeapLabel(fkey))) {
//                            s_1.weakUpdate(frame.regHeapLabel(fkey), from)
//                          } else {
//                            s_1.updateHeaps(frame.regHeapLabel(fkey)).bindEval(frame.regHeapLabel(fkey), exp)
//                          }
//                        }
//                    }                 
//                }
//              }
            } else {
              m
            }
          } else {
            m
          }

        if(ConfigManager.hasAccessPath) {    
          val pk = GlobalHeap.getPointerKey(m.cgnode,i.getRef())        
          val m_strong_ap = 
            m_weak_abstract.mapState { s => //(2) strong updates via access path
            val exp = Expression.Term(s.readvalue(frame.localLabel(i.getVal())).get)
            s.bindEval(frame.accessPathLabel(pk, field), exp)
          }
                  
          val aliases = GlobalHeap.getAliases(m.cgnode, i.getRef())    
          val m_weak_ap = m_strong_ap.flatMap { s => //(3) weak updates via access paths of aliases
            val s1 = aliases.foldLeft(s){
              case(s, alias) =>
                s.readvalue(frame.accessPathLabel(alias, field)) match {
                  case None => s
                  case _ => 
                    val exp = Expression.Term(s.readvalue(frame.localLabel(i.getVal())).get)
                    s.bindEval(frame.accessPathLabel(alias, field), exp)
                }  
            }
            s.joinOrNot(s1) match {
              case None =>
                List(s, s1)
              case Some(joined_s: AbstractState) =>
                List(joined_s)
            }
          }
  
          (Some(m_weak_ap.copy(state = m_weak_ap.state.joinAllStates())), List())
        } else {
          (Some(m_weak_abstract.copy(state = m_weak_abstract.state.joinAllStates())), List())
        }
      } else { //static field write     
        if(ConfigManager.hasSummarizedDimensions) {
          val pk = GlobalHeap.getPointerKeyForStaticField(field) //static field key              
          val newm = m.flatMap { s =>        
            s.readvalue(frame.regHeapLabel(pk)) match {
              case None => // the case pk does not exist as a regular heap label
                val exp = Expression.Term(s.readvalue(frame.localLabel(i.getVal())).get)                                
                List(s.bindEval(frame.regHeapLabel(pk), exp))
              case _ =>
                val exp = Expression.Term(s.readvalue(frame.localLabel(i.getVal())).get)
                val s1 = s.bindEval(frame.regHeapLabel(pk), exp)             
                s.joinOrNot(s1) match {
                  case None => List(s, s1)
                  case Some(joined_s: AbstractState) =>
                    List(joined_s)
                }
             }
          }         
          (Some(newm.copy(state = newm.state.joinAllStates())), List())
        } else {
          (Some(m), List())
        }
      }
      case _ => (Some(m), List())
    }
  }
  
  def stepGetI
    (i: GetI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    
    val frame = m.topFrame //top frame of the machine
    
    if(!ConfigManager.hasAccessPath && !ConfigManager.hasSummarizedDimensions) {
      val field = GlobalHeap.getField(i.getDeclaredField()) //field reference
      if(field != null) {
        WALA.getTypeType(field.getFieldTypeReference) match {  
            case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                              
              return (Some(m.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))), List())
            case _ =>
              return (Some(m), List())
        }
      } else {
        return (Some(m), List())
      }
    }
    
    if(!ConfigManager.isTopDown) { // a bottom-up analysis
      stepGetIBU(i, m)
    } else { //top-down analysis                
      stepGetITD(i, m)
    }
  }
 
  def stepGetIBU
    (i: GetI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    
    val frame = m.topFrame //top frame of the machine
        
    val field = GlobalHeap.getField(i.getDeclaredField()) //field reference
    if(field != null) { //the case the field reference lookup not returning null
      WALA.getTypeType(field.getFieldTypeReference) match {  
        case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                             
          if(ConfigManager.checkFieldAccess) {
            Clients.checkFieldAccess(i, m)
          }
          if(!i.isStatic) { //non-static field read       
            val pts = GlobalHeap.getPointsToSet(m.cgnode, i) //points-to set of base variable        
            val newm = if(pts.length>0) {                                 
              val ret  = m.flatMap { s =>
                //TODO: use fold to implement read from summarized dimensions?
                var list = List[AbstractState]()
                pts.foreach { ik =>               
                  val fkey = GlobalHeap.getFieldPK(ik, field) //field key          
                  val pk = GlobalHeap.getPointerKey(m.cgnode,i.getRef()) //pointer key                                                                    
                  s.readvalue(frame.accessPathLabel(pk, field)) match {
                    case None =>  //the case access path pk.field does not exist
                      assert(!ConfigManager.hasAccessPath && ConfigManager.hasSummarizedDimensions)
                      s.readvalue(frame.regHeapLabel(fkey)) match {
                        case None => //the case fkey does not exist as a regular heap label
                            val s_init = s.initHeap(frame.parHeapLabel(fkey))
                            list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(fkey)) :: list
//                            val s_init =
//                              if(ConfigManager.hasAccessPath) {
//                                if(ConfigManager.hasSummarizedDimensions) {
//                                  s_init0.copyBind(frame.accessPathLabel(pk, field), frame.parHeapLabel(fkey))     
//                                } else {
//                                  s_init0.alloc(frame.accessPathLabel(pk, field))
//                                }
//                              } else {
//                                s_init0
//                              }
//                            if(ConfigManager.hasAccessPath) {
//                              val exp = Expression.Term(s_init.readvalue(frame.accessPathLabel(pk, field)).get)
//                              list = s_init.bindEval(frame.localLabel(i.getDef()), exp) :: list
//                            } else {
//                              list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(fkey)) :: list
//                            }
                        case _ => //the case fkey already exists as a regular heap label
                          if(s.hasUpdated(frame.regHeapLabel(fkey))) {
                              val s_init = s.initHeap(frame.parHeapLabel(fkey))
                              list = s_init.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(fkey)) :: list
                              list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(fkey)) :: list
                          } else {
                              val s_init = s.initHeap(frame.parHeapLabel(fkey))
                              list = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(fkey)) :: list
                          }
                      }
                    case _ => //the case access path pk.field exists
                      assert(ConfigManager.hasAccessPath)
                      if(s.linear.constraints(frame.accessPathLabel(pk, field))) {
                        val exp = Expression.Term(s.readvalue(frame.accessPathLabel(pk, field)).get)
                        list = s.bindEval(frame.localLabel(i.getDef()), exp) :: list
                      } else {
                        s.readvalue(frame.regHeapLabel(fkey)) match {
                          case None => //the case fkey does not exist as a regular heap label
                              val s_init0 =  
                                if(ConfigManager.hasSummarizedDimensions) {
                                  s.initHeap(frame.parHeapLabel(fkey))
                                } else {
                                  s
                                }
                              val s_init =
                                if(ConfigManager.hasSummarizedDimensions) {
                                  s_init0.copyBind(frame.accessPathLabel(pk, field), frame.parHeapLabel(fkey))     
                                } else {
                                  s_init0
                                }
                              val exp = Expression.Term(s_init.readvalue(frame.accessPathLabel(pk, field)).get)
                              list = s_init.bindEval(frame.localLabel(i.getDef()), exp) :: list
                          case _ => //the case fkey already exists as a regular heap label
                            if(s.hasUpdated(frame.regHeapLabel(fkey))) {
                              val s_init = s.initHeap(frame.parHeapLabel(fkey))
                              val s1 = s_init.copyBindRemove(frame.accessPathLabel(pk, field), frame.regHeapLabel(fkey))
                              val s2 = s_init.copyBind(frame.accessPathLabel(pk, field), frame.parHeapLabel(fkey))                              
                              
                              val exp1 = Expression.Term(s1.readvalue(frame.accessPathLabel(pk, field)).get)
                              val exp2 = Expression.Term(s2.readvalue(frame.accessPathLabel(pk, field)).get)                            
                              list = s1.bindEval(frame.localLabel(i.getDef()), exp1) :: list
                              list = s2.bindEval(frame.localLabel(i.getDef()), exp2) :: list
                            } else {
                              val s_init = s.initHeap(frame.parHeapLabel(fkey)).copyBind(frame.accessPathLabel(pk, field), frame.parHeapLabel(fkey))                        
                              val exp = Expression.Term(s_init.readvalue(frame.accessPathLabel(pk, field)).get)
                              list = s_init.bindEval(frame.localLabel(i.getDef()), exp) :: list
                            }
                        }
                      }
                  }                  
                }
                val new_list = list//AbstractState.alignManyX(list)
                AbstractState.join(new_list)
              }
              ret
            } else {
              ifVerbose{
                Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
              }
              m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
            }
            (Some(newm.copy(state = newm.state.joinAllStates())), List())
          } else { //static field read
            if(ConfigManager.hasSummarizedDimensions) {
              val pk = GlobalHeap.getPointerKeyForStaticField(field)                              
              val newm  = m.flatMap { s =>
                var list = List[AbstractState]() 
                s.readvalue(frame.regHeapLabel(pk)) match {
                  case None => //the case fkey does not exist as a regular heap label 
                    list = s.initHeap(frame.parHeapLabel(pk)).copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(pk)) :: list                                        
                  case _ => //the case fkey already exists as a regular heap label
                    val s_init = s.initHeap(frame.parHeapLabel(pk))
                    val s1 = s_init.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(pk))                    
                    val s2 = s_init.copyBind(frame.localLabel(i.getDef()), frame.parHeapLabel(pk))                            
                    list = s1 :: list
                    list = s2 :: list
                 }
                 val new_list = list//AbstractState.alignManyX(list)
                AbstractState.join(new_list)
              }
              (Some(newm.copy(state = newm.state.joinAllStates())), List())
            } else {
              val newm = m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
              (Some(newm.copy(state = newm.state.joinAllStates())), List())
            }
          }
        case _ => (Some(m), List()) //load an array element whose type is not numeric
     }
    } else {
      ifVerbose{
        Log.println("WARNING: field lookup returned null")
      }
//      (Some(m.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))), List())
      (Some(m), List())
    }
  }

  
  def stepGetITD
    (i: GetI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    
    val frame = m.topFrame //top frame of the machine
                  
    val field = GlobalHeap.getField(i.getDeclaredField()) //field reference      
    if(field != null) {      
      WALA.getTypeType(field.getFieldTypeReference) match {
        case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                           
          if(ConfigManager.checkFieldAccess) {
            Clients.checkFieldAccess(i, m)
          }
          if(!i.isStatic) { //non-static field read                                
            val pts = GlobalHeap.getPointsToSet(m.cgnode, i)            
            val newm = if(pts.length>0) {               
              val ret  = m.flatMap { s =>
                //TODO: also use fold to implement get instead of list?
                var list = List[AbstractState]()                   
                
                pts.foreach { ik =>                      
                  val fkey = GlobalHeap.getFieldPK(ik, field) //field key                
                  val pk = GlobalHeap.getPointerKey(m.cgnode,i.getRef()) //pointer key                                
                  s.readvalue(frame.accessPathLabel(pk, field)) match {
                    case None =>  //the case access path pk.field does not exist
                      assert(!ConfigManager.hasAccessPath && ConfigManager.hasSummarizedDimensions)
                      s.readvalue(frame.regHeapLabel(fkey)) match {
                        case None => //the case fkey does not exist as a regular heap label
                          throw InterpException("TD analysis: both summarized objects and access paths do not exist")
//                          val s_1 =
//                            if(ConfigManager.hasSummarizedDimensions) {
//                              if(ConfigManager.isInterProc) { //from an inter-procedural perspective, a heap label should always exist before read. Reading it without existence meaning implicit initialization (i.e., = 0)
//                                val exp:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
//                                s.bindEval(frame.regHeapLabel(fkey), exp)
//                              } else {
//                                s.alloc(frame.regHeapLabel(fkey))
//                              }
//                            } else {
//                              s
//                            }
//                          
//                          val s_init = 
//                            if(ConfigManager.hasAccessPath) {
//                              if(ConfigManager.hasSummarizedDimensions) {
//                                s_1.copyBindRemove(frame.accessPathLabel(pk, field), frame.regHeapLabel(fkey))      
//                              } else {
//                                s_1.alloc(frame.accessPathLabel(pk, field))
//                              }
//                            } else {
//                              s_1
//                            }
//                          
////                            val s_init = s_1.copyBindRemove(frame.accessPathLabel(pk, field), frame.regHeapLabel(fkey))  
//                          if(ConfigManager.hasAccessPath) {
//                            val exp = Expression.Term(s_init.readvalue(frame.accessPathLabel(pk, field)).get)
//                            list = s_init.bindEval(frame.localLabel(i.getDef()), exp) :: list
//                          } else {
//                            list = s_init.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(fkey)) :: list
//                          }
                        case _ => //the case fkey already exists as a regular heap label
                            list = s.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(fkey)) :: list
                      }
                    case _ => //the case access path pk.field exists
                      assert(ConfigManager.hasAccessPath)
                      if(s.linear.constraints(frame.accessPathLabel(pk, field))) {
                        val exp = Expression.Term(s.readvalue(frame.accessPathLabel(pk, field)).get)
                        list = s.bindEval(frame.localLabel(i.getDef()), exp) :: list
                      } else {
                        s.readvalue(frame.regHeapLabel(fkey)) match {
                          case None => //the case fkey does not exist as a regular heap label
                            assert(!ConfigManager.hasSummarizedDimensions)
                            val exp = Expression.Term(s.readvalue(frame.accessPathLabel(pk, field)).get)
                            list = s.bindEval(frame.localLabel(i.getDef()), exp) :: list
                          case _ => //the case fkey already exists as a regular heap label
                            val s1 = s.copyBindRemove(frame.accessPathLabel(pk, field), frame.regHeapLabel(fkey))
                            val exp1 = Expression.Term(s1.readvalue(frame.accessPathLabel(pk, field)).get)                                               
                            list = s1.bindEval(frame.localLabel(i.getDef()), exp1) :: list
                        }
                      }
                  }
                  AbstractState.join(list).toList
                }
                AbstractState.join(list)
              }
              ret
            } else {
              ifDebug {
                Log.println("WARNING: should have found an instance key but not (incomplete points-to graph)")
              }
              m.mapState { s => s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef())) }
            }
            (Some(newm.copy(state = newm.state.joinAllStates())), List())
          } else { //static field dereference
            if(ConfigManager.hasSummarizedDimensions) {
              val pk = GlobalHeap.getPointerKeyForStaticField(field)                                   
              val newm  = m.flatMap { s =>
                var list = List[AbstractState]() 
                s.readvalue(frame.regHeapLabel(pk)) match {
                  case None => //the case fkey does not exist as a regular heap label 
                    list = s.alloc(frame.regHeapLabel(pk)).copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(pk)) :: list                                        
                  case _ => //the case fkey already exists as a regular heap label
                    val s1 = s.copyBindRemove(frame.localLabel(i.getDef()), frame.regHeapLabel(pk))                              
                    list = s1 :: list
                 }
                AbstractState.join(list)
              }           
              (Some(newm.copy(state = newm.state.joinAllStates())), List())
            } else {
              (Some(m.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))), List())
            }
          }
        case _ => 
          (Some(m), List()) //load an array element whose type is not numeric
     }
    } else { 
      ifDebug {
        Log.println("WARNING: field lookup returned null")
      }
      (Some(m), List())
    }
    
  }

  
  def stepBinopI
    (i: BinopI, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val typeInference = m.typeInference
    val t1 = typeInference.getType(i.getUse(0)).getTypeReference //the type of the first operand
    val t2 = typeInference.getType(i.getUse(1)).getTypeReference //the type of the second operand            
    (WALA.getTypeType(t1), WALA.getTypeType(t2))  match {    
        case (WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>   
          val op = WALAUtil.binopOfBinop(i) //numerical binary operator          
          op match {
            case None => //unhandled binary operator (e.g., andnot)
              ifDebug {
                Log.println("WARNING: Binary operator " + i.getOperator + "not handled.")
              }
              val new_m = m.mapState{s =>
                s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
              }
              (Some(new_m), List())
            case _ =>
              val exp = Expression.Binop(
                Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(0)))),
                op.get,
                Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(1))))
              )
                            
              if(ConfigManager.checkDivideByZero) {
                op.get match {
                  case Operator.NumericBinop./ => Clients.checkDivideByZero(i, m)
                  case _ =>
                }
              }
              
              val new_m = m.mapState{s =>
                val cexp = exp.close(s.dispatch)
                try {
                  s.bindEval(frame.localLabel(i.getDef()), cexp)
                } catch {
                  case NonLinear("modulus") =>
                    val op_1 = Operator.NumericRelation.≥
                    
                    val exp_1 = Expression.Binop(
                      Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(1)))),
                      op_1,
                      Expression.Term[Term](Term.Constant(CIV(0)))
                    )
                    val cexp_1 = exp_1.close(s.dispatch)
                            
                    val sfalse = s.assume(!cexp_1).filter(_.isFeasible)
                    
                    if(sfalse.isEmpty) {
                      val s_init = s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
                      val exp1 = Expression.Term(s_init.readvalue(frame.localLabel(i.getDef())).get)
                      val exp2 = Expression.Term(s_init.readvalue(frame.localLabel(i.getUse(1))).get)
                      s_init.copy(linear = s_init.linear.assumeSingle(exp1 < exp2))
                    } else {
                      s.unbind(frame.localLabel(i.getDef())).alloc(frame.localLabel(i.getDef()))
                    }
                }
              }
              
            (Some(new_m.copy(state = new_m.state.joinAllStates())), List[AbstractMachine]())
          }                          
        case (_, _) => (Some(m), List()) //binary opertion on non-numeric operands
    }
  }
  
  def stepConversionI
    (i: com.ibm.wala.ssa.SSAConversionInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val from = i.getFromType() //the type of the rhs variable
    val to   = i.getToType() //the type of the lhs variable
    (WALA.getTypeType(from), WALA.getTypeType(to)) match {
      case (WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>
        val new_m = m.mapState{s =>
          val v1 = s.readvalue(frame.localLabel(i.getUse(0))).get
          s.bind(frame.localLabel(i.getDef()), v1)
        }
        (Some(new_m.copy(state = new_m.state.joinAllStates())), List())
      case (_, WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean) =>
        ifDebug {
          Log.println("WARNING: conversion from non-linear type to linear type")
        }
        val new_m = m.mapState{s =>
          s.alloc(frame.localLabel(i.getDef()))
        }
        (Some(new_m.copy(state = new_m.state.joinAllStates())), List())
      case (_, _) => //conversion between non-numeric types
        (Some(m), List())
    }
  }
  
  def stepThrowI
    (i: SSAThrowInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val cfg = m.ir.getControlFlowGraph() //control flow graph
    val expSuccs = cfg.getExceptionalSuccessors(cfg.getBlockForInstruction(i.iindex)) //the successor basic blocks via expectional edges
    var l = List[Int]()
    expSuccs.foreach{bb =>
      val index = bb.getFirstInstructionIndex
      if(index != -1)
        l = index :: l
    }    
    if(l.isEmpty) { //the case a throw is not caught?
      (None, List(m.doReturn))
    } else {
      val m_l = l.foldLeft(List[Machine]()) {
        case(list, index) =>
          m.gotoTop(index) :: list
      }      
      (None, m_l)
    }
  }
  
  def stepInstanceOfI
    (i: SSAInstanceofInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val new_m = m.mapState{s =>       
      val s_init = s.unbind(frame.localLabel(i.getDef)).alloc(frame.localLabel(i.getDef))        
      val exp1 = Expression.Term(s_init.readvalue(frame.localLabel(i.getDef)).get)
      val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
      //TODO: currently setting the lhs to >= 0; a more precise implementaiton is to look up the points-to set or at least make the value 0 | 1
      s_init.copy(linear = s_init.linear.assumeSingle(exp1 ≥ exp2))        
    }    
    (Some(new_m.copy(state = new_m.state.joinAllStates())), List[AbstractMachine]())
  }
  
  def stepLoadMetadataI
    (i: SSALoadMetadataInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    WALA.getTypeType(i.getType) match {    
        case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>                                   
          throw NotImplemented(i.toString())
        case _ => (Some(m), List())
    }
  }
  
  def stepComparisonI
    (i: SSAComparisonInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val new_m = m.mapState{s =>
      if(i.hasDef) {
        s.unbind(frame.localLabel(i.getDef)).alloc(frame.localLabel(i.getDef)) 
        //TODO: what are the actual effects of compare instruction?
      } else {
        s
      }
    }    
    (Some(new_m.copy(state = new_m.state.joinAllStates())), List[AbstractMachine]())
  }
  
  def stepUnaryOpI
    (i: SSAUnaryOpInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val typeInference = m.typeInference
    val t = typeInference.getType(i.getDef).getTypeReference // the type of the lhs variable            
    WALA.getTypeType(t) match {
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
        val op = WALAUtil.unopOfUnop(i) //the unary operator
        op match {
          case None =>
            ifVerbose {
              Log.println("WARNING: Unary operator " + i.getOpcode + "not handled.")
            }
            val new_m = m.mapState{s =>
              s.alloc(frame.localLabel(i.getDef()))
            }
            (Some(new_m.copy(state = new_m.state.joinAllStates())), List())
          case _ =>
            val exp = Expression.Unop(
              Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(0)))),
              op.get
            )
            val new_m = m.mapState{s =>
              val cexp = exp.close(s.dispatch)
              s.bindEval(frame.localLabel(i.getDef()), cexp)
            }
            (Some(new_m.copy(state = new_m.state.joinAllStates())), List[AbstractMachine]())
        }
      case _ => (Some(m), List()) //unary operation on non-numeric types
    }
  }
  
  def stepSwitchI
    (i: SSASwitchInstruction, m: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
    val frame = m.topFrame //top frame of the machine
    val typeInference = m.typeInference
    val t = typeInference.getType(i.getUse(0)).getTypeReference //the type of the rhs variable            
    WALA.getTypeType(t) match {    
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>   
        val casesAndLabels = i.getCasesAndLabels //cases and labels in an array
        var list = List[AbstractMachine]()
        for (j <- 0 until casesAndLabels.size by 2) {
          val caseMatch = casesAndLabels(j) //an array element representing the case value
          val target = casesAndLabels(j+1) //the next array element representing the instruction index to goto
          val op = Operator.NumericRelation.==
          val exp = Expression.Binop(
            Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(0)))),
            op,
            Expression.Term[Term](Term.Constant(CIV(caseMatch)))
          )
          val newm = m.flatMap{s =>
            val cexp = exp.close(s.dispatch)
            s.assume(cexp)
          }.gotoTop(target)               
          list = newm :: list                
        }              
        //TODO: the default case should be handled more precisely?
        list = m.gotoTop(i.getDefault) :: list          
        (None, list.filter(_.isFeasible))
      case _ => //switch case on non-numeric variable
        val casesAndLabels = i.getCasesAndLabels
        var list = List[AbstractMachine]()
        for (j <- 0 until casesAndLabels.size by 2) {
          val target = casesAndLabels(j+1)      
          val newm = m.gotoTop(target)               
          list = newm :: list                
        }             
        list = m.gotoTop(i.getDefault) :: list          
        (None, list.filter(_.isFeasible))
    }
  }
  
}

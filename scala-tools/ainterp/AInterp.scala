import edu.illinois.wala.Facade._

import Util._
import Core._
import AbstractMachine._
import WALAUtil._
import Config._
import Annotation._
import Label._

import com.ibm.wala.ssa.SSAConversionInstruction
import com.ibm.wala.classLoader.ProgramCounter
import com.ibm.wala.ssa.SSAThrowInstruction
import com.ibm.wala.ssa.SSACheckCastInstruction
import com.ibm.wala.ssa.SSAInstanceofInstruction
import com.ibm.wala.ssa.SSALoadMetadataInstruction
import com.ibm.wala.ssa.SSAComparisonInstruction
import com.ibm.wala.ssa.SSAUnaryOpInstruction
import com.ibm.wala.ssa.SSAMonitorInstruction
import com.ibm.wala.ssa.SSASwitchInstruction

import scala.collection.Map
import scala.collection.immutable.{Map=>IMap, HashMap, Set=>ISet}
import scala.collection.mutable.{Set,Stack,Queue}

import collection.JavaConversions._



import scalaz._
import Scalaz._

import java.io._

object AInterp {
  type Machine = AbstractMachine
  val  Machine = AbstractMachine
  
  var cache = Map[WALA.CGNode, (Machine, Machine)]()
  
  var reachable_labels = Map[WALA.CGNode, List[Label]]()
    
  var clinits = List[WALA.CGNode]()
  
  var reachable_methods = List[String]()
  
  var initialized_lengths = List[Label]()

  /* solveMethod: method-focused use case, evaluate from start to finish
   * of a method, assuming all inputs not mentioned in EvalContext are
   * symbolic. Returns a representatio of the abstract values at any
   * point in the method, indexed by CFGNode. */
  def solveMethod(
    context: InterpContext
  )(implicit conf: AInterpConfig): Option[Machine] = {
    val InterpContext(econtext, wcontext) = context

    val ir : WALA.IR = wcontext.ir
    val method       = ir.getMethod()

    val cfg   = ir.getControlFlowGraph()
    val entry = cfg.entry()
    
    ifVerbose{
      Log.println(ir.toString())
    }

    val initm = initMachine(context)//.copy(annots = test_annots))

    //ad-hoc initialization of static fields for all classes in the call graph
    //TODO: figure out the right way to handle clinit methods
    val initm_clinit = 
      if(ConfigManager.isTopDown && !ConfigManager.isHybrid && ConfigManager.isInterProc) {
        val clinit_nodes = wcontext.cg.iterator().filter { node => node.getMethod.isClinit() }.toList

        clinit_nodes.foreach { n => 
          val clinit_context = new InterpContext(new EvalContext(), new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, n)) 
          val clinit_m = initMachine(clinit_context)
          stepMethod(clinit_context, clinit_m)
        }
        
        initm
      } else {
        initm
      }
    
    ifVerbose{
      Log.println("Annotations:")
      Log.println(tab(
        initm.annots.toList.map{case (method, methodAnnots) =>
          s"$method:\n" + tab(methodAnnots.toList.map{case (iindex, annot) =>
            tab(s"$iindex: " + annot.mkString(", "))
          }.mkString("\n"))
        }.mkString("\n")
      )+"\n")
    }

    stepMethod(context, initm_clinit.gotoTop(entry))(conf) match {
      case Some(ret) =>
        ifDebug {
          Log.println("Final (unprojected) return is\n" + tab(ret))
        }
        
        //TODO: should the projection happen here?
        val ret_projected = ret
        
        ifDebug{Log.println("Final return is\n" + tab(ret_projected))}  // moved printing to CLI
        ret_projected.state.linear.printAbstract0()
        Some(ret_projected)
      case _ => None
    }

  }

  type Node = WALA.ICFGNode
  type Edge = (Node, Node)

  def stepRegion(
    context: InterpContext,
    in_m_temp: Machine,   // The starting abstract machine
    in_edge: Edge,        // The edge in control flow graph we start
                          // evaluation at
    out_edges: ISet[Edge] // The edges to NOT traverse. These can be
                          // used to designate the range of code to
                          // evaluate
  )(implicit
    conf: AInterpConfig,
    doExitPhis: Boolean = true): Map[Edge, Machine] = {

    var work        = Map[Edge, Option[Machine]]()
    // Abstract interpretation tasks are stored here per cfg edge.
    // There is only one task per edge due to joining.

    var workHistory = Map[Edge, List[Machine]]()
    // This one stores the sequence of machines that have arrived at
    // each edge. It is not the work that still needs to be done but
    // rather all the work that has ever arrived at said edges.

    var retHistory = Map[Edge, List[Machine]]()
    // Similar to above, except for the machines to be returned,
    // without evaluating their phi's as necessary for compute bound.

    val in_m = in_m_temp
      .gotoTop(in_edge._2.asInstanceOf[WALA.CFGNode])
      .stepPhis(context, in_edge._1.asInstanceOf[WALA.CFGNode])
    // In case the machine is at the wrong spot. Also process phi's in
    // case the starting node has phis.
      
    work += (in_edge -> Some(in_m))
    //workHistory += (in_edge -> List(in_m))

    var hasWork = Queue(in_edge)
    // This one is for book-keeping of work that needs to be done. If
    // an edge is in this, it should also be in "work".
    
    while (! hasWork.isEmpty) {
      val work_edge = hasWork.head
      hasWork = hasWork.drop(1)
      val m = work(work_edge).get
            
      work -= work_edge
      // Main loop is done as long as "hasWork" is non empty, the
      // actual machine to process is retrieved from "work"
      val (origin_block, target_block) = work_edge
      // Note: phis are now processed by stepBlock as branches are generated

      // Start with processing the phi-instructions for current
      // machine, the origin block needed for processing phi's comes
      // from the edge at which we found the machine.

      ifProgress{
        Log.println("Working on " + work_edge._1.getMethod().getSelector() + ": " +
          "edge \t[BB" + work_edge._1.getGraphNodeId() + "=>BB" + work_edge._2.getGraphNodeId() + "]" +
          " \twith " + m.numLabels + " label(s)" +
          " \t" + m.complexity +  " disjunct(s)" +
          " \t" + m.numConstraints + " constraint(s)" +
          " \timprecision=" + m.countMachines + ""
        )
        Log.println(Core.memStats)
      }
      val branches_1 = stepBlock(m)(conf)        
      
      val branches = //bottom-up analysis needs to be aligned
        if(!ConfigManager.isTopDown && ConfigManager.hasSummarizedDimensions)
//          Machine.alignManyX(branches_1)
          branches_1
        else
          branches_1
      
      // Process the rest of the instructions in the current block the
      // current machine is at. This results in a set of machines,
      // each representing a jump in the just-interpreted block.

      branches.foreach{next_m_prephi =>
        // We need to get through all the branches to integrate them
        // into future work queues.
        val next_m = next_m_prephi.stepPhis(context, target_block.asInstanceOf[WALA.CFGNode])
        val next_block = next_m.topFrame.block
        val next_edge = (target_block, next_block)
        // Get the block this branch will be executing, and the edge
        // we just traversed to get to this block. This last part is
        // for the book-keeping as per notes above.
        if (out_edges.contains(next_edge)) {
          // If this is a specified exit edge, add this machine (pre
          // phi) to the retHistory.
          val prior_results_ret = retHistory.getOrElse(next_edge,default=List())
          retHistory += (next_edge ->
            ((if (doExitPhis) next_m else next_m_prephi) :: prior_results_ret)
          )
        }
        val prior_results = workHistory.getOrElse(next_edge,default=List())
        val other_queued = work.getOrElse(next_edge,default=None)
        // "prior_results" gets the history of machines that were ever
        // processed at "next_edge". "other_queued" looks into whether
        // there is already work waiting at "next_edge".
        
        val next_m_joined = if (next_m.backedges.contains(next_edge)) {
          Joiner.joinerBack(prior_results)(other_queued)(next_m)
        } else {
          Joiner.joinerForward(prior_results)(other_queued)(next_m)
        }
        // We need to integrate this branch with potentially existing
        // work waiting at that edge. The prior machines at this edge
        // can also be used for this integration in some cases. This
        // is where join and/or widening would used.
        next_m_joined match {
          case Some(m_joined) =>
            // Record the branch (integrated with other work at the
            // same node) into the history and add it to work queue.
            workHistory += (next_edge -> (m_joined :: prior_results))
            if (! m_joined.hasTopReturned && ! out_edges.contains(next_edge)) {
              work += (next_edge -> Some(m_joined))
              if (! hasWork.contains(next_edge))
                hasWork += next_edge
            }
          case None => ()
            // "next_m_joined" could be None if the joiner decides it
            // is not needed to evaluate this branch, due to it being
            // included in another machine considered in the past (see
            // "leq").
        }
      }
    }
    //workHistory.map{case (e, h) => (e -> Machine.join(h))}
    retHistory.map{case (e, h) => (e -> Machine.join(h))}
    // At the very end we return a machine for each edge traversed.
    // This one machine is the joined version of all the machines that
    // were ever processed at that edge.
  }

  def stepMethod
    (context: InterpContext, inm: Machine)
    (implicit conf: AInterpConfig)
      : Option[Machine] = {
    
    val InterpContext(econtext, wcontext) = context

    val newm = inm
    
    val method = newm.ir.getMethod.getSignature
    if(!reachable_methods.contains(method)) {
      reachable_methods = method :: reachable_methods
    }

    val initedge = WALA.getEntryEdges(newm.cfg).head
    val outedges = WALA.getExitEdges(newm.cfg)
    val results = stepRegion(context, newm.nextTop, initedge, outedges)(conf)

    val resultEdges = results.keys

    val outresults = outedges.filter{results.contains(_)}

    ifDebug{
      Log.println("all results for this method:")
      Log.println(tab(results.mkString("\n")))
    }

    if (0 == outresults.size) {
      None
    } else {
      val rets = outedges.filter{results.contains(_)}.foldLeft(List[Machine]()) {
        case (l, edge) =>
          results.get(edge).get :: l
      }
            
      val ret = Machine.join(rets)
      
      if(ConfigManager.checkReturn) {
        Clients.checkReturn(ret)
      }
      
      Some(ret)
    }
    
  }
  
  def stepBlock
    (startm: Machine)
    (implicit conf: AInterpConfig)
      : List[Machine] = {
    /* Step though the instructions in the current block and collecting a
     * list of branches that were jumped to in the process. Handles
     * the phi instructions here.
     */

    val ir = startm.ir
    var m: Option[Machine] = Some(startm)
    var branches = List[Machine]()
    
    while (! m.isEmpty) {
      val (nextm, newbranches) = stepInstruction(m.get)(conf)
      branches = branches ++ newbranches
      m = nextm match {
        case None => None
        case Some(anm) =>
            val nextm = anm.nextTop
            if (nextm.topFrame.block != anm.topFrame.block) {
              branches = nextm :: branches
              None
            } else {
              Some(nextm)
            }
      }
    }
    ifDebug{
      Log.println("emitted branches\n" + tab(branches.map(_.topFrame.toString).mkString("\n")))
    }
    branches
  }

  def stepInstruction
    (m_pre_annot: Machine)
    (implicit conf: AInterpConfig)
      : (Option[Machine],List[Machine]) = {
        
    val frame = m_pre_annot.topFrame
    val addr = frame.pc    
    val inst = addr.instr
    val n = addr.block
    val m_1 = m_pre_annot.stepAnnotation(Order.Before)
    
    val m = m_1
    
    ifVerbose{
      if (null != inst) {
        Log.println("stepInstruction BB" + addr.block.getNumber() + " I" + addr.index + ":\n" + tab(m))
      } else {
        Log.println("stepInstruction BB" + addr.block.getNumber() + " I" + addr.index + ":\n" + tab(m))
      }
    }
                
    val (nextm, branch):(Option[Machine], List[Machine]) = inst match {
      case i: InvokeI => InstructionHandler.stepInvokeI(i, m)(conf)                
      case i: NewI => InstructionHandler.stepNewI(i, m)(conf)   
      case i: ArrayLengthI => InstructionHandler.stepArrayLengthI(i, m)(conf)        
      case i: ArrayStoreI => InstructionHandler.stepArrayStoreI(i, m)(conf)
      case i: ArrayLoadI => InstructionHandler.stepArrayLoadI(i, m)(conf)        
      case i: GotoI =>
        val nextm = m.gotoTop(i.getTarget())
        (None, List(nextm))
      case i: BranchI => InstructionHandler.stepBranchI(i, m)(conf)               
      case i: ReturnI => InstructionHandler.stepReturnI(i, m)(conf)          
      case i: PutI => InstructionHandler.stepPutI(i, m)(conf)
      case i: GetI => InstructionHandler.stepGetI(i, m)(conf)
      case i: BinopI => InstructionHandler.stepBinopI(i, m)(conf)
      case i: SSAConversionInstruction => InstructionHandler.stepConversionI(i, m)(conf)      
      case i: SSAThrowInstruction => InstructionHandler.stepThrowI(i, m)(conf)
      case i: SSACheckCastInstruction => (Some(m), List())
      case i: SSAInstanceofInstruction => InstructionHandler.stepInstanceOfI(i, m)(conf)
      case i: SSALoadMetadataInstruction => InstructionHandler.stepLoadMetadataI(i, m)(conf)
      case i: SSAComparisonInstruction => InstructionHandler.stepComparisonI(i, m)(conf)
      case i: SSAUnaryOpInstruction => InstructionHandler.stepUnaryOpI(i, m)(conf)
      case i: SSAMonitorInstruction => (Some(m), List())        
      case i: SSASwitchInstruction => InstructionHandler.stepSwitchI(i, m)(conf)        
      case null => (Some(m), List())   
      case i =>
        throw NotImplemented(i.toString())
    }
    (nextm.map{_.stepAnnotation(Order.After)(addr.index)},
      branch.map{_.stepAnnotation(Order.After)(addr.index)})
  }
  
}

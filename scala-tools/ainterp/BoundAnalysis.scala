//import parma_polyhedra_library._
//
//import edu.illinois.wala.Facade._
//
//import Util._
//import PPL.LExp._
//import PPL._
//import Core._
//import CommonLoopBounds._
//import AbstractMachine._
//import WALA.CGNode
//import WALAUtil._
//import Config._
//import StateTypes._
//import Label.isParRet
//import Specification._
//import Trail._
//
//import com.ibm.wala.classLoader.ProgramCounter
//
//import scala.collection.Map
//import scala.collection.Map._
//import scala.collection.immutable.{Map=>IMap, HashMap, Set=>ISet}
//import scala.collection.mutable.{Set,Stack,Queue,ListBuffer}
//import scala.sys.process.Process
//
////import com.ibm.wala.types.TypeReference
//import scala.sys.exit
//
//import collection.JavaConversions._
//
////import edu.illinois.wala.Facade._
//
//import scala.Function2
//
//import scalaz._
//import Scalaz._
//
//import CFGAnnotations._
//
//import AInterp._
//
//import BoundExpressions._
//
//import java.io.{File,PrintWriter,FileWriter,BufferedWriter}
//import java.nio.file.{Files,Path}
//
//object BoundAnalysis {
//  var doJoin = true
//
//  var indent_level = 0
//  def indent() { indent_level += 1 }
//  def deindent() { indent_level -= 1 }
//  def indented_println(s : String = "") {
//    Log.print(" | " * indent_level)
//    Log.println(s)
//  }
//
//  def trailbounds(
//    node: CGNode,
//    outDir: Path,
//    htmlWriter: PrintWriter,
//    do_interprocedural: Boolean
//  )(implicit conf: AInterpConfig): BoundExpression = {
//    Log.println("enter interprocedural")
//    // mapping from call to ainterp summary
//    var map = scala.collection.mutable.Map[CGNode,Machine]()
//
//    // mapping from call to bound summary
//    var boundSummaries = scala.collection.mutable.Map[String,BoundExpression]()
//
//    // TODO: load libraries of bounds
//    // FunctionBounds.loadLibraries(boundSummaries)
//
//    var visited = List[CGNode]()
//    def traverseCallGraph(node: CGNode) : BoundExpression = {
//      visited ::= node
//
//      Log.println("enter traverseCallGraph")
//
//      val funname  = node.getMethod.getSignature()
//
//      indented_println("entering " + funname)
//      indent()
//
//      // TODO: make this a cli option
//      if (do_interprocedural) {
//        var callees = GlobalHeap.cg.getSuccNodes(node).toList
//        val childbounds = callees.map { callee =>
//          val sig = callee.getMethod.getSignature()
//          val loader = callee.getMethod.getDeclaringClass.getClassLoader.getName.toString()
//
//          if(! visited.contains(callee) &&
//            !SpecialLibHandling.libs.contains(sig) &&
//            loader.equals("Application")) {
//            traverseCallGraph(callee)
//          }
//        }.toList
//
//        ifDebug{Log.println("child bounds " + childbounds)}
//      }
//
//      val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)
//      val spec     = Specification.getSpecs.get( funname )
//
//      // ****************************************************
//      // load handy aliases
//      val ir : WALA.IR = wcontext.ir
//      val method       = ir.getMethod()
//      val cfg          = ir.getControlFlowGraph()
//      var edgeMap = scala.collection.mutable.Map[WALA.CFGNode,List[WALA.CFGNode]]()
//      val trails = TrailAnalysis.trailBlaze(new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node), edgeMap)
//      // val g = TrailAnalysis.getAllNodes(wcontext.cfg.entry().asInstanceOf[WALA.CFGNode], List(), wcontext)
//      // val ttt = TrailAnalysis.corTrail(wcontext.cfg.entry(), g(g.length-1), g.length-1, wcontext, g)
//      // println("THE trail")
//      // println(toStringRE(ttt))
//      println("all trails")
//      trails.foreach{trail =>
//        println(toStringRE(trail))
//      }
//
//      // ****************************************************
//      // build Control Tree
//      val controlTree = (new ASTNodeAny(null, null, null))
//        .buildLoopTreeFromCFG(wcontext.cfg.entry(), wcontext)
//      ifDebug {Log.println("\ncontrol tree\n" + controlTree.toString(1))}
//
//      // ****************************************************
//      // construct trailspec
//      // println("all trails")
//      // val trails = TrailAnalysis.getTrails(wcontext.cfg.entry(), wcontext)
//      // trails.foreach{trail => println(Trail.toStringRE(trail))}
//
//      // val generalTrail = TrailAnalysis.run(controlTree, wcontext, null)
//      // println("+++++++++++++++++++++++++++++++++++++++")
//      // println("generalTrail " + Trail.toStringRE(generalTrail))
//      // println("controlTree " + controlTree.toString(1))
//      // val splittedTrails = TrailAnalysis.getSplittedTrails(generalTrail)
//      // var i = 0
//      // for ((s1,s2) <- splittedTrails) {
//      //   i+=1
//      //   println("\npreTrail " + i + ": " + Trail.toStringRE(s1))
//      //   println("trail " + i + ": " + Trail.toStringRE(s2)) + "\n"
//      // }
//
//      // val all_trails = splittedTrails.toList
//
//      // // val (preTrail, trail) = if (true)
//      // //   splittedTrails.head
//      // // else
//      // //   TrailAnalysis.simple(wcontext)
//
//      var context = InterpContext(new EvalContext(), wcontext)
//      val initm   = initMachine(context)
//      val initedge = WALA.getEntryEdges(wcontext.cfg).toList(0)
//      val first_node = initedge._1.asInstanceOf[WALA.CFGNode]
//
//      // // TEMP: use full ainterp on method for pre and post conditions
//      // val outedges = WALA.getExitEdges(wcontext.cfg)
//      // println("initedge " + initedge)
//      // println("outedges " + outedges)
//      // // val methodmachines = stepRegion(context, initm, initedge, outedges)
//
//      val allloopinfo = WALA.getLoopStructure(wcontext)
//
//      indented_println("computing bounds for " + funname)
//
//      // ****************************************************
//      // Get Procedure Bound, perhaps from a Spec
//      // a mapping from a node to it's list of bounds
//      var node2boundTrail = scala.collection.mutable.Map[WALA.CFGNode, List[(Trail,BoundExpression)]]()
//      var all_trail_string = "<ul>"
//      val trailsWriter = TrailRenderer.open(wcontext,edgeMap,outDir)
//      val save_indent_level = indent_level  // avoid problems of deindenting after exception
//      spec match {
//        //case Some(Specification.Specification(sb,_,_)) => (m /*FIX*/, sb)
//        case Some(_) =>
//        case None =>
//          for (trail <- trails) {
//            val m = initm
//            val start_bound = new BoundExpressionConst(0)
//            // val (m, new_last) = stepTrail(context, cfg, ir, initm, preTrail, start_block, trailVars)
//            //val trailVars : Iterable[String] = Trail.getTrailVars(context, cfg, ir, trail, new_last)
//            indent_level = save_indent_level
//            indented_println("top-level getTCBound call on " + Trail.toStringRE(trail))
//            indent()
//            try {
//              var trailVars = scala.collection.mutable.Map[Trail.Trailspec, List[String]]()
//              val (tc, bound) = getTCBound(context, m, start_bound, trail, first_node, allloopinfo, node2boundTrail, trailVars)
//              all_trail_string += TrailRenderer.addFeasibleTrail(trail,trailsWriter,trailVars,bound)
//            } catch {
//              case e: Core.LogicException =>
//                indent_level = save_indent_level + 1
//                indented_println("infeasible trail")
//                all_trail_string += "<li><b>Trail:</b><font color=\"gray\">"+Trail.toStringRE(trail) + "</font><br>"
//	              all_trail_string += "<i>Infeasible trail</i><br>"
//
//            }
//          }
//      }
//      all_trail_string += "</ul>"
//      TrailRenderer.close(trailsWriter)
//
//      // build control tree and compute bound summary
//      var nodeBoundsHack = Map[WALA.CFGNode, List[(Trail,BoundExpression)]]()
//      try {
//        val nodeBounds = BoundAnalysis.procedureSummary(context, controlTree)
//
//        nodeBounds.foreach{ case (n, b) =>
//          b.foreach{b =>
//            nodeBoundsHack += n -> List((List[Trailspec](), b))
//          }
//        }
//      } catch {
//        case e: Exception =>
//          // TODO better information in output, e.g., with
//          // BoundExpressionNan("procedureSummary failed")
//          println("FAILED procedureSummary")
//      }
//
//      // ****************************************************
//      // Some code that renders the trail along with the control tree
//      //val nodeBounds = BoundAnalysis.procedureSummary(context, controlTree)
//      val boundSummary = summarizeBoundsAndDisplay(
//        context, outDir, htmlWriter, spec, boundSummaries, controlTree, nodeBoundsHack,
//        all_trail_string, Some(trailsWriter.getOutputFileName())
//      )
//      indented_println()
//      indented_println("function bounds " + boundSummary)
//      indented_println("function polynomial " + PolynomialUtil.construct(boundSummary, context))
//
//      // TODO: memoize procedure summary
//      // val ret_m = m.filter(isParRet)
//      // map += (node ->
//      //   ret_m.copy(state = ret_m.state.joinAllStates())
//      // )
//
//      // memoize bound summary
//      boundSummaries += (funname -> boundSummary)
//
//      deindent()
//      indented_println("leaving " + funname)
//      indented_println()
//
//      boundSummary
//    }
//
//    val procedureBounds = traverseCallGraph(node)
//    // println("final bounds: " + funBounds)
//    // println("final polynomial: " + PolynomialUtil.construct(funBounds, context))
//
//    procedureBounds
//  }
//
//  def trailbounds_pure(
//    node: CGNode,
//    outDir: Path,
//    htmlWriter: PrintWriter
//  )(implicit conf: AInterpConfig): BoundExpression = {
//    Log.println("enter interprocedural")
//    // mapping from call to ainterp summary
//    var map = scala.collection.mutable.Map[CGNode,Machine]()
//
//    // mapping from call to bound summary
//    var boundSummaries = scala.collection.mutable.Map[String,BoundExpression]()
//
//    // TODO: load libraries of bounds
//    // FunctionBounds.loadLibraries(boundSummaries)
//
//    var visited = List[CGNode]()
//    def traverseCallGraph(node: CGNode) : BoundExpression = {
//      visited ::= node
//
//      Log.println("enter traverseCallGraph")
//
//      val funname  = node.getMethod.getSignature()
//
//      indented_println("entering " + funname)
//      indent()
//
//      var callees = GlobalHeap.cg.getSuccNodes(node).toList
//      val childbounds = callees.map { callee =>
//        val sig = callee.getMethod.getSignature()
//        val loader = callee.getMethod.getDeclaringClass.getClassLoader.getName.toString()
//
//       if(! visited.contains(callee) &&
//          !SpecialLibHandling.libs.contains(sig) &&
//          loader.equals("Application")) {
//          traverseCallGraph(callee)
//        }
//      }.toList
//
//      ifDebug{Log.println("child bounds " + childbounds)}
//
//      val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)
//      val spec     = Specification.getSpecs.get( funname )
//
//      // ****************************************************
//      // load handy aliases
//      val ir : WALA.IR = wcontext.ir
//      val method       = ir.getMethod()
//      val cfg          = ir.getControlFlowGraph()
//      var edgeMap = scala.collection.mutable.Map[WALA.CFGNode,List[WALA.CFGNode]]()
//      val trails = TrailAnalysis.trailBlaze(new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node), edgeMap)
//      val ttt = TrailAnalysis.corTrail(wcontext.cfg.entry(), wcontext.cfg.entry(), -1, wcontext, TrailAnalysis.getAllNodes(wcontext.cfg.entry().asInstanceOf[WALA.CFGNode], List(), wcontext))
//      println("THE trail")
//      println(toStringRE(ttt))
//      println("all trails")
//      trails.foreach{trail =>
//        println(toStringRE(trail))
//      }
//
//      // ****************************************************
//      // build Control Tree
//      val controlTree = (new ASTNodeAny(null, null, null))
//        .buildLoopTreeFromCFG(wcontext.cfg.entry(), wcontext)
//      ifDebug {Log.println("\ncontrol tree\n" + controlTree.toString(1))}
//
//      // ****************************************************
//      // construct trailspec
//      // println("all trails")
//      // val trails = TrailAnalysis.getTrails(wcontext.cfg.entry(), wcontext)
//      // trails.foreach{trail => println(Trail.toStringRE(trail))}
//
//      // val generalTrail = TrailAnalysis.run(controlTree, wcontext, null)
//      // println("+++++++++++++++++++++++++++++++++++++++")
//      // println("generalTrail " + Trail.toStringRE(generalTrail))
//      // println("controlTree " + controlTree.toString(1))
//      // val splittedTrails = TrailAnalysis.getSplittedTrails(generalTrail)
//      // var i = 0
//      // for ((s1,s2) <- splittedTrails) {
//      //   i+=1
//      //   println("\npreTrail " + i + ": " + Trail.toStringRE(s1))
//      //   println("trail " + i + ": " + Trail.toStringRE(s2)) + "\n"
//      // }
//
//      // val all_trails = splittedTrails.toList
//
//      // // val (preTrail, trail) = if (true)
//      // //   splittedTrails.head
//      // // else
//      // //   TrailAnalysis.simple(wcontext)
//
//      var context = InterpContext(new EvalContext(), wcontext)
//      val initm   = initMachine(context)
//      val initedge = WALA.getEntryEdges(wcontext.cfg).toList(0)
//      val first_node = initedge._1.asInstanceOf[WALA.CFGNode]
//
//      // // TEMP: use full ainterp on method for pre and post conditions
//      // val outedges = WALA.getExitEdges(wcontext.cfg)
//      // println("initedge " + initedge)
//      // println("outedges " + outedges)
//      // // val methodmachines = stepRegion(context, initm, initedge, outedges)
//
//      val allloopinfo = WALA.getLoopStructure(wcontext)
//
//      indented_println("computing bounds for " + funname)
//
//      // ****************************************************
//      // Get Procedure Bound, perhaps from a Spec
//      // a mapping from a node to it's list of bounds
//      var node2bound = scala.collection.mutable.Map[WALA.CFGNode, List[(Trail,BoundExpression)]]()
//      var all_trail_string = "<ul>"
//      val trailsWriter = TrailRenderer.open(wcontext,edgeMap,outDir)
//      val save_indent_level = indent_level  // avoid problems of deindenting after exception
//      spec match {
//        //case Some(Specification.Specification(sb,_,_)) => (m /*FIX*/, sb)
//        case Some(_) =>
//        case None =>
//          for (trail <- trails) {
//            val m = initm
//            val start_bound = new BoundExpressionConst(0)
//            // val (m, new_last) = stepTrail(context, cfg, ir, initm, preTrail, start_block, trailVars)
//            //val trailVars : Iterable[String] = Trail.getTrailVars(context, cfg, ir, trail, new_last)
//            indent_level = save_indent_level
//            indented_println("top-level getTCBound call on " + Trail.toStringRE(trail))
//            indent()
//            try {
//              var trailVars = scala.collection.mutable.Map[Trail.Trailspec, List[String]]()
//              val (tc, bound) = getTCBound(context, m, start_bound, trail, first_node, allloopinfo, node2bound, trailVars)
//	      all_trail_string += TrailRenderer.addFeasibleTrail(trail,trailsWriter,trailVars,bound)
//            } catch {
//              case e: Core.LogicException =>
//                indented_println("infeasible trail")
//
//            }
//            // TODO: use bound as whole trail bound summary
//          }
//      }
//      all_trail_string += "</ul>"
//      TrailRenderer.close(trailsWriter)
//
//      // node2bound.foreach{ (n, (t, b)) => println() }
//
//      // ****************************************************
//      // Some code that renders the trail along with the control tree
//      //val nodeBounds = BoundAnalysis.procedureSummary(context, controlTree)
//      val boundSummary = summarizeBoundsAndDisplay(
//        context, outDir, htmlWriter, spec, boundSummaries, controlTree, node2bound,
//        all_trail_string, Some(trailsWriter.getOutputFileName())
//      )
//      indented_println()
//      indented_println("function bounds " + boundSummary)
//      //indented_println("function bounds ATS: " + all_trail_string)
//      // indented_println("function polynomial " + PolynomialUtil.construct(boundSummary, context))
//      // ****************************************************
//
//
//      // TODO: memoize procedure summary
//      // val ret_m = m.filter(isParRet)
//      // map += (node ->
//      //   ret_m.copy(state = ret_m.state.joinAllStates())
//      // )
//
//      // memoize bound summary
//      boundSummaries += (funname -> boundSummary)
//
//      deindent()
//      indented_println("leaving " + funname)
//      indented_println()
//
//      boundSummary
//    }
//
//    val procedureBounds = traverseCallGraph(node)
//    // println("final bounds: " + funBounds)
//    // println("final polynomial: " + PolynomialUtil.construct(funBounds, context))
//
//    procedureBounds
//  }
//
//  def interprocedural(
//    node: CGNode,
//    outDir: Path,
//    htmlWriter: PrintWriter
//  )(implicit conf: AInterpConfig): BoundExpression = {
//    Log.println("enter interprocedural")
//    // mapping from call to ainterp summary
//    var map = scala.collection.mutable.Map[CGNode,Machine]()
//
//    // mapping from call to bound summary
//    var boundSummaries = scala.collection.mutable.Map[String,BoundExpression]()
//
//    // TODO: load libraries of bounds
//    // FunctionBounds.loadLibraries(boundSummaries)
//
//    var visited = List[CGNode]()
//    def traverseCallGraph(node: CGNode) : BoundExpression = {
//      visited ::= node
//
//      Log.println("enter traverseCallGraph")
//
//      val funname  = node.getMethod.getSignature()
//
//      indented_println("entering " + funname)
//      indent()
//
//      var callees = GlobalHeap.cg.getSuccNodes(node).toList
//      val childbounds = callees.map { callee =>
//        val sig = callee.getMethod.getSignature()
//        val loader = callee.getMethod.getDeclaringClass.getClassLoader.getName.toString()
//
//       if(! visited.contains(callee) &&
//          !SpecialLibHandling.libs.contains(sig) &&
//          loader.equals("Application")) {
//          traverseCallGraph(callee)
//        }
//      }.toList
//
//      ifDebug{Log.println("child bounds " + childbounds)}
//
//      val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)
//      val spec     = Specification.getSpecs.get( funname )
//
//      // ****************************************************
//      // load handy aliases
//      val ir : WALA.IR = wcontext.ir
//      val method       = ir.getMethod()
//      val cfg          = ir.getControlFlowGraph()
//
//      // ****************************************************
//      // build Control Tree
//      val controlTree = (new ASTNodeAny(null, null, null))
//        .buildLoopTreeFromCFG(wcontext.cfg.entry(), wcontext)
//      ifDebug {Log.println("\ncontrol tree\n" + controlTree.toString(1))}
//
//      var context = InterpContext(new EvalContext(), wcontext)
//
//      // ****************************************************
//      // TODO: optionally get summary spec
//      // val m = spec match {
//      //   case Some(ss) =>
//      //     // val Specification(pre,post,bound) = ss
//      //   case None =>
//      // }
//
//      // TODO: optionally get bound from spec
//      val bspec = spec match {
//        case Some(ss) =>
//          ss.toString()
//        case None => "(no spec)"
//      }
//      //     //val Specification(pre,post,bound) = ss
//      //     ss.bound
//      //   case None =>
//      //     BoundAnalysis.funSummary(context, dotwriter)
//      // }
//
//      indented_println("computing bounds for " + funname)
//      indented_println("spec " + bspec)
//
//      // build control tree and compute bound summary
//      val nodeBounds = BoundAnalysis.procedureSummary(context, controlTree)
//      var nodeBoundsHack = Map[WALA.CFGNode, List[(Trail,BoundExpression)]]()
//
//      nodeBounds.foreach{ case (n, b) =>
//        b.foreach{b =>
//          nodeBoundsHack += n -> List((List[Trailspec](), b))
//        }
//      }
//      val boundSummary = summarizeBoundsAndDisplay(
//        context, outDir, htmlWriter, spec, boundSummaries, controlTree, nodeBoundsHack, "n/a", None
//      )
//
//      indented_println()
//      indented_println("function bounds " + boundSummary)
//      indented_println("function polynomial " + PolynomialUtil.construct(boundSummary, context))
//
//      // TODO: memoize procedure summary
//      // val ret_m = m.filter(isParRet)
//      // map += (node ->
//      //   ret_m.copy(state = ret_m.state.joinAllStates())
//      // )
//
//      // memoize bound summary
//      boundSummaries += (funname -> boundSummary)
//
//      deindent()
//      indented_println("leaving " + funname)
//      indented_println()
//
//      boundSummary
//    }
//
//    val procedureBounds = traverseCallGraph(node)
//    // println("final bounds: " + funBounds)
//    // println("final polynomial: " + PolynomialUtil.construct(funBounds, context))
//
//    procedureBounds
//  }
//
//  /**
//    * This method gets the transitive closure by ghosting the intial
//    * machine (after processing the head node's phis) and following
//    * the trail until the end.
//    */
//  def getTCBound(
//    context: InterpContext,
//    start_m:AbstractMachine,
//    start_bound:BoundExpression,
//    trail:Trail.Trail,
//    last_cfg_node:WALA.CFGNode,
//    allloopinfo:Map[WALA.CFGNode, WALA.LoopStructure],
//    n2b : scala.collection.mutable.Map[WALA.CFGNode, List[(Trail,BoundExpression)]],
//    varAccum:scala.collection.mutable.Map[Trail.Trailspec, List[String]]
//  ) : (AbstractMachine, BoundExpression) = {
//    val InterpContext(econtext, wcontext) = context
//    val cfg = wcontext.cfg
//    val ir = wcontext.ir
//
//    // the running accumulation of the transitive closure
//    var cur_m = start_m
//
//    // the running accumulation of the bound
//    var cur_bound = start_bound
//
//    // the previous cfg node
//    var last = last_cfg_node
//
//    indented_println("getTCBound on trail " + Trail.toStringRE(trail))
//    indent()
//    val trailq = Queue(trail:_*)
//    while (! trailq.isEmpty()) {
//      val t = trailq.dequeue()
//      t match {
//        case Repeat(repeat_t,s) => s match { // assert repeat_t has backedge to t.head
//          case SFix() =>
//            indented_println("processing Repeat: " + Trail.toStringRE(t))
//            // assume Repeats start with Node
//            val head_trail_node = repeat_t.head.asInstanceOf[Trail.Node]
//            val head_cfg_node = head_trail_node.curr
//
//            val last_trail_node = repeat_t.last.asInstanceOf[Trail.Node]
//            val last_cfg_node = last_trail_node.curr
//
//            indented_println("getting one_step machine " + Trail.toStringRE(repeat_t))
//            var (m_one_step, transition_bound) =
//              getTCBound(context, cur_m, cur_bound, repeat_t, last, allloopinfo, n2b, varAccum)
//            // if (transition_bound.isInstanceOf[BoundExpressionNaN]) {
//            //   val cur_bound_list = (n2b get head_cfg_node) match {
//            //     case Some(l_tr_be) => l_tr_be
//            //     case None => List[(Trail,BoundExpression)]()
//            //   }
//            //   n2b(head_cfg_node) = ((repeat_t, transition_bound) :: cur_bound_list)
//            //   return (cur_m, transition_bound)
//            // }
//            // m_one_step = m_one_step.stepPhis(context, last_cfg_node)
//
//            val loopinfo = allloopinfo.get(head_cfg_node).get
//
//            // Follow exit trail
//            indented_println("getting exit machine")
//            val (m_after, exit_node) = {
//              // assumes trail has at least one or more Node trailspecs after
//              // Repeat that form the exit rail
//              val (repeat_m, repeat_last) = stepTrailspec(context, cfg, ir, cur_m, t, last, varAccum)
//              var tmp_m = repeat_m
//              var tmp_last = repeat_last
//              if (trailq.isEmpty()) {
//                throw new RuntimeException("invalid trail")
//              }
////              var tmp_m = repeat_m.stepPhis(context, repeat_last)
//              var next_trail = t
//              var stop = false
//
//              while (! stop) {
//                next_trail = trailq.dequeue()
//                indented_println("tmp_last " + tmp_last)
//                indented_println("next_trail to process " + Trail.toStringRE(next_trail))
//                val (updated_m, updated_last) = stepTrailspec(context, cfg, ir, tmp_m, next_trail, tmp_last, varAccum)
//                indented_println("finished next_trail")
//                tmp_last = updated_last
////                tmp_m = updated_m.stepPhis(context, tmp_last)
//                tmp_m = updated_m
//                // once we reach an exit edge for the loop, stop
//                if (! loopinfo.nodeset.contains(next_trail.asInstanceOf[Trail.Node].next))
//                  stop = true
//              }
//              (tmp_m, tmp_last)
//            }
//
//            // val (_, _, old_post_machine) = getInvariants(head_cfg_node, loopinfo, context)
//            // val post_machine = old_post_machine
//            val post_machine = m_after
//
//            def processNextPhis(
//              m: AbstractMachine,
//              next: WALA.CFGNode,
//              cur: WALA.CFGNode
//            ) : AbstractMachine = {
//              var p = m.newFrame
//      	      val o = p.stack.head
//      	      val n = o.copy(pc = WALA.IAddr(ir, next, next.getFirstInstructionIndex()))
//      	      p = p.copy( stack = n :: p.stack)
//              p = p.stepPhis(context, cur)
//              p
//            }
//
//            // process phis of the loop header
//            val pre_machine = processNextPhis(cur_m, head_cfg_node, last)
//            val one_step_machine = processNextPhis(m_one_step, head_cfg_node, last_cfg_node)
//
//            // align machines for consistent mapping
//            val alignedtrans :: alignedpost :: alignedpre :: Nil = AbstractMachine.alignMany(
//              one_step_machine, post_machine, pre_machine
//            )(init = true)
//
//            // get header phis
//            val header_phis = loopinfo.header.iteratePhis().map{phi =>
//              phi
//            }.toList
//
//            var bounds = List[BoundExpression]()
//
//            header_phis.map{ phi =>
//              val header_phi_num = phi.getDef().asInstanceOf[VarIndex]
//              val header_phi_label = alignedtrans.topFrame.localLabel(header_phi_num)
//              val bindings = alignedtrans.simpleContext.bindings
//              if (bindings.containsKey(header_phi_label)) {
//                val header_phi_dim = bindings.get(header_phi_label).get
//                // val prednum = cfg.getPredNodes(head_cfg_node).indexOf(last_cfg_node)
//                // val prime_phi_num = argsOfI(phi)(prednum)
//                // val prime_phi_label = alignedtrans.topFrame.localLabel(prime_phi_num)
//
//                val bound = matchLemmaNew(context, alignedpre, alignedpost,
//                  alignedtrans, transition_bound, header_phi_dim)
//
//                bounds ++= List[BoundExpression](bound)
//
//              } else {
//                Log.println("machine does not have header phi "
//                  + header_phi_label + " in " + head_cfg_node)
//              }
//            }
//
//            // TODO: need duplication of bounds
//
//            bounds = bounds.map{bound =>
//              bound.renameWithBinding(alignedtrans.simpleContext.bindings)
//            }
//
//            bounds.foreach{bound =>
//              indented_println("loop bound " + bound)
//            }
//
//            val real_bounds = bounds.filter{bound => ! bound.isInstanceOf[BoundExpressionNaN]}
//
//            val matched_bound =
//              if (real_bounds.size() > 0) {
//                Some(real_bounds.head)
//              } else if (bounds.size() > 0) {
//                Some(bounds.head)
//              } else {
//                None
//              }
//
//            matched_bound match {
//              case Some(bound) =>
//                cur_bound = new BoundExpressionSum(cur_bound, bound)
//              case None =>
//                Log.println("unable to find bounds for " + head_cfg_node)
//            }
//
//            // TODO: implement size tracking
//            // cur_m = trackSize(cur_m, cur_m, m_one_step, matched_bound)
//            cur_m = m_after
//
//
//            // add this bound to the hashmap
//            val cur_bound_list = (n2b get head_cfg_node) match {
//              case Some(l_tr_be) => l_tr_be
//              case None => List[(Trail,BoundExpression)]()
//            }
//            n2b(head_cfg_node) = ((repeat_t, cur_bound) :: cur_bound_list)
//
//            last = exit_node
//        }
//        case Trail.Node(from,to) =>
//          indented_println("processing Node: " + Trail.toStringRE(t))
//          val (new_tc, new_last) = stepTrailspec(context, cfg, ir, cur_m, t, last, varAccum)
//          cur_m = new_tc
//          last = new_last
//          cur_bound = new BoundExpressionSum(cur_bound, getBlockBound(context, from))
//      }
//    }
//    deindent()
//    indented_println("finished getTCBound")
//    indented_println("final bound " + cur_bound)
//    (cur_m, cur_bound)
//  }
//
//  /** Get the bound of a CFG block, including any interprocedural bound. */
//  def getBlockBound(
//    context: InterpContext,
//    block: WALA.CFGNode
//  ) :BoundExpression = {
//    val InterpContext(econtext, wcontext) = context
//
//    // TODO: get summary bound of interprocedural function calls
//
//    var sum = 0  // the number of non-function calls
//    var funcalls = List[String]()  // the names of function calls with arguments
//    for (idx <- block.getFirstInstructionIndex() to block.getLastInstructionIndex()) {
//      val (inst, _) = WALA.getInstruction(idx, wcontext.ir)
//
//      inst match {
//        case i: InvokeI => {
//          val funname = i.toString()
//          funcalls ++= List[String](funname)
//        }
//        case i => {
//          sum += 1
//        }
//      }
//    }
//
//    // add the constants to all the funcalls
//    funcalls.map{funcall =>
//      new BoundExpressionVarCustom(funcall)
//    }.foldLeft((new BoundExpressionConst(sum)).asInstanceOf[BoundExpression])(
//      (b, a) => new BoundExpressionSum(a, b)
//    )
//  }
//
////   /**
////     * This method gets the transitive closure by ghosting the intial
////     * machine (after processing the head node's phis) and following
////     * the trail until the end.
////     */
////   def getTCBoundHack(
////     context: InterpContext,
////     start_tc:AbstractMachine,
////     start_bound:BoundExpression,
////     trail:Trail.Trail,
////     last_cfg_node:WALA.CFGNode,
////     allloopinfo:Map[WALA.CFGNode, WALA.LoopStructure],
////     n2b : scala.collection.mutable.Map[WALA.CFGNode, List[(Trail,BoundExpression)]],
////     varAccum:scala.collection.mutable.Map[String, Boolean]
////   ) : (AbstractMachine, BoundExpression) = {
////     val InterpContext(econtext, wcontext) = context
////     val cfg = wcontext.cfg
////     val ir = wcontext.ir
//
////     // the running accumulation of the transitive closure
////     var cur_tc = start_tc
//
////     // the running accumulation of the bound
////     var cur_bound = start_bound
//
////     // the previous cfg node
////     var last = last_cfg_node
//
////     println("getTCBound on trail " + Trail.toStringRE(trail))
////     val trailq = Queue(trail:_*)
////     while (! trailq.isEmpty()) {
////       val t = trailq.dequeue()
////       println("processing trail t " + Trail.toStringRE(t))
//
////       t match {
////         case Repeat(repeat_t,s) => s match { // assert repeat_t has backedge to t.head
////           case SFix() =>
////             // assume Repeats start with Node
////             val head_trail_node = repeat_t.head.asInstanceOf[Trail.Node]
////             val head_cfg_node = head_trail_node.curr
//
////             val last_trail_node = repeat_t.last.asInstanceOf[Trail.Node]
////             val last_cfg_node = last_trail_node.curr
//
////             // use one iteration through the loop for the transition
////             // val finite_t = List[Trailspec](Trail.Repeat(repeat_t, Trail.SInt(1)))
////             val finite_t = List[Trailspec](Trail.Repeat(repeat_t, Trail.SInt(1)))
//
////             val useNewLemmaMatcher = true
//
////             // TODO: put this back when trail-based ainterp is working
////             // // do stepPhi on loop_head_cfg_node and ghost
////             // val init_tc = if (useNewLemmaMatcher)
////             //   cur_tc.stepPhis(context, last)
////             // else
////             //   cur_tc.stepPhis(context, last).duplicateVars
//
////             // var (transition_tc, transition_bound) =
////             //   getTCBound(context, init_tc, cur_bound, finite_t, last, allloopinfo, n2b)
////             var (transition_tc, transition_bound) =
////               getTCBoundHack(context, cur_tc, cur_bound, finite_t, last, allloopinfo, n2b, varAccum)
////             if (transition_bound.isInstanceOf[BoundExpressionNaN]) {
////               val cur_bound_list = (n2b get head_cfg_node) match {
////                 case Some(l_tr_be) => l_tr_be
////                 case None => List[(Trail,BoundExpression)]()
////               }
////               n2b(head_cfg_node) = ((repeat_t, transition_bound) :: cur_bound_list)
////               return (cur_tc, transition_bound)
////             }
//// //            transition_tc = transition_tc.stepPhis(context, last_cfg_node)
//
////             // use the old method to get pre/post invariants, ignoring
////             // trans (which getTCBound does instead)
////             val loopinfo = allloopinfo.get(head_cfg_node).get
//
////             // // TODO: use this once stepTrail can handle pre/post invariants
////             // Compute the fixpoint over the body of the Repeat
////             // // TODO: fix stepTrailspec to do Repeat(_, *) correctly with ainterp
////             // println("last " + last)
////             // println("t" + t)
////             // val (tmptc1, tmplast1) = stepTrailspec(context, cfg, ir, cur_tc, t, last)
////             // // val (tmptc1, tmplast1) = (cur_tc, last)
////             // if (trailq.isEmpty()) {
////             //   throw new RuntimeException("invalid trail")
////             // }
//
////             // // Compute the exit arc
////             // val (tmptc2, tmplast2) = stepTrailspec(context, cfg, ir, tmptc1, trailq.dequeue(), tmplast1)
//
////             // val loop_tc = tmptc2
////             // val loop_last = tmplast2
//
////             // val premachine = cur_tc
////             // val postmachine = loop_tc
//
////             // val (premachine, postmachine) = (init_tc, loop_tc)
////             val (premachine, _, postmachine) = getInvariants(head_cfg_node, loopinfo, context)
//
////             // align machines for consistent mapping
////             val alignedtrans :: alignedpost :: alignedpre :: Nil = AbstractMachine.alignMany(
////               transition_tc, postmachine, premachine
////             )(init = true)
//
////             val matched_bound = if (useNewLemmaMatcher)
////               matchLemmaNew(context, alignedpre, alignedpost, alignedtrans, transition_bound, loopinfo)
////             else
////               matchLemmaComputeBounds(context, alignedpre, alignedpost, alignedtrans, transition_bound, loopinfo)
//
////             // // TODO: replace with above once stepTrail on Repeat * is working
////             // val (loop_tc, loop_last) = stepTrailspec(context, cfg, ir, cur_tc, trailq.dequeue(), last)
////             // // TODO: remove this once we have well-formed trails with loop exit trail information
////             // val (loop_tc, loop_last) = {
////             //   var tmp_tc = cur_tc
////             //   var tmp_last = last
////             //   var next_trail = t
////             //   var stop = false
////             //   // assumes trail has at least one or more Node trailspecs after
////             //   // Repeat that form the exit rail
////             //   val (repeat_tc, repeat_last) = stepTrailspec(context, cfg, ir, tmp_tc, t, tmp_last)
////             //   tmp_tc = repeat_tc
////             //   tmp_last = repeat_last
//
////             //   while (! stop) {
////             //     next_trail = trailq.dequeue()
////             //     println("tmp_last " + tmp_last)
////             //     println("next_trail to process " + Trail.toStringRE(next_trail))
////             //     val (updated_tc, updated_last) = stepTrailspec(context, cfg, ir, tmp_tc, next_trail, tmp_last)
////             //     println("finished next_trail")
////             //     tmp_tc = updated_tc
////             //     tmp_last = updated_last
////             //     // once we reach an exit edge for the loop, stop
////             //     if (! loopinfo.nodeset.contains(next_trail.asInstanceOf[Trail.Node].next))
////             //       stop = true
////             //   }
////             //   (tmp_tc, tmp_last)
////             // }
//
////             // TODO: for now, just get last node, getInvariants gets
////             // postcondition
////             val (loop_tc, loop_last) = {
////               var tmp_last = last
////               var tmp_tc = cur_tc
////               var next_trail = t
////               var stop = false
////               // assumes trail has at least one or more Node trailspecs after
//
////               while (! stop && ! trailq.isEmpty()) {
////                 next_trail = trailq.dequeue()
////                 println("tmp_last " + tmp_last)
////                 println("next_trail " + next_trail)
////                 // tmp_tc = tmp_tc.stepPhis(context, tmp_last)
////                 tmp_last = next_trail.asInstanceOf[Trail.Node].curr
////                 if (! loopinfo.nodeset.contains(next_trail.asInstanceOf[Trail.Node].curr))
////                   stop = true
////               }
////               if (trailq.isEmpty())
////                 (tmp_tc, next_trail.asInstanceOf[Trail.Node].next)
////               else
////                 (tmp_tc, tmp_last)
////             }
//
////             // TODO: implement size tracking
////             // cur_tc = loop_tc
////             // cur_tc = trackSize(init_tc, cur_tc, transition_tc, matched_bound)
//
////             cur_bound = new BoundExpressionSum(cur_bound, matched_bound)
//
////             // add this bound to the hashmap
////             val cur_bound_list = (n2b get head_cfg_node) match {
////               case Some(l_tr_be) => l_tr_be
////               case None => List[(Trail,BoundExpression)]()
////             }
////             n2b(head_cfg_node) = ((repeat_t, cur_bound) :: cur_bound_list)
//
////             last = loop_last
////           case SInt(n) =>
////             try {
////               val (new_tc, new_last) = stepTrailspec(context, cfg, ir, cur_tc, t, last, varAccum)
////               cur_tc = new_tc
////               last = new_last
////               // TODO: this should probably be n * length of trail
////               cur_bound = new BoundExpressionSum(cur_bound, new BoundExpressionConst(n))
////             } catch {
////               case e: Core.LogicException =>
////                 return (cur_tc, new BoundExpressionNaN("impossible path"))
////             }
////         }
////         case Trail.Node(from,to) =>
////           // TODO do this once trail-based ainterp is working
////           // val (new_tc, new_last) = stepTrailspec(context, cfg, ir, cur_tc, t, last, varAccum)
////           // cur_tc = new_tc
////           // last = new_last
////           last = from
////           cur_bound = new BoundExpressionSum(cur_bound, new BoundExpressionConst(1))
////       }
////     }
////     (cur_tc, cur_bound)
////   }
//
//  def matchLemmaNew(
//    context : InterpContext,
//    init_state : AbstractMachine,
//    post_state : AbstractMachine,
//    one_step_state : AbstractMachine,
//    one_step_bound : BoundExpression,
//    header_phi_dim : DimIndex
//  ) : BoundExpression = {
//    val InterpContext(_, wcontext) = context
//
//    def getValue(m: AbstractMachine) : (Boolean, Boolean, Option[LinearArray]) = {
//      // println(m)
//      val spec = edgeSpecOfPoly(header_phi_dim, m.simpleContext.poly.rep)
//      // println(spec)
//      // pull out the loop var relations
//      val LoopEdgeSpec(_, loopvarRelations, otherRelations) = spec
//      // println(loopvarRelations)
//
//      if (loopvarRelations.size() == 0) {
//        return (false, false, None)
//      }
//
//      // look only at equality relations
//      // TODO: combine multiple equality relations
//      // val CommonLoopBounds.SolvedLinearConstraint(lhs_coeff, lhs_varindex, _, rhs_coefficients) =
//      //   loopvarRelations.filter{ case CommonLoopBounds.SolvedLinearConstraint(_, _, relation, _) =>
//      //     relation == Relation_Symbol.EQUAL
//      //   }.head
//      val CommonLoopBounds.SolvedLinearConstraint(lhs_coeff, lhs_varindex, _, rhs_coefficients) =
//        loopvarRelations.head
//
//      // determine type of init value, zero, constant, or expression
//      // TODO: handle lhs_coeff
//      val LinearArray (constantCoefficient: Coeff, linearCoefficients: scala.Array[Coeff]) = rhs_coefficients
//      val nonzeros = linearCoefficients.zipWithIndex.filter{case (c,i) => c != 0}.map{case (c,i) => (c, i.toLong)}
//      val isZero = nonzeros.length == 0 && constantCoefficient == 0
//      val isConstant = nonzeros.length == 0 && constantCoefficient != 0
//
//      if (isZero || isConstant)
//        (isZero, isConstant, Some(rhs_coefficients))
//      else
//        (isZero, isConstant, Some(rhs_coefficients))
//    }
//
//    println(init_state)
//    println(one_step_state)
//    println(post_state)
//
//    // 1. get the init value, zero, constant, or expression
//    val (initZero, initConst, initValue) = getValue(init_state)
//    // println("initZero " + initZero)
//    // println("initConst " + initConst)
//    indented_println("initValue " + initValue)
//
//    // 2. get the trans value, zero, constant, or expression
//    val (transZero, transConst, transValue) = getValue(one_step_state)
//    // println("transZero " + transZero)
//    // println("transConst " + transConst)
//    indented_println("transValue " + transValue)
//
//    // 3. find the step by subtracting trans from init
//
//    initValue match {
//      case Some(initValue) =>
//      case None =>
//        return new BoundExpressionNaN("missing initial state")
//    }
//
//    transValue match {
//      case Some(transValue) =>
//      case None =>
//        return new BoundExpressionNaN("missing one_step_state")
//    }
//
//    // TODO: check whether trans and init are the same
//
//    // do the subtraction, using LinearArrays operators (subtraction doesn't appear to work)
//    // TODO: don't forget the lhs_coefficients from SolvedLinearConstraint!
//    val stepValue = transValue.get + initValue.get.neg
//
//    indented_println("stepValue " + stepValue)
//
//    // 4. get the post value, zero, constant, or expression
//
//    val loop_bound = {
//      val (postZero, postConst, postValue) = getValue(post_state)
//      // println("postZero " + postZero)
//      // println("postConst " + postConst)
//      indented_println("postValue " + postValue)
//
//      postValue match {
//        case Some(postValue) =>
//          // 5. get the range of the loop variable by subtracting post from
//          // init
//          val rangeValue = postValue + initValue.get.neg
//
//          new BoundExpressionDiv(
//            new BoundExpression().fromLinearArray(false)(rangeValue),
//            new BoundExpression().fromLinearArray(false)(stepValue))
//        case None =>
//          new BoundExpressionNaN("missing postcondition")
//      }
//    }
//
//    val bound = new BoundExpressionMult(
//      loop_bound,
//      one_step_bound
//    )
//
//    return bound
//  }
//
//  def matchLemmaComputeBounds(
//    context : InterpContext,
//    pre_tc : AbstractMachine,
//    post_tc : AbstractMachine,
//    trans_tc : AbstractMachine,
//    trans_bound : BoundExpression,
//    loopinfo : WALA.LoopStructure
//  ) : BoundExpression =
//  {
//    val InterpContext(_, wcontext) = context
//
//    // get header phis
//    val header_phis = loopinfo.header.iteratePhis().map{phi =>
//      phi.getDef().asInstanceOf[VarIndex]
//    }.toList
//
//    // TODO: do this for all header phis instead of header_phis.head,
//    // the first one
//    val header_phi_label = trans_tc.topFrame.localLabel(header_phis.head)
//    val header_phi_dim = trans_tc.simpleContext.bindings.get(header_phi_label).get
//
//    // iterate over ghosts
//    // for each: do all this
//
//    // TODO is it safe to just use simpleContext?
//    println(trans_tc.simpleContext.bindings)
//    println(trans_tc)
//
//    // get all loop variables
//
//    // get simpleContext for each AbstractMachine
//
//    // figure out which variables to remove: stuff not in the loop?
//    // don't forget to remove the ghost versions too
//
//    // TODO: use loop labels, i.e., lhs of (first) phi
//    val label = header_phi_label
//    val inlabel1 = trans_tc.topFrame.localLabel(1)
//    val inlabel2 = trans_tc.topFrame.localLabel(2)
//    val inlabel3 = trans_tc.topFrame.localLabel(3)
//    val ghost_label = Label.GhostLabel(label)  // need to get ghost
//
//    // TODO: rem_labels should be nonlooplabels!
//
//    val rem_labels = trans_tc.simpleContext.bindings.filter{ case (k, v) =>
//      (k != label && k != ghost_label && k != inlabel1 && k != inlabel2 && k != inlabel3)
//    }.map{ case (k, v) => k }.toList
//
//    println(rem_labels)
//
//    // val removed = trans_tc.simpleContext.removeDims(rem_labels)
//    // val removed = trans_tc.simpleContext.removeDims(List(inlabel2))
//    // val removed = trans_tc.simpleContext.filterDims(x => {!rem_labels.contains(x)})
//    val removed_tc = trans_tc.filter(x => {!rem_labels.contains(x)})
//
//    var ghost_pre_tc = pre_tc.duplicateVars()
//    // var ghost_pre_tc = pre_tc
//
//    val alignedremovedtrans :: alignedpost :: alignedpre :: Nil = AbstractMachine.alignMany(
//        removed_tc, post_tc, ghost_pre_tc
//    )(init = true)
//
//    print("pre")
//    print(ghost_pre_tc)
//    print("trans")
//    print(trans_tc)
//    print("post")
//    print(post_tc)
//
//    print("aligned pre")
//    print(alignedpre)
//    print("aligned removed trans")
//    print(alignedremovedtrans)
//    print("aligned post")
//    print(alignedpost)
//
//    // val removed = alignedremovedtrans.simpleContext
//
//    // remove the variable in the constraint
//
//    val bindings = alignedremovedtrans.simpleContext.bindings
//
//    println("aligned.removed.bindings " + alignedremovedtrans.simpleContext.bindings)
//    println("aligned.pre.bindings " + alignedpre.simpleContext.bindings)
//    println("aligned.post.bindings " + alignedpost.simpleContext.bindings)
//
//    println("trans_tc " + trans_tc)
//
//    val label_dim = bindings.get(label).get
//    val ghost_dim = bindings.get(ghost_label).get
//
//    println(label_dim)
//    println(ghost_dim)
//
//    // trans_tc.copy(linear = trans_tc.linear.removeDims(List(label))).alloc(label)
//
//    // TODO: handle special case for constants
//
//    // ****************************************************
//    // Set up for call to computeBounds
//    // val nodeset = loopinfo.nodeset
//    // val backedges = loopinfo.backedges
//    // val inedges = loopinfo.inedges
//    // val exitedges = loopinfo.exitedges
//    // val transexits = loopinfo.transexits
//    // val innerloopexits = loopinfo.innerloopexits
//    // val loopvars = loopinfo.loopvars
//    // val primevars = loopinfo.primevars            // Needs the ghost, dim(ghost
//    //
//    // val frame   = trans_tc.topFrame
//    // val trans_bindings = trans_tc.simpleContext.bindings
//    // val filtered_primevars = primevars
//    //   .filter{ case (v, u) => v != u }
//    //   .filter{ case (v, u) => ! wcontext.ir.getSymbolTable().isConstant(u) }
//    // ifDebug {println("\nfiltered_primevars: " + filtered_primevars)}
//    // val dimvars = filtered_primevars
//    //   .filter{ case (v, u) =>
//    //     (trans_bindings.get(frame.localLabel(v)) != None) &&
//    //     (trans_bindings.get(frame.localLabel(u)) != None)
//    // } .map{ case (v, u) =>
//    //     (trans_bindings.get(frame.localLabel(v)).get,
//    //       trans_bindings.get(frame.localLabel(u)).get)
//    // } .toList.asInstanceOf[List[(Core.DimIndex, Core.DimIndex)]]
//
//    val ghost_dimvars = alignedremovedtrans.simpleContext.bindings.filter{ case (k, v) =>
//      k match {
//        case Label.GhostLabel(_) => true
//        case _ => false
//      }
//    }.map{ case (ghostvar, ghostdim) => {
//      val Label.GhostLabel(primevar) = ghostvar
//      val primedim = alignedremovedtrans.simpleContext.bindings.get(primevar).get
//      (primedim, ghostdim)
//    }}.toList
//
//    println("edgeSpecOfPoly pre")
//    println(edgeSpecOfPoly(ghost_dim, alignedpre.simpleContext.poly.rep))
//
//    println("transSpecOfPoly ")
//    // println(removed)
//    println(alignedremovedtrans.simpleContext)
//
//    // println(transSpecOfPoly(label_dim, ghost_dim, removed.poly.rep))
//    println(transSpecOfPoly(ghost_dim, label_dim, alignedremovedtrans.simpleContext.poly.rep))
//
//    println("\nghost_dimvars")
//    println(ghost_dimvars)
//
//    println("edgeSpecOfPoly post")
//    println(edgeSpecOfPoly(label_dim, alignedpost.simpleContext.poly.rep))
//
//
//
//    // ifDebug {Log.println("\ndimvars: " + dimvars)}
//    // ifDebug {Log.println("\ncalling computeBounds")}
//
//    // determine relationships like ghost(v18) ~ v18
//    // TRANS_SPEC_OF_POLY
//
//    Core.debug = true
//    val bounds = ComputeBound.computeBounds(
//      LoopSummaryPolyhedra(
//        ghost_dimvars,  // ghost_dimvars ++ dimvars,
//        alignedpre.simpleContext.poly.rep,
//        // trans_tc.simpleContext.poly.rep,
//        alignedremovedtrans.simpleContext.poly.rep,
//        alignedremovedtrans.simpleContext.bindings,
//        alignedpost.simpleContext.poly.rep),
//      true
//    )
//
//    println("bounds " + bounds)
//
//    // hack, pick a singleton
//    if (bounds.length == 0)
//      new BoundExpressionConst(1)
//    else
//      bounds.head
//  }
//
//  /***
//    * Summarize a control tree, finding its abstract state and bounds.
//    */
//  def procedureSummary(
//    context: InterpContext,
//    controlTree: ASTNode
//  )(implicit conf: AInterpConfig) : Map[WALA.CFGNode, List[BoundExpression]] = {
//    val InterpContext(econtext, wcontext) = context
//    val allloopinfo: Map[WALA.CFGNode, WALA.LoopStructure] = WALA.getLoopStructure(wcontext);
//    val boundAnnotation = scala.collection.mutable.Map[WALA.CFGNode, List[BoundExpression]]()
//
//    ifDebug {Log.println(wcontext.ir.toString())}
//
//    // TODO: use return values to replace control tree nodes with
//    // transition summarizes (which themselves may be new control tree
//    // fragments
//    def traverseControlTree(node:ASTNode) : Unit = {
//      node match {
//        case ASTNodeLoop(fc,ns,head:WALA.CFGNode) =>
//          traverseControlTree(fc)
//          traverseControlTree(ns)
//          val loopinfo = allloopinfo.get(head).get
//          val (alignedpre, alignedtrans, alignedpost) = getInvariants(head, loopinfo, context)
//          // TODO set invariants to loop header map (may need ainterp
//          // edge invariant too for exits
//          val dbounds = getBounds(head, loopinfo, alignedpre, alignedtrans, alignedpost, context)
//          // TODO: do size tracking
//          boundAnnotation += (head -> dbounds)
//
//        case ASTNodeIf(fc,ns,wn:WALA.CFGNode) =>
//          traverseControlTree(fc)
//          traverseControlTree(ns)
//
//        case ASTNodeSeq(fc,ns,i:String) =>
//          traverseControlTree(fc)
//          traverseControlTree(ns)
//
//        case ASTNodeSLC(null,ns,wn:WALA.CFGNode) =>
//          traverseControlTree(ns)
//
//        case ASTNodeFunc(null,ns,wn:WALA.CFGNode) =>
//          traverseControlTree(ns)
//
//        case null =>
//          // base case
//
//        case default =>
//          throw new RuntimeException("unexpected node: " + node)
//      }
//    }
//
//    traverseControlTree(controlTree)
//
//    // TODO: return a summarized control tree (still need original for
//    // display though!)
//    boundAnnotation
//  }
//
//  /***
//   * For a given loop, get its pre, trans, and post invariants.
//   */
//  def getInvariants(
//    head: WALA.CFGNode,
//    loopinfo: WALA.LoopStructure,
//    context: InterpContext
//  ) : (Machine, Machine, Machine) = {
//    val InterpContext(econtext, wcontext) = context
//    val cfg = wcontext.cfg
//    val head = loopinfo.header
//    val nodeset = loopinfo.nodeset
//    val backedges = loopinfo.backedges
//    val inedges = loopinfo.inedges
//    val exitedges = loopinfo.exitedges
//    val transexits = loopinfo.transexits
//    val innerloopexits = loopinfo.innerloopexits
//    val loopvars = loopinfo.loopvars
//    val primevars = loopinfo.primevars
//
//    // disable ainterp debugging output
//    var oldDebug = Core.debug
//    Core.debug = false
//
//    // create initial machine with all loop vars as linear
//    val initm = initMachine(context)
//    val initedge = WALA.getEntryEdges(cfg).toList(0)
//
//    val methodmachines = stepRegion(context, initm, initedge, inedges)
//    // val outedges = WALA.getExitEdges(cfg)
//    // val methodmachines = stepRegion(context, initm, initedge, outedges)
//
//    var preinvariants = methodmachines
//      .filterKeys(inedges).map{ case (edge, machine) =>
//        // just interpret the phis in order to get the preconditions
//        // for the loop variables
//        var newmachine = machine.gotoTop(edge._2.asInstanceOf[WALA.CFGNode])
//        //.stepPhis(edge._1 .asInstanceOf[WALA.CFGNode])
//
//        // promote loopvars to put in polyhedron
//        primevars.foreach{ case (v, _) =>
//          newmachine = newmachine.promote(newmachine.topFrame.localLabel(v))
//        }
//        newmachine
//    }
//
//    if (preinvariants.size() == 0) {
//      // if there are no preinvariants (i.e., the loop is the first
//      // statement), just create an unconstrained machine
//      preinvariants = List(initm)
//    }
//    ifDebug {Log.println("\npreinvariants\n" + preinvariants)}
//
//    // var loopmachine = initm
//    val premachine = AbstractMachine.join(preinvariants).copy(name = "pre")
//    var loopmachine = premachine
//
//    // // initial all vars from precondition (except loop or primed vars perhaps?)
//    // for ((_, rhs) <- primevars) {
//    //   // loopmachine = loopmachine.alloc(initm.topFrame.localLabel(rhs))
//    //   // loopmachine = loopmachine.alloc(premachine.topFrame.localLabel(rhs))
//    //   if (! loopmachine.simpleContext.bindings.contains(premachine.topFrame.localLabel(rhs))) {
//    //     loopmachine = loopmachine.alloc(premachine.topFrame.localLabel(rhs))
//    //   }
//    // }
//    // // or just all vars from preconditions
//
//    for ((_, rhs) <- primevars) {
//      loopmachine = loopmachine.unbind(loopmachine.topFrame.localLabel(rhs))
//      loopmachine = loopmachine.alloc(loopmachine.topFrame.localLabel(rhs))
//    }
//
//    // // initial all vars from precondition (except loop or primed vars perhaps?)
//    // for ((k, v) <- premachine.simpleContext.bindings) {
//    //   if (! loopmachine.simpleContext.bindings.contains(k)) {
//    //     loopmachine = loopmachine.alloc(k)
//    //   }
//    // }
//
//    ifDebug {println("\nloopmachine\n" + loopmachine)}
//
//    // TODO improve computation of transition invariants in the
//    // presence of inner loops
//
//    // run ainterp on all backedges, stopping at exits and backedges
//    val stopedges = (transexits ++ innerloopexits).asInstanceOf[ISet[Edge]]
//
//    //do not perform join for computing transition machine
//    BoundAnalysis.doJoin = false
//    val transmachines = backedges.map( backedge =>
//      if (false) {
//        // the merging/widening version of ainterp
//        backedge -> stepRegion(context, loopmachine, backedge, stopedges)
//      } else {
//        // the no-merge version of ainterp
//        backedge -> stepRegionInner(context, loopmachine, backedge, transexits.asInstanceOf[ISet[Edge]], innerloopexits)
//      }
//    ).toMap
//    //perform join for all other computations
//    BoundAnalysis.doJoin = true
//
//    var transinvariants = backedges.filter{ backedge =>
//      transmachines(backedge).containsKey(backedge)
//    }.map( backedge =>
//      transmachines(backedge)(backedge).prePhi
//    )
//    // ifDebug {println("\ntransinvariants\n" + transinvariants)}
//
//    if (transinvariants.size() == 0) {
//      // if there are no transinvariants (i.e., the loop always
//      // exits), just create an unconstrained machine
//      transinvariants = ISet(initm)
//    }
//
//    val postinvariants = backedges.flatMap{ backedge =>
//      transmachines(backedge).filterKeys(exitedges).values.map{_.prePhi}
//    }
//    // ifDebug {println("\npostinvariants\n" + postinvariants)}
//
//    // restore debugging state
//    Core.debug = oldDebug
//
//    val joinedpre   = AbstractMachine.join(preinvariants).copy(name = "pre")
//
//    //start trimming the unrelevant variables in transition machine
//    val joinedtrans = AbstractMachine.join(transinvariants).copy(name = "trans")   //       val joinedtrans_tmp = AbstractMachine.join(transinvariants).copy(name = "trans")
//                                                                                   // val loop_labels = loopvars.map { v =>
//                                                                                   //   joinedtrans_tmp.topFrame.localLabel(v)
//                                                                                   // }.toSet[Label.Label]
//                                                                                   // val lhs_prime_labels = primevars.map { case (v, p) =>
//                                                                                   //   joinedtrans_tmp.topFrame.localLabel(v)
//                                                                                   // }
//                                                                                   // val rhs_prime_labels = primevars.map { case (v, p) =>
//                                                                                   //   joinedtrans_tmp.topFrame.localLabel(p)
//                                                                                   // }
//                                                                                   // val nonloop_labels = joinedtrans_tmp.simpleContext.bindings.forward.keys.toSet -- loop_labels -- lhs_prime_labels -- lhs_prime_labels
//                                                                                   // val joinedtrans = joinedtrans_tmp.mapState { s =>
//                                                                                   //   nonloop_labels.foldLeft(s) {
//                                                                                   //     case(s, label) =>
//                                                                                   //       s.copy(linear = s.linear.removeDims(List(label))).alloc(label)
//                                                                                   //   }
//                                                                                   // }
//                                                                                   //complete trimming the unrelevant variables in transition machine
//
//    val joinedpost  = AbstractMachine.join(postinvariants).copy(name = "post")
//
//    // ifDebug {
//    //   println("\njoinedtrans\n" + joinedtrans)
//    //   println("\njoinedpost\n" + joinedpost)
//    //   println("\njoinedpre\n" + joinedpre)
//
//    //   println("\nsimple joinedtrans\n" + joinedtrans.simpleContext)
//    //   println("\nsimple joinedpost\n"  + joinedpost.simpleContext)
//    //   println("\nsimple joinedpre\n"   + joinedpre.simpleContext)
//
//    //   println("\nsimple joinedtrans bindings\n" + joinedtrans.simpleContext.bindings)
//    //   println("\nsimple joinedpost bindings\n"  + joinedpost.simpleContext.bindings)
//    //   println("\nsimple joinedpre bindings\n"   + joinedpre.simpleContext.bindings)
//    // }
//
//    // align machines for consistent mapping
//    val alignedtrans :: alignedpost :: alignedpre :: Nil = AbstractMachine.alignMany(
//      joinedtrans, joinedpost, joinedpre
//    )(init = true)
//
//    ifDebug{
//      Log.println("\nalignedpre\n"   + alignedpre)
//      Log.println("\nalignedtrans\n" + alignedtrans)
//      Log.println("\nalignedpost\n"  + alignedpost)
//
//      Log.println("\nalignedpre simple poly\n"   + alignedpre.simpleContext.poly)
//      Log.println("\nalignedtrans simple poly\n" + alignedtrans.simpleContext.poly)
//      Log.println("\nalignedpost simplep poly\n" + alignedpost.simpleContext.poly)
//
//      Log.println("\nalignedpre simple bindings\n"   + alignedpre.simpleContext.bindings)
//      Log.println("\nalignedtrans simple bindings\n" + alignedtrans.simpleContext.bindings)
//      Log.println("\nalignedpost simple bindings\n"  + alignedpost.simpleContext.bindings)
//    }
//
//    (alignedpre, alignedtrans, alignedpost)
//  }
//
//  /***
//   * Compute the bounds for a given loop given its loopinfo and
//   * invariants.
//   */
//  def getBounds(
//    head: WALA.CFGNode,
//    loopinfo: WALA.LoopStructure,
//    alignedpre: Machine,
//    alignedtrans: Machine,
//    alignedpost: Machine,
//    context: InterpContext
//  ) : List[BoundExpression] = {
//    val InterpContext(econtext, wcontext) = context
//    val cfg = wcontext.cfg
//    val nodeset = loopinfo.nodeset
//    val backedges = loopinfo.backedges
//    val inedges = loopinfo.inedges
//    val exitedges = loopinfo.exitedges
//    val transexits = loopinfo.transexits
//    val innerloopexits = loopinfo.innerloopexits
//    val loopvars = loopinfo.loopvars
//    val primevars = loopinfo.primevars
//
//    val frame   = alignedtrans.topFrame
//    val bindings = alignedtrans.simpleContext.bindings
//    val filtered_primevars = primevars
//      .filter{ case (v, u) => v != u }
//      .filter{ case (v, u) => ! wcontext.ir.getSymbolTable().isConstant(u) }
//    ifDebug {println("\nfiltered_primevars: " + filtered_primevars)}
//    val dimvars = filtered_primevars
//      .filter{ case (v, u) =>
//        (bindings.get(frame.localLabel(v)) != None) &&
//        (bindings.get(frame.localLabel(u)) != None)
//    } .map{ case (v, u) =>
//        (bindings.get(frame.localLabel(v)).get,
//          bindings.get(frame.localLabel(u)).get)
//    } .toList.asInstanceOf[List[(Core.DimIndex, Core.DimIndex)]]
//
//    ifDebug {Log.println("\ndimvars: " + dimvars)}
//    ifDebug {Log.println("\ncalling computeBounds")}
//
//    // oldDebug = Core.debug
//    // Core.debug = false
//    val bounds = ComputeBound.computeBounds(
//      LoopSummaryPolyhedra(
//        dimvars,
//        alignedpre.simpleContext.poly.rep,
//        alignedtrans.simpleContext.poly.rep,
//        alignedtrans.simpleContext.bindings,
//        alignedpost.simpleContext.poly.rep)
//    )
//    // Core.debug = oldDebug
//
//    ifDebug {
//      Log.println("head " + head)
//      bounds.foreach{ b =>
//        Log.println("boundexpression " + b)
//        Log.println("polynomial " + PolynomialUtil.construct(b, context))
//      }
//    }
//
//    // instead of simple context, compute all combinations of bounds
//    // of the disjunctive power state: machine.state.states of types
//    // Machine.PowerState.List[AbstractState]
//    var dbounds = ListBuffer[List[BoundExpression]]()
//    var alldbounds = List[BoundExpression]()
//
//    alldbounds ++= bounds
//
//    var allcombos : ListBuffer[(AbstractState, AbstractState, AbstractState)]
//    = ListBuffer[(AbstractState, AbstractState, AbstractState)]()
//    for (pre <- alignedpre.state.states) {
//      for (trans <- alignedtrans.state.states) {
//        for (post <- alignedpost.state.states) {
//          allcombos.+=( (pre, trans, post) )
//        }
//      }
//    }
//
//    def getListsFromTuples(tuples: List[(VarIndex, VarIndex)]) : List[List[VarIndex]] = {
//      def combineLists(list1: List[VarIndex], list2: List[VarIndex]) : List[VarIndex] = {
//        if ((list1.toSet & list2.toSet).size >= 1) {
//          return (list1.toSet | list2.toSet).toList
//        } else {
//          return list1
//        }
//      }
//
//      if (tuples.length <= 0) {
//        return List()
//      } else if (tuples.length == 1) {
//        return List(List(tuples.head._1, tuples.head._2).sorted)
//      } else {
//        var tailLists = getListsFromTuples(tuples.tail)
//        var headList = List(tuples.head._1, tuples.head._2).sorted
//        if (tailLists.filter{ x => (x.toSet & headList.toSet).size >= 1 }.length >= 1) {
//          return tailLists.map{ x => combineLists(x, headList).sorted}
//        } else {
//          return tailLists:::List(headList)
//        }
//      }
//
//      return List()
//    }
//
//    val looplabels = loopvars.map { v =>
//      frame.localLabel(v)
//    }.toSet[Label.Label]
//
//    val primelabels = primevars.filter{ case (v, p) =>
//      loopvars.contains(p)
//    }.map { case (v, p) =>
//        frame.localLabel(v)
//    }
//
//    val labelEquivalence = getListsFromTuples(primevars.toList).map{x => frame.localLabel(x.head) -> x.tail.map{y => frame.localLabel(y)}}.toMap
//
//    ifDebug{
//      Log.println(labelEquivalence)
//      Log.println(loopvars)
//      Log.println(primevars)
//      Log.println(looplabels)
//      Log.println(primelabels)
//      Log.println("loppvarsequiv")
//    }
//
//    val nonlooplabels = bindings.forward.keys.toSet -- looplabels -- primelabels
//
//    var pLabels = primevars.map{ case (x,y) => (frame.localLabel(x),frame.localLabel(y))}
//
//    allcombos.map{ case (pre, trans, post) =>
//      var bTrans : AbstractState = trans
//      labelEquivalence.toList.foreach{ k =>
//        if (bTrans.dispatch.contains(k._1)) {
//          k._2.foreach{ l =>
//            if (!(pLabels.contains((k._1,l)) || pLabels.contains((l,k._1)))) {
//              if (bTrans.dispatch.contains(l)) {
//                val exp = Expression.Binop(
//                  Expression.Term[Term](Term.Variable(k._1)),
//                  Operator.NumericRelation.==,
//                  Expression.Term[Term](Term.Variable(l))
//                )
//                val cexp = Exp.close(exp, bTrans.dispatch)
//                bTrans = bTrans.assume(cexp).head
//              }
//            }
//          }
//        }
//      }
//
//      var bPost : AbstractState = post
//      labelEquivalence.foreach{k =>
//        if (bPost.dispatch.contains(k._1)) {
//          k._2.foreach{ l =>
//            if (!(pLabels.contains((k._1,l)) || pLabels.contains((l,k._1)))) {
//              if (bPost.dispatch.contains(l)) {
//                val exp = Expression.Binop(
//                  Expression.Term[Term](Term.Variable(k._1)),
//                  Operator.NumericRelation.==,
//                  Expression.Term[Term](Term.Variable(l))
//                )
//                val cexp = Exp.close(exp, bPost.dispatch)
//                bPost = bPost.assume(cexp).head
//              }
//            }
//          }
//        }
//      }
//
//      val isNLLdisjoint = trans.isDisjointOn(post, nonlooplabels)
//      val isALLdisjoint = trans.isDisjoint(bPost)
//
//      ifDebug{
//        Log.println("nonlooplabels " + nonlooplabels)
//        Log.println("trans " + trans.linear)
//        Log.println("btrans " + bTrans.linear)
//        Log.println("post " + post.linear)
//        Log.println("bpost " + bPost.linear)
//        Log.println("isNLLdisjoint " + isNLLdisjoint)
//        Log.println("isALLdisjoint " + isALLdisjoint)
//        Log.println()
//      }
//
//      (pre, trans, post, !isNLLdisjoint && isALLdisjoint)
//    }.foreach{ case (pre_in, trans_in, post_in, upper) =>
//        // project polyhedra to only the loop vars
//        val loop_labels = loopvars.map { v =>
//          alignedtrans.topFrame.localLabel(v)
//        }.toSet[Label.Label]
//        val lhs_prime_labels = primevars.map { case (v, p) =>
//          alignedtrans.topFrame.localLabel(v)
//        }
//        val rhs_prime_labels = primevars.map { case (v, p) =>
//          alignedtrans.topFrame.localLabel(p)
//        }
//        val nonloop_labels = alignedtrans.simpleContext.bindings.forward.keys.toSet -- loop_labels -- lhs_prime_labels -- lhs_prime_labels
//
//        val trans_projected = nonloop_labels.foldLeft(trans_in) {
//          case(trans_in, label) =>
//            trans_in.copy(linear = trans_in.linear.removeDims(List(label))).alloc(label)
//        }
//
//        // // messy alignment
//        // val (pre1, trans1) = pre_in.align(trans_projected)
//        // val (trans2, post1) = trans1.align(post_in)
//        // val (post2, pre2) = post1.align(pre1)
//        // val pre = pre2
//        // val trans = trans2
//        // val post = post2
//
//        // // this for some reason yields extra bounds!
//        // val (trans, post :: pre :: Nil) = AbstractState.alignTo(
//        //   trans_projected, post_in :: pre_in :: Nil
//        // )
//
//        // pre and post are already aligned, just need to re-align
//        // trans, because we removed dims above
//        val (pre, trans) = pre_in.align(trans_projected)
//        val post = post_in
//
//        // println("disjoint pre " + pre)
//        // println("disjoint trans " + trans)
//        // println("disjoint post " + post)
//
//        // println("dimvars before " + dimvars)
//
//        val disjunct_bindings = trans.linear.bindings
//        val disjunct_dimvars = filtered_primevars
//          .filter{ case (v, u) =>
//            (disjunct_bindings.get(frame.localLabel(v)) != None) &&
//            (disjunct_bindings.get(frame.localLabel(u)) != None)
//        } .map{ case (v, u) =>
//            (disjunct_bindings.get(frame.localLabel(v)).get,
//              disjunct_bindings.get(frame.localLabel(u)).get)
//        }.toList.asInstanceOf[List[(Core.DimIndex, Core.DimIndex)]]
//
//        // println("dimvars after " + disjunct_dimvars)
//
//        ifDebug{
//          Log.println("\ncomputing disjunctive bounds")
//          Log.println(upper)
//          Log.println(pre.linear)
//          Log.println(trans.linear)
//          Log.println(trans.linear.bindings)
//          Log.println(post.linear)
//          Log.println()
//        }
//
//        // oldDebug = Core.debug
//        // Core.debug = false
//        val dbound = ComputeBound.computeBounds(
//          LoopSummaryPolyhedra(
//            disjunct_dimvars,
//            pre.linear.poly.rep,
//            trans.linear.poly.rep,
//            trans.linear.bindings,
//            post.linear.poly.rep),
//          upper
//            // true
//        )
//        // Core.debug = oldDebug
//        ifDebug {
//          dbound.foreach{ b =>
//            Log.println("boundexpression " + b)
//          }
//          dbound.map{ b =>
//            PolynomialUtil.construct(b, context)
//          }.toSet[Polynomial].foreach{ p =>
//            Log.println("polynomial " + p)
//          }
//          Log.println()
//        }
//        dbounds += dbound
//        alldbounds ++= dbound
//    }
//
//    ifDebug {
//      val allpolynomials : ISet[Polynomial] = alldbounds.map{ b =>
//        PolynomialUtil.construct(b, context)
//      }.toSet[Polynomial]
//
//      Log.println("all bounds, flattened")
//      alldbounds.foreach{ b =>
//        Log.println("boundexpression " + b)
//      }
//      allpolynomials.foreach { p =>
//        Log.println("polynomial " + p)
//      }
//      Log.println()
//    }
//
//    alldbounds
//  }
//
//  // TODO: factor out bound summary from display
//  /***
//   * Summarize bounds and display control trees.
//   */
//  def summarizeBoundsAndDisplay(
//    context: InterpContext,
//    outDir: Path,
//    htmlWriter: PrintWriter,
//    spec: Option[Specification],
//    boundSummaries: Map[String,BoundExpression],
//    controlTree: ASTNode,
//    node2bound : Map[WALA.CFGNode, List[(Trail,BoundExpression)]],
//    trailStr:String,
//    trailsDiag:Option[String]
//  )(implicit conf: AInterpConfig): BoundExpression = {
//    val InterpContext(econtext, wcontext) = context
//    val funname = wcontext.method.getName().toString()
//    val sig = wcontext.method.getSignature()
//    ifDebug {Log.println("function name: "+funname)}
//
//    /**
//      * Compare bounds x and y, -1 if x < y, 0 if x == y, 1 if x > y
//      */
//    def compareBounds(x: Polynomial, y: Polynomial, context: InterpContext): Int = {
//
//      // TODO take very large and very small input parameters into account
//      val adeg = x.degree()
//      val bdeg = y.degree()
//
//      // if (adeg == bdeg) {
//      //   if (0 == adeg) {
//      //     x.constant.compareTo(y.constant)
//      //   } else {
//      //     val amax = x.maxTerm()
//      //     val bmax = y.maxTerm()
//      //     x.terms.get(amax).compareTo(y.terms.get(bmax))
//      //   }
//      // } else {
//      adeg - bdeg
//      // }
//    }
//
//    htmlWriter.write("<div style=\"padding: 10px; border: 1pt solid black;\">\n")
//    htmlWriter.write("<h2>"+funname+"</h2>\n");
//
//    val dotWriter = new DotWriter(funname, outDir)
//    dotWriter.write("digraph g {\n")
//
//    val polynomialbounds = node2bound.map{ case (h, blist) =>
//      val bset = blist.map{ tr_b => /* ignore the trail tr */
//        PolynomialUtil.construct(tr_b._2, context).toString
//      }.toSet[String]
//
//      (h.toString, bset)
//    }
//    polynomialbounds.keys.toList.sorted.foreach { k =>
//      indented_println()
//      indented_println("loop bounds " + k)
//      polynomialbounds(k).toList.sorted.foreach { p =>
//        indented_println("loop polynomial " + p)
//      }
//    }
//
//    val allBexpr = FunctionBounds.summarize(controlTree,node2bound,spec,funname,sig,dotWriter,htmlWriter,boundSummaries,context)
//    dotWriter.write("}\n")
//
//    dotWriter.close()
//
//    trailsDiag match {
//      case Some(fn) =>
//        htmlWriter.write(FunctionBounds.htmlExpandable("Trails Diagram","<img src=\""+fn+"\">\n"))
//      case None =>
//    }
//
//    htmlWriter.write("<b>Trail Bounds:</b>\n"+trailStr+"\n")
//    htmlWriter.write(FunctionBounds.htmlExpandable("Control Tree Diagram","<img src=\""+dotWriter.getOutputFileName()+"\">\n"))
//    //htmlWriter.write(FunctionBounds.htmlExpandable("Trail","<pre>\n"+trailStr+"</pre>\n"))
//
//    htmlWriter.write("</div>")
//
//    def liftMax(
//      b: BoundExpression,
//      context: InterpContext
//    ) : ISet[BoundExpression] = {
//      def expand(
//        xin: BoundExpression,
//        yin: BoundExpression,
//        context: InterpContext) : ISet[BoundExpression] = {
//        val xs = liftMax(xin, context)
//        val ys = liftMax(yin, context)
//        // TODO: trim infeasible paths
//          (for {
//            x <- xs
//            y <- ys
//            val n = new BoundExpressionMult(x, y)
//          } yield n).toSet[BoundExpression]
//      }
//      b match {
//        case i: BoundExpressionConst =>
//          ISet(i)
//        case i: BoundExpressionVar =>
//          ISet(i)
//        case i: BoundExpressionVarLabel =>
//          ISet(i)
//        case c: BoundExpressionConstC =>
//          ISet(c)
//        case i: BoundExpressionPolynomial =>
//          ISet(i)
//        case i: BoundExpressionMult =>
//          expand(i.x, i.y, context)
//        case i: BoundExpressionDiv =>
//          expand(i.x, i.y, context)
//        case i: BoundExpressionSum =>
//          expand(i.x, i.y, context)
//        case i: BoundExpressionDiff =>
//          expand(i.x, i.y, context)
//        case i: BoundExpressionMax =>
//          ISet(i.x, i.y)
//        case i: BoundExpressionMin =>
//          throw new RuntimeException("min expression not yet implemented " + b);
//        case i: BoundExpressionPow =>
//          throw new RuntimeException("pow expression not yet implemented " + b);
//        case i: BoundExpressionLog =>
//          throw new RuntimeException("log expression not yet implemented " + b);
//        case default =>
//          throw new RuntimeException("bound expression not yet implemented " + b);
//      }
//    }
//
//    // // take all combinations of possible bounds and compare them
//    // // TODO: preserve path conditions in loopSummary above
//    // val lifted_polys = liftMax(allBexpr, context).map{ b => PolynomialUtil.construct(b, context) }
//    // // lifted_polys.foreach{ p =>
//    // //   println("p " + p)
//    // // }
//
//    // // TODO: perform this on all top-level loops instead?
//
//    // // TODO: divide bounds by secret/tainted/untainted, compare
//    // // tainted with secret, etc
//
//    // // pairwise comparison of bounds
//    // // TODO: use a partial ordering to reduce number of comparisons
//    // val diffs = Set[Polynomial]()
//    // lifted_polys.toSeq.combinations(2).foreach{ case Seq(x, y) =>
//    //   // TODO print path conditions for difference, program locations of difference, loop header
//    //   if (compareBounds(x, y, context) != 0) {
//    //     diffs += x
//    //     diffs += y
//    //   }
//    // }
//
//    // if (diffs.size > 0) {
//    //   indented_println("potential attack")
//    //   diffs.foreach{ x =>
//    //     indented_println(x.toString.replace(context.wala.ir.getMethod().getSignature().toString() + ":", ""))
//    //   }
//    //   indented_println()
//    // }
//
//    allBexpr
//  }
//}

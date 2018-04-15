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
//object Loops {
//  
//  var doJoin = true
//  
//  var indent_level = 0
//
//  def indent() { indent_level += 1 }
//
//  def deindent() { indent_level -= 1 }
//
//  def indented_println(s : String = "") {
//    Log.print(" | " * indent_level)
//    Log.println(s)
//  }
//
//  def interprocBounds(
//    node: CGNode,
//    outDir: Path,
//    htmlWriter: PrintWriter,
//    use_ct: Boolean
//  )(implicit conf: AInterpConfig): BoundExpression = {
//
//    //the mapping from the call graph node to its summary machine
//    var map = scala.collection.mutable.Map[CGNode,Machine]()
//    //the mapping from the call graph node to its bound summary
//    var mapBound = scala.collection.mutable.Map[String,BoundExpression]()
//    //load libraries of bounds
//    //FunctionBounds.loadLibraries(mapBound)
//
//    var visited = List[CGNode]()
//
//    def getFunSum(node: CGNode) : BoundExpression = {
//      visited ::= node
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
//        if(! visited.contains(callee) &&
//          !SpecialLibHandling.libs.contains(sig) &&
//          loader.equals("Application")) {
//          getFunSum(callee)
//        }
//      }.toList
//
//      ifDebug{Log.println("child bounds " + childbounds)}
//
//      val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)
//      val spec     = Specification.getSpecs.get( funname )
//
//
//      // ****************************************************
//      // call oneStep
//      // returns: List[(List[WALA.CFGNode],AbstractMachine)] =
//      val ir : WALA.IR = wcontext.ir
//      val method       = ir.getMethod()
//      val cfg          = ir.getControlFlowGraph()
//      var context      = InterpContext(new EvalContext(), wcontext)
//      val initm        = initMachine(context)
//
//      // Get a machine either from spec or AInterp
//      // var oldDebug = Core.debug
//      // var context = InterpContext(new EvalContext(), wcontext)
//      // Core.debug = false
//      // val m = spec match {
//      //   case Some(ss) =>
//      //     //val Specification(pre,post,bound) = ss
//      //     // TODO : create a machine from pre/post
//      //     AInterp.solveMethod(context)
//      //   case None =>
//      //     AInterp.solveMethod(context)
//      // }
//      // Core.debug = oldDebug
//
//      
//      // Get a bounds either from spec or funSummary
//      val bspec = spec match {
//        case Some(ss) =>
//          ss.toString()
//        case None => "(no spec)"
//      }
//      //     //val Specification(pre,post,bound) = ss
//      //     ss.bound
//      //   case None =>
//      //     Loops.funSummary(context, dotwriter)
//      // }
//      indented_println("computing bounds for " + funname)
//      indented_println("spec " + bspec)
//
//      val b_funsum = Loops.funSummary(context, outDir, htmlWriter, spec, mapBound)
//      val b_ctsum = Loops.ctSummary(context, outDir, htmlWriter, spec, mapBound)
//
//      val b = if (use_ct) b_ctsum else b_funsum
//
//      indented_println()
//      indented_println("function bounds " + b)
//      indented_println("function polynomial " + PolynomialUtil.construct(b, context))
//      // *******************************
//      // memoize procedure summary
//      //val ret_m = m.filter(isParRet)
//      //map += (node ->
//        //ret_m.copy(state = ret_m.state.joinAllStates())
//      //)
//
//      // *******************************
//      // memoize bound summary
//      mapBound += (funname -> b)
//
//      deindent()
//      indented_println("leaving " + funname)
//      indented_println()
//
//      b
//    }
//
//    val funBounds = getFunSum(node)
//
//    // println("final bounds: " + funBounds)
//    // println("final polynomial: " + PolynomialUtil.construct(funBounds, context))
//
//    funBounds
//  }
//
//  class LoopInfo(
//      /** the loop header */
//      val header: WALA.CFGNode,
//
//      /** the set of nodes comprising the loop, including the header */
//      val nodeset: ISet[WALA.CFGNode],
//
//      /** backedges to loop header */
//      val backedges: ISet[(WALA.CFGNode, WALA.CFGNode)],
//
//      /** in edges to loop header */
//      val inedges: ISet[Edge],
//
//      /** exit edges from loop header */
//      val exitedges: ISet[Edge],
//
//      /** loop exits and backedges */
//      val transexits: ISet[Edge],
//
//      /** inner loop backedges */
//      val innerloopexits: ISet[Edge],
//
//      /** vars on lhs of all instructions */
//      val loopvars: ISet[VarIndex],
//
//      /** phi nodes lhs, rhs mappings */
//      val primevars: ISet[(VarIndex, VarIndex)]
//    ) {
//  }
//
//  /***
//   * For each loop header in a given method, find its nodeset,
//   * relevant edges, and loop variables.
//   */
//  def getLoopInfo(
//    context: InterpContext
//  )(implicit conf: AInterpConfig) :
//      Map[WALA.CFGNode, LoopInfo] = {
//
//    val InterpContext(econtext, wcontext) = context
//    val allbackedges = WALA.getBackEdges(wcontext.ir)
//    ifDebug {Log.println("allbackedges " + allbackedges)}
//    val heads = allbackedges.map{ case (_, head) => head }
//    ifDebug {Log.println("heads " + heads)}
//
//    val cfg = wcontext.cfg
//
//    // find loop information, nodeset, edges, variables, etc
//    heads.asInstanceOf[ISet[WALA.CFGNode]].map{ head =>
//      ifDebug {Log.println("processing loop head " + head)}
//
//      val backedges = allbackedges.filter{ case(_, to) => to == head }
//      ifDebug {Log.println("backedges " + backedges)}
//
//      // union natural loop sets for all backedges, treating them as a
//      // single loop
//      var nodeset = ISet[WALA.CFGNode]()
//      backedges.foreach{ case(m, n) =>
//        nodeset = nodeset ++ WALA.getNaturalLoop(cfg, m, n)
//      }
//      ifDebug {Log.println("nodeset " + nodeset)}
//
//      // // find all body edges, i.e., those from the loop header to
//      // // a loop node
//      // var bodyedges = ISet[Edge]()
//      // cfg.getSuccNodes(head).foreach{ succ =>
//      //   val s = succ.asInstanceOf[WALA.CFGNode]
//      //   if (nodeset.contains(s)) {
//      //     bodyedges += (head, s).asInstanceOf[Edge]
//      //   }
//      // }
//      // ifDebug {println("bodyedges " + bodyedges)}
//
//      // find all exit edges, i.e. those from a loop node to a
//      // non-loop node
//      var exitedges = ISet[Edge]()
//      def find_exits(
//        p : WALA.CFGNode,
//        path : List[WALA.CFGNode],
//        visited : Set[WALA.CFGNode]
//      ) : Unit = {
//        cfg.getSuccNodes(p).foreach{ succ =>
//          val q = succ.asInstanceOf[WALA.CFGNode]
//          if (! visited.contains(q)) {
//            if (! nodeset.contains(q)) {
//              exitedges = exitedges + (p, q).asInstanceOf[Edge]
//            } else {
//              find_exits(q, path ++ List(q), visited + q)
//            }
//          }
//        }
//      }
//      find_exits(head, List(head), Set(head))
//      ifDebug {Log.println("exitedges " + exitedges)}
//
//      // find inedges.  inedges are all predecessors of head that are
//      // not in loopSet
//      val inedges = cfg.getPredNodes(head).filter{ pred =>
//        val p = pred.asInstanceOf[WALA.CFGNode]
//        ! nodeset.contains(p)
//      }.map{ p => (p, head).asInstanceOf[Edge] }.toSet
//      ifDebug {Log.println("inedges " + inedges)}
//
//      // find loopvars, those which are the lhs of operations, and
//      // primevar pairs, those which are the lhs of a phi node paired
//      // with the rhs from an edge within the loop (maybe in the
//      // future will only be from backedges)
//      // TODO test with nested loops
//      var loopvars = ISet[VarIndex]()
//      var primevars = ISet[(VarIndex, VarIndex)]()
//      val visited = Set[WALA.CFGNode]()
//      def find_loopvars(
//        n: WALA.CFGNode
//      ) : Unit = {
//        visited += n
//        // println(n)
//
//        // add lhs of phis
//        val preds = cfg.getPredNodes(n).toList
//        n.iteratePhis().foreach{phi =>
//          val lhs = phi.getDef().asInstanceOf[VarIndex]
//          for (idx <- 0 to (phi.getNumberOfUses() - 1)) {
//            if (nodeset.contains(preds.get(idx))) {
//              val rhs = phi.getUse(idx).asInstanceOf[VarIndex]
//              primevars = primevars + ((lhs, rhs))
//              loopvars += lhs
//            }
//          }
//        }
//
//        // add lhs of instructions
//        for (idx <- n.getFirstInstructionIndex() to n.getLastInstructionIndex()) {
//          val (inst, _) = WALA.getInstruction(idx, wcontext.ir)
//          // println(inst)
//          inst match {
//            case i: BinopI =>
//              loopvars += i.getDef()
//            case i: ArrayLengthI =>
//              loopvars += i.getDef()
//            case i: GetI =>
//              // TODO may need special handling for field
//              loopvars += i.getDef()
//            case i: ArrayLoadI =>
//              // TODO may need special handling for index var
//              loopvars += i.getDef()
//            case i => // do nothing
//          }
//        }
//
//        cfg.getSuccNodes(n).foreach{m =>
//          if (! visited.contains(m) && nodeset.contains(m)) {
//            find_loopvars(m.asInstanceOf[WALA.CFGNode])
//          }
//        }
//      }
//      find_loopvars(head)
//      ifDebug {Log.println("loopvars " + loopvars)}
//      ifDebug {Log.println("primevars " + primevars)}
//
//      // ifDebug {println("\ninitial machine\n" + loopmachine)}
//
//      // stop ainterping at exit and backedges to get one iteration of
//      // the loop
//      val transexits = (backedges ++ exitedges).asInstanceOf[ISet[Edge]]
//
//      // collect inner loop backedges to stop ainterp from widening inner loops
//      // var innerloopexits = allbackedges.map{ case(_, to) => to }.toSet.filter{ n => nodeset.contains(n) }
//      // var innerloopexits = allbackedges
//      var innerloopexits = allbackedges.filter{ case(from, to) => nodeset.contains(to) }.asInstanceOf[ISet[Edge]]
//
//      val loopinfo = new LoopInfo(
//        head,
//        nodeset,
//        backedges,
//        inedges,
//        exitedges,
//        transexits,
//        innerloopexits,
//        loopvars,
//        primevars
//      )
//
//      (head, loopinfo)
//    }.toMap
//  }
//
//  /***
//   * For a given loop, get its pre, trans, and post invariants.
//   */
//  def getInvariants(
//    head: WALA.CFGNode,
//    loopinfo: LoopInfo,
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
//    Loops.doJoin = false
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
//    Loops.doJoin = true
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
//    loopinfo: LoopInfo,
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
//  /* loopSummary: loop-focused use case; expects a node that begins the
//   * loop initialization and returns the loop's summary. */
//  def loopSummary(
//    allloopinfo: Map[WALA.CFGNode, LoopInfo],
//    context: InterpContext
//  )(implicit conf: AInterpConfig) :
//      // Map[WALA.CFGNode, ISet[Polynomial]] = {
//      Map[WALA.CFGNode, List[BoundExpression]] = {
//    val InterpContext(econtext, wcontext) = context
//    val map = scala.collection.mutable.Map[WALA.CFGNode, List[BoundExpression]]()
//
//    ifDebug {Log.println(wcontext.ir.toString())}
//
//    // find pre, trans, and post invariants
//    val allinvariants = allloopinfo.map{ case (head, loopinfo)  =>
//      (head, getInvariants(head, loopinfo, context))
//    }.toMap
//
//    // compute bounds for each loop header
//    allinvariants.map{ case (head, (alignedpre, alignedtrans, alignedpost)) =>
//      val loopinfo = allloopinfo.get(head).get
//      (head, getBounds(head, loopinfo, alignedpre, alignedtrans, alignedpost, context))
//    }.toMap
//  }
//
//  /* loopSummary: loop-focused use case; expects a node that begins the
//   * loop initialization and returns the loop's summary. */
//  def ctloopSummary(
//    controlTree: ASTNode,
//    allloopinfo: Map[WALA.CFGNode, LoopInfo],
//    context: InterpContext
//  )(implicit conf: AInterpConfig) :
//      // Map[WALA.CFGNode, ISet[Polynomial]] = {
//      Map[WALA.CFGNode, List[BoundExpression]] = {
//    val InterpContext(econtext, wcontext) = context
//    val map = scala.collection.mutable.Map[WALA.CFGNode, List[BoundExpression]]()
//
//    ifDebug {Log.println(wcontext.ir.toString())}
//
//    def traverse(node:ASTNode) : Unit = {
//      node match {
//        case ASTNodeLoop(fc,ns,head:WALA.CFGNode) =>
//          traverse(fc)
//          traverse(ns)
//          val loopinfo = allloopinfo.get(head).get
//          val (alignedpre, alignedtrans, alignedpost) = getInvariants(head, loopinfo, context)
//          val dbounds = getBounds(head, loopinfo, alignedpre, alignedtrans, alignedpost, context)
//          map += (head -> dbounds)
//
//        case ASTNodeIf(fc,ns,wn:WALA.CFGNode) =>
//          traverse(fc)
//          traverse(ns)
//
//        case ASTNodeSeq(fc,ns,i:String) =>
//          traverse(fc)
//          traverse(ns)
//
//        case ASTNodeSLC(null,ns,wn:WALA.CFGNode) =>
//          traverse(ns)
//
//        case ASTNodeFunc(null,ns,wn:WALA.CFGNode) =>
//          traverse(ns)
//
//        case null =>
//          // base case
//
//        case default =>
//          throw new RuntimeException("unexpected node: " + node)
//      }
//    }
//
//    traverse(controlTree)
//
//    // // find pre, trans, and post invariants
//    // val allinvariants = allloopinfo.map{ case (head, loopinfo)  =>
//    //   (head, getInvariants(head, loopinfo, context))
//    // }.toMap
//
//    // // compute bounds for each loop header
//    // allinvariants.map{ case (head, (alignedpre, alignedtrans, alignedpost)) =>
//    //   val loopinfo = allloopinfo.get(head).get
//    //   (head, getBounds(head, loopinfo, alignedpre, alignedtrans, alignedpost, context))
//    // }.toMap
//
//    map
//  }
//
//  def funSummary(
//    context: InterpContext,
//    outDir: Path,
//    htmlWriter: PrintWriter,
//    spec: Option[Specification],
//    mapBound: Map[String,BoundExpression]
//  )(implicit conf: AInterpConfig): BoundExpression = {
//    val InterpContext(econtext, wcontext) = context
//    val funname = wcontext.method.getName().toString()
//    val sig = wcontext.method.getSignature()
//    ifDebug {Log.println("function name: "+funname)}
//
//    htmlWriter.write("<div style=\"padding: 10px; border: 1pt solid black;\">\n")
//    htmlWriter.write("<h2>"+funname+"</h2>\n");
//
//    val dotWriter = new DotWriter(funname, outDir)
//    dotWriter.write("digraph g {\n")
//
//    var expNode = new ASTNodeAny(null, null, null)
//    val controlTree = expNode.buildLoopTreeFromCFG(wcontext.cfg.entry(),
//      wcontext)
//    ifDebug {Log.println("\ncontrol tree\n" + controlTree.toString(1))}
//
//    // // disable debugging output
//    // var oldDebug = Core.debug
//    // Core.debug = false
//    val node2bound : Map[WALA.CFGNode, List[BoundExpression]] = loopSummary(getLoopInfo(context), context)
//    // Core.debug = oldDebug
//    val polynomialbounds = node2bound.map{ case (h, blist) =>
//      val bset = blist.map{ b =>
//        PolynomialUtil.construct(b, context).toString
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
//    val node2trailbound : Map[WALA.CFGNode, List[(Trail,BoundExpression)]] =
//      node2bound.map{ case (n, bl) => (n, bl.map{ b => (Trail.dummy(n), b) }) }
//
//    val allBexpr = FunctionBounds.summarize(controlTree,node2trailbound,spec,funname,sig,dotWriter,htmlWriter,mapBound,context)
//    dotWriter.write("}\n")
//
//    dotWriter.close()
//
//    htmlWriter.write(FunctionBounds.htmlExpandable("Control Tree Diagram","<img src=\""+dotWriter.getOutputFileName()+"\">\n"))
//
//    htmlWriter.write("</div>")
//
//    // take all combinations of possible bounds and compare them
//    // TODO: preserve path conditions in loopSummary above
//    val lifted_polys = liftMax(allBexpr, context).map{ b => PolynomialUtil.construct(b, context) }
//    // lifted_polys.foreach{ p =>
//    //   println("p " + p)
//    // }
//
//    // TODO: perform this on all top-level loops instead?
//
//    // TODO: divide bounds by secret/tainted/untainted, compare
//    // tainted with secret, etc
//
//    // pairwise comparison of bounds
//    // TODO: use a partial ordering to reduce number of comparisons
//    val diffs = Set[Polynomial]()
//    lifted_polys.toSeq.combinations(2).foreach{ case Seq(x, y) =>
//      // TODO print path conditions for difference, program locations of difference, loop header
//      if (compareBounds(x, y, context) != 0) {
//        diffs += x
//        diffs += y
//      }
//    }
//
//    if (diffs.size > 0) {
//      indented_println("potential attack")
//      diffs.foreach{ x =>
//        indented_println(x.toString.replace(context.wala.ir.getMethod().getSignature().toString() + ":", ""))
//      }
//      indented_println()
//    }
//
//    allBexpr
//  }
//
//  def ctSummary(
//    context: InterpContext,
//    outDir: Path,
//    htmlWriter: PrintWriter,
//    spec: Option[Specification],
//    mapBound: Map[String,BoundExpression]
//  )(implicit conf: AInterpConfig): BoundExpression = {
//    val InterpContext(econtext, wcontext) = context
//    val funname = wcontext.method.getName().toString()
//    val sig = wcontext.method.getSignature()
//    ifDebug {Log.println("function name: "+funname)}
//
//    htmlWriter.write("<div style=\"padding: 10px; border: 1pt solid black;\">\n")
//    htmlWriter.write("<h2>"+funname+"</h2>\n");
//
//    val dotWriter = new DotWriter(funname, outDir)
//    dotWriter.write("digraph g {\n")
//
//    var expNode = new ASTNodeAny(null, null, null)
//    val controlTree = expNode.buildLoopTreeFromCFG(wcontext.cfg.entry(),
//      wcontext)
//    ifDebug {Log.println("\ncontrol tree\n" + controlTree.toString(1))}
//
//    // // disable debugging output
//    // var oldDebug = Core.debug
//    // Core.debug = false
//    val node2bound : Map[WALA.CFGNode, List[BoundExpression]] = ctloopSummary(
//      controlTree,
//      getLoopInfo(context),
//      context)
//    // Core.debug = oldDebug
//    val polynomialbounds = node2bound.map{ case (h, blist) =>
//      val bset = blist.map{ b =>
//        PolynomialUtil.construct(b, context).toString
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
//    val node2trailbound : Map[WALA.CFGNode, List[(Trail,BoundExpression)]] =
//      node2bound.map{ case (n, bl) => (n, bl.map{ b => (Trail.dummy(n), b) }) }
//    val allBexpr = FunctionBounds.summarize(controlTree,node2trailbound,spec,funname,sig,dotWriter,htmlWriter,mapBound,context)
//    dotWriter.write("}\n")
//
//    dotWriter.close()
//
//    htmlWriter.write(FunctionBounds.htmlExpandable("Control Tree Diagram","<img src=\""+dotWriter.getOutputFileName()+"\">\n"))
//
//    htmlWriter.write("</div>")
//
//    // take all combinations of possible bounds and compare them
//    // TODO: preserve path conditions in loopSummary above
//    val lifted_polys = liftMax(allBexpr, context).map{ b => PolynomialUtil.construct(b, context) }
//    // lifted_polys.foreach{ p =>
//    //   println("p " + p)
//    // }
//
//    // TODO: perform this on all top-level loops instead?
//
//    // TODO: divide bounds by secret/tainted/untainted, compare
//    // tainted with secret, etc
//
//    // pairwise comparison of bounds
//    // TODO: use a partial ordering to reduce number of comparisons
//    val diffs = Set[Polynomial]()
//    lifted_polys.toSeq.combinations(2).foreach{ case Seq(x, y) =>
//      // TODO print path conditions for difference, program locations of difference, loop header
//      if (compareBounds(x, y, context) != 0) {
//        diffs += x
//        diffs += y
//      }
//    }
//
//    if (diffs.size > 0) {
//      indented_println("potential attack")
//      diffs.foreach{ x =>
//        indented_println(x.toString.replace(context.wala.ir.getMethod().getSignature().toString() + ":", ""))
//      }
//      indented_println()
//    }
//
//    allBexpr
//  }
//
//  /**
//    * Compare bounds x and y, -1 if x < y, 0 if x == y, 1 if x > y
//    */
//  def compareBounds(x: Polynomial, y: Polynomial, context: InterpContext): Int = {
//
//    // TODO take very large and very small input parameters into account
//    val adeg = x.degree()
//    val bdeg = y.degree()
//          
//    // if (adeg == bdeg) {
//    //   if (0 == adeg) {
//    //     x.constant.compareTo(y.constant)
//    //   } else {
//    //     val amax = x.maxTerm()
//    //     val bmax = y.maxTerm()
//    //     x.terms.get(amax).compareTo(y.terms.get(bmax))
//    //   }
//    // } else {
//      adeg - bdeg
//    // }
//  }
//
//  def liftMax(
//    b: BoundExpression,
//    context: InterpContext
//  ) : ISet[BoundExpression] = {
//    def expand(
//      xin: BoundExpression,
//      yin: BoundExpression,
//      context: InterpContext) : ISet[BoundExpression] = {
//      val xs = liftMax(xin, context)
//      val ys = liftMax(yin, context)
//      // TODO: trim infeasible paths
//        (for {
//          x <- xs
//          y <- ys
//          val n = new BoundExpressionMult(x, y)
//        } yield n).toSet[BoundExpression]
//    }
//    b match {
//      case i: BoundExpressionConst =>
//        ISet(i)
//      case i: BoundExpressionVar =>
//        ISet(i)
//      case i: BoundExpressionVarLabel =>
//        ISet(i)
//      case c: BoundExpressionConstC =>
//        ISet(c)
//      case i: BoundExpressionPolynomial =>
//        ISet(i)
//      case i: BoundExpressionMult =>
//        expand(i.x, i.y, context)
//      case i: BoundExpressionDiv =>
//        expand(i.x, i.y, context)
//      case i: BoundExpressionSum =>
//        expand(i.x, i.y, context)
//      case i: BoundExpressionDiff =>
//        expand(i.x, i.y, context)
//      case i: BoundExpressionMax =>
//        ISet(i.x, i.y)
//      case i: BoundExpressionMin =>
//        throw new RuntimeException("min expression not yet implemented " + b);
//      case i: BoundExpressionPow =>
//        throw new RuntimeException("pow expression not yet implemented " + b);
//      case i: BoundExpressionLog =>
//        throw new RuntimeException("log expression not yet implemented " + b);
//      case default =>
//        throw new RuntimeException("bound expression not yet implemented " + b);
//    }
//  }
//}

//import edu.illinois.wala.Facade._
//
//import Util._
//import Core._
//
//import com.ibm.wala.classLoader.ProgramCounter
//
//import scala.collection.Map
//import scala.collection.Map._
//import scala.collection.immutable.{Map=>IMap, HashMap, Set=>ISet}
//import scala.collection.mutable.{Set,Stack,ListBuffer,Map=>MMap}
//import CFGAnnotations._
//import Trail._
//import collection.JavaConversions._
//
//object TrailAnalysis {
//  /**
//    * Assume the first trail has inedge from the entry node,
//    * wcontext.cfg.entry().
//    *
//    * TODO: return a set of trail pairs (pre and body).
//    */
//  def simple(wcontext:WALA.WALAContext) : (Trail, Trail) = {
//    val allloopinfo: scala.collection.Map[WALA.CFGNode, WALA.LoopStructure] = WALA.getLoopStructure(wcontext);
//    val initedge = WALA.getEntryEdges(wcontext.cfg).toList(0)
//    var n = initedge._2.asInstanceOf[WALA.CFGNode]
//
//    // create pretrail up to first loop
//    var preTrail : Trail = List[Trailspec]()
//    while (! allloopinfo.keySet.contains(n)) {
//      val succs = wcontext.cfg.getNormalSuccessors(n)
//      val next = wcontext.cfg.getSuccNodes(n).toList.asInstanceOf[List[WALA.CFGNode]].head
//      preTrail = preTrail ++ List[Trailspec](Trail.Node(n,next))
//      n = next
//    }
//    // n is now the head of a loop
//
//    // get one trail through the loop
//    val nodes = allloopinfo.get(n).get.nodeset
//    val trails = Trail.buildTrailsForLoop(wcontext.cfg, n, nodes)
//
//    // pick the first trail that has a backedge to the head n
//    var loopTrail = List[Trailspec]()
//
//    for (trail <- trails; if (loopTrail.length == 0)) {
//      // check that last trailNode has an edge to loop header
//
//      // WARNING: this assumes all elements are Nodes, doesn't handle
//      // Ors or Repeats
//      val tnode : Trail.Node = trail.last.asInstanceOf[Trail.Node]
//      val cfgnode : WALA.CFGNode = tnode.next
//
//      if (wcontext.cfg.getNormalSuccessors(tnode.next).contains(n)) {
//        // the last trail node has a backedge to loop header, so make
//        // a new trailNode for this backedge to complete the Repeat
//        loopTrail = trail ::: List[Trailspec](Trail.Node(tnode.next, n))
//      }
//    }
//
//    // create a repeat trail out of it
//    // var exitnode = n
//    // for ((f, t) <- allloopinfo.get(n).get.exitedges) {
//    //   if (wcontext.cfg.getNormalSuccessors(n).contains(t)) {
//    //     exitnode = t.asInstanceOf[WALA.CFGNode]
//    //   }
//    // }
//    var exitnode = allloopinfo.get(n).get.exitedges.toList.get(0)._2.asInstanceOf[WALA.CFGNode]
//    var onemore = wcontext.cfg.getNormalSuccessors(exitnode).toList.get(0).asInstanceOf[WALA.CFGNode]
//    // val bodyTrail = List[Trailspec](
//    //   Repeat(loopTrail, new SFix()),
//    //   Repeat(
//    //     List[Trailspec](
//    //       Trail.Node(n, exitnode),
//    //       Trail.Node(exitnode, onemore)),
//    //     SInt(1)))
//    val bodyTrail = List[Trailspec](
//      Repeat(loopTrail, new SFix()),
//          Trail.Node(n, exitnode))
//
//    println(bodyTrail)
//
//    // at loop, call andrew's trails from loop
//    //   WALA.getLoopStructure
//    // pick one trail and (if it has a backedge) make a new loop trail
//    (preTrail, bodyTrail)
//  }
//
//  def addToAll(trails: List[Trail], tspec: Trailspec) : List[Trail] = {
//    trails.map{trail =>
//      trail ++ List[Trailspec](tspec)
//    }
//  }
//
//  def addToAll(trails1: List[Trail], trails2: List[Trail]) : List[Trail] = {
//    for {
//      t1 <- trails1
//      t2 <- trails2
//    } yield (t1 ++ t2)
//  }
//
//    def trailBlaze(
//	  wcontext:WALA.WALAContext,
//	  edgeMap:scala.collection.mutable.Map[WALA.CFGNode,List[WALA.CFGNode]]
//    ) : List[Trail] = {
//    val allloopinfo: scala.collection.Map[WALA.CFGNode, WALA.LoopStructure] = WALA.getLoopStructure(wcontext);
//    val initedge = WALA.getEntryEdges(wcontext.cfg).toList(0)
//    val outedges = WALA.getExitEdges(wcontext.cfg)
//    val exitnodes = outedges.map{case (_, to) => to}.toSet.asInstanceOf[ISet[WALA.CFGNode]]
//
//    val entry_node = initedge._1.asInstanceOf[WALA.CFGNode]
//    val start_node = initedge._2.asInstanceOf[WALA.CFGNode]
//    var starting_trails = List[Trail](List[Trailspec]())
//    val visited = ISet[WALA.CFGNode]()
//    return trailBlaze(wcontext, allloopinfo, None, start_node, starting_trails, exitnodes, visited, edgeMap)
//  }
//
//    def addEdges(n:WALA.CFGNode,
//		 ms:List[WALA.CFGNode],
//		 edgeMap:scala.collection.mutable.Map[WALA.CFGNode,List[WALA.CFGNode]]) {
//	val new_ms = edgeMap.get(n) match {
//	case None => ms
//	case Some(mss) => ms ++ mss
//	}
//      	edgeMap += n -> new_ms
//    }
//
//  /** Get the bound of a CFG block, including any interprocedural bound. */
//  def containsReturn(
//    wcontext:WALA.WALAContext,
//    block: WALA.CFGNode
//  ) : Boolean = {
//    var hasReturn = false
//
//    for (idx <- block.getFirstInstructionIndex() to block.getLastInstructionIndex()) {
//      val (inst, _) = WALA.getInstruction(idx, wcontext.ir)
//
//      inst match {
//        case i: ReturnI =>
//          hasReturn = true
//        case i =>
//      }
//    }
//
//    hasReturn
//  }
//
//  def trailBlaze(
//    wcontext:WALA.WALAContext,
//    allloopinfo: scala.collection.Map[WALA.CFGNode, WALA.LoopStructure],
//    loop_head: Option[WALA.CFGNode],
//    start_node: WALA.CFGNode,
//    trails: List[Trail],
//    exitnodes: ISet[WALA.CFGNode],
//    visited_in: ISet[WALA.CFGNode],
//    edgeMap: scala.collection.mutable.Map[WALA.CFGNode,List[WALA.CFGNode]]
//  ) : List[Trail] = {
//    var loop_trails = List[Trail]()
//
//    var n = start_node
//    val succs = wcontext.cfg.getSuccNodes(n).toList.asInstanceOf[List[WALA.CFGNode]]
//    // println("succs " + succs)
//    addEdges(n,succs,edgeMap)
//    // split processing at branches, continuing with pending trail
//    var newTrails = List[Trail]()  // prepare for new trails
//
//    if (succs.size() == 0) {
//      return trails
//    }
//
//    // don't continue making trails for already visited loop headers
//    if (visited_in.contains(start_node)) {
//      println("found repeated trail")
//      return trails
//    }
//
//    val visited = visited_in + start_node
//
//    // exit edges are the base case
//    succs.filter{next => exitnodes.contains(next)}.foreach{next =>
//      // ************************************
//      // TODO: add switch.  this calls to containsReturn turns off exception trails
//      // ************************************
//      if (containsReturn(wcontext, start_node)) {
//        newTrails ++= trails
//      }
//    }
//
//    succs.filter{next => ! exitnodes.contains(next)}.foreach{next =>
//      // println("next " + next)
//      val tspec = Trail.Node(n, next)
//
//      // build up trail along path
//      val pending_trails = addToAll(trails, tspec)
//
//      var done = false
//      // find loop backedges (through trails) and exit edges (exit trails)
//      loop_head match {
//        case Some(loop_head) =>
//          val loopinfo = allloopinfo.get(loop_head).get
//          if (loopinfo.exitedges.filter{ case (from, to) => to == next }.size() > 0) {
//            // next exits the loop
//            // save the trail as an exit trail and stop
//            // println("found exit trails")
//            newTrails ++= pending_trails
//            done = true
//          } else if (next == loop_head) {
//            // this is a back edge
//            // save the trail as a through trail and stop
//            // println("found through trails")
//            newTrails ++= pending_trails
//            done = true
//          }
//        case None =>
//      }
//
//      if (! done) {
//        if (allloopinfo.containsKey(next)) {
//          val starting_trails = List[Trail](List[Trailspec]())
//          val new_loop_head = next
//          // println("loop " + new_loop_head)
//          val loop_trails = trailBlaze(wcontext, allloopinfo, Some(new_loop_head), new_loop_head, starting_trails, exitnodes, visited, edgeMap)
//          // take all combinations of through and exit trails
//          // val repeat = List[Trailspec](Repeat(trail, SFix()))
//          // println("end loop: trails")
//          loop_trails.foreach{trail =>
//            // println(toStringRE(trail))
//          }
//
//          // find through trails and exit trails
//          val loopinfo = allloopinfo.get(new_loop_head).get
//
//          var throughtrails = List[Trail]()
//          var exittrails = List[Trail]()
//          loop_trails.filter{trail => trail.size() > 0}.foreach{trail =>
//            val tail = trail.last.asInstanceOf[Trail.Node].next
//            if (loopinfo.exitedges.filter{ case (from, to) => to == tail }.size() > 0) {
//              exittrails ++= List[Trail](trail)
//            } else if (tail == new_loop_head) {
//              throughtrails ++= List[Trail](List[Trailspec](Repeat(trail, SFix())))
//            }
//          }
//
//          // println("throughtrails")
//          throughtrails.foreach{trail =>
//            // println(toStringRE(trail))
//          }
//          // println("exittrails")
//          exittrails.foreach{trail =>
//            // println(toStringRE(trail))
//          }
//
//          // take all combinations of through trails and exit trails
//          val repeat_exit_trails = for {
//            through <- throughtrails
//            exit <- exittrails
//          } yield( through ++ exit )
//
//          // // try all pairs of throughtrails followed by an exit trail
//          // println(throughtrails zip throughtrails.tail)
//          // // System.exit(1)
//
//          // also try each exit path without going through the loop
//          val complete_loop_trails = repeat_exit_trails ++ exittrails
//
//          // println("complete_loop_trails")
//          complete_loop_trails.foreach{trail =>
//            // println(toStringRE(trail))
//          }
//
//          // ********************************************************
//          // TODO: here we can unroll the loops in any order we want!
//          // ********************************************************
//
//          // keep going after a loop
//          complete_loop_trails.foreach{trail =>
//            val trail_tail = trail.last.asInstanceOf[Trail.Node].next
//            println("trail_tail " + trail_tail)
//            val pending_trails_after_loop = for { pending <- pending_trails } yield(pending ++ trail)
//	          val completed_trails = trailBlaze(wcontext, allloopinfo, loop_head, trail_tail, pending_trails_after_loop, exitnodes, visited, edgeMap)
//            newTrails ++= completed_trails
//          }
//        } else {
//          // add tspec to old trails and pass it to trailBlaze
//	        val completed_trails = trailBlaze(wcontext, allloopinfo, loop_head, next, pending_trails, exitnodes, visited, edgeMap)
//          // completed_trails.foreach{trail => // println(toStringRE(trail))}
//          newTrails ++= completed_trails
//        }
//        // println("end multinext")
//      }
//    }
//
//    return newTrails
//  }
//
//  def run(ct:ASTNode, wcontext:WALA.WALAContext, untilNode: ASTNode) : Trail = {
//      // create a Trail, which is a list of trailspecs
//    var (stop, trailFromRun) = runRight(ct, wcontext, untilNode)
//    var finalTrail = removeNullNodes(trailFromRun)
//    return finalTrail
//  }
//
//  def runDown(ct:ASTNode, wcontext:WALA.WALAContext, untilNode:ASTNode) : (Boolean, Trail) = {
//    // runDown returns a Trail context: all nodes in the beginning of the trail
//    // have null the first CFGNode and all the nodes in the end have null the second CFGNode.
//    var trueStop = false
//    if (ct == untilNode) {
//        // return (true, List())
//        trueStop = true
//    }
//
//    ct match {
//        case ASTNodeLoop(fc,ns,v) => {
//            if (fc != null) {
//                var (stop, t) = runRight(fc, wcontext, untilNode)
//                var tf = plugToFrontOfTrail(t,v.asInstanceOf[WALA.CFGNode])
//                var tfb = plugToBackOfTrail(tf,v.asInstanceOf[WALA.CFGNode])
//                return (trueStop || stop, List(Trail.Node(null,v.asInstanceOf[WALA.CFGNode])):::List(Repeat(tfb, SFix())):::List(Trail.Node(v.asInstanceOf[WALA.CFGNode],null)))
//            }
//
//            return null
//        }
//        case ASTNodeSLC(fc,ns,v) => {
//            return (trueStop || false, List(Trail.Node(null,v.asInstanceOf[WALA.CFGNode]),Trail.Node(v.asInstanceOf[WALA.CFGNode],null)))
//        }
//        case ASTNodeIf(fc,ns,v) => {
//            var (stop, t) = runRight(fc, wcontext, untilNode)
//            return (trueStop || stop,List(Trail.Node(null,v.asInstanceOf[WALA.CFGNode])):::plugToFrontOfTrail(t, v.asInstanceOf[WALA.CFGNode]))
//        }
//        case ASTNodeSeq(fc,ns,v) => {
//            var (stop, t) = runRight(fc, wcontext, untilNode)
//            return (trueStop || stop, t)
//        }
//        case ASTNodeFunc(fc,ns,v) => return null
//    }
//
//    return null
//  }
//
//  def runRight(ct:ASTNode, wcontext:WALA.WALAContext, untilNode:ASTNode) : (Boolean, Trail) = {
//      ct match {
//          case ASTNodeLoop(fc,ns,v) => {
//              var (stop, t) = runDown(ct, wcontext, untilNode)
//              if (ns != null && !stop) {
//                  var (stop, tns) = runRight(ns, wcontext, untilNode)
//                  return (stop, connectTwoTrails(t, tns))
//              }
//              return (stop, t)
//          }
//          case ASTNodeSLC(fc,ns,v) => {
//              var (stop, t) = runDown(ct, wcontext, untilNode)
//              if (ns != null && !stop) {
//                  var (stop, tns) = runRight(ns, wcontext, untilNode)
//                  return (stop, connectTwoTrails(t, tns))
//              }
//              return (stop, t)
//          }
//          case ASTNodeIf(fc,ns,v) => {
//              var (stop, t) = runDown(ct, wcontext, untilNode)
//              if (ns != null && !stop) {
//                  var (stop, tns) = runRight(ns, wcontext, untilNode)
//                  return (stop, connectTwoTrails(t, tns))
//              }
//              return (stop, t)
//          }
//          case ASTNodeSeq(fc,ns,v) => {
//              var (stop, t) = runDown(ct, wcontext, untilNode)
//              if (ns != null && !stop) {
//                  var (stop, nst) = runRight(ns, wcontext, untilNode)
//                  if (!nst.isEmpty) {
//                      return (stop, List(Or(t,nst)))
//                  }
//                  return (stop, t)
//              }
//              return (stop, t)
//          }
//          case ASTNodeFunc(fc,ns,v) => return null
//      }
//
//      return null
//  }
//
//  def flattenOrs(t: Trail) : List[Trail] = {
//      if (!t.isEmpty) {
//          t.head match {
//              case Trail.Node(c,n) => return flattenOrs(t.tail).map(y => Trail.Node(c,n)::y)
//              case Or(t1,t2) => return flattenOrs(t.tail).map(y => {
//                  (flattenOrs(t1):::flattenOrs(t2)).map( z => z:::y)
//              }).flatten
//              case Repeat(t1, s) => return flattenOrs(t.tail).map(y => {
//                  flattenOrs(t1).map( z => Repeat(z,s)::y)
//              }).flatten
//          }
//      }
//
//      return List(List())
//  }
//
//  def getSplittedTrails(t: Trail) : List[(Trail, Trail)] = {
//      val flattenOrsTrails = TrailAnalysis.flattenOrs(t)
//      var splittedTrails = flattenOrsTrails.map{x => TrailAnalysis.splitAtRepeats(x)}.flatten
//
//      return splittedTrails
//  }
//
//  def splitAtRepeats(t: Trail) : List[(Trail, Trail)] = {
//      if (!t.isEmpty) {
//          var nodes = findRepeatCFGNodes(t)
//          var retList = ListBuffer[(Trail, Trail)]()
//          for (n <- nodes) {
//              retList += splitTrail(t, n)
//          }
//          return retList.toList
//      }
//
//      return List((t, List()))
//  }
//
//  def splitTrail(t: Trail, v: WALA.CFGNode) : (Trail, Trail) = {
//      //Assume t does not contain Ors
//      if (!t.isEmpty) {
//          t.head match {
//              case Trail.Node(c,n) => {
//                  if (n == v) {
//                      return (List(t.head), splitTrail(t.tail, null)._1)
//                  } else {
//                      var (t1, t2) = splitTrail(t.tail, v)
//                      return (t.head::t1, t2)
//                  }
//              }
//            //   case Or(t1,t2) => return flattenOrs(t.tail).map(y => {
//            //       (flattenOrs(t1):::flattenOrs(t2)).map( z => z:::y)
//            //   }).flatten
//              case Repeat(t1, s) => {
//                  var (s1, s2) = splitTrail(t1, v)
//                  if (s2.isEmpty) {
//                      var (p1, p2) = splitTrail(t.tail, v)
//                      return (t.head::p1, p2)
//                  } else {
//                      return (s1, s2:::splitTrail(t.tail, null)._1)
//                  }
//              }
//          }
//      }
//
//      return (t, List())
//  }
//
//  def findFirstCFGNodes(t: Trail) : List[WALA.CFGNode] = {
//      if (!t.isEmpty) {
//          t.head match {
//              case Trail.Node(c,v) => return List(c)
//              case Or(t1,t2) => return (findFirstCFGNodes(t1):::findFirstCFGNodes(t2)).distinct
//              case Repeat(t1, s) => return findFirstCFGNodes(t1)
//          }
//      }
//
//      return List()
//  }
//
//  def findRepeatCFGNodes(t: Trail) : List[WALA.CFGNode] = {
//      if (!t.isEmpty) {
//          t.head match {
//              case Trail.Node(c,v) => return findRepeatCFGNodes(t.tail)
//              case Or(t1,t2) => return (findRepeatCFGNodes(t1):::findRepeatCFGNodes(t2)).distinct
//              case Repeat(t1, s) => return (findFirstCFGNodes(t1):::findRepeatCFGNodes(t1):::findRepeatCFGNodes(t1.tail)).distinct
//          }
//      }
//
//      return List()
//  }
//
//  def plugToFrontOfTrail(t: Trail, v:WALA.CFGNode) : Trail = {
//      if (t != null) {
//          if (!t.isEmpty) {
//              t.head match {
//                  case Trail.Node(c,n) => return Trail.Node(v,n)::t.tail
//                  case Or(t1,t2) => return Or(plugToFrontOfTrail(t1,v), plugToFrontOfTrail(t2,v))::t.tail
//                  case Repeat(t1, s) => return Repeat(plugToFrontOfTrail(t1,v), s)::t.tail
//              }
//          }
//      }
//
//      return t
//  }
//
//  def plugToBackOfTrail(t: Trail, v:WALA.CFGNode) : Trail = {
//      if (t != null) {
//          if (!t.isEmpty) {
//              t.last match {
//                  case Trail.Node(c,n) => return t.take(t.length-1):::List(Trail.Node(c,v))
//                  case Or(t1,t2) => return t.take(t.length-1):::List(Or(plugToBackOfTrail(t1,v), plugToBackOfTrail(t2,v)))
//                  case Repeat(t1, s) => return t.take(t.length-1):::List(Repeat(plugToBackOfTrail(t1,v), s))
//              }
//          }
//      }
//
//      return t
//  }
//
//  def connectTwoTrails(t1: Trail, t2: Trail) : Trail = {
//      if (t2 != null) {
//          if (t2.isEmpty) {
//              return t1
//          }
//          var (v,tn2) = getFirstCFGNodeOfTrail(t2)
//          var connectedTrail = plugToBackOfTrail(t1, v):::tn2
//          return connectedTrail
//      } else {
//          return t1
//      }
//
//  }
//
//  def getFirstCFGNodeOfTrail(t: Trail) : (WALA.CFGNode, Trail) = {
//      t.head match {
//          case Trail.Node(c,n) => return (n,removeFirstNodeOfTrail(t))
//          case Or(t1,t2) => {
//              var f1 = getFirstCFGNodeOfTrail(t1)._1
//              var f2 = getFirstCFGNodeOfTrail(t2)._1
//              if (f1 == f2) {
//                  return (f1, removeFirstNodeOfTrail(t))
//              }
//          }
//        //   case Repeat(t1, s) => (null,Repeat(removeFirstNodeOfTrail(t1), s)::t.tail)
//          //the latter should not happen
//      }
//
//      return (null, removeFirstNodeOfTrail(t))
//  }
//
//  def removeFirstNodeOfTrail(t : Trail) : Trail = {
//      t.head match {
//          case Trail.Node(c,n) => t.tail
//          case Or(t1,t2) => Or(removeFirstNodeOfTrail(t1), removeFirstNodeOfTrail(t2))::t.tail
//          case Repeat(t1, s) => Repeat(removeFirstNodeOfTrail(t1), s)::t.tail
//          //the latter should not happen
//      }
//  }
//
//  def removeNullNodes(t: Trail) : Trail = {
//      if (t.isEmpty) {
//          return t
//      }
//      t.head match {
//          case Trail.Node(c,n) => {
//              if (c == null || n == null) {
//                  return removeNullNodes(t.tail)
//              }
//              return t.head::removeNullNodes(t.tail)
//          }
//          case Or(t1,t2) => Or(removeNullNodes(t1), removeNullNodes(t2))::removeNullNodes(t.tail)
//          case Repeat(t1, s) => Repeat(removeNullNodes(t1), s)::removeNullNodes(t.tail)
//          //the latter should not happen
//      }
//  }
//
//  def getTrails(root: WALA.CFGNode, wcontext: WALA.WALAContext) : List[Trail] = {
//      allTrails(root, wcontext, List[WALA.CFGNode](), List[(WALA.CFGNode,WALA.CFGNode)]()).map{case (x,y) => x}
//  }
//
//  def allTrails(root: WALA.CFGNode, wcontext: WALA.WALAContext, visited:List[WALA.CFGNode], avoidEdges:List[(WALA.CFGNode,WALA.CFGNode)]) : List[(Trail, WALA.CFGNode)] = {
//      var succNodes = wcontext.cfg.getSuccNodes(root).toList.filter{x => !avoidEdges.contains((root,x.asInstanceOf[WALA.CFGNode]))}
//
//      if (visited.contains(root)) {
//          return List((List(),root))
//      }
//
//      if (succNodes.isEmpty) {
//          return List((List(), null))
//      }
//
//      var tailTrails = succNodes.map{x =>
//          {
//              var l = allTrails(x.asInstanceOf[WALA.CFGNode], wcontext, root::visited,avoidEdges)
//              l.map{case (z,y) =>
//                  {
//                      if (y == root) {
//                          var repeatTrail = List(Trail.Repeat(Trail.Node(root,x.asInstanceOf[WALA.CFGNode])::z, SFix()))
//                          var otherTrails = allTrails(root, wcontext, visited, (root, x.asInstanceOf[WALA.CFGNode])::avoidEdges)
//
//                          otherTrails.map{
//                              case (t,v) => (repeatTrail:::t,v)
//                          }
//                      } else {
//                          List((Trail.Node(root,x.asInstanceOf[WALA.CFGNode])::z,y))
//                      }
//                  }
//              }.flatten
//          }
//      }.flatten
//
//      tailTrails
//  }
//
//  def corTrail(from: WALA.CFGNode, to: WALA.CFGNode, less: Int, wcontext: WALA.WALAContext, nodes:List[WALA.CFGNode]) : Trail = {
//      if (less < 0) {
//          if (wcontext.cfg.getSuccNodes(from).contains(to)) {
//              return List(Trail.Node(from,to))
//          } else {
//              return null
//          }
//      }
//
//      return constructOr(
//          constructSeq(
//              List(
//                  corTrail(from, nodes(less), less-1, wcontext, nodes),
//                  constructRep(
//                      corTrail(nodes(less), nodes(less), less-1, wcontext, nodes)
//                  ),
//                  corTrail(nodes(less), to, less-1, wcontext, nodes)
//              )
//          ),
//          corTrail(from, to, less-1, wcontext, nodes)
//      )
//  }
//
//  def getAllNodes(root: WALA.CFGNode, visited: List[WALA.CFGNode], wcontext: WALA.WALAContext) : List[WALA.CFGNode] = {
//      var newSuccNodes = wcontext.cfg.getSuccNodes(root).toList.filter{x => !(root::visited).contains(x)}
//      return root::newSuccNodes.map{x => getAllNodes(x.asInstanceOf[WALA.CFGNode], root::visited, wcontext)}.flatten.distinct
//  }
//
//  def constructSeq(l: List[Trail]) : Trail = {
//      return l.filter{ x => x != null}.flatten
//  }
//
//  def constructOr(l: Trail, r: Trail) : Trail = {
//      if (l == null || l.isEmpty()) {
//          return r
//      }
//      if (r == null || r.isEmpty()) {
//          return l
//      }
//
//      if (toStringRE(l) == toStringRE(r)) {
//          return l
//      }
//
//      return List(Trail.Or(l,r))
//  }
//
//  def constructRep(l: Trail) : Trail = {
//      if (l != null) {
//          if (l.isEmpty()) {
//              return l
//          }
//          return List(Trail.Repeat(l, SFix()))
//      }
//      return null
//  }
//
//
//
//
//
//  //---------------------Most General Trail
//
//
//  // Algorithm: Given CFG with source and sink
//  //            Return trail that describe all ways from getting from source to sink
//
//  // Based on state removal algorithm:
//  //            Find all loop nodes
//  //            Find all loop header nodes: Nodes that have appear on cycles that contain only nodes they dominate.
//  //            (Source and Sink nodes are never removed)
//  //            At each iteration step, if there is a non-loop node remove it.
//  //                                    else if there is loop node that is not a loop header, remove it
//  //                                    else remove loop header that does not dominate any other loop header
//  //                                    else return regexp from Source to Sink.
//
//
//  def MGT(from: WALA.CFGNode, to: WALA.CFGNode, wcontext: WALA.WALAContext) : Trail = {
//    println("from")
//    println(from)
//    println("to")
//    println(to)
//
//
//    println("\n\n\n\n\n\n\n\n\n getAdjMatGraph")
//    var adjMatGraph = getAdjMatGraph(from, wcontext)
//
//    adjMatGraph = findDominators(from, adjMatGraph)
//
//    println(toStringAdjMat(adjMatGraph))
//    println("\n\n\n\n\n\n\n\n\n")
//
//    // var fromN = from
//    // var toN = to
//    //
//    // for (n <- adjMatGraph.keys) {
//    //     if (n.getGraphNodeId() == 0) {
//    //         fromN = n
//    //     }
//    //     if (n.getGraphNodeId() == 15) {
//    //         toN = n
//    //     }
//    // }
//
//    while(!adjMatGraph.keys.filter{x => x != from && x != to}.isEmpty()) {
//        // var f = adjMatGraph.keys.filter{x => x.getGraphNodeId()!= 0 && x.getGraphNodeId() != 15}.head
//        var nextNodeToRemove = findNextNodeToRemove(from, to, adjMatGraph)
//        if (nextNodeToRemove != null) {
//            adjMatGraph = removeNode(nextNodeToRemove, adjMatGraph)
//
//            println("\n\n\n\n\n\n\n\n\n removed " + nextNodeToRemove.getGraphNodeId())
//            println(toStringAdjMat(adjMatGraph))
//            println("\n\n\n\n\n\n\n\n\n")
//        } else {
//            println("error")
//        }
//    }
//
//    //   val allNodes = getAllNodes(root: WALA.CFGNode, new List(), wcontext)
//    //   val adjMat = Array.ofDim[Trail](allNodes.size,allNodes.size)
//    return adjMatGraph(from)(to).edgeTrail
//    // return null
//  }
//
//  class GraphInfo(d: Boolean, l: Boolean, t: Trail) {
//      val dominates: Boolean = d
//      val loophead: Boolean = l
//      val edgeTrail: Trail = t
//  }
//
//  // def getAllNodes(root: WALA.CFGNode, wcontext: WALA.WALAContext, visited: List[WALA.CFGNode]) : List[WALA.CFGNode] = {
//  //     if (visited.contains(root)) {
//  //         return List[WALA.CFGNode]()
//  //     }
//  //
//  //     var visitedN = root::visited
//  //     var newNodes = wcontext.cfg.getSuccNodes(root).toList.filter{ x=> !visitedN.contains(x) }
//  //     var allNodes = List[WALA.CFGNode]()
//  //     while (!newNodes.isEmpty()) {
//  //         var node = newNodes.head.asInstanceOf[WALA.CFGNode]
//  //         allNodes = getAllNodes(node, wcontext, visitedN):::allNodes
//  //         visitedN = node::allNodes
//  //         newNodes = newNodes.filter{ x=> !visitedN.contains(x) }
//  //     }
//  //
//  //     allNodes
//  // }
//
//  type AdjMatGraph = MMap[WALA.CFGNode, MMap[WALA.CFGNode,GraphInfo]]
//
//  def getAdjMatGraph(root: WALA.CFGNode, wcontext: WALA.WALAContext) : AdjMatGraph = {
//      var visited : List[WALA.CFGNode] = List()
//      val allNodes = getAllNodes(root, List[WALA.CFGNode](),wcontext)
//
//      var adjMat = MMap[WALA.CFGNode, MMap[WALA.CFGNode,GraphInfo]]()
//      for (nodeFrom <- allNodes) {
//          var rowM = MMap[WALA.CFGNode, GraphInfo]()
//          for (nodeTo <- allNodes) {
//              var t : Trail = null
//              if (wcontext.cfg.getSuccNodes(nodeFrom).contains(nodeTo)) {
//                  t = List(Trail.Node(nodeFrom,nodeTo))
//              } else if (nodeFrom == nodeTo) {
//                  t = List()
//              }
//              var g = new GraphInfo(true, false, t)
//              rowM = rowM + (nodeTo -> g)
//          }
//
//          adjMat = adjMat + (nodeFrom -> rowM)
//
//      }
//
//      return adjMat
//  }
//
//  def removeNode(remNode: WALA.CFGNode, graph: AdjMatGraph) : AdjMatGraph = {
//      var adjMat = MMap[WALA.CFGNode, MMap[WALA.CFGNode,GraphInfo]]()
//
//      var allNodes = graph.keys.filter{ x => x != remNode}
//
//      for (nodeFrom <- allNodes) {
//          var rowM = MMap[WALA.CFGNode, GraphInfo]()
//          for (nodeTo <- allNodes) {
//              var t1 : Trail = graph(nodeFrom)(remNode).edgeTrail
//              var t2 : Trail = graph(remNode)(remNode).edgeTrail
//              var t3 : Trail = graph(remNode)(nodeTo).edgeTrail
//              var t : Trail = null
//              if (t1 != null && t3 != null) {
//                  t = t1:::getTrailRepeat(t2):::t3
//              }
//              var g = new GraphInfo(graph(nodeFrom)(nodeTo).dominates, graph(nodeFrom)(nodeTo).loophead, getTrailOr(graph(nodeFrom)(nodeTo).edgeTrail,t))
//              rowM = rowM + (nodeTo -> g)
//          }
//
//          adjMat = adjMat + (nodeFrom -> rowM)
//      }
//      return adjMat
//  }
//
//  def toStringAdjMat(graph: AdjMatGraph) : String = {
//      var s = "\t\t"
//      for (k <- graph.keys) {
//          s += k.getGraphNodeId().toString() + "\t"
//      }
//
//      s+= "\n"
//
//      for (k <- graph.keys) {
//          s += k.getGraphNodeId().toString() + "\t"
//          for (j <- graph(k).keys) {
//              s += Trail.toStringRE(graph(k)(j).edgeTrail) + "\t"
//            // s += graph(k)(j).dominates + "\t"
//          }
//          s+="\n"
//      }
//
//      s
//  }
//
//  def dfs(node: WALA.CFGNode, graph: AdjMatGraph, visited : List[WALA.CFGNode]) : List[WALA.CFGNode] = {
//      var visitedN = visited:::List(node)
//
//      var succNodes = getSuccNodes(node, graph)
//      for (s <- succNodes) {
//          if (!visitedN.contains(s)) {
//              visitedN = dfs(s, graph, visitedN)
//          }
//          visitedN = visitedN:::List(node)
//      }
//
//      return visitedN
//  }
//
//  def getReversePostOrdering(source: WALA.CFGNode, graph: AdjMatGraph) : List[WALA.CFGNode] = {
//      var visitedNodesOrder = List[WALA.CFGNode]()
//      var reversePostOrdering = List[WALA.CFGNode]()
//
//      visitedNodesOrder = dfs(source, graph, List[WALA.CFGNode]())
//
//      while (!visitedNodesOrder.isEmpty()) {
//          var v = visitedNodesOrder.last
//          visitedNodesOrder = visitedNodesOrder.reverse.tail.reverse
//
//          if (!reversePostOrdering.contains(v)) {
//              reversePostOrdering = v::reversePostOrdering
//          }
//      }
//
//      return reversePostOrdering.reverse
//  }
//
//  def findNextNodeToRemove(source: WALA.CFGNode, sink: WALA.CFGNode, graph: AdjMatGraph) : WALA.CFGNode = {
//      var allNodes = graph.keys.filter{ x=> x!= source && x!= sink}
//
//      for (n <- allNodes) {
//          if (getPredNodes(n,graph).filter{ y => y!= n}.length <= 1 && getSuccNodes(n,graph).filter{ y => y!= n}.length <= 1) {
//              return n
//          }
//      }
//
//      return findMostDominatedNode(source, sink, graph)
//  }
//
//  def findMostDominatedNode(source: WALA.CFGNode, sink: WALA.CFGNode, graph: AdjMatGraph) : WALA.CFGNode = {
//      var allNodes = graph.keys.filter{ x=> x!=source && x!= sink}
//
//      print("allNodes")
//      print(allNodes)
//      for (node <- allNodes) {
//          if (getDominatees(node, graph).filter{ x=> x!= source && x!= sink && x!= node}.isEmpty) {
//              return node
//          }
//      }
//
//      return null
//  }
//
//  def findDominators(source: WALA.CFGNode, graph: AdjMatGraph) : AdjMatGraph = {
//      //one pass using the reverse postordering of the graph may be enough only for reducible graphs
//      var changed = true
//      var reversePostOrdering = getReversePostOrdering(source, graph)
//      println("reverse post order:")
//      println(reversePostOrdering)
//      var graphN = graph
//
//      while (changed) {
//          changed = false
//          var reversePostOrderingMutable = reversePostOrdering
//          while (!reversePostOrderingMutable.isEmpty()) {
//              var v = reversePostOrderingMutable.head
//              reversePostOrderingMutable = reversePostOrderingMutable.tail
//              var predNodes = getPredNodes(v, graphN)
//              var intersectionSet = List[WALA.CFGNode]()
//
//              if (!predNodes.isEmpty()) {
//                  intersectionSet = getDominators(predNodes.head, graphN)
//
//                  for (g <- predNodes) {
//                      intersectionSet = getDominators(g, graphN).filter{x => intersectionSet.contains(x)}
//                  }
//              }
//
//              var newListOfDominators = List(v):::intersectionSet
//
//              if (!newListOfDominators.toSet.equals(getDominators(v, graphN).toSet)) {
//                  graphN = setDominators(v, graphN, newListOfDominators)
//                  changed = true
//              }
//          }
//      }
//
//      return graphN
//  }
//
//  def setDominators(node: WALA.CFGNode, graph: AdjMatGraph, newDominators: List[WALA.CFGNode]) : AdjMatGraph = {
//      var adjMat = MMap[WALA.CFGNode, MMap[WALA.CFGNode,GraphInfo]]()
//
//      for (nodeFrom <- graph.keys) {
//          var rowM = MMap[WALA.CFGNode, GraphInfo]()
//          for (nodeTo <- graph.keys) {
//              var dom = graph(nodeFrom)(nodeTo).dominates
//              if (nodeTo == node) {
//                  dom = newDominators.contains(nodeFrom)
//              }
//              var g = new GraphInfo(dom, graph(nodeFrom)(nodeTo).loophead, graph(nodeFrom)(nodeTo).edgeTrail)
//              rowM = rowM + (nodeTo -> g)
//          }
//
//          adjMat = adjMat + (nodeFrom -> rowM)
//
//      }
//
//      return adjMat
//  }
//
//  def getDominatees(node: WALA.CFGNode, graph: AdjMatGraph) : List[WALA.CFGNode] = {
//      var dom = List[WALA.CFGNode]()
//
//      for (curnode <- graph.keys) {
//          if (graph(node)(curnode).dominates) {
//              dom = curnode::dom
//          }
//      }
//
//      return dom
//  }
//
//  def getDominators(node: WALA.CFGNode, graph: AdjMatGraph) : List[WALA.CFGNode] = {
//      var dom = List[WALA.CFGNode]()
//
//      for (curnode <- graph.keys) {
//          if (graph(curnode)(node).dominates) {
//              dom = curnode::dom
//          }
//      }
//
//      return dom
//  }
//
//  def getSuccNodes(node: WALA.CFGNode, graph: AdjMatGraph) : List[WALA.CFGNode] = {
//      graph(node).filter{ case (x,y) => x != node && y.edgeTrail != null}.keys.toList
//  }
//
//  def getPredNodes(node: WALA.CFGNode, graph: AdjMatGraph) : List[WALA.CFGNode] = {
//    //   graph.filter{ case (x,y) => { case y => { case (z,w) => z == node && w.edgeTrail != null}}}.keys.toList
//
//      var predNodes = List[WALA.CFGNode]()
//
//      for (n <- graph.keys) {
//          if (n != node && graph(n)(node).edgeTrail != null) {
//              predNodes = n::predNodes
//          }
//      }
//
//      return predNodes
//  }
//
//  def getSuccNodesIncludingSelf(node: WALA.CFGNode, graph: AdjMatGraph) : List[WALA.CFGNode] = {
//      (graph(node).filter{ case (x,y) => y.edgeTrail != null}).keys.toList
//  }
//
//  //use Trail constructors for the following:
//  def getTrailOr(t1: Trail, t2: Trail) : Trail = {
//      if (t1 == null) {
//          if (t2 == null) {
//              return null
//          } else {
//              return t2
//          }
//      } else {
//          if (t2 == null) {
//              return t1
//          } else {
//              return List(Trail.Or(t1,t2))
//          }
//      }
//  }
//
//  def getTrailRepeat(t: Trail) : Trail = {
//      if (t == null) {
//          return null
//      } else if (t.isEmpty) {
//          return t
//      } else {
//          return List(Trail.Repeat(t, SFix()))
//      }
//  }
//}

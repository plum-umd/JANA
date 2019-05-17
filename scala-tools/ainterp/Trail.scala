//
//import scala.collection.immutable.{Map=>IMap, HashMap, Set=>ISet}
//import scala.collection.mutable.{Set,Stack,Queue}
//
//import collection.JavaConversions._
//
//import Core._
//import WALA.Edge
//import WALA._
////import AInterp.Edge
//
//object Trail {
//
//  abstract class S
//
//  case class SInt(i:Int) extends S
//  case class SFix() extends S
//
//  abstract class Trailspec
//
//  type Trail = List[Trailspec]
//
//  case class Node(curr:WALA.CFGNode,next:WALA.CFGNode) extends Trailspec
//
//  /**
//    * A well-formed Or trail's subtrails must have an edge to the same
//    * block.
//    */
//  case class Or(t1:Trail, t2:Trail) extends Trailspec
//
//  /**
//    * A well-formed Repeat trail's subtrail must have a backedge to
//    * head block.
//    */
//  case class Repeat(t:Trail, s:S) extends Trailspec
//
//  def dummy(cfg:WALA.CFGNode) : Trail = {
//    return List[Trailspec](Node(cfg,cfg))
//  }
//
//  def toString(tr:Trail) : String = {
//    tr.map{ trs => toString(trs) }.mkString(",")
//  }
//  def toString(trspec:Trailspec) : String = {
//    trspec match {
//      case Node(f,t) => "Node("+ f.toString() + ","+ t.toString() + ")"
//      case Or(t1,t2) => "Or("+ toString(t1) + ","+toString(t2)+")"
//      case Repeat(t,s) => "Repeat("+toString(t)+",s)"
//      case null => "_"
//    }
//  }
//
//  def toStringRE(tr:Trail) : String = {
//    tr match {
//        case null => "_"
//        case _ => "("+tr.map{ trs => toStringRE(trs) }.mkString(";")+")"
//    }
//  }
//  def toStringRE(trspec:Trailspec) : String = {
//    trspec match {
//      case Node(f,t) => "("+ f.getGraphNodeId().toString() + ","+ t.getGraphNodeId().toString() + ")"
//    //   case Node(f,t) => "Node("+ f.toString() + ","+ t.toString() + ")"
//      case Or(t1,t2) => "{ "+ toStringRE(t1) + " || "+toStringRE(t2)+" }"
//      case Repeat(t,s) =>
//        s match {
//          case SFix() =>
//            "[ "+toStringRE(t)+" ]*"
//          case SInt(n) =>
//            "[ "+toStringRE(t)+" ]^" + n
//        }
//      case null => "_"
//    }
//  }
//
//  def buildTrailsForLoop( cfg:WALA.CFG,
//												  head:WALA.CFGNode,
//		                      nodes:ISet[WALA.CFGNode]) :
//                          List[Trail] =
//  {
//    var trails = List[Trail]()
//    var wl = Queue[(WALA.CFGNode,Trail)]((head, List[Trailspec]()))
//    var visited = Set[WALA.CFGNode]()
//
//    while(!wl.isEmpty) {
//      val frontier = wl.head._1
//      val trail : Trail = wl.head._2
//
//      wl = wl.drop(1)
//
//      val succs = cfg.getNormalSuccessors(frontier)
//
//      visited = visited + frontier
//
//      for( succc <- succs ) {
//        val succ = succc.asInstanceOf[WALA.CFGNode]
//
//        val newtrail = trail ++ List[Trailspec](Node(frontier,succ))
//
//        if( !succ.isExitBlock &&
//            (nodes contains succ) &&
//            !(visited contains succ))
//        {
//          wl += ((succ,newtrail))
//        }
//        else
//        {
//          trails = trail :: trails
//        }
//      }
//    }
//
//    trails
//	}
//
//  def stepTrailspec(
//    context: InterpContext,
//    cfg:WALA.CFG,
//    ir:WALA.IR,
//    m:AbstractMachine,
//    t:Trailspec,
//    lastin:WALA.CFGNode,
//    varAccum:scala.collection.mutable.Map[Trailspec, List[String]]
//  ) : (AbstractMachine, WALA.CFGNode) = {
//    var last = lastin
//    t match {
//      case Node(from,to) =>
//        // println("stepTrailspec " + from + " " + to)
//        // //take one step, from to to
//        // //return the machine where the next state is 'to'
//        // val inedge = (last, from)
//
//        // // get all succs, remove outedge
//        // // val next = wcontext.cfg.getSuccNodes(n).toList.asInstanceOf[List[WALA.CFGNode]].head
//        // val outedges = cfg.getSuccessorNodes(from)
//
//        // val map = stepRegion()
//
//        // map.get((from, to).asInstanceOf[Edge])
//
////      	var p = m.stepPhis(context, last).newFrame
//        var p = m.newFrame
//      	val o = p.stack.head
//      	val n = o.copy(pc = WALA.IAddr(ir, from, from.getFirstInstructionIndex()))
//      	p = p.copy( stack = n :: p.stack)
//        p = p.stepPhis(context, last)
//        val nm = AInterp.stepBlock(p)
//        val next = WALA.IAddr(ir, to, to.getFirstInstructionIndex())
//        val nml = nm.filter((b) => b.stack.head.pc == next)
//        // println("m " + m)
//        // // println("from " + from)
//        // // println("to " + to)
//        // println("next " + next)
//        // println("nm " + nm)
//         // println("nml " + nml)
//
//        if (nml.length == 1) {
//          //WALA.getSourceVarNames(ir,from).foreach { v => varAccum(v) = true }
//          varAccum(t) = WALA.getSourceVarNames(ir,from)
//          (nml.head, from)
//        } else {
//          throw Core.LogicException("no branches for next follow - impossible path")
//        }
//      case Or(t1,t2) =>
//        val (m1, last1) = stepTrail(context, cfg,ir,m,t1,last,varAccum)
//        val (m2, last2) = stepTrail(context, cfg,ir,m,t2,last,varAccum)
//        val new_m = AbstractMachine.join(m1, m2)
//        var new_last = last1 // TODO what is the new "last" node when there is an Or?
//        (new_m, new_last)
//      case Repeat(t,s) => s match {
//        case SFix() =>
//          //stepTrail on t until fixed
//          //we are basically always considering this as while(true) { t }
////          var m2 = m.stepPhis(context, last)
//          var m2 = m
//          var hist = List[AbstractMachine]()
//          var stop = false
//          var finalm = m2
//          var new_last = last
//          // TODO: test with twoloops1, which appears to widen, or at
//          // least for the next frame to be the loop exit
//          // println("REPEAT OVER " + toStringRE(t))
//          while (!stop) {
//            // TODO we need widening here
//            // println("JFDSLJFLDSJKFLDSJKLFDSJLKFJLDSKFJDSLK")
//            // // val (tmpm, tmp_last) = stepTrail(context, cfg,ir,hist.head,t,new_last)
//            // println(t)
//            val (tmpm, tmp_last) = stepTrail(context, cfg,ir,m2,t,new_last,varAccum)
////            m2 = tmpm.stepPhis(context, tmp_last)
//            m2 = tmpm
//            new_last = tmp_last
//            (Joiner.joinerBack(hist)(None)(m2)) match {
//              case Some(joined_m) =>
//                // println("SOME")
//                hist = joined_m :: hist
//                // if the through-trail of the loop is no longer
//                // feasible, then we have already reached the
//                // fixpoint, so stop
//                val isRepeatFeasible = try {
//                  // println(new_last)
//                  // println(t.head)
//                  stepTrail(context, cfg, ir, m2, t,      new_last,varAccum)
//                  // stepTrail(context, cfg, ir, m2, t.head, new_last, varAccum)
//                  true
//                } catch {
//                  case e: Core.LogicException =>
//                    false
//                }
//                // println("isRepeatFeasible" + isRepeatFeasible)
//                if (!isRepeatFeasible) {
//                  stop = true
//                  finalm = AbstractMachine.join(hist)
//                }
//              case None =>
//                // println("NONE")
//                stop = true
//                finalm = AbstractMachine.join(hist)
//            }
//          }
////          finalm.stepPhis(context, new_last)
//          (finalm.joinAllStates(), new_last)
//        case SInt(n) =>
//          //stepTrail on t i times, don't care about fixpoint really
//          var cm = m
//          var new_last = last
//          for(i <- 1 to n) {
//            val (tmp_cm, tmp_last) = stepTrail(context, cfg,ir,cm,t,last,varAccum)
//            cm = tmp_cm
//            new_last = tmp_last
//          }
//          (cm, new_last)
//      }
//    }
//  }
//
//  //step over each thing in the trail
//  def stepTrail(
//    context: InterpContext,
//    cfg:WALA.CFG,
//    ir:WALA.IR,
//    startm:AbstractMachine,
//    trail:Trail.Trail,
//    last:WALA.CFGNode,
//    varAccum:scala.collection.mutable.Map[Trailspec, List[String]]) : (AbstractMachine, WALA.CFGNode) =
//  {
//    var m = startm
//    var new_last = last
//    for(t <- trail) {
//      // println("FDJSKL")
////      m = m.stepPhis(context, new_last)
//      val (tmp_m, tmp_last) = stepTrailspec(context, cfg, ir, m, t, new_last, varAccum)
//      m = tmp_m
//      new_last = tmp_last
////      m = m.stepPhis(context, new_last)
//    }
//
//    (m, new_last)
//  }
//
//}

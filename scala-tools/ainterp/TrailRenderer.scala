//import scala.collection.Map
//import scala.collection.Map._
//import scala.collection.mutable.{Map}
//import scala.sys.process.Process
//import java.nio.file.{Files,Path}
//import Trail._
//import WALA._
//import BoundExpressions._
//
//object TrailRenderer {
//  val INCOMPATcolorList = List("brown1", "burlywood1", "darkgreen", "darkolivegreen4",
//    "darkorange", "darkorchid3", "dogerblue1", "firebrick2", "gold3",
//    "cadetblue4")
//
//  val colorList = List("brown", "red", "green", "blue", "cyan",
//    "gold", "purple", "orange", "violet", "gray", "darkgreen")
//
//  var colors = colorList
//  var curColor : String = "black"
//
//  def open(wcontext:WALA.WALAContext,
//    edgeMap:scala.collection.mutable.Map[WALA.CFGNode,List[WALA.CFGNode]],
//    outDir:Path) : DotWriter = {
//	
//    colors = colorList
//    val dotWriter = new DotWriter("diag_"+wcontext.method.getName().toString(), outDir)
//    dotWriter.write("digraph g {\n")
//    dotWriter.write(" 1 [style=filled, label=\"1\", fillcolor=black]\n")
//    for ((k,v) <- edgeMap) {
//      for (vi:WALA.CFGNode <- v.toSet) {
//        dotWriter.write("  "+k.getGraphNodeId()+" -> "+vi.getGraphNodeId()+"\n")
//      }
//    }
//    return dotWriter
//  }
//
//  def incColor() : String = {
//    colors match {
//      case c :: r =>
//        curColor = c
//        colors = r
//      case _ =>
//    }
//    curColor
//  }
//
//  def addTrailspec(trailspec:Trailspec,dotWriter:DotWriter,step:Int,
//    vars:scala.collection.mutable.Map[Trail.Trailspec,List[String]]) : Int = {
//    trailspec match {
//      case Node(n,n2) =>
//        dotWriter.write("  "+n.getGraphNodeId()+" -> "+n2.getGraphNodeId()+" [penwidth=2.0, color="+curColor+", labelcolor="+curColor+", label=\""+step+"\"]\n")
//        val vs = vars.get(trailspec) match {
//          case Some(sl) => sl
//          case None => (n.getGraphNodeId()+"->"+n2.getGraphNodeId()) :: List[String]()
//        }
//        dotWriter.write("  "+n.getGraphNodeId()+" [label=\""+n.getGraphNodeId()+"-["+vs.mkString(", ")+"]->"+n2.getGraphNodeId()+"\"];\n")
//        step
//      case Or(t1,t2) =>
//        var i = addTrail(t1,dotWriter,step,vars)
//        addTrail(t2,dotWriter,i+1,vars)
//      case Repeat(t,SFix()) =>
//        addTrail(t,dotWriter,step,vars)
//      case Repeat(t,SInt(i)) =>
//        addTrail(t,dotWriter,step,vars)
//    }
//  }
//
//  def addTrail(trail:Trail,dotWriter:DotWriter,step:Int,
//  vars:scala.collection.mutable.Map[Trail.Trailspec,List[String]]) : Int = {
//    var nextstep = step
//    for (ts <- trail) {
//      nextstep = addTrailspec(ts,dotWriter,nextstep+1,vars)
//    }
//    nextstep
//  }
//
//  def addFeasibleTrail(
//    trail:Trail,
//    trailsWriter:DotWriter,
//    trailVars:scala.collection.mutable.Map[Trail.Trailspec,List[String]],
//    bound:BoundExpression
//  ) : String =
//  {
//    var html = ""
//    val color = incColor()
//    addTrail(trail,trailsWriter,0,trailVars)
//    html += "<li><b>Trail:</b><font color=\""+color+"\">"+Trail.toStringRE(trail) + "</font><br>"
//    html += "<b>Bound:</b>"+bound.toString()+"<br>"
//    // Get the list of all variables on all segments of this trail
//    val allVars = trailVars.map{ x => x match { case (trsp,vars) => vars } }.foldLeft(List[String]()) {
//      case (l, vl) => l ++ vl
//    }.toSet[String]
//    html += "<b>Vars on this trail:</b> <font face=Courier>"+allVars.mkString(", ")+"</font>"
//    return html
//  }
//
//  def close(dotWriter:DotWriter) {
//    dotWriter.write("}\n")
//    dotWriter.close()
//  }
//}	

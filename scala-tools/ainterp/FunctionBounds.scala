//import edu.illinois.wala.Facade._
//
//import BoundExpressions._
//import CFGAnnotations._
//import java.io._
//import Label._
//import Util._
//import Core._
//import Trail._
//
//
//import Specification._
//
//import scala.collection.Map
//import scala.collection.Map._
//import scala.collection.immutable.{Map=>IMap, HashMap, Set=>ISet}
//import GlobalHeap._
//
//import scala.collection.JavaConversions._
//
//object FunctionBounds {
//  type BExpr = BoundExpression
//  type BEAggregator = BExpr => BExpr => BExpr
//
//  // Previously computed FunctionBounds
//  var mapBound : Map[String, BoundExpression] = null
//
//  class InvalidTreeException(message: String = null, cause: Throwable = null)
//      extends RuntimeException(message, cause)
//
//  // ***********************************************
//  // Output to DOT helper functions
//  // ***********************************************
//  var dotWriter : DotWriter = null
//  var dotPrefix : String = "default"
//  def dotout(s:String) : Unit = {
//    dotWriter.write(s + "\n")
//  }
//  def nodeID_of_ASTNode(n:ASTNode) : String = {
//    if (n == null) { return dotPrefix+"_origin"; }
//
//    n match {
//      case ASTNodeLoop(_,_,wn2:WALA.CFGNode) =>
//        return dotPrefix + "_L_" + wn2.getGraphNodeId().toString()
//      case ASTNodeIf(_,_,wn2:WALA.CFGNode) =>
//        return dotPrefix + wn2.getGraphNodeId().toString()
//      case ASTNodeSLC(_,_,wn2:WALA.CFGNode) =>
//        return dotPrefix + wn2.getGraphNodeId().toString() 
//      case ASTNodeSeq(_,_,i:String) =>
//        return dotPrefix + i
//      case ASTNodeFunc(_,_,wn2:WALA.CFGNode) =>
//        return dotPrefix + wn2.getGraphNodeId().toString() 
//      case ASTNodeAny(_,_,_) =>
//        throw new InvalidTreeException("err: nodeID_of_ASTNode(ASTNodeAny)")
//      case null =>
//        throw new InvalidTreeException("err: nodeID_of_ASTNode=null")
//      case _ =>
//        throw new InvalidTreeException("err: nodeID_of_ASTNode="+n.toString(1))
//    }
//  }
//  def node2dot(n:ASTNode,lab:String,extras:String) : String = {
//    return nodeID_of_ASTNode(n) + " [label = \""+lab+"\" "+extras+"]"
//  }
//  def nodeLabel(fc:ASTNode, ns:ASTNode, nodeType:String) : String = {
//    return (nodeType + "("
//      + (if (fc==null) "" else "fc," )
//      + (if (ns==null) "" else "ns" ) + ")")
//  }
//  def startEdge2dot(src:ASTNode) : String = {
//    return (dotPrefix+"_origin -> "+nodeID_of_ASTNode(src)+"\n"
//      + dotPrefix+"_origin [style=filled, label=\"\", fillcolor=black]")
//  }
//  def termEdge2dot(n1ID:String) : String = {
//    return dotPrefix+"dead"+n1ID + " [style=filled, label=\"slc"+n1ID+"\"]\n" + dotPrefix + n1ID + " -> "+dotPrefix+"dead"+n1ID
//  }
//  def edge2dotAN(src:ASTNode,dest:ASTNode,l:String) : String = {
//    //if (src==null || dest==null) { throw new InvalidTreeException("edge2dot got null"); }
//    return (nodeID_of_ASTNode(src) + " -> " + nodeID_of_ASTNode(dest) + " " + l)
//  }
//
//  // ***********************************************
//  // Looking up loop bounds
//  // ***********************************************
//  var n2b : Map[WALA.CFGNode, List[(Trail,BExpr)]] = null
//  def beList2poly(bel:List[BoundExpression], context: InterpContext) : ISet[Polynomial] = {
//    bel.map{ b => PolynomialUtil.construct(b, context) }.toSet[Polynomial]
//  }
//  def renderCalleeBound(an:ASTNode,wn:WALA.CFGNode, be:BExpr) : String = {
//    val nID = nodeID_of_ASTNode(an)
//    //val allbounds = be.toHTMLString()
//    val allbounds = be.toString()
//    return (nID + "_callbounds -> " + nID  + "\n"
//      + nID + "_callbounds [ style=filled, label=\"Callee Bounds:\\l "+allbounds+"\", shape=rectangle, fillcolor=pink ]\n"
//      + "{rank=same}; " + nID + "_callbounds " + nID
//    )
//  }
//  def renderBoundList(an:ASTNode,wn:WALA.CFGNode, context: InterpContext) : String = {
//    val nID = nodeID_of_ASTNode(an)
//    val allbounds = n2b.get(wn) match {
//      case Some( l_tr_be ) =>
//        beList2poly(l_tr_be.map(_._2), context).map(x => "Bound: "+x.toString).mkString(",\\l")
//      case None => "(bound not found)"
//    }
//    return (nID + "_bounds -> " + nID  + "\n"
//      + nID + "_bounds [ style=filled, label=\""+allbounds+"\", shape=rectangle, fillcolor=lightblue ]\n"
//      + "{rank=same}; " + nID + "_bounds " + nID
//    )
//  }
//  def mkLoopBoundExp(wcfgnode:WALA.CFGNode,body:BExpr, context: InterpContext) : BExpr = {
//    return new BoundExpressionMult(
//      n2b.get(wcfgnode) match {
//        case Some( l_tr_be ) =>
//          // convert to polynomials
//          val polys = beList2poly(l_tr_be.map(_._2), context).toList
//          // create a max of the polys
//          if (polys.length > 0) {
//            BoundExpressions.maxOfPolyList(polys)
//          } else {
//            null
//          }
//        case None =>
//          println("warning: no bound specified for " + wcfgnode)
//          // new BoundExpressionNaN("no bound found")
//          new BoundExpressionConst(0)
//      },
//      body)
//  }
//
//  // ***********************************************
//  // Building the function summary tree
//  // ***********************************************
//  def getCalledFun(wn: WALA.CFGNode) : String = {
//    // lookup last instrution
//    val instr = wn.getLastInstruction()
//    instr match {
//      case i: InvokeI => i.getDeclaredTarget.getSignature()
//      case _ => throw new InvalidTreeException("getCalledFun: dunno target")
//    }
//  }
//  def dotAll(fc:ASTNode,ns:ASTNode,aa:Any,annNode:ASTNode,parent:ASTNode,extras0:String) : Unit = {
//    val (ntype,extras) = annNode match {
//      case ASTNodeLoop(_,_,wn2:WALA.CFGNode) => ("Loop","")
//      case ASTNodeIf(_,_,wn2:WALA.CFGNode)   =>
//        if (annNode.hasLoopNodeDescendant()) {
//          ("If", ", color=red")
//        } else { ("If","") }
//      case ASTNodeSLC(_,_,wn2:WALA.CFGNode)  => ("SLC","")
//      case ASTNodeSeq(_,_,i:String)          => ("Seq","")
//      case ASTNodeFunc(_,_,wn2:WALA.CFGNode) => ("Func["+getCalledFun(wn2)+"]","")
//    }
//    dotout(node2dot(annNode,nodeLabel(fc,ns,ntype),extras+extras0))
//    if (fc==null) { } // dotout(termEdge2dot(wnID)); }
//    else          { dotout(edge2dotAN(annNode,fc," [label=\"body\"]")); }
//    if (ns!=null) {
//      //dotout(edge2dotAN(annNode,ns," [style=dashed,label=\"ns\"]"))
//      dotout(edge2dotAN(parent,ns," [label=\"body\"]"))
//    }
//  }
//  def build(annNode:ASTNode, currAgg:BEAggregator, context: InterpContext, parent:ASTNode) : BExpr = {
//    if(annNode == null) return new BoundExpressionConst(777);
//
//    annNode match {
//
//      case ASTNodeLoop(fc,ns,wn:WALA.CFGNode) =>
//        val extras = n2b.get(wn) match {
//          case Some( l_tr_be ) =>
//            val polys = beList2poly(l_tr_be.map(_._2), context).toList
//            if (polys.length > 1) { ", color=red" } else { "" }
//          case _ => ""
//        }
//        dotAll(fc,ns,wn,annNode,parent,extras)
//        dotout(renderBoundList(annNode,wn, context))
//
//        // Set up new aggregate for children
//        val fc_be = build(fc,
//          (be1:BExpr) => (be2:BExpr) => new BoundExpressionSum(be1,be2),
//          context, annNode)
//        val my_be = mkLoopBoundExp(wn,fc_be, context)
//        if(ns==null) { return my_be }
//        else {
//          // Pass the current aggregate to siblings and use it here
//          val ns_be = build(ns,currAgg, context, parent)
//          return currAgg(my_be)(ns_be)
//        }
//
//      case ASTNodeIf(fc,ns,wn:WALA.CFGNode) =>
//        dotAll(fc,ns,wn,annNode,parent,"")
//        // Set up new aggregate for children
//        val fc_be = build(fc,
//          (be1:BExpr) => (be2:BExpr) => new BoundExpressionMax(be1,be2),
//          context, annNode)
//        if(ns==null) { return fc_be; }
//        else {
//          // Pass the current aggregate to siblings and use it here
//          val ns_be = build(ns,currAgg, context, parent)
//          return currAgg(fc_be)(ns_be)
//        }
//
//      case ASTNodeSeq(fc,ns,i:String) =>
//        dotAll(fc,ns,i,annNode,parent,"")
//        // Set up new aggregate for children
//        val fc_be = build(fc,
//          (be1:BExpr) => (be2:BExpr) => new BoundExpressionSum(be1,be2), context, annNode)
//        if(ns==null) { return fc_be; }
//        else {
//          // Pass the current aggregate to siblings and use it here
//          val ns_be = build(ns,currAgg, context,parent)
//          return currAgg(fc_be)(ns_be)
//        }
//
//      case ASTNodeSLC(null,ns,wn:WALA.CFGNode) =>
//        dotAll(null,ns,wn,annNode,parent,"")
//        // TODO : instruction count
//        // val my_be = new BoundExpressionConstC()  // breaks polynomial
//        val my_be = new BoundExpressionConst(1)
//        if(ns==null) { return my_be; }
//        else {
//          // Pass the current aggregate to siblings and use it here
//          val ns_be = build(ns,currAgg, context, parent)
//          return currAgg(my_be)(ns_be)
//        }
//
//      case ASTNodeFunc(null,ns,wn:WALA.CFGNode) =>
//        dotAll(null,ns,wn,annNode,parent,"")
//        // Get the bound expresssion of the callee
//        // TODO : substitute the args.
//        val callee_name = getCalledFun(wn)
//        //getFunName(GlobalHeap.cg.getSuccNodes(wn).toList)
//        val callee_be = mapBound.get(callee_name) match {
//          case Some(be) => be
//          case None => 
//            // Maybe it is a library?
//            Specification.getSpecs.get( callee_name ) match {
//              case Some(yamlspec) =>
//                println("FunctionBounds: use bound spec for "+callee_name)
//                yamlspec.bound
//              case None =>
//                println("FunctionBounds: no bound spec found for "+callee_name)
//                //new BoundExpressionConst(666)
//                new BoundExpressionConst(1)
//            }
//        }
//        if(callee_be.isConst() == false) {
//          dotout(renderCalleeBound(annNode,wn,callee_be))
//        }
//        if(ns==null) { return callee_be }
//        else {
//          // Pass the current aggregate to siblings and use it here
//          val ns_be = build(ns,currAgg, context, parent)
//          return currAgg(callee_be)(ns_be)
//        }
//
//      //case ASTNodeCall...) =>
//        // Specification.lookup(other fun)
//
//      case ASTNodeAny(fc,ns,wn:WALA.CFGNode) =>
//        throw new InvalidTreeException("unexpected case: ASTNodeAny!!!")
//
//      case null =>
//        throw new InvalidTreeException("case: null!!!")
//
//      case _ =>
//        throw new InvalidTreeException("case: unknown!!!")
//    }
//    return new BoundExpressionConst(0);
//  }
//
//  def summarize(
//    annNode:ASTNode,
//    n2b0:Map[WALA.CFGNode, List[(Trail,BExpr)]],
//    spec:Option[Specification],
//    funname:String, sig:String,
//    dotwriter0:DotWriter,
//    htmlWriter:PrintWriter,
//    mapBound0:Map[String, BExpr],
//    context: InterpContext
//  ) : BExpr = {
//
//    n2b = n2b0
//    dotWriter = dotwriter0
//    dotPrefix = funname
//    mapBound = mapBound0
//    val annNode1 = annNode.simplify(null)
//    val (specStr,(computedStr,boundOut:BExpr)) = spec match {
//      case Some(s) =>
//        (s.toString(),("(Have bound spec, so did not analyze)",s))
//      case None =>
//        dotout(startEdge2dot(annNode1))
//        val overall = build(annNode1,
//          (be1:BExpr) => (be2:BExpr) => new BoundExpressionSum(be1,be2), context,null).simplify(context)
//        ("(no spec provided)",(overall.toHTMLString(),overall))
//    }
//
//    // val summaryPolynomial = PolynomialUtil.construct(boundOut, context)
//
//    htmlWriter.write("<p>")
//    htmlWriter.write("<b>Function Signature:</b><font face=\"Courier\">"+sig+"</font><br>\n")
//    htmlWriter.write("<b>User Specification:</b>\n<pre>\n"+specStr+"</pre>\n")
//    htmlWriter.write("<b>Bound (Spec):</b>\n<pre>\n"+specStr+"</pre>\n")
//    // htmlWriter.write("<b>Bound (Computed):</b>\n<font face=\"Courier\">\n" + summaryPolynomial.toString().replaceAll("ⁱⁿ", "<sup>in</sup>")+"</font>\n")
//    htmlWriter.write(htmlExpandable("InterBound Computation","<pre>\n"+computedStr+"</pre>\n"))
//    return boundOut
//  }
//
//  // def loadLibraries(mapBound:scala.collection.mutable.Map[String,BExpr]) : Unit = {
//  //   mapBound += ("java.lang.String.length()I" -> 
//  //     new BoundExpressionVarLabel(new FrameParameter((0,0),1)))
//  // }
//  def htmlExpandable(title:String,payload:String) : String = {
//    return ("<div class=\"container\">\n" +
//      "<div class=\"preheader\">"+title+"</div>\n" +
//      "<div class=\"header\"><span>Click to Expand</span></div>\n" +
//      "<div class=\"content\">"+payload+"</div>\n" +
//      "</div>\n")
//  }
//  def htmlHeader() : String = {
//    return ("<!DOCTYPE html>\n" +
//      "<html><head>\n" +
//      "<script type=\"text/javascript\" src=\"http://code.jquery.com/jquery-1.9.1.js\"></script>\n" +
//      "<style type=\"text/css\">\n" +
//      ".container {\n" +
//      "  width:100%;\n" +
//      "  border:1px solid #d3d3d3;\n" +
//      "}\n" +
//      ".container div {\n" +
//      "  width:100%;\n" +
//      "}\n" +
//      ".container .preheader {\n" +
//      "  background-color:#d3d3d3;\n" +
//      "  padding: 2px;\n" +
//      "  font-weight: bold;\n" +
//      "}\n" +
//      ".container .header {\n" +
//      "  background-color:#e3e3e3;\n" +
//      "  padding: 2px;\n" +
//      "  cursor: pointer;\n" +
//      "  text-decoration: underline;\n" +
//      "  color: blue;\n" +
//      "}\n" +
//      ".container .content {\n" +
//      "  display: none;\n" +
//      "  padding : 5px;\n" +
//      "}\n" +
//      "</style>\n" +
//      "<script type='text/javascript'>//<![CDATA[\n" +
//      "$(window).load(function(){\n" +
//      "$(\".header\").click(function () {\n" +
//      "    $header = $(this);\n" +
//      "    $content = $header.next();\n" +
//      "    $content.slideToggle(500, function () {\n" +
//      "        $header.text(function () {\n" +
//      "            return $content.is(\":visible\") ? \"Click to Collapse\" : \"Click to Expand\";\n" +
//      "        });\n" +
//      "    });\n" +
//      "});\n" +
//      "});//]]> \n" +
//      "</script>\n" +
//      "</head>\n" +
//      "<body>\n" +
//      "<h1>InterBounds Summary</h1>")
//  }
//  def htmlFooter() : String = {
//    return "</body></html>"
//  }
//}
///*
//WORKING
//sbt 'run -a funsum -D../tests/ --debug -SNestedLoops.* "NestedLoops.nestedLoops1(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SNestedLoops.* "NestedLoops.nestedLoops2(II)I"'
//
//STAGED
//sbt 'run -a funsum -D../../examples/staged/4/ -SDelimSearch.* "DelimSearch.search(IIII)"'
//
// */
//
///*
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.testChat()I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.testForLoop(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.search(IIII)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test1(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test1anno(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test1minus(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test1renamed(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test2(III)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test3(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test4(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test5(I)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.twovars1(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.twovars2(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.twoloops1(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.twoloops1sep(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.test_image(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.bar1(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.bar2(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.bar3(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.goo0(I)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.goo1(I)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.goo2(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.updown(II)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.goo3()I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.nested(I)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.testJoin6(I)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.testJoin7(I)I"'
//sbt 'run -a funsum -D../tests/ --debug -SPPLTest.* "PPLTest.nonconstantPre(III)I"'
// */

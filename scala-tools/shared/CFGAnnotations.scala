// import MyExpression.{Expression=>MExpression}
import edu.illinois.wala.Facade._

import Util._
import Core._

import com.ibm.wala.classLoader.ProgramCounter

import scala.collection.Map
import scala.collection.Map._
import scala.collection.immutable.{Map=>IMap, HashMap}
import scala.collection.mutable.{Set,Stack}

//import com.ibm.wala.types.TypeReference
import scala.sys.exit

import collection.JavaConversions._

/**
 * Annotations to the WALA IR CFG.
 */
object CFGAnnotations {
  /** The annotation superclass. */
  class Annotation {
  }

  /** An annotation that holds loop bounds. */
  // class Bounds(bound : MExpression) extends Annotation {
  case class Bounds(bound : Integer) extends Annotation {
  }

  /** An annotation that enables structure to avoid traverse entire CFG. */
  case class Node(firstChild : Node, nextSibling : Node) extends Annotation {
    def getNextSibling : Node = return nextSibling
    def getFirstChild : Node = return firstChild
  }

  // won't create instances of ASTNode, so we can make it abstract
  abstract class ASTNode(val firstChild : ASTNode, val nextSibling : ASTNode, val value: Any) {
    var unique_id : Int = 81721239
    def getNextSibling : ASTNode = return nextSibling
    def getFirstChild : ASTNode = return firstChild
    def getValue : Any = return value
      override def toString() : String = {
          var s :String = ""
          s += value.toString()
          if (firstChild != null) {
            s += "\n First child: " + firstChild.toString()
          } else {
            s+= "\n no first child"
          }
          if (nextSibling != null) {
            s += "\n Next Sibling: " + nextSibling.toString()
          } else {
            s+= "\n no next sibling"
          }

          return s
      }

      def toString(level: Int) : String = {
          var s :String = ""
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      def simplify(parentNode: ASTNode) : ASTNode = {
          return this
      }

      def overSimplify(parentNode: ASTNode) : ASTNode = {
          return simplify(parentNode)
      }

      def addNextSibling(ns : ASTNode) : ASTNode = {
          return this
      }

      def isForest() : Boolean = {
          return (nextSibling != null)
      }

      def hasLoopNodeDescendant(lookAtNS : Boolean = false) : Boolean = {
          var down : Boolean = false
          var right : Boolean = false
          if (getFirstChild != null) {
              down = getFirstChild.hasLoopNodeDescendant(true)
          }
          if (getNextSibling != null && lookAtNS) {
              right = getNextSibling.hasLoopNodeDescendant(true)
          }
          return down || right
      }

      def isAncestorNode(node1: WALA.CFGNode, node2: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : Boolean = {
          if (slcNodesSorted.contains(node1)) {
              return false
          } else if (!conditionalNodesSorted.contains(node1)) {
              if (isBefore(order)(node1, node2) && node1 != node2) {
                  return (isReachableLoopNode(node1, node2, node1, order, Set[WALA.CFGNode](), wcontext) && isReachableLoopNode(node2, node1, node1, order, Set[WALA.CFGNode](), wcontext))
              }
          } else {
              if (isBefore(order)(node1, node2) && node1 != node2) {
                  if (isReachableLoopNode(node1, node2, node1, order, Set[WALA.CFGNode]() + node1, wcontext)) {
                    //   println("getReachablePaths")
                    //   println("from: " + node1)
                    //   println("to: " + node2)
                    //   println(getReachablePaths(node1, node2, node1, order, Set[WALA.CFGNode](), wcontext))
                    // println("existMultiplePathsWithDistinctPrefix")
                    // println(node1, node2)
                    // if (node1.toString() == "BB[SSA:0..3]1 - Demo.inner([I[II)I") {
                    //     println("DEMO")
                    // }
                    // var b = !existMultiplePathsWithDistinctPrefix(getReachablePaths(node1, node2, node1, order, Set[WALA.CFGNode]() + node1, wcontext))
                    // println(b)
                      return !existMultiplePathsWithDistinctPrefix(getReachablePaths(node1, node2, node1, order, Set[WALA.CFGNode]() + node1, wcontext))
                  }
                //   return isReachableLoopNode(node1, node2, node1, order, Set[WALA.CFGNode](), wcontext)
              }
          }

          return false
      }

      def existMultiplePathsWithDistinctPrefix(paths: List[List[WALA.CFGNode]]) : Boolean = {
          //check if first two element of all paths are the same
          return paths.filter{ x=> x.length > 0
          }.map{ y => y.tail
          }.filter{ z => z.length > 0
          }.map{ w => w.head }.toSet.size > 1
      }


      //   def getReachablePaths(node1: WALA.CFGNode, node2: WALA.CFGNode, laterThan: WALA.CFGNode, order: List[WALA.CFGNode], alreadyVisited: Set[WALA.CFGNode], wcontext : WALA.WALAContext, prv: Boolean) : List[List[WALA.CFGNode]] = {
      def getReachablePaths(node1: WALA.CFGNode, node2: WALA.CFGNode, laterThan: WALA.CFGNode, order: List[WALA.CFGNode], alreadyVisited: Set[WALA.CFGNode], wcontext : WALA.WALAContext) : List[List[WALA.CFGNode]] = {
          var pr = false
        //   if (node1.toString() == "BB[SSA:0..3]1 - Demo.inner([I[II)I" && node2.toString() == "BB[SSA:-1..-2]10 - Demo.inner([I[II)I") {
        //       println("DEMO")
        //       pr = true;
        //   }

        //   if (pr) {
        //       println("laterThan")
        //       println(laterThan)
        //       println("from: " + node1 + " to: " + node2)
        //       println("alreadyVisited")
        //       println(alreadyVisited)
        //   }
          if (node1 == node2) {
              return List(List[WALA.CFGNode](node1))
          } else {
              var cfg = wcontext.cfg
              return cfg.getSuccNodes(node1).toList.filter{y => !alreadyVisited.contains(y)
              }.filter{x => isReachableLoopNode(x.asInstanceOf[WALA.CFGNode], node2, laterThan, order, alreadyVisited + x.asInstanceOf[WALA.CFGNode], wcontext)
              }.map{m =>
                getReachablePaths(m.asInstanceOf[WALA.CFGNode], node2, laterThan, order, alreadyVisited + m.asInstanceOf[WALA.CFGNode], wcontext)
              }.flatten.map{ y =>
                  List(node1):::y.asInstanceOf[List[WALA.CFGNode]]
              }
          }
      }

      def isReachableLoopNode(node1: WALA.CFGNode, node2: WALA.CFGNode, laterThan: WALA.CFGNode, order: List[WALA.CFGNode], alreadyVisited: Set[WALA.CFGNode], wcontext : WALA.WALAContext) : Boolean = {
          if (node1 == node2) {
              return true
          } else {
              var cfg = wcontext.cfg
              return cfg.getSuccNodes(node1).toList.map{m =>
                  if (isBefore(order)(laterThan, m.asInstanceOf[WALA.CFGNode]) && !alreadyVisited.contains(m)) {
                      isReachableLoopNode(m.asInstanceOf[WALA.CFGNode], node2, laterThan, order, alreadyVisited + m.asInstanceOf[WALA.CFGNode], wcontext)
                  } else {
                      false
                  }
              }.fold(false)(_ || _)
          }
      }

      def hasAncestorNode(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : Boolean = {
          (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).map{loopNode => isAncestorNode(loopNode, node, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.fold(false)(_ || _)
      }

      def isBefore(order: List[WALA.CFGNode])(node1: WALA.CFGNode, node2: WALA.CFGNode) : Boolean = {
          return order.indexOf(node1) <= order.indexOf(node2)
      }

      def isStrictlyBefore(order: List[WALA.CFGNode])(node1: WALA.CFGNode, node2: WALA.CFGNode) : Boolean = {
          return order.indexOf(node1) < order.indexOf(node2)
      }

      def isParent(strict: Boolean)(node1: WALA.CFGNode, node2: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : Boolean = {
          if (conditionalNodesSorted.contains(node1)) {
              ifDebug {
                //   println("isParent: ")
                //   println("from: " + node1)
                //   println("to: " + node2)
                //   println("strict: " + strict)
              }
          }
          if (!conditionalNodesSorted.contains(node1) || !strict) {
              return isAncestorNode(node1, node2, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext) && !(getOrderedDescendantsOfNode(node1, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext).filter{m => m != node1 && m != node2}.map{m => isAncestorNode(m, node2, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.fold(false)(_ || _))
          } else {
              return isAncestorNode(node1, node2, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext) && !(getOrderedDescendantsOfNode(node1, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext).filter{m => m != node1 && m != node2}.map{m => isReachableLoopNode(m, node2, m, order, Set[WALA.CFGNode](), wcontext)}.fold(false)(_ || _))
          }
      }

      def getOrderedChildrenOfNode(strict: Boolean)(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : List[WALA.CFGNode] = {
          return (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).filter { loopNode => isParent(strict)(node, loopNode, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.sortWith(isBefore(order))
      }

      def getOrderedDescendantsOfNode(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : List[WALA.CFGNode] = {
          (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).filter { loopNode => isAncestorNode(node, loopNode, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.sortWith(isBefore(order))
      }

      def getAncestorsOfNode(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : List[WALA.CFGNode] = {
          (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).filter { loopNode => isAncestorNode(loopNode, node, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.sortWith(isBefore(order))
      }

      def getOrderedSiblingsOfNode(strict: Boolean)(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : List[WALA.CFGNode] = {
          var parents = (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).filter{loopNode => isParent(false)(loopNode, node, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}
          var siblings = List[WALA.CFGNode]()
          if (parents.length > 0) {
              siblings = (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).filter{loopNode => isParent(strict)(parents(0), loopNode, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.filter{m => (m != node) && (isBefore(order)(node, m))}.sortWith(isBefore(order))
          } else {
              siblings = (loopNodesSorted:::conditionalNodesSorted:::slcNodesSorted:::functionCallNodesSorted).filter{loopNode => !hasAncestorNode(loopNode, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)}.filter{m => (m != node) && (isBefore(order)(node, m))}.sortWith(isBefore(order))
          }

          if (!strict) {
              var reachableStrictSiblings = getOrderedSiblingsOfNode(true)(node, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext).map{
                  x => List(x):::getOrderedSiblingsOfNode(false)(x, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)
              }.foldLeft(List[WALA.CFGNode]()){_ ::: _}

            //   println("getOrderedSiblingsOfNode")
            //   println(node)
            //   println(siblings)
            //   println(reachableStrictSiblings)
              siblings = siblings.toSet.diff(reachableStrictSiblings.toSet).toList.sortWith(isBefore(order))
            //   println(siblings)
          }

          return siblings
      }

      def getNextSiblingOfNode(strict : Boolean)(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : WALA.CFGNode = {
          var siblings = getOrderedSiblingsOfNode(strict)(node, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)

          if (siblings.length > 0) {
              siblings(0)
          } else {
              null
          }
      }

      def getFirstChildOfNode(strict : Boolean)(node: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted: List[WALA.CFGNode], conditionalNodesSorted: List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : WALA.CFGNode = {
          var children = getOrderedChildrenOfNode(strict)(node, order, loopNodesSorted, conditionalNodesSorted, slcNodesSorted, functionCallNodesSorted, wcontext)

          if (children.length > 0) {
              children(0)
          } else {
              null
          }
      }

    //   def getOrderedRootLoopNodes(order: List[WALA.CFGNode], loopNodes: List[WALA.CFGNode], wcontext: WALA.WALAContext) : List[WALA.CFGNode] = {
    //       var loopNodesSorted = loopNodes.sortWith(isBefore(order))
    //       var rootLoopNodes = loopNodes.filterNot{loopNode => hasAncestorNode(loopNode, order, loopNodesSorted, wcontext)}
    //       return rootLoopNodes
    //   }

      def buildNodeFromCFGNode(strict : Boolean)(currentNode: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted:List[WALA.CFGNode], conditionalNodesSorted:List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], functionCallAndLoopNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : ASTNode = {
          if (currentNode == null) {
              return null
          }

          var fs = getFirstChildOfNode(true)(
              currentNode,
              order,
              loopNodesSorted,
              conditionalNodesSorted,
              slcNodesSorted,
              functionCallNodesSorted,
              wcontext
          )

          var ns = getNextSiblingOfNode(strict)(
              currentNode,
              order,
              loopNodesSorted,
              conditionalNodesSorted,
              slcNodesSorted,
              functionCallNodesSorted,
              wcontext
          )

          if (conditionalNodesSorted.contains(currentNode)) {
              return new ASTNodeIf(
                  buildSeqNodeFromCFGNode(
                      fs,
                      currentNode,
                      order,
                      loopNodesSorted,
                      conditionalNodesSorted,
                      slcNodesSorted,
                      functionCallNodesSorted,
                      functionCallAndLoopNodesSorted,
                      wcontext
                  ),
                  buildNodeFromCFGNode(strict)(
                      ns,
                      order,
                      loopNodesSorted,
                      conditionalNodesSorted,
                      slcNodesSorted,
                      functionCallNodesSorted,
                      functionCallAndLoopNodesSorted,
                      wcontext
                  ),
                  currentNode
              )
          } else if (functionCallAndLoopNodesSorted.contains(currentNode)) {
              return new ASTNodeLoop(
                  new ASTNodeFunc(
                      null,
                      buildNodeFromCFGNode(true)(
                          fs,
                          order,
                          loopNodesSorted,
                          conditionalNodesSorted,
                          slcNodesSorted,
                          functionCallNodesSorted,
                          functionCallAndLoopNodesSorted,
                          wcontext
                      ),
                      currentNode
                  ),
                  buildNodeFromCFGNode(strict)(
                      ns,
                      order,
                      loopNodesSorted,
                      conditionalNodesSorted,
                      slcNodesSorted,
                      functionCallNodesSorted,
                      functionCallAndLoopNodesSorted,
                      wcontext
                  ),
                  currentNode
              )
          } else if (loopNodesSorted.contains(currentNode)) {
              return new ASTNodeLoop(
                  buildNodeFromCFGNode(true)(
                      fs,
                      order,
                      loopNodesSorted,
                      conditionalNodesSorted,
                      slcNodesSorted,
                      functionCallNodesSorted,
                      functionCallAndLoopNodesSorted,
                      wcontext
                  ),
                  buildNodeFromCFGNode(strict)(
                      ns,
                      order,
                      loopNodesSorted,
                      conditionalNodesSorted,
                      slcNodesSorted,
                      functionCallNodesSorted,
                      functionCallAndLoopNodesSorted,
                      wcontext
                  ),
                  currentNode
              )
          } else if (functionCallNodesSorted.contains(currentNode)) {
              return new ASTNodeFunc(
                  null,
                  buildNodeFromCFGNode(strict)(
                      ns,
                      order,
                      loopNodesSorted,
                      conditionalNodesSorted,
                      slcNodesSorted,
                      functionCallNodesSorted,
                      functionCallAndLoopNodesSorted,
                      wcontext
                  ),
                  currentNode
              )
          }

          return new ASTNodeSLC(
              null,
              buildNodeFromCFGNode(strict)(
                  ns,
                  order,
                  loopNodesSorted,
                  conditionalNodesSorted,
                  slcNodesSorted,
                  functionCallNodesSorted,
                  functionCallAndLoopNodesSorted,
                  wcontext
              ),
              currentNode
          )
      }

      def buildSeqNodeFromCFGNode(currentNode: WALA.CFGNode, parentNode: WALA.CFGNode, order: List[WALA.CFGNode], loopNodesSorted:List[WALA.CFGNode], conditionalNodesSorted:List[WALA.CFGNode], slcNodesSorted:List[WALA.CFGNode], functionCallNodesSorted:List[WALA.CFGNode], functionCallAndLoopNodesSorted:List[WALA.CFGNode], wcontext: WALA.WALAContext) : ASTNode = {
          if (currentNode == null) {
              return null
          }

          var ns = getNextSiblingOfNode(true)(
              currentNode,
              order,
              loopNodesSorted,
              conditionalNodesSorted,
              slcNodesSorted,
              functionCallNodesSorted,
              wcontext
          )

          return new ASTNodeSeq(
              buildNodeFromCFGNode(false)(
                  currentNode,
                  order,
                  loopNodesSorted,
                  conditionalNodesSorted,
                  slcNodesSorted,
                  functionCallNodesSorted,
                  functionCallAndLoopNodesSorted,
                  wcontext
              ),
              buildSeqNodeFromCFGNode(
                  ns,
                  parentNode,
                  order,
                  loopNodesSorted,
                  conditionalNodesSorted,
                  slcNodesSorted,
                  functionCallNodesSorted,
                  functionCallAndLoopNodesSorted,
                  wcontext
              ),
              parentNode.getGraphNodeId()+"_"+currentNode.getGraphNodeId()
          )
      }

      def isExceptionToExit(root: WALA.CFGNode, wcontext: WALA.WALAContext) : Boolean = {
          return root.getNumber()  == wcontext.cfg.exit().getNumber()
      }

      def hasUnwantedException(root: WALA.CFGNode, wcontext: WALA.WALAContext) : Boolean = {
          for (block <- wcontext.cfg.getSuccNodes(root).toList) {
              if (block.getNumber()  == wcontext.cfg.exit().getNumber()) {
                //   println("Should never come in here!")
                  return true
              }
          }

          return false
      }

      def hasFunctionCall(root: WALA.CFGNode, wcontext: WALA.WALAContext) : Boolean = {

          for (idx <- root.getFirstInstructionIndex() to root.getLastInstructionIndex()) {
              val (inst, _) = WALA.getInstruction(idx, wcontext.ir)

              ifDebug {
                //   println(inst)
              }
              inst match {
                  case i: InvokeI => {
                      ifDebug {
                        //   println(root)
                        //   println("it invokes!!!")
                      }
                      return true
                  }
                  case i => {
                      ifDebug {
                        //   println(root)
                        //   println("does not invoke")
                      }
                  }
              }
          }

          ifDebug {
            //   println(root)
            //   println("false")
          }
          return false
      }

      def buildLoopTreeFromCFG(root: WALA.CFGNode, wcontext : WALA.WALAContext): ASTNode = {
          var cfg = wcontext.cfg
          var visited = List[WALA.CFGNode]()
          var visitedConditional = List[WALA.CFGNode]()
          var loopNodes = List[WALA.CFGNode]()
          var conditionalNodes = List[WALA.CFGNode]()
          var slcNodes = List[WALA.CFGNode]()
          var functionCallNodes = List[WALA.CFGNode]()
          var functionCallAndLoopNodes = List[WALA.CFGNode]()

          def getConditionalNodes(root: WALA.CFGNode, preds: List[WALA.CFGNode]): Unit = {
              visitedConditional = visitedConditional ::: List(root)

            //   if (cfg.getSuccNodes(root).toList.length > 1) {
            // println("\n\n\n\nSTART cfg.getSuccNodes(root).toList")
            // println(cfg.getSuccNodes(root).toList.length)
            // println(cfg.getSuccNodes(root).toList.filter{ x => !isExceptionToExit(x.asInstanceOf[WALA.CFGNode], wcontext)}.length)
            // println("END cfg.getSuccNodes(root).toList\n\n\n\n")
            if (cfg.getSuccNodes(root).toList.filter{ x => !isExceptionToExit(x.asInstanceOf[WALA.CFGNode], wcontext)}.length > 1) {
                  if (!hasFunctionCall(root, wcontext) && !hasUnwantedException(root, wcontext)) {
                      conditionalNodes = conditionalNodes ::: List(root)
                  } else if (hasFunctionCall(root, wcontext)) {
                      functionCallNodes = functionCallNodes ::: List(root)
                  } else {
                      slcNodes = slcNodes ::: List(root)
                  }
              } else {
                  if (hasFunctionCall(root, wcontext)) {
                      functionCallNodes = functionCallNodes ::: List(root)
                  } else {
                      slcNodes = slcNodes ::: List(root)
                  }
              }
              cfg.getSuccNodes(root).foreach{m =>
                  if (preds.contains(m)) {
                      loopNodes = loopNodes ::: List(m.asInstanceOf[WALA.CFGNode])
                  }
                  if (!visitedConditional.contains(m)) {
                      getConditionalNodes(m.asInstanceOf[WALA.CFGNode], preds ::: List(m.asInstanceOf[WALA.CFGNode]))
                  }
              }
          }

          getConditionalNodes(root, List[WALA.CFGNode]())

          loopNodes = loopNodes.sortWith(isBefore(visitedConditional))
          conditionalNodes = conditionalNodes.sortWith(isBefore(visitedConditional))
          slcNodes = slcNodes.filterNot{loopNodes.toSet}
          slcNodes = slcNodes.sortWith(isBefore(visitedConditional))
          functionCallNodes = functionCallNodes.sortWith(isBefore(visitedConditional))

          conditionalNodes = conditionalNodes.filterNot{loopNodes.toSet}
          functionCallAndLoopNodes = functionCallNodes.filter{loopNodes.toSet}
          functionCallNodes = functionCallNodes.filterNot{loopNodes.toSet}

          ifDebug {
              println("loop, if, slc nodes, func, funcAndLoop:")
              println(loopNodes)
              println(conditionalNodes)
              println(slcNodes)
              println(functionCallNodes)
              println(functionCallAndLoopNodes)
              println(visitedConditional)
          }

        return buildNodeFromCFGNode(true)((visitedConditional:::List(root))(0), visitedConditional, loopNodes, conditionalNodes, slcNodes, functionCallNodes, functionCallAndLoopNodes, wcontext)

        // return t.simplify(null)
        //   }

        //   return null
      }
  }

  case class ASTNodeSeq(override val firstChild : ASTNode, override val nextSibling : ASTNode, override val value: Any) extends ASTNode(firstChild, nextSibling, value) {
      override def toString(level: Int) : String = {
          var s :String = ""
          s += "Seq: "
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      override def simplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.simplify(parentNode)
          }

          parentNode match {
              case p : ASTNodeIf => {
                  if (simFC.isForest()) {
                      return new ASTNodeSeq(simFC, simNS, getValue)
                  } else {
                      simFC.addNextSibling(simNS)
                  }
              }
              case p : ASTNodeSeq => {
                  if (simFC != null) {
                      return simFC.addNextSibling(simNS)
                  } else {
                      return simNS
                  }

              }
              case _ => {
                  return new ASTNodeSeq(simFC, simNS, getValue)
              }
          }
      }

      override def addNextSibling(ns : ASTNode) : ASTNode = {
          if (nextSibling == null) {
              new ASTNodeSeq(firstChild, ns, getValue)
          } else {
              new ASTNodeSeq(firstChild, nextSibling.addNextSibling(ns), getValue)
          }
      }
  }

  case class ASTNodeAny(override val firstChild : ASTNode, override val nextSibling : ASTNode, override val value: Any) extends ASTNode(firstChild, nextSibling, value) {
      override def toString(level: Int) : String = {
          var s :String = ""
          s += "SLC Any: "
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      override def simplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.simplify(parentNode)
          }

          return new ASTNodeAny(simFC, simNS, getValue)
      }

      override def addNextSibling(ns : ASTNode) : ASTNode = {
          if (nextSibling == null) {
              new ASTNodeAny(firstChild, ns, getValue)
          } else {
              new ASTNodeAny(firstChild, nextSibling.addNextSibling(ns), getValue)
          }
      }
  }

  case class ASTNodeIf(override val firstChild : ASTNode, override val nextSibling : ASTNode, override val value: Any) extends ASTNode(firstChild, nextSibling, value) {
    override def toString(level: Int) : String = {
          var s :String = ""
          s += "IF: "
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      override def simplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.overSimplify(parentNode)
          }

          return new ASTNodeIf(simFC, simNS, getValue)
      }

      override def addNextSibling(ns : ASTNode) : ASTNode = {
          if (nextSibling == null) {
              new ASTNodeIf(firstChild, ns, getValue)
          } else {
              new ASTNodeIf(firstChild, nextSibling.addNextSibling(ns), getValue)
          }
      }
  }

  case class ASTNodeLoop(override val firstChild : ASTNode, override val nextSibling : ASTNode, override val value: Any) extends ASTNode(firstChild, nextSibling, value) {
      override def toString(level: Int) : String = {
          var s :String = ""
          s += "LOOP: "
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      override def simplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.overSimplify(parentNode)
          }

          return new ASTNodeLoop(simFC, simNS, getValue)
      }

      override def addNextSibling(ns : ASTNode) : ASTNode = {
          if (nextSibling == null) {
              new ASTNodeLoop(firstChild, ns, getValue)
          } else {
              new ASTNodeLoop(firstChild, nextSibling.addNextSibling(ns), getValue)
          }
      }

      override def hasLoopNodeDescendant(lookAtNS : Boolean = false) : Boolean = {
          return true
      }
  }

  case class ASTNodeSLC(override val firstChild : ASTNode, override val nextSibling : ASTNode, override val value: Any) extends ASTNode(firstChild, nextSibling, value) {
    override def toString(level: Int) : String = {
          var s :String = ""
          s += "SLC: "
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      override def simplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.simplify(parentNode)
          }

          parentNode match {
              case p : ASTNodeIf => {
                  return new ASTNodeSLC(simFC, simNS, getValue)
              }
              case _ => {
                  if (simNS == null) {
                      return new ASTNodeSLC(simFC, null, getValue)
                  } else {
                      return simNS
                  }
              }
          }
      }

      override def overSimplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.overSimplify(parentNode)
          }

          parentNode match {
              case p : ASTNodeIf => {
                  return new ASTNodeSLC(simFC, simNS, getValue)
              }
              case _ => {
                  return simNS
              }
          }
      }

      override def addNextSibling(ns : ASTNode) : ASTNode = {
          if (nextSibling == null) {
              new ASTNodeSLC(firstChild, ns, getValue)
          } else {
              new ASTNodeSLC(firstChild, nextSibling.addNextSibling(ns), getValue)
          }
      }
  }

  case class ASTNodeFunc(override val firstChild : ASTNode, override val nextSibling : ASTNode, override val value: Any) extends ASTNode(firstChild, nextSibling, value) {
    override def toString(level: Int) : String = {
          var s :String = ""
          s += "Func: "
          s += value.toString()
          if (firstChild != null) {
            s += "\n" + "  " * level + "First child: " + firstChild.toString(level+1)
          } else {
            // s+= "\n" + "  " * level + "no first child"
          }
          if (nextSibling != null) {
            s += "\n" + "  " * (level-1) + "Next Sibling: " + nextSibling.toString(level)
          } else {
            // s+= "\n" + "  " * (level-1) + "no next sibling"
          }

          return s
      }

      override def simplify(parentNode: ASTNode) : ASTNode = {
          var simFC : ASTNode = null
          var simNS : ASTNode  = null
          if (getFirstChild != null) {
              simFC = getFirstChild.simplify(this)
          }
          if (getNextSibling != null) {
              simNS = getNextSibling.overSimplify(parentNode)
          }

          return new ASTNodeFunc(simFC, simNS, getValue)
        //   parentNode match {
        //       case p : ASTNodeIf => {
        //           new ASTNodeFunc(simFC, simNS, getValue)
        //       }
        //       case p : ASTNodeSeq => {
        //           new ASTNodeFunc(simFC, simNS, getValue)
        //       }
        //       case p : ASTNodeLoop => {
        //           new ASTNodeFunc(simFC, simNS, getValue)
        //       }
        //       case _ => {
          //
        //       }
        //   }
      }


      override def addNextSibling(ns : ASTNode) : ASTNode = {
          if (nextSibling == null) {
              new ASTNodeFunc(firstChild, ns, getValue)
          } else {
              new ASTNodeFunc(firstChild, nextSibling.addNextSibling(ns), getValue)
          }
      }
  }

}

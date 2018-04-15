import parma_polyhedra_library._
import scala.collection.immutable.TreeSet._
import collection.JavaConversions._

import PPL.LExp._
import PPL.LinearArray
import Core._
import CommonLoopBounds._

import Lemmas._
import BoundExpressions.BoundExpression
import MyLanguage._

sealed trait NestedLoopSummary
case class Empty() extends NestedLoopSummary
case class Node(location: Long, summary: LoopSummaryParam, innerLoops:List[NestedLoopSummary]) extends NestedLoopSummary

case class LoopSummaryParam (
    val wrtLocation: Long,
    val summary: LoopSummary
)

case class ProgramLocation ()

// case class NestedLoopSummary(
//
// )

case class Loop ()

object ComputeBoundAdvanced {
    def computeBoundAdvanced(nestedLoopSummary: NestedLoopSummary) : List[BoundExpression] = {
        return null;
    }

    def functionB(code: Language): (List[BoundExpression], List[(BoundExpression, Integer)]) = {
        return null;
    }

    def computeBoundForLoops(loop1: Loop, loop2: Loop, nestedLoopSummary: NestedLoopSummary) : List[BoundExpression] = {
        //This is the I(L,L') in the PLDI paper
        return null;
    }

    def computeBoundForLoop(loop: Loop, nestedLoopSummary: NestedLoopSummary) : List[BoundExpression] = {
        //This is the T(L) in the PLDI paper
        return null;
    }

    def buildANestedLoopSummary() = {
        println("here")
        var g = Node(1, null, List(
            Node(2, null, List(
                Node(5, null, List(
                    Empty()
                )),
                Node(6, null, List(
                    Empty()
                ))
            )),
            Node(3, null, List(
                Empty()
            )),
            Node(4, null, List(
                Empty()
            ))
        ));

        println(printNestedLoopSummary(g))
    }

    def printNestedLoopSummary(in: NestedLoopSummary) : String = in match {
        case Empty() => "."
        case Node(l, _, list) => l.toString() + "(" + list.map(x => printNestedLoopSummary(x)).reduceLeft(_ ++ _) + ")"
    }

    def buildLanguage() = {
        println("language")

        var f = new RepeatPlus(
            new Sequence(
                new Skip(),
                new Sequence(
                    new Assign("x", "3"),
                    new Choose(
                        List(
                            new Skip(),
                            new Skip(),
                            new Repeat(
                                new Assign("x","x+1")
                            )
                        )
                    )
                )
            )
        )

        var h = new Sequence(
            new Choose(
                List(
                    new Assign("x", "3"),
                    new Assign("y", "4"),
                    new Assign("z", "5")
                )
            ),
            new Choose(
                List(
                    new Assign("x", "6"),
                    new Assign("y", "7"),
                    new Assign("z", "8")
                )
            )
        )

        println(f.toString())

        println(h.flatten())


    }

    // def depth[A](tree: Tree[A]): Int = tree match {
    //     case Empty => 0
    //     case Node(_, left, right) => 1 + max(depth(left), depth(right))
    // }
}

import parma_polyhedra_library._

import Util._

import PPL._
import PPL.LExp._
import Core._
import CommonLoopBounds._

import ComputeBound._
import ComputeBoundAdvanced._
import RefineProcedure._

object PPLExampleNested {
  /*
   Step through one iteration of the following program:

   entry:
     v0 = unknown between 1 and 100
     v1 = unknown between 1 and 100

   loop_init:
     v2 = 0

   loop_head:
     v3 = phi(v2,v4)                      // output that v3 is connected via v2 v4
                                          // we actually need here is v3' = v3 + v1
     if v3 <= v0 then goto loop_body
     goto loop_done

   loop_body:
     v4 = v3 + v1
     goto loop_head

   loop_done:
     return
   */


  def main(argv: scala.Array[String]) {
    PPL.init

    // buildANestedLoopSummary()
    refineT()
    // tt()
  }
}

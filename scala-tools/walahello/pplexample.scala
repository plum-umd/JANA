import parma_polyhedra_library._

import Util._

import PPL._
import PPL.LExp._
import Core._
import CommonLoopBounds._

import ComputeBound._

object PPLExample {
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

    val apoly = new C_Polyhedron(0, Degenerate_Element.UNIVERSE)
    println("before entry:\n" + tab(apoly.toString()))

    apoly.add_space_dimensions_and_embed(3);
    apoly.add_constraint(LVar(0) >= LConst(1))
    apoly.add_constraint(LVar(0) <= LConst(100))
    apoly.add_constraint(LVar(1) >= LConst(1))
    apoly.add_constraint(LVar(1) <= LConst(100))
    println("after entry:\n" + tab(apoly.toString()))

    apoly.add_space_dimensions_and_embed(1);
    val apoly_pre_init = new C_Polyhedron(apoly); // for stuff at the end of this example

    apoly.add_constraint(LVar(2) == LConst(0))
    println("after init:\n" + tab(apoly.toString()))

    apoly.add_constraint(LVar(3) == LVar(2))
    val apoly_no_init      = new C_Polyhedron(apoly_pre_init); // for later
    val apoly_no_init_head = new C_Polyhedron(apoly_pre_init); // for later
    apoly_no_init.add_constraint(LVar(3) == LVar(2)) // for later
    val apoly_head = new C_Polyhedron(apoly);
    println("after loop_head phi:\n" + tab(apoly_head.toString()))

    val apoly_true = new C_Polyhedron(apoly_head)
    apoly_true.add_constraint(LVar(3) <= LVar(0))
    apoly_no_init.add_constraint(LVar(3) <= LVar(0)) // for later
    println("after loop_head case assuming guard was true:\n" + tab(apoly_true.toString()))

    val apoly_false = new C_Polyhedron(apoly_head)
    apoly_false.add_constraint(LVar(3) >= LVar(0) + LConst(1))
    println("after loop_head case assuming guard was false:\n" + tab(apoly_false.toString()))

    apoly_true.add_space_dimensions_and_embed(1);
    apoly_true.add_constraint(LVar(4) == LVar(1) + LVar(3))
    apoly_no_init.add_space_dimensions_and_embed(1); // for later
    apoly_no_init.add_constraint(LVar(4) == LVar(1) + LVar(3)) // for later
    println("after loop_body:\n" + tab(apoly_true.toString()))

    apoly_true.affine_image(new Variable(3), LVar(4), new Coefficient (1))
    //apoly_no_init.affine_image(new Variable(3), LVar(4), new Coefficient (1)) // for later
    println("before loop_head the second time:\n" + tab(apoly_true.toString()))

    // this is "later"
    println("\n");
    val apoly_exit = new C_Polyhedron(apoly_no_init_head);
    apoly_exit.add_constraint(LVar(3) >= LVar(0) + LConst(1)) // negation of the loop guard

    val imps = List(0L,1,3,4)
    /* 0,1 are presumed method inputs, 2 is the initial loop counter, 3 is
     * the phi of 3/4, 4 is the next value of the counter after a loop
     * body. I'm removing v2 here and expressing everything in terms
     * of v3 (which becomes v2 after removal of v2) and v4 (which
     * becomes v3). Note that the initial value v2 == 0 is preserved
     * given the constraint v3 == v2 in the polyhedron used for the
     * entry spec. */

    val apoly_head_proj = project_to_variables(apoly_head, imps)
    val apoly_no_init_proj = project_to_variables(apoly_no_init, imps)
    val apoly_exit_proj = project_to_variables(apoly_exit, imps)

    println("loop init:\n" + tab(apoly_head_proj.toString()));
    println("derived spec:\n" + tab(edgeSpecOfPoly(2, apoly_head_proj).toString));

    println("loop trans:\n" + tab(apoly_no_init_proj.toString()));
    println("derived spec (note V3 is V2'):\n" + tab(transSpecOfPoly(2, 3, apoly_no_init_proj).toString));

    println("loop exit:\n" + tab(apoly_exit_proj.toString()));
    println("derived spec:\n" + tab(edgeSpecOfPoly(2, apoly_exit_proj).toString));

    // var bounds = computeBounds(
    //     new LoopSummary(
    //         List(edgeSpecOfPoly(2, apoly_head_proj)),
    //         List(transSpecOfPoly(2, 3, apoly_no_init_proj)),
    //         List(edgeSpecOfPoly(2, apoly_exit_proj))
    //     )
    // )
    //printBounds(bounds)
  }
}

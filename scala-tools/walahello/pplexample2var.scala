import parma_polyhedra_library._

import Util._
import PPL._
import PPL.LExp._
import CommonLoopBounds._

import ComputeBound._

object PPLExample2Var {
  /*
   Step through one iteration of the following program:

   entry:
     v0 = unknown between 1 and 100
     v1 = unknown between 1 and 100
     v2 = 10

   loop_init:
     v3 = 0
     v4 = 0

   loop_head:
     v5 = phi(v3,v7)
     v6 = phi(v4,v8)

     if v5+v6 <= v0 then goto loop_body
     goto loop_done

   loop_body:
     v7 = v5 + v1
     v8 = 2*v6
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
    apoly.add_constraint(LVar(2) == LConst(10))
    println("after entry:\n" + tab(apoly.toString()))

    apoly.add_space_dimensions_and_embed(4);
    val apoly_pre_init = new C_Polyhedron(apoly); // for stuff at the end of this example

    apoly.add_constraint(LVar(3) == LConst(0))
    apoly.add_constraint(LVar(4) == LConst(0))
    println("after init:\n" + tab(apoly.toString()))

    apoly.add_constraint(LVar(5) == LVar(3))
    apoly.add_constraint(LVar(6) == LVar(4))
    val apoly_no_init      = new C_Polyhedron(apoly_pre_init); // for later
    val apoly_no_init_head = new C_Polyhedron(apoly_pre_init); // for later
    apoly_no_init.add_constraint(LVar(5) == LVar(3)) // for later
    apoly_no_init.add_constraint(LVar(6) == LVar(4)) // for later
    val apoly_head = new C_Polyhedron(apoly);
    println("after loop_head phi:\n" + tab(apoly_head.toString()))

    val apoly_true = new C_Polyhedron(apoly_head)
    apoly_true.add_constraint   (LVar(5) + LVar(6) <= LVar(0))
    apoly_no_init.add_constraint(LVar(5) + LVar(6) <= LVar(0)) // for later
    println("after loop_head case assuming guard was true:\n" + tab(apoly_true.toString()))

    val apoly_false = new C_Polyhedron(apoly_head)
    apoly_false.add_constraint(LVar(5) + LVar(6) >= LVar(0) + LConst(1))
    println("after loop_head case assuming guard was false:\n" + tab(apoly_false.toString()))

    apoly_true.add_space_dimensions_and_embed(2);
    apoly_true.add_constraint(LVar(7) == LVar(5) + LVar(1))
    apoly_true.add_constraint(LVar(8) == (LVar(6) * LConst(2)))
    apoly_no_init.add_space_dimensions_and_embed(2); // for later
    apoly_no_init.add_constraint(LVar(7) == LVar(5) + LVar(1)) // for later
    apoly_no_init.add_constraint(LVar(8) == (LVar(6) * LConst(2))) // for later
    println("after loop_body:\n" + tab(apoly_true.toString()))

    apoly_true.affine_image(new Variable(5), LVar(7), new Coefficient (1))
    apoly_true.affine_image(new Variable(6), LVar(8), new Coefficient (1))
    //apoly_no_init.affine_image(new Variable(3), LVar(4), new Coefficient (1)) // for later
    println("before loop_head the second time:\n" + tab(apoly_true.toString()))

    // this is "later"
    println("\n");
    val apoly_exit = new C_Polyhedron(apoly_no_init_head);
    apoly_exit.add_constraint(LVar(5) + LVar(6) >=  LVar(0) + LConst(1)) // negation of the loop guard

    val imps = List(0L,1,2,5,6,7,8) // removing 3,4 so 5,6,7,8 will become 3,4,5,6,7

    val apoly_head_proj    = project_to_variables(apoly_head, imps)
    val apoly_no_init_proj = project_to_variables(apoly_no_init, imps)
    val apoly_exit_proj    = project_to_variables(apoly_exit, imps)

    println("loop init:\n" + tab(apoly_head_proj.toString()));
    println("derived spec (1 of 2):\n" + tab(edgeSpecOfPoly(3, apoly_head_proj).toString));
    println("derived spec (2 of 2):\n" + tab(edgeSpecOfPoly(4, apoly_head_proj).toString));

    println("loop trans:\n" + tab(apoly_no_init_proj.toString()));
    println("derived spec (1 of 2):\n" + tab(transSpecOfPoly(3, 5, apoly_no_init_proj).toString));
    println("derived spec (2 of 2):\n" + tab(transSpecOfPoly(4, 6, apoly_no_init_proj).toString));

    println("loop exit:\n" + tab(apoly_exit_proj.toString()));
    println("derived spec (1 of 2):\n" + tab(edgeSpecOfPoly(3, apoly_exit_proj).toString));
    println("derived spec (2 of 2):\n" + tab(edgeSpecOfPoly(4, apoly_exit_proj).toString));

    // computeBound(
    //   new LoopSummary(
    //       List(
    //           edgeSpecOfPoly(3, apoly_head_proj),
    //           edgeSpecOfPoly(4, apoly_head_proj)
    //       ),
    //       List(
    //           transSpecOfPoly(3, 5, apoly_no_init_proj),
    //           transSpecOfPoly(4, 6, apoly_no_init_proj)
    //       ),
    //       List(
    //           edgeSpecOfPoly(3, apoly_exit_proj),
    //           edgeSpecOfPoly(4, apoly_exit_proj)
    //       )
    //   )
    // )
  }
}

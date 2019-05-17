import parma_polyhedra_library._
import collection.JavaConversions._

import Util._
import Core._
import PPL._

/* To be implemented:

    def computeBound(pre: LoopEdgeSpec, trans: LoopTransSpec, post: LoopEdgeSpec) = { ... }

 */

object CommonLoopBounds {
  case class LinearConstraint(
    val relation : Relation_Symbol,
    val coefficients : LinearArray
  ) {
    override def toString = "0 " + relation.implicitToString + " " + coefficients.toString
  }
  // LinearArray stores coefficients.
  // 0 RELATION C + C_0*V_0 + C_1*V_1 + ...

  case class SolvedLinearConstraint(
    val lhs_coefficient : Coeff,
    val lhs_varindex : DimIndex,
    val relation : Relation_Symbol,
    val rhs_coefficients : LinearArray
  ) {
    override def toString =
      (lhs_coefficient, lhs_varindex).implicitToString + " " + relation.implicitToString + " " + rhs_coefficients.toString
  }
  // Same as above but solved for a particular variable i.
  // C_i * V_i RELATION C + C_0*V_0 + C_1*V_1 + ... (with C_i being 0 on the right side)

  case class LoopEdgeSpec(
    val loopvar: DimIndex, // A variable being adjusted by the loop.
    val loopvarRelations: List[SolvedLinearConstraint],
    // The relationship between that variable and other variables for entry or exit of the loop.
    val otherRelations: List[LinearConstraint]
      // The relations between other variables under which
      // loopvarRelations hold. That is, relations which do not mention loopvar.
  ) {
    override def toString = {
      "whenever\n" + tab(otherRelations.map(o => o.toString).mkString("\n")) + "\nthen\n" +
      tab(loopvarRelations.map{ lc => lc.toString }.mkString("\n"))
    }
  }
  /*
   Spec for the initial condition of a loop counter, and the exit
   condition. The last part is a list of constraints that do not
   involve VarIndex.

   Variable * Coefficient <= LinearArray (this class is defined below)

   That is, the first element in the tuple inside is the coefficient
   of the variable indexed by VarIndex. Doing this instead of dealing
   with fractions inside LinearArray for now.

   For example; the specification:

   4 * V0 <= 42 + 10*V1 - 12*V2,
   1 * V0 >= 19

   would become

   loopvar = 0
   loopvarRelations has 2 elements:
   (4, "<=", LinearArray with 42 as constant and 0,10,-12 as coefficients
   (1, ">=", LinearArray with 19 as constant and 0,0,0 as coefficients

   */

  case class LoopTransSpec(
    val loopvarOld: DimIndex,
    val loopvarNew: DimIndex,
    val loopvarRelations: List[SolvedLinearConstraint],
    val otherRelations:   List[LinearConstraint]
  ) {
    override def toString = {
      "whenever\n" + tab(otherRelations.map(o => o.toString).mkString("\n")) + "\nthen\n" +
      tab(loopvarRelations.map{ slc => "V" + loopvarOld + "' is " + slc.toString }.mkString("\n"))
    }
  }
  /*
   This one also includes the new variable id for the next value of a
   variable in the loop, the original comes first. For example if we
   had a program with loop:

   ...
     loop_init:
       v2 = 0
     loop_head:
       v3 = phi(v2,v4)
     ...
     loop_body:
       v4 = v3 + v1
     goto loop_head
     ...

   Then a loop entry spec will be in terms of v2. A loop transition
   spec will be have v3 as the variable holding the next iteration of
   v2. LoopTransSpec will thus begin with indices 2 and 3, followed by
   the constraint

   v3 == v2 + v1

   , along with potentially other constraints.

   */

  case class LoopSummary(
    val pre: List[LoopEdgeSpec],
    val trans: List[LoopTransSpec],
    val post: List[LoopEdgeSpec]
  )

  case class LoopSummaryPolyhedra(
    val primedVars: List[(DimIndex, DimIndex)],
    val pre: PPL_Object,
    val trans: PPL_Object,
    val transBindings: Bimap[Label.Label, DimIndex],
    val post: PPL_Object
  )

  case class NestedLoopSummary(

  )

  implicit class VarCoeffPair(val r: (Coeff, DimIndex)) {
    def implicitToString = r match {
      case (c, vid) =>
        (if (c == 1) {
          "+"
        } else if (c == -1) {
          "-"
        } else if (c > 0) {
          "+" + c.toString()
        } else if (c < 0) {
          "-" + c.abs.toString()
        } else {
          "0*"
        }) + "V" + vid.toString()
    }
  }
}

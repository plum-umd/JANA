import BoundExpressions.{_}
import Label._
import WALA.IAddr

object PolynomialUtil {
  val debug = false

  def main(argv: scala.Array[String]) = {
    // construct test expression

    val tests = List(

      // simple test
      new BoundExpressionMult(
        new BoundExpressionSum(new BoundExpressionVar(1), new BoundExpressionConst(1)),
        new BoundExpressionSum(new BoundExpressionVar(2), new BoundExpressionConst(2))
      ),
      // test division
      new BoundExpressionDiv(
        new BoundExpressionDiff(
          new BoundExpressionSum(
            new BoundExpressionMult(
              new BoundExpressionConst(1),
              new BoundExpressionVar(1)),
            new BoundExpressionConst(0)),
          new BoundExpressionConst(0)),
        new BoundExpressionConst(1)
      ),
      // test division, inverse polynomial
      new BoundExpressionDiv(
        new BoundExpressionDiff(
          new BoundExpressionSum(
            new BoundExpressionMult(
              new BoundExpressionConst(2),
              new BoundExpressionVar(1)),
            new BoundExpressionConst(1)),
          new BoundExpressionConst(0)),
        new BoundExpressionSum(
          new BoundExpressionMult(
            new BoundExpressionConst(2),
            new BoundExpressionVar(2)),
          new BoundExpressionConst(1))
      ),
      // test division, negative exponent
      new BoundExpressionDiv(
        new BoundExpressionDiff(
          new BoundExpressionSum(
            new BoundExpressionMult(
              new BoundExpressionConst(2),
              new BoundExpressionVar(1)),
            new BoundExpressionConst(1)),
          new BoundExpressionConst(0)),
        new BoundExpressionSum(
          new BoundExpressionMult(
            new BoundExpressionConst(2),
            new BoundExpressionVar(2)),
          new BoundExpressionConst(0))
      ),
      // test zero exponent simplification
      new BoundExpressionDiv(
        new BoundExpressionDiv(
          new BoundExpressionDiff(
            new BoundExpressionSum(
              new BoundExpressionMult(
                new BoundExpressionConst(2),
                new BoundExpressionVar(1)),
              new BoundExpressionConst(1)),
            new BoundExpressionConst(0)),
          new BoundExpressionSum(
            new BoundExpressionMult(
              new BoundExpressionConst(2),
              new BoundExpressionVar(2)),
            new BoundExpressionConst(0))),
        new BoundExpressionVar(1)
      ),
      // test division, negative exponent, fractional coeffs
      // (((1 * v1ⁱⁿ) + 1) - 20) / ((3 * v2ⁱⁿ) + 0)
      new BoundExpressionDiv(
        new BoundExpressionDiff(
          new BoundExpressionSum(
            new BoundExpressionMult(
              new BoundExpressionConst(1),
              new BoundExpressionVar(1)),
            new BoundExpressionConst(1)),
          new BoundExpressionConst(20)),
        new BoundExpressionSum(
          new BoundExpressionMult(
            new BoundExpressionConst(3),
            new BoundExpressionVar(2)),
          new BoundExpressionConst(0))
      ),
      new BoundExpressionMax(
        new BoundExpressionDiv(
          new BoundExpressionDiff(
            new BoundExpressionSum(
              new BoundExpressionMult(
                new BoundExpressionConst(2),
                new BoundExpressionVar(1)),
              new BoundExpressionConst(1)),
            new BoundExpressionConst(0)),
          new BoundExpressionSum(
            new BoundExpressionMult(
              new BoundExpressionConst(2),
              new BoundExpressionVar(2)),
            new BoundExpressionConst(0))
        ), new BoundExpressionSum(
          new BoundExpressionMax(
            new BoundExpressionDiv(
              new BoundExpressionDiff(
                new BoundExpressionSum(
                  new BoundExpressionMult(
                    new BoundExpressionConst(2),
                    new BoundExpressionVar(1)),
                  new BoundExpressionConst(1)),
                new BoundExpressionConst(0)),
              new BoundExpressionSum(
                new BoundExpressionMult(
                  new BoundExpressionConst(2),
                  new BoundExpressionVar(2)),
                new BoundExpressionConst(1))
            ), new BoundExpressionConst(4)
          ), new BoundExpressionMult(
            new BoundExpressionVar(3),
            new BoundExpressionVar(4)
          )
        )
      )
    )

    tests.foreach{ exp =>
      println("expression: " + exp)
      println("polynomial: " + construct(exp, null))
      println("simplify: " + exp.simplify(null))
      // compute big-oh (no constants, biggest degree of each term)
    //   println("big-oh:     " + "")
      println()
    }

    // test equality

    val p1 = construct(new BoundExpressionMult(
      new BoundExpressionConst(-1),
      new BoundExpressionVar(6)), null)
    val p2 = construct(new BoundExpressionMult(
      new BoundExpressionConst(-1),
      new BoundExpressionVar(6)), null)
    println(p1)
    println(p2)
    println(p1.equals(p2))
    println(new Polynomial.LabelFactor("bob").hashCode())
    println(new Polynomial.LabelFactor("bob").hashCode())
    println(new Polynomial.LabelFactor("bob").equals(new Polynomial.LabelFactor("bob")))
  }

  def construct(exp: BoundExpression, context: Core.InterpContext) : Polynomial = {
    exp match {
      case i: BoundExpressionConst =>
        val r = new Polynomial(i.constant.bigInteger)
        if (debug) println("const " + r);
        r
      case i: BoundExpressionVar =>
        val r = new Polynomial(i.index.bigInteger, BigInt(0).bigInteger)
        if (debug) println("var " + r);
        r
      case i: BoundExpressionVarLabel =>
        if (debug) println("label " + i.label)
        val s =
          if (null == context) i.label.toString()
          else context.wala.ir.getMethod().getSignature().toString() + ":" + i.label.toStringWithLocals(new IAddr(context.wala.ir, context.wala.ir.getControlFlowGraph().entry()))
        val r = new Polynomial(s, BigInt(0).bigInteger)
        if (debug) println("varlabel " + r);
        r
      case i: BoundExpressionVarCustom =>
        if (debug) println("varcustom " + i.str)
        val s = i.str
        val r = new Polynomial(s, BigInt(0).bigInteger)
        if (debug) println("varcustom " + r);
        r
      case c: BoundExpressionConstC =>
        // val r = new Polynomial(1)
        // if (debug) println("constc" + r);
        // r
        if (debug) println("const var " + c.toString())
        val r = new Polynomial(c.toString(), BigInt(0).bigInteger)
        if (debug) println("const var " + r);
        r
      case i: BoundExpressionMult =>
        if (i.x == null) construct(i.y, context)
        else if (i.y == null) construct(i.x, context)
        else {
          val left = construct(i.x, context)
          val right = construct(i.y, context)
          if (debug) println("multl " + left);
          if (debug) println("multr " + right);
          val r = left.times(right)
          if (debug) println("mult " + r);
          r
        }
      case i: BoundExpressionDiv =>
        if (i.x == null) construct(i.y, context)
        else if (i.y == null) construct(i.x, context)
        else {
          val left = construct(i.x, context)
          val right = construct(i.y, context)
          if (debug) println("divl " + left);
          if (debug) println("divr " + right);
          val inverse = right.invert()
          if (debug) println("inverser " + inverse);
          val r = left.times(inverse)
          if (debug) println("div " + r);
          r
        }
      case i: BoundExpressionSum =>
        if (i.x == null) construct(i.y, context)
        else if (i.y == null) construct(i.x, context)
        else {
          val left = construct(i.x, context)
          val right = construct(i.y, context)
          if (debug) println("suml " + left);
          if (debug) println("sumr " + right);
          val r = left.plus(right)
          if (debug) println("sum " + r);
          r
        }
      case i: BoundExpressionDiff =>
        if (i.x == null) construct(i.y, context)
        else if (i.y == null) construct(i.x, context)
        else {
          val left = construct(i.x, context)
          val right = construct(i.y, context)
          if (debug) println("diffl " + left);
          if (debug) println("diffr " + right);
          val r = left.plus(right.negate())
          if (debug) println("diff " + r);
          r
        }
      case i: BoundExpressionPolynomial =>
        i.polynomial
      case i: BoundExpressionMax =>
        val left = construct(i.x, context)
        val right = construct(i.y, context)
        if (debug) println("maxl " + left);
        if (debug) println("maxr " + right);
        val compare = left.compareTo(right)
        if (debug) println("max " + compare);
        if (compare > 0) {
          left
        } else if (compare < 0) {
          right
        } else {
          right
        }
      case i: BoundExpressionMin =>
        throw new RuntimeException("min expression not yet implemented " + exp);
      case i: BoundExpressionPow =>
        throw new RuntimeException("pow expression not yet implemented " + exp);
      case i: BoundExpressionLog =>
        throw new RuntimeException("log expression not yet implemented " + exp);
      case default =>
        throw new RuntimeException("bound expression not yet implemented " + exp);
    }
  }
}

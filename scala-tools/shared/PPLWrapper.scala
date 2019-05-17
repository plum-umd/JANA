import parma_polyhedra_library._
import collection.JavaConversions._

import Util._
import Core._
import CommonLoopBounds._
import java.io._

object PPLWrapper {
  def init = {
    Util.findAndLoadNativeLib("ppl_java")
    Parma_Polyhedra_Library.initialize_library()
  }

  type Coeff = BigInt /* coefficients in linear expressions */

  case class NonLinear(s: String) extends Exception(s)
  case class Disjunctive(s: String) extends Exception(s)

  def PPLConstraintSymbolOfNumericRelation(op: Operator.NumericRelation): Relation_Symbol = op match {
    case Operator.NumericRelation.== => Relation_Symbol.EQUAL
    case Operator.NumericRelation.≠  => throw Disjunctive("not equal")
    case Operator.NumericRelation.<  => Relation_Symbol.LESS_THAN
    case Operator.NumericRelation.≤  => Relation_Symbol.LESS_OR_EQUAL
    case Operator.NumericRelation.>  => Relation_Symbol.GREATER_THAN
    case Operator.NumericRelation.≥  => Relation_Symbol.GREATER_OR_EQUAL
  }

  def PPLConstraintOfNumericRelation(op: Operator.NumericRelation): (Linear_Expression => Linear_Expression => Constraint) = {
    a => b => new Constraint(a, PPLConstraintSymbolOfNumericRelation(op), b)
  }

  def PPLExpressionOfNumericBinop(op: Operator.NumericBinop)
      : (Linear_Expression => Linear_Expression => Linear_Expression) = op match {
    case Operator.NumericBinop.+ => {a => b => new Linear_Expression_Sum(a,b)}
    case Operator.NumericBinop.- => {a => b => new Linear_Expression_Difference(a,b)}
    case Operator.NumericBinop.* => throw NonLinear("multiplication")
    case Operator.NumericBinop./ => throw NonLinear("division")
    case Operator.NumericBinop.% => throw NonLinear("modulus")
  }

  def PPLExpressionOfNumericUnop(op: Operator.NumericUnop)
      : (Linear_Expression => Linear_Expression) = op match {
    case Operator.NumericUnop.- => {a => new Linear_Expression_Unary_Minus(a)}
  }

  implicit class RelationSymbolClass(val r: Relation_Symbol) {
    import Relation_Symbol._

    def implicitToString = r match {
      case GREATER_THAN     => ">"
      case GREATER_OR_EQUAL => "≥"
      case LESS_OR_EQUAL    => "≤"
      case LESS_THAN        => "<"
      case EQUAL            => "="
      case NOT_EQUAL        => "≠"
    }
    def neg = r match {
      case GREATER_THAN     => Relation_Symbol.LESS_OR_EQUAL
      case GREATER_OR_EQUAL => Relation_Symbol.LESS_THAN
      case LESS_OR_EQUAL    => Relation_Symbol.GREATER_THAN
      case LESS_THAN        => Relation_Symbol.GREATER_OR_EQUAL
      case EQUAL            => Relation_Symbol.NOT_EQUAL
      case NOT_EQUAL        => Relation_Symbol.EQUAL
    }
  }

  implicit class ConstraintClass(val c: Constraint) {
    import Relation_Symbol._

    def neg: Constraint =
      new Constraint(c.left_hand_side(), c.kind().neg, c.right_hand_side())

    def negIntegral: List[Constraint] =
      (new Constraint(c.left_hand_side(), c.kind().neg, c.right_hand_side()))
        .integral

    def integral: List[Constraint] = {
      val a = c.left_hand_side()
      val ap1 = a.sum(LExp.LConst(1))
      val am1 = a.sum(LExp.LConst(-1))
      val b = c.right_hand_side()
      val bp1 = b.sum(LExp.LConst(1))
      val bm1 = b.sum(LExp.LConst(-1))
      c.kind() match {
        case GREATER_THAN => List(new Constraint(a, GREATER_OR_EQUAL, bp1))
        case LESS_THAN    => List(new Constraint(ap1, LESS_OR_EQUAL, b))
        case NOT_EQUAL    => List(
          new Constraint(a, LESS_OR_EQUAL, bm1),
          new Constraint(am1, GREATER_OR_EQUAL, b)
        )
        case _ => List(c)
      }
    }
  }

  trait LExp extends Linear_Expression {
    def ==(b: Linear_Expression): Constraint = new Constraint(this, Relation_Symbol.EQUAL, b)
    def <=(b: Linear_Expression): Constraint = new Constraint(this, Relation_Symbol.LESS_OR_EQUAL, b)
    def >=(b: Linear_Expression): Constraint = new Constraint(this, Relation_Symbol.GREATER_OR_EQUAL, b)
    def +(b: Linear_Expression): Linear_Expression = this.sum(b)
    def -(b: Linear_Expression): Linear_Expression = this.subtract(b)
    def *(b: Linear_Expression_Coefficient): Linear_Expression = this.times(b.argument())
  }

  implicit class Linear_Expression_Class(val le: Linear_Expression) {
    def ==(b: Linear_Expression): Constraint = new Constraint(le, Relation_Symbol.EQUAL, b)
    def <=(b: Linear_Expression): Constraint = new Constraint(le, Relation_Symbol.LESS_OR_EQUAL, b)
    def >=(b: Linear_Expression): Constraint = new Constraint(le, Relation_Symbol.GREATER_OR_EQUAL, b)
    def + (b: Linear_Expression): Linear_Expression = le.sum(b)
    def - (b: Linear_Expression): Linear_Expression = le.subtract(b)
    def * (b: Linear_Expression_Coefficient): Linear_Expression = le.times(b.argument())
  }

  object LExp {
    case class LConst(v: Long)     extends Linear_Expression_Coefficient(new Coefficient(v)) with LExp
    case class LVar  (i: DimIndex) extends Linear_Expression_Variable   (new Variable   (i)) with LExp

    def flatten(dims: Int, le: Linear_Expression): LinearArray = {
      val rec = flatten(dims,_:Linear_Expression)
      le match {
        case e: Linear_Expression_Difference  => rec(e.left_hand_side()) - rec(e.right_hand_side())
        case e: Linear_Expression_Sum         => rec(e.left_hand_side()) + rec(e.right_hand_side())
        case e: Linear_Expression_Unary_Minus => rec(e.argument).neg
        case e: Linear_Expression_Times       => rec(e.linear_expression()) * e.coefficient().getBigInteger()
        case e: Linear_Expression_Variable    => new LinearArray(dims, e.argument().id(), 1)
        case e: Linear_Expression_Coefficient => new LinearArray(dims, e.argument().getBigInteger())
      }
    }
  }

  def copyInterval(inp: Rational_Box): Rational_Box = {
    catchInterval(inp, {new Rational_Box(_)})
  }

  def catchInterval[T](inp: Rational_Box, f: Rational_Box => T): T = {
    try {
      f(inp)
    } catch {
      case e: Exception =>
        println("===catchInterval===")
        println(inp)
        println("===============")
        throw e
    }
  }

  def isEmpty(inp: Rational_Box): Boolean = {
    catchInterval(inp, {_.is_empty()})
  }
  
  case class LinearArray (const: Coeff, lins: scala.Array[Coeff]) {
    /* This is a flattened version of ppl's Linear_Expression, composed of
     * the constant coefficient followed by an array of coefficients of
     * the various variables a linear_expression mentions.
     */

    // ConstantCoefficient + Var0 * Coefficient0 + Var1 * Coefficient1 + ...

    val size : Int = lins.length
    val constantCoefficient : Coeff = const
    val linearCoefficients : scala.Array[Coeff] = lins

    def this(a: LinearArray) = this(a.constantCoefficient, a.linearCoefficients.clone)

    def this(initsize: Int) = this(0, scala.Array.fill[Coeff](initsize)(0))

    def this(initsize: Int, vid: DimIndex, c: Coeff) = {
      this(0, scala.Array.fill[Coeff](initsize)(0))
      this.linearCoefficients(vid.toInt) = c
    }

    def this(initsize: Int, c: Coeff) = {
      this(c, scala.Array.fill[Coeff](initsize)(0))
    }

    override def clone(): LinearArray = new LinearArray(this)

    override def toString: String = {
      val nonzeros = linearCoefficients.zipWithIndex.filter{case (c,i) => c != 0}.map{case (c,i) => (c, i.toLong)}

      val coeff_string = if (this.constantCoefficient != 0) {
        this.constantCoefficient.toString
      } else { "" }

      val lins_string = if (nonzeros.length == 0) { "0" } else {
        nonzeros.map{p => p.implicitToString}.mkString("")
      }

      coeff_string + lins_string
    }

    def +(b: LinearArray): LinearArray = combine(_ + _, b)
    def -(b: LinearArray): LinearArray = combine(_ - _, b)
    def *(b: Coeff): LinearArray = {
      return new LinearArray(
        this.constantCoefficient * b,
        this.linearCoefficients.map{a => a*b}
      )
    }
    def neg: LinearArray = this * (-1)

    def combine(op: (Coeff, Coeff) => Coeff, b: LinearArray): LinearArray = {
      if (this.size != b.size) {
        throw new IllegalArgumentException
      }
      return new LinearArray(
        this.constantCoefficient + b.constantCoefficient,
        this.linearCoefficients.zip(b.linearCoefficients).map{case (a,b) => op(a,b)}
      )
    }
  }

}

import parma_polyhedra_library._
// import collection.JavaConversions._

import PPL._
import Core._
import Label._
import CommonLoopBounds._
import PolynomialUtil._
import scala.util.matching.Regex


object BoundExpressions {
  class BoundExprException(message: String = null, cause: Throwable = null)
      extends RuntimeException(message, cause)

    class BoundExpression() {
        def fromLinear(l: Linear_Expression) : BoundExpression = {
            l match {
                case l : Linear_Expression_Coefficient => new BoundExpressionConst(l.argument().getBigInteger())
                case l : Linear_Expression_Variable => new BoundExpressionVar(l.argument().id())
                case l : Linear_Expression_Sum => new BoundExpressionSum(this.fromLinear(l.left_hand_side()),this.fromLinear(l.right_hand_side()))
                case l : Linear_Expression_Difference => new BoundExpressionDiff(this.fromLinear(l.left_hand_side()),this.fromLinear(l.right_hand_side()))
                case l : Linear_Expression_Times => new BoundExpressionMult(this.fromLinear(new Linear_Expression_Coefficient(l.coefficient())),this.fromLinear(l.linear_expression()))
                case _ => null
            }
        }

        def fromSolvedLinearConstraint(negate: Boolean)(s: SolvedLinearConstraint) : BoundExpression = {
            s.lhs_coefficient match {
                case c0 if c0 == 0 => null
                case c1 if c1 == 1 => this.fromLinearArray(negate)(s.rhs_coefficients)
                case _ => new BoundExpressionDiv(this.fromLinearArray(negate)(s.rhs_coefficients), new BoundExpressionConst(s.lhs_coefficient))
            }
        }

        def fromLinearArray(negate: Boolean)(a: LinearArray) : BoundExpression = {
            return this.fromLinearArrayCoefficientsWithSize(negate)(a, a.linearCoefficients.size)

        }

        def applyNegation(value: Coeff, negate: Boolean) : Coeff = {
            if (negate) {
                return -value
            } else {
                return value
            }
        }

        def fromLinearArrayCoefficientsWithSize(negate: Boolean)(a: LinearArray, s: Int) : BoundExpression = {
            var r = a.size match {
                case 0 => new BoundExpressionConst(applyNegation(a.constantCoefficient, negate))
                case _ => new BoundExpressionSum(this.fromCoefAndVariable(negate)(a.linearCoefficients(0), s-a.linearCoefficients.size), this.fromLinearArrayCoefficientsWithSize(negate)(new LinearArray(a.constantCoefficient, a.linearCoefficients.tail), s))
            }

            if (r == null) {
                return new BoundExpressionConst(0)
            }

            return r
        }

        def fromCoefAndVariable(negate: Boolean)(c: Coeff, vIndex: BigInt) : BoundExpression = {
            c match {
                case x if x != 0 => new BoundExpressionMult(new BoundExpressionConst(applyNegation(c, negate)), new BoundExpressionVar(vIndex))
                case _ => null
            }
        }

        def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            return this
        }

        def containsOnlySimpleOperations() : Boolean = {
            return false
        }

        def isConst() : Boolean = { return false }

        def simplify(context: InterpContext) : BoundExpression = {
            return null
        }

        // def closedParenthesis(str: String) : Boolean = {
            // It seems Scala does not support recursive regular expressions
            // if (str.matches("""^(\((?:[^\(\)]*|(?-1))*\))$""")) {
            //     return true
            // }
        //
        //     return false
        // }

        def closedParenthesis(str: String) : Boolean = {
            if (str.head == '(' && str.last == ')') {
                return matchedParenthesis(str.init.tail, 0)
            }
            return false
        }

        def matchedParenthesis(str: String, n: Int) : Boolean = {

            if (n < 0) {
                return false
            } else if (str.isEmpty()) {
                return n == 0
            } else if (str.head == '(') {
                return matchedParenthesis(str.tail, n+1)
            } else if (str.head == ')') {
                return matchedParenthesis(str.tail, n-1)
            } else {
                return matchedParenthesis(str.tail, n)
            }
        }

        def addParenthesis(str: String) : String = {
            if (closedParenthesis(str)) {
                return str
            } else if (str.matches("""^[^\s]*$""")) {
                return str
            } else {
                return "(" + str + ")"
            }
        }

        override def toString(): String = {
            return ""
        }

        def toHTMLString(): String = {
            return this.toString().replaceAll("ⁱⁿ", "<sup>in</sup>")
        }
    }

    class BoundExpressionConst(ic: BigInt) extends BoundExpression {
        val constant: BigInt = ic

        override def toString(): String = {
            return constant.toString()
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            return new BoundExpressionConst(constant)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return true
        }

        override def isConst() : Boolean = { return true }

        override def simplify(context: InterpContext) : BoundExpression = {
            return new BoundExpressionPolynomial(PolynomialUtil.construct(this, context))
            // return this
        }
    }

    class BoundExpressionVar(i: BigInt) extends BoundExpression {
        val index: BigInt = i

        def this(l: Linear_Expression_Variable) = this(l.argument().id())

        override def toString(): String = {
            return "v_" + index.toString()
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            return new BoundExpressionVarLabel(binding.backward(index.intValue()))
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return true
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }

    class BoundExpressionVarLabel(l: Label.Label) extends BoundExpression {
        val label: Label.Label = l

        // def this(l: Linear_Expression_Variable) = this(l.argument().id())

        override def toString(): String = {
            return label.toString()
        }

        override def toHTMLString(): String = {
            return label.toString().replaceAll("ⁱⁿ", "<sup>in</sup>")
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return true
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }

    class BoundExpressionVarCustom(s: String) extends BoundExpression {
        val str: String = s

        override def toString(): String = {
            return s
        }

        override def toHTMLString(): String = {
            return s.replaceAll("ⁱⁿ", "<sup>in</sup>")
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return true
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }


    class BoundExpressionConstC() extends BoundExpression {
        override def toString(): String = { return "c" }

        override def toHTMLString(): String = { return toString() }

        override def containsOnlySimpleOperations() : Boolean = {
            return true
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }

    class BoundExpressionPolynomial(p: Polynomial) extends BoundExpression {
        val polynomial: Polynomial = p

        override def toString(): String = {
            return "(" + polynomial.toString() + ")"
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return true
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }

    class BoundExpressionMax(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = "max("

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " , "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            retString += ")"
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression = {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionMax(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return false
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            var xSimplified : BoundExpression = null
            var ySimplified : BoundExpression = null

            if (x != null) {
                xSimplified = x.simplify(context: InterpContext)
            }

            if (y != null) {
                ySimplified = y.simplify(context: InterpContext)
            }

            // println("x " + xSimplified)
            // println("y " + ySimplified)

            if (xSimplified.isInstanceOf[BoundExpressionPolynomial]
              && ySimplified.isInstanceOf[BoundExpressionPolynomial]) {
              val xp = xSimplified.asInstanceOf[BoundExpressionPolynomial].polynomial
              val yp = ySimplified.asInstanceOf[BoundExpressionPolynomial].polynomial
              // println("xp " + xp)
              // println("yp " + yp)

              if (xp.equals(yp)) return xSimplified;

              if (xp.isConstant() && yp.isConstant()) {
                if (xp.constant.compareTo(yp.constant) >= 0) {
                  return xSimplified
                } else {
                  return ySimplified
                }
              // } else if (!xp.isConstant() && yp.isConstant()) {
              //   return xSimplified
              // } else if (xp.isConstant() && !yp.isConstant()) {
              //   return ySimplified
              }
            }
            if (xSimplified.isInstanceOf[BoundExpressionConst]
              && ySimplified.isInstanceOf[BoundExpressionConst]) {
              // println("xc")
              // println("yc")
              if (xSimplified.asInstanceOf[BoundExpressionConst].constant
                >= ySimplified.asInstanceOf[BoundExpressionConst].constant) {
                return xSimplified;
              } else {
                return ySimplified;
              }
            }
            // if (xSimplified.isInstanceOf[BoundExpressionConst]
            //   && ySimplified.isInstanceOf[BoundExpressionPolynomial]) {

            // }


            return new BoundExpressionMax(xSimplified, ySimplified)
        }
    }

    class BoundExpressionMin(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = "min("

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " , "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            retString += ")"
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression = {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionMin(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return false
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            var xSimplified : BoundExpression = null
            var ySimplified : BoundExpression = null

            if (x != null) {
                xSimplified = x.simplify(context: InterpContext)
            }

            if (y != null) {
                ySimplified = y.simplify(context: InterpContext)
            }

            return new BoundExpressionMin(xSimplified, ySimplified)
        }
    }

    class BoundExpressionSum(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " + "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression = {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionSum(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            var ret = true

            if (x != null) {
                ret = ret && x.containsOnlySimpleOperations()
            }

            if (y != null) {
                ret = ret && y.containsOnlySimpleOperations()
            }

            return ret
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            if (this.containsOnlySimpleOperations()) {
                return new BoundExpressionPolynomial(PolynomialUtil.construct(this, context))
            }

            var xSimplified : BoundExpression = null
            var ySimplified : BoundExpression = null

            if (x != null) {
                xSimplified = x.simplify(context: InterpContext)
            }

            if (y != null) {
                ySimplified = y.simplify(context: InterpContext)
            }

            return new BoundExpressionSum(xSimplified, ySimplified)
        }
    }

    class BoundExpressionDiff(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " - "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionDiff(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            var ret = true

            if (x != null) {
                ret = ret && x.containsOnlySimpleOperations()
            }

            if (y != null) {
                ret = ret && y.containsOnlySimpleOperations()
            }

            return ret
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            if (this.containsOnlySimpleOperations()) {
                return new BoundExpressionPolynomial(PolynomialUtil.construct(this, context))
            }

            var xSimplified : BoundExpression = null
            var ySimplified : BoundExpression = null

            if (x != null) {
                xSimplified = x.simplify(context: InterpContext)
            }

            if (y != null) {
                ySimplified = y.simplify(context: InterpContext)
            }

            return new BoundExpressionDiff(xSimplified, ySimplified)
        }
    }

    class BoundExpressionMult(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " * "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionMult(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            var ret = true

            if (x != null) {
                ret = ret && x.containsOnlySimpleOperations()
            }

            if (y != null) {
                ret = ret && y.containsOnlySimpleOperations()
            }

            return ret
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            if (this.containsOnlySimpleOperations()) {
                return new BoundExpressionPolynomial(PolynomialUtil.construct(this, context))
            }
            var xSimplified : BoundExpression = null
            var ySimplified : BoundExpression = null

            if (x != null) {
                xSimplified = x.simplify(context: InterpContext)
            }

            if (y != null) {
                ySimplified = y.simplify(context: InterpContext)
            }

            return new BoundExpressionMult(xSimplified, ySimplified)
        }
    }

    class BoundExpressionDiv(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " / "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionDiv(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            var ret = true

            if (x != null) {
                ret = ret && x.containsOnlySimpleOperations()
            }

            if (y != null) {
                ret = ret && y.containsOnlySimpleOperations()
            }

            return ret
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            if (this.containsOnlySimpleOperations()) {
                return new BoundExpressionPolynomial(PolynomialUtil.construct(this, context))
            }
            var xSimplified : BoundExpression = null
            var ySimplified : BoundExpression = null

            if (x != null) {
                xSimplified = x.simplify(context: InterpContext)
            }

            if (y != null) {
                ySimplified = y.simplify(context: InterpContext)
            }

            return new BoundExpressionDiv(xSimplified, ySimplified)
        }

        // If we want divisions to not be flattened:

        // override def simplify(context: InterpContext) : BoundExpression = {
        //     var xSimplified : BoundExpression = null
        //     var ySimplified : BoundExpression = null
        //
        //     if (x != null) {
        //         xSimplified = x.simplify(context: InterpContext)
        //     }
        //
        //     if (y != null) {
        //         ySimplified = y.simplify(context: InterpContext)
        //     }
        //
        //     return new BoundExpressionMax(xSimplified, ySimplified)
        // }
    }

    class BoundExpressionPow(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += addParenthesis(x.toString)
            }
            if (x != null && y != null) {
                retString += " + "
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            }
            if (x == null && y == null) {
                return "1"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionPow(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return false
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }

    class BoundExpressionLog(l: BoundExpression, r: BoundExpression) extends BoundExpression {
        val x : BoundExpression = l
        val y : BoundExpression = r

        override def toString(): String = {
            var retString = "log_"

            if (x != null) {
                retString += addParenthesis(x.toString)
            } else {
                retString += "2"
            }
            if (y != null) {
                retString += addParenthesis(y.toString)
            } else {
                return "0"
            }

            return retString
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            var xRenamed : BoundExpression = null
            var yRenamed : BoundExpression = null

            if (x != null) {
                xRenamed = x.renameWithBinding(binding)
            }

            if (y != null) {
                yRenamed = y.renameWithBinding(binding)
            }

            return new BoundExpressionLog(xRenamed, yRenamed)
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return false
        }

        override def simplify(context: InterpContext) : BoundExpression = {
            return this
        }
    }

  /**
    * NaN - not a number, for when bound analysis cannot determine a
    * bound, or its infinite.  This enables bound analysis to know
    * about what it doesn't know.  msg is an optional parameter to
    * explain how the bound analysis came to Nan.
    */
    class BoundExpressionNaN(msg: String = "") extends BoundExpression {
        override def toString(): String = {
            return "NaN" + (if (msg == "") "" else "(" + msg + ")")
        }

        override def renameWithBinding(binding: Bimap[Label.Label, DimIndex]) : BoundExpression= {
            return this
        }

        override def containsOnlySimpleOperations() : Boolean = {
            return false
        }

        override def isConst() : Boolean = { return false }

        override def simplify(context: InterpContext) : BoundExpression = {
          return this
        }
    }

  def maxOfPolyList(polys:List[Polynomial]) : BoundExpression = {
    polys match {
      case p :: Nil => new BoundExpressionPolynomial(p)
      case p :: r =>
        val pp = new BoundExpressionPolynomial(p)
        new BoundExpressionMax( pp, maxOfPolyList(r) )
      case _ => assert(false)
        throw new BoundExprException("maxofList: empty")
    }
  }

  def ofExpr[T](e:Expression[T]) : BoundExpression = {
    e match {
      case Expression.Binop(e1, Operator.NumericBinop.+, e2) =>
        new BoundExpressionSum( ofExpr(e1), ofExpr(e2) )

      case Expression.Term(Term.Variable(l)) =>
        new BoundExpressionVarLabel(l)
      case Expression.Term(Term.Constant(CIV(l))) =>
        new BoundExpressionConst(l)
      case Expression.Binop(e1, Operator.NumericBinop.+, e2) =>
        new BoundExpressionSum( ofExpr(e1), ofExpr(e2) )
      case Expression.Binop(e1, Operator.NumericBinop.-, e2) =>
        new BoundExpressionDiff( ofExpr(e1), ofExpr(e2) )
      case Expression.Binop(e1, Operator.NumericBinop.*, e2) =>
        new BoundExpressionMult( ofExpr(e1), ofExpr(e2) )
      case _ => println("unable to convert expr to BoundExpr")
        sys.exit(-1)
    }
  }

  def parse(s:String) : BoundExpression = {
    val p = new Expression.ExpressionParser(Term.Parser)
    val e = p.parseString(s)

    println("parsed BoundExpr as an Expr to: "+e.toString())
    val be = ofExpr(e)
    println("   BE: "+be.toString())
    be
  }
}

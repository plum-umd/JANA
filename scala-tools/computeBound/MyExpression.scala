import parma_polyhedra_library._
// import collection.JavaConversions._

import PPL._
import CommonLoopBounds._


object MyExpression {
    class Expression() {
        def fromLinear(l: Linear_Expression) : Expression = {
            l match {
                case l : Linear_Expression_Coefficient => new ExpressionConst(l.argument().getBigInteger())
                case l : Linear_Expression_Variable => new ExpressionVar(l.argument().id())
                case l : Linear_Expression_Sum => new ExpressionSum(this.fromLinear(l.left_hand_side()),this.fromLinear(l.right_hand_side()))
                case l : Linear_Expression_Difference => new ExpressionDiff(this.fromLinear(l.left_hand_side()),this.fromLinear(l.right_hand_side()))
                case l : Linear_Expression_Times => new ExpressionMult(this.fromLinear(new Linear_Expression_Coefficient(l.coefficient())),this.fromLinear(l.linear_expression()))
                case _ => null
            }
        }

        def fromSolvedLinearConstraint(s: SolvedLinearConstraint) : Expression = {
            s.lhs_coefficient match {
                case c0 if c0 == 0 => null
                case c1 if c1 == 1 => this.fromLinearArray(s.rhs_coefficients)
                case _ => new ExpressionDiv(this.fromLinearArray(s.rhs_coefficients), new ExpressionConst(s.lhs_coefficient))
            }
        }

        def fromLinearArray(a: LinearArray) : Expression = {
            this.fromLinearArrayWithSize(a, a.linearCoefficients.size)
        }

        def fromLinearArrayWithSize(a: LinearArray, s: Int) : Expression = {
            var r = a.size match {
                case 0 => new ExpressionConst(a.constantCoefficient)
                case _ => new ExpressionSum(this.fromCoefAndVariable(a.linearCoefficients(0), s-a.linearCoefficients.size), this.fromLinearArrayWithSize(new LinearArray(a.constantCoefficient, a.linearCoefficients.tail), s))
            }

            if (r == null) {
                return new ExpressionConst(0)
            }

            return r
        }

        def fromCoefAndVariable(c: Coeff, vIndex: BigInt) : Expression = {
            c match {
                case x if x > 0 => new ExpressionMult(new ExpressionConst(x), new ExpressionVar(vIndex))
                case _ => null
            }
        }
    }

    class ExpressionConst(ic: BigInt) extends Expression {
        val constant: BigInt = ic

        override def toString(): String = {
            return constant.toString()
        }
    }

    class ExpressionVar(i: BigInt) extends Expression {
        val index: BigInt = i

        def this(l: Linear_Expression_Variable) = this(l.argument().id())

        override def toString(): String = {
            return "v_" + index.toString()
        }
    }

    class ExpressionSum(l: Expression, r: Expression) extends Expression {
        val x : Expression = l
        val y : Expression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += "(" + x.toString + ")"
            }
            if (x != null && y != null) {
                retString += " + "
            }
            if (y != null) {
                retString += "(" + y.toString() + ")"
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }
    }

    class ExpressionDiff(l: Expression, r: Expression) extends Expression {
        val x : Expression = l
        val y : Expression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += "(" + x.toString + ")"
            }
            if (x != null && y != null) {
                retString += " - "
            }
            if (y != null) {
                retString += "(" + y.toString + ")"
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }
    }

    class ExpressionMult(l: Expression, r: Expression) extends Expression {
        val x : Expression = l
        val y : Expression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += "(" + x.toString + ")"
            }
            if (x != null && y != null) {
                retString += " * "
            }
            if (y != null) {
                retString += "(" + y.toString + ")"
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }
    }

    class ExpressionDiv(l: Expression, r: Expression) extends Expression {
        val x : Expression = l
        val y : Expression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += "(" + x.toString + ")"
            }
            if (x != null && y != null) {
                retString += " / "
            }
            if (y != null) {
                retString += "(" + y.toString + ")"
            }
            if (x == null && y == null) {
                return "0"
            }

            return retString
        }
    }

    class ExpressionPow(l: Expression, r: Expression) extends Expression {
        val x : Expression = l
        val y : Expression = r

        override def toString(): String = {
            var retString = ""

            if (x != null) {
                retString += "(" + x.toString + ")"
            }
            if (x != null && y != null) {
                retString += " + "
            }
            if (y != null) {
                retString += "(" + y.toString + ")"
            }
            if (x == null && y == null) {
                return "1"
            }

            return retString
        }
    }

    class ExpressionLog(l: Expression, r: Expression) extends Expression {
        val x : Expression = l
        val y : Expression = r

        override def toString(): String = {
            var retString = "log_"

            if (x != null) {
                retString += "(" + x.toString + ")"
            } else {
                retString += "2"
            }
            if (y != null) {
                retString += "(" + y.toString + ")"
            } else {
                return "0"
            }

            return retString
        }
    }
}

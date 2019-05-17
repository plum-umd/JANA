import Util._
import Core._

import scalaz._
import Scalaz._

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._

/* Design:

 There is a bunch of classes to store expressions along with companion
 objects for each that handle parsing into their accompanied classes.

 Operator: These are all objects, the Operator class constructs them for convenience.
 + BinaryOperator: Class is for convenience.
   - NumericRelation: ≥, >, ==, <, ≤, ≠
   - NumericBinop: +,-,*,/,%
   - LogicalRelation: ∧,∨,⊕
 + UnaryOperator: Class for convenience.
   - NumericUnop: -
   - LogicalUnop: ¬

 Expression[T]:
 + Term(t: T)
 + BinaryOp(t1: T, op: Operator, t2: T)
 + UnaryOp(t1: T, op: Operator)
 */

object Operator {
  sealed abstract class Operator(
    val name: String,
    val symbol: String
  ) extends BaseOperations[Operator]

  trait BaseOperations[T] extends Parsing[T] { self: T =>
    val name: String
    val symbol: String
    override def toString = symbol

    lazy val parser: PT[T] =
      (logOrNot(keyword(name))(s"named $name") ||| logOrNot(keyword(symbol))(s"symboled $symbol")) ^^^ self

    //lexical.delimiters ++= Seq(symbol)
    lexical.delimiters += symbol
    lexical.reserved   += name
    //lexical.reserved ++= Seq(symbol)
  }

  sealed abstract class BinaryOperator(name: String, symbol: String)
      extends Operator(name, symbol)

  sealed abstract class UnaryOperator(name: String, symbol: String)
      extends Operator(name, symbol)

  sealed abstract class NumericRelation(name: String, symbol: String)
      extends BinaryOperator(name, symbol) {
    val neg: NumericRelation
  }

  object NumericRelation extends Parsing[NumericRelation] {
    case object GreaterOrEqual extends NumericRelation("GreaterOrEqual", "≥") {
      lazy val neg = Lesser
    }
    case object LesserOrEqual extends NumericRelation("LesserOrEqual", "≤") {
      lazy val neg = Greater
    }
    case object Lesser extends NumericRelation("Lesser", "<") {
      lazy val neg = GreaterOrEqual
    }
    case object Greater extends NumericRelation("Greater", ">") {
      lazy val neg = LesserOrEqual
    }
    case object Equal extends NumericRelation("Equal", "=") {
      lazy val neg = NotEqual
    }
    case object NotEqual extends NumericRelation("NotEqual", "≠") {
      lazy val neg = Equal
    }
    
    val == = Equal
    val ≠ = NotEqual
    val ≥ = GreaterOrEqual
    val > = Greater
    val < = Lesser
    val ≤ = LesserOrEqual

    val numericRelations = Seq(==, ≠, ≥, >, <, ≤)

    lexical.delimiters ++= numericRelations.toList.flatMap{_.lexical.delimiters}
    lexical.reserved   ++= numericRelations.toList.flatMap{_.lexical.reserved}

//    lazy val parser: PT[NumericRelation] = logOrNot(<.parser.asInstanceOf[PT[NumericRelation]])("NumericRelation")

    lazy val parser: PT[NumericRelation] = optIter[NumericRelation](
      numericRelations.toList.map{p => logOrNot(p.parser.asInstanceOf[PT[NumericRelation]])("part")}
    )
  }

  sealed abstract class LogicalRelation(name: String, symbol: String)
      extends BinaryOperator(name, symbol)

  object LogicalRelation extends Parsing[LogicalRelation] {
    case object And extends LogicalRelation("And", "∧")
    case object Or  extends LogicalRelation("Or",  "∨")
    case object Xor extends LogicalRelation("Xor", "⊕")

    val ∧ = And
    val ∨ = Or
    val ⊕ = Xor

    val logicalRelations = Seq(∧, ∨, ⊕)

    lexical.delimiters ++= logicalRelations.toList.flatMap{_.lexical.delimiters}
    lexical.reserved   ++= logicalRelations.toList.flatMap{_.lexical.reserved}

    lazy val parser: PT[LogicalRelation] = optIter[LogicalRelation](
      logicalRelations.toList.map{_.parser.asInstanceOf[PT[LogicalRelation]]}
     )
  }

  abstract class LogicalUnop(name: String, symbol: String)
      extends UnaryOperator(name, symbol)

  object LogicalUnop extends Parsing[LogicalUnop] {
    case object Not extends LogicalUnop("Not", "¬")
    val ¬ = Not
    val logicalUnops = Seq(¬)
    lazy val parser: PT[LogicalUnop] = ¬.parser.asInstanceOf[PT[LogicalUnop]]
  }

  abstract class NumericBinop(name: String, symbol: String)
      extends BinaryOperator(name, symbol)

  object NumericBinop extends Parsing[NumericBinop] {
    case object Plus  extends NumericBinop("Plus",  "+")
    case object Minus extends NumericBinop("Minus", "-")
    case object Times extends NumericBinop("Times", "*")
    case object Div   extends NumericBinop("Div",   "/")
    case object Mod   extends NumericBinop("Mod",   "%")

    val + = Plus
    val - = Minus
    val * = Times
    val / = Div
    val % = Mod

    val numericBinops1 = Seq(Plus, Minus)
    val numericBinops2 = Seq(*, /, %)
    val numericBinops = numericBinops1 ++ numericBinops2

    lexical.delimiters ++= numericBinops.toList.flatMap{_.lexical.delimiters}
    lexical.reserved   ++= numericBinops.toList.flatMap{_.lexical.reserved}

    lazy val parser1: PT[NumericBinop] = optIter[NumericBinop](
      numericBinops1.toList.map{_.parser.asInstanceOf[PT[NumericBinop]]}
    )
    lazy val parser2: PT[NumericBinop] = optIter[NumericBinop](
      numericBinops2.toList.map{_.parser.asInstanceOf[PT[NumericBinop]]}
    )
    lazy val parser: PT[NumericBinop] = optIter[NumericBinop](
      numericBinops.toList.map{_.parser.asInstanceOf[PT[NumericBinop]]}
    )
  }

  abstract class NumericUnop(name: String, symbol: String)
      extends UnaryOperator(name, symbol)

  object NumericUnop extends Parsing[NumericUnop] {
    case object Neg extends NumericUnop("Neg", "-")

    /*case class Cast(t: Type) {
      override def toString = s"($t)"
    }*/

    val - = Neg

    val numericUnops = Seq(Neg)

    lexical.delimiters ++= numericUnops.toList.flatMap{_.lexical.delimiters}
    lexical.reserved   ++= numericUnops.toList.flatMap{_.lexical.reserved}

    lazy val parser: PT[NumericUnop] = optIter[NumericUnop](
      numericUnops.toList.map{_.parser.asInstanceOf[PT[NumericUnop]]}
    )
  }
}

trait FoldRec[M] {
  def fold[A](init: A, f: M => A, plus: A => A => A): A
  def all(f: M => Boolean): Boolean = {
    fold[Boolean](true, f, {a => b => a.&&(b)})
  }
  def any(f: M => Boolean): Boolean = {
    fold[Boolean](false, f, {a => b => a.||(b)})
  }
}

abstract class Expression[T] extends FoldRec[Expression[T]] {
  //val typ: Type
  //def subst[T](id: String, v: Expression[T]): Expression[T] = this.asInstanceOf[Expression[T]]

  def mapTerms[U](m: T => U): Expression[U]

  val neg: Expression[T]
  lazy val unary_¬ = neg // this doesn't work because of scala
  lazy val unary_! = neg

  def +(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericBinop.+, v2)
  def -(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericBinop.-, v2)
  def *(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericBinop.*, v2)
  def /(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericBinop./, v2)

  def ∧(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.LogicalRelation.∧, v2)
  def ∨(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.LogicalRelation.∨, v2)
  def ≥(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericRelation.≥, v2)
  def >(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericRelation.>, v2)
  def ≤(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericRelation.≤, v2)
  def <(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericRelation.<, v2)
  def ==(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericRelation.==, v2)
  def ≠(v2: Expression[T]): Expression[T] =
    Expression.Binop[T](this, Operator.NumericRelation.≠, v2)
}

object Expression {
  import Operator._

  class ExpressionParser[T](val term_parser: Parsing[T]) extends Parsing[Expression[T]] {
    val tp               = castp(term_parser)
    val op_logical_rel   = castp(LogicalRelation)
    val op_logical_unop  = castp(LogicalUnop)
    val op_numeric_rel   = castp(NumericRelation)
    val op_numeric_binop1 = NumericBinop.parser1.asInstanceOf[PT[NumericBinop]]
    val op_numeric_binop2 = NumericBinop.parser2.asInstanceOf[PT[NumericBinop]]
    val op_numeric_unop  = castp(NumericUnop)

    lexical.delimiters ++= Seq("(", ")")
    lexical.delimiters ++= LogicalRelation.lexical.delimiters
    lexical.delimiters ++= LogicalUnop.lexical.delimiters
    lexical.delimiters ++= NumericRelation.lexical.delimiters
    lexical.delimiters ++= NumericBinop.lexical.delimiters
    lexical.delimiters ++= NumericUnop.lexical.delimiters
    lexical.reserved ++= LogicalRelation.lexical.reserved
    lexical.reserved ++= LogicalUnop.lexical.reserved
    lexical.reserved ++= NumericRelation.lexical.reserved
    lexical.reserved ++= NumericBinop.lexical.reserved
    lexical.reserved ++= NumericUnop.lexical.reserved

    lazy val term: PT[Term[T]] = tp ^^ { case t => Term[T](value = t) }

    lazy val bool_expression: PT[Expression[T]] = {
      logOrNot("(" ~> bool_expression <~ ")")("parened bool_expression") |||
      logOrNot(logical_unop)("logical_unop") |||
      logOrNot(logical_relation)("logical_relation") |||
      logOrNot(numeric_relation)("numeric_relation")
    }

    lazy val numeric_expression: PT[Expression[T]] = {
      logOrNot(numeric_binop)("numeric_binop") |||
      numeric_expression_term
    }

    lazy val numeric_expression_term: PT[Expression[T]] = {
      logOrNot(term)("term") |||
      ("(" ~> numeric_expression <~ ")") |||
      logOrNot(numeric_unop)("numeric_unop")
    }

    lazy val logical_relation: PT[Expression[T]] =
      bool_expression ~ op_logical_rel ~ bool_expression ^^
        { case e1 ~ r ~ e2 => Binop(e1, r, e2) }

    lazy val logical_unop: PT[Expression[T]] =
      op_logical_unop ~ bool_expression ^^
        { case r ~ e2 => Unop(e2, r) }

    lazy val numeric_relation: PT[Expression[T]] =
      numeric_expression ~ logOrNot(op_numeric_rel)("op_numeric_rel") ~ numeric_expression ^^
        { case e1 ~ r ~ e2 => Binop(e1, r, e2) }

    // This strangeness for proper arithmetic operator precedence is
    // from here:
    // http://jim-mcbeath.blogspot.com/2008/09/scala-parser-combinators.html
    lazy val numeric_binop: PT[Expression[T]] =
      (numeric_expression_term * (op_numeric_binop2 ^^ { r => (a:Expression[T],b:Expression[T]) => Binop(a, r, b) }) *
        (op_numeric_binop1 ^^ { r => (a:Expression[T],b:Expression[T]) => Binop(a, r, b) }))

    lazy val numeric_unop: PT[Expression[T]] =
      op_numeric_unop ~ numeric_expression ^^
        { case r ~ e2 => Unop(e2, r) }

    lazy val parser: PT[Expression[T]] = {
      logOrNot(bool_expression)("bool_expression") |||
      logOrNot(numeric_expression)("numeric_expression")

    }
  }

  case class Term[T](value:T) extends Expression[T] {
    override def toString = value.toString
    def fold[A](init: A, f: Expression[T] => A, plus: A => A => A): A = {
      plus(f(this))(init)
    }
    lazy val neg = ???
    def mapTerms[U](f: T => U): Term[U] = Term[U](f(value))
  }

  /*
  case class Variable[T](v:String) extends Expression[T] {
    val name = v
    override def subst[T](id: String, new_v: Expression[T]): Expression[T] = {
      if (name == id) {
        new_v
      } else
        this.asInstanceOf[Expression[T]]
    }
   }
   */
  case class Binop[T](
    val v1: Expression[T],
    val r: Operator.BinaryOperator,
    val v2: Expression[T]) extends Expression[T] with ShowBinop[T] {

    lazy val neg = r match {
      case nr: NumericRelation => copy(r = nr.neg)
      case _ => ???
    }

    def fold[A](init: A, f: Expression[T] => A, plus: A => A => A): A = {
      v1.fold(
        v2.fold(plus(f(this))(init), f, plus),
        f, plus)
    }

    def mapTerms[U](f: T => U): Binop[U] = {
      Binop[U](v1.mapTerms(f), r, v2.mapTerms(f))
    }

    //lazy val neg: BooleanExpression[T] =
    //  LogicalUnop(this, Operator.LogicalUnop.¬).asInstanceOf[BooleanExpression[T]]
    // lazy val neg = this.copy(v1, r = r.neg, v2)

    /*
    override def subst[T](id: String, new_v: Expression[T]): Expression[T] = {
      LogicalRelation(
        v1.subst[T](id, new_v).asInstanceOf[Expression[T]],
        r,
        v2.subst[T](id, new_v).asInstanceOf[Expression[T]]
      )
    }
     */
  }

  case class Unop[T](
    val v1: Expression[T],
    val r: Operator.UnaryOperator) extends Expression[T] with ShowUnop[T] {

    def fold[A](init: A, f: Expression[T] => A, plus: A => A => A): A = {
      v1.fold(
        plus(f(this))(init),f,plus)
    }

    lazy val neg = r match {
      case NumericUnop.Neg => v1
      case LogicalUnop.Not => v1
      case _ => ???
    }

    def mapTerms[U](f: T => U): Unop[U] = {
      Unop[U](v1.mapTerms(f), r)
    }

    /*
    override def subst[T](id: String, new_v: Expression[T]): Expression[T] = {
      LogicalUnop(
        v1.subst[T](id, new_v).asInstanceOf[Expression[T]],
        r
      )
    }*/
  }

  def ∧[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, LogicalRelation.∧, v2)
  def ∨[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, LogicalRelation.∨, v2)
  def ≥[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericRelation.≥, v2)
  def >[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericRelation.>, v2)
  def ≤[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericRelation.≤, v2)
  def <[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericRelation.<, v2)
  def ==[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericRelation.==, v2)
  def ≠[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericRelation.≠, v2)

  def +[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericBinop.+, v2)
  def -[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericBinop.-, v2)
  def *[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericBinop.*, v2)
  def /[T](v1: Expression[T], v2: Expression[T]): Expression[T] =
    Binop[T](v1, NumericBinop./, v2)
}

trait ShowBinop[T] {
  val v1: Expression[T]
  val v2: Expression[T]
  val r: Operator.BinaryOperator
  override def toString:String = s"($v1 $r $v2)"
}

trait ShowUnop[T] {
  val v1: Expression[T]
  val r: Operator.UnaryOperator
  override def toString:String = s"($r $v1)"
}

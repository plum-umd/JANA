import Label._

object Exp {
  type Closed = Expression[Term.Closed]
  type Open   = Expression[Term]

  def close[O <: Term](e: Expression[O], mf: Label => Term.Closed): Closed = {
    e.mapTerms{
      case Term.Variable(l) => mf(l)
      case x: Term.Closed => x
    }
  }

  implicit class closeExpression(e: Exp.Open) {
    def close(mf: Label => Term.Closed): Exp.Closed = Exp.close(e, mf)
  }
}

abstract class Term

object Term {
  abstract class Closed extends Term
  abstract class Open   extends Term
  
  object Parser extends Parsing[Term] {
    lazy val parser: PT[Term] = castp(Constant) ||| castp(Null) ||| castp(Variable)
    // Note, there is no Linear parsing as those should instead be
    // parsed as variables, which may or may not be linear. The
    // evaluation of an open expression into a closed expression would
    // fill in the linear terms given their labels inside variable.
    lexical.reserved ++= Constant.lexical.reserved ++ Null.lexical.reserved ++ Variable.lexical.reserved
  }

  object Variable extends Parsing[Variable] {
    lazy val parser: PT[Variable] = castp(Label.Parser) ^^ { case l => Variable(l) }
  }
  case class Variable(v: Label) extends Open {
    override def toString = v.toString
  }

  // A constant value.
  object Constant extends Parsing[Constant] {
    lazy val parser: PT[Constant] = int ^^ {case i => Constant(CIV(i))}
  }
  case class Constant(v: Value) extends Closed {
    override def toString = s"$v"
  }

  // Pointer to a single linear dimension.
  object Linear extends Parsing[Linear] {
    lazy val parser: PT[Linear] = ???
  }
  case class Linear(label: Label) extends Closed {
    override def toString = s"Linear" //($label)"
  }

  object Null extends Parsing[Null] {
    lazy val parser: PT[Null] = ("null" ||| "Null" ||| "NULL") ^^^ Null()
    lexical.reserved += ("null", "Null", "NULL")
  }
  case class Null() extends Closed {
    override def toString = s"Null"
  }
}

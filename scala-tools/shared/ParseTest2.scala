import Expression._
import Annotation._
import AnnotationSerialize._ 
import scala.util.parsing.input._

object ParseTest2 {
  def main(argv: scala.Array[String]) {
    //Core.debug = true

    //val label1 = Label.Parser.parseString("pc") // see ../shared/Label.scala for parsers

    //val term1 = Term.Parser.parseString("42")  // see Term.scala for parsers
    //val term2 = Term.Parser.parseString("v10") // can also parse "open" terms which are labels

    val exp_parser = new Expression.ExpressionParser(Term.Parser) // see Expression.scala

    val e1: Expression[Term] = exp_parser.parseString("42 ≤ 1 + null")
    val e2: Expression[Term] = exp_parser.parseString("11 * 42 + 10 * 20 < ret + v0")
    val e3: Expression[Term] = exp_parser.parseString("11 * (42 + 10) * 20 + 100 ≥ ret + v0")

    println(s"e1 = $e1")
    println(s"e2 = $e2")
    println(s"e3 = $e3")

    //t1

  }
}

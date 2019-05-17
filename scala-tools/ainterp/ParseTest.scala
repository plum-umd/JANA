import Expression._
import Annotation._
import AnnotationSerialize._ 
import Specification._
import SpecificationSerialize._
import scala.util.parsing.input._

object ParseTest {
  private val t1_s =
"""
PPLTest.test1(II)I:

    [1, before]: 
      - [assert, v1]
    [2, after]:  
      - [assume, v1]

PPLTest.test2(I)I:

    [7, before]: 
      - [assert, v1]
"""

  private val t2_s =
"""
PPLTest.test1(II)I:
    pre: v1 = 0
    post: v2 > 5
    bound: v3
"""

  def t1 {
    val res : Annots = AnnotationSerialize.deserialize(t1_s)
    val t1_s2 : String = AnnotationSerialize.serialize(res)

    Log.println(res)
    Log.println(t1_s2) 
  }

  def t2 {
    val res : Specs = SpecificationSerialize.deserialize(t2_s)
    val t2_s2 : String = SpecificationSerialize.serialize(res)

    Log.println(res)
    Log.println(t2_s2)
  }

  def main(argv: scala.Array[String]) {

    PPL.init

    Core.debug = true

    val label1 = Label.Parser.parseString("pc") // see ../shared/Label.scala for parsers

    val term1 = Term.Parser.parseString("42")  // see Term.scala for parsers
    val term2 = Term.Parser.parseString("v10") // can also parse "open" terms which are labels

    val exp_parser = new Expression.ExpressionParser(Term.Parser) // see Expression.scala

    val e1: Expression[Term] = exp_parser.parseString("(42 < (1 + null))")
    val e2: Expression[Term] = exp_parser.parseString("(42 < (ret + v0))")

    Log.println(s"e1 = $e1")
    Log.println(s"e2 = $e2")

    t1
    t2

  }
}

import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._

import Expression._
import Term._
import BoundExpressions._

object Specification extends SpecificationProtocol {

    class Specification(val bound : BoundExpression, val pre : Expression[Term], val post : Expression[Term]) {

        override def toString : String = {
            this.toYaml.prettyPrint
        }
    }

    type Specs = Map[String, Specification]

    val emptySpecs = Map[String, Specification]()

    var current = emptySpecs

    def putSpecs (s : Specs) : Unit = {
        current = s
    }

    def getSpecs : Specs = {
        current
    }
}

trait SpecificationFormat {
    implicit object SpecificationYamlFormat extends YamlFormat[Specification.Specification] {
        def write(s : Specification.Specification) : YamlValue = {
            YamlObject(Map[YamlValue, YamlValue](YamlString("pre") -> YamlString(s.pre.toString), 
                                                 YamlString("post") -> YamlString(s.post.toString),
                                                 YamlString("bound") -> YamlString(s.bound.toString)))
        }

        def read(y : YamlValue) : Specification.Specification = {
            val p = new Expression.ExpressionParser(Term.Parser)

            y match {
                case YamlObject(m) => 
                    val b = m(YamlString("bound")) match {
                        case YamlString(s) => BoundExpressions.parse(s)
                        case err => deserializationError("(Bound) Expected YamlString(...), but got " + err)
                    }

                    val pre = m(YamlString("pre")) match {
                        case YamlString(s) => p.parseString(s)
                        case err => deserializationError("(Pre) Expected YamlString(...), but got " + err)
                    }

                    val post = m(YamlString("post")) match {
                        case YamlString(s) => p.parseString(s)
                        case err => deserializationError("(Post) Expected YamlString(...), but got " + err)
                    }

                    new Specification.Specification(b, pre, post)
                case _ => deserializationError("(Specification) Expected YamlObject(...), but got " + y)
            }
        }
    }
}

trait SpecificationProtocol
    extends DefaultYamlProtocol
    with SpecificationFormat

object SpecificationSerialize extends SpecificationProtocol {
    def deserialize(y : String) : Specification.Specs = {
        y.parseYaml.convertTo[Specification.Specs]
    }

    def serialize(a : Specification.Specs) : String = {
        a.toYaml.prettyPrint
    }
}

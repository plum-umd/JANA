import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._ 

import Expression._

object Annotation {
  abstract class Annotation
  case class Assume(val e:Exp.Open) extends Annotation {
    // Add constraint e.
    override def toString = s"assume($e)"
  }

  case class Assert(val e:Exp.Open) extends Annotation {
    // Assert the negation of e, and if that is satisfiable fail.
    override def toString = s"assert($e)"
  }

  type AnnoLoc      = (WALA.IIndex, Order.t)

  type Annots       = Map[String, MethodAnnots]
  type MethodAnnots = Map[AnnoLoc, List[Annotation]]

  val emptyAnnots       = Map[String, MethodAnnots]()
  val emptyMethodAnnots = Map[AnnoLoc, List[Annotation]]()
}

object Order extends Enumeration {
  type t = Value
  val Before, After = Value
}

trait AnnotationFormat {
  implicit object OrderTYamlFormat extends YamlFormat[Order.t] {
    def write(o : Order.t) : YamlValue = o match {
      case Order.Before => YamlString("before")
      case Order.After => YamlString("after")
      case x => deserializationError("Expected Order.t, but got " + x)
    }

    def read(v : YamlValue) : Order.t = v match {
      case YamlString(str) =>
        if (str == "before")
          Order.Before
        else if (str == "after")
          Order.After
        else
          deserializationError("Expected YamlString(\"before\") or YamlString(\"after\")")
      case _ => deserializationError("Expected YamlString(\"before\") or YamlString(\"after\")")
    }
  }

  implicit object AnnotationYamlFormat extends YamlFormat[Annotation.Annotation] {
    def write(a : Annotation.Annotation) : YamlValue = a match {
      case Annotation.Assume(e) => YamlArray(Vector(YamlString("assume"), YamlString(e.toString)))
      case Annotation.Assert(e) => YamlArray(Vector(YamlString("assert"), YamlString(e.toString)))
    }

    def read(v : YamlValue) : Annotation.Annotation = {
      val p = new Expression.ExpressionParser(Term.Parser)

      v match {
        case YamlArray(Vector(YamlString(ts), YamlString(es))) =>
          if (ts == "assume")
            Annotation.Assume(p.parseString(es))
          else if (ts == "assert")
            Annotation.Assert(p.parseString(es))
          else
            deserializationError("Expected YamlString(\"assert\") or YamlString(\"assume\")")
        case err => deserializationError(s"Expected YamlArray(...) but got $err")
      }
    }
  }
}

trait AnnotationProtocol 
  extends DefaultYamlProtocol
  with AnnotationFormat

object AnnotationSerialize extends AnnotationProtocol {
  def deserialize(y : String) : Annotation.Annots = {
    y.parseYaml.convertTo[Annotation.Annots]
  }

  def serialize(a : Annotation.Annots) : String = {
    a.toYaml.prettyPrint
  }
}
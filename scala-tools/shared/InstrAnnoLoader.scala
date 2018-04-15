// - instruction location
// (function id ++ block id ++ instruction index ++ pre|post)
// instruction index accounts for phi nodes in a block, index starts at 0

import Annotation._
import WALA._
//import AnnoLoc._

import Expression._

import play.api.libs.json._

import java.io._

// https://www.playframework.com/documentation/2.0.4/ScalaJson
// JsObject
// JsNull
// JsUndefined
// JsBoolean
// JsNumber
// JsArray
// JsString

object InstrAnnoLoader {
  def loadFilename(fileName:String): Map[String, MethodAnnots] = {
    val p = new File(fileName)
    load(p)
  }

  def load(f:File): Map[String, MethodAnnots] = {
    val source = scala.io.Source.fromFile("file.txt")
    val lines = try source.mkString finally source.close()
    val json: JsValue = Json.parse(lines)

    //val maybeName = (json \ "user" \ name).asOpt[String]
    //val emails = (json \ "user" \\ "emails").map(_.as[String])
    //val loc2anno = collection.mutable.Map[AnnoLoc, Annotation]()
    val gt =   	 /* v2 */
      Expression.Term[Term](Term.Constant(CIV(4))) > Expression.Term[Term](Term.Constant(CIV(0)))

    val sampleMethodAnnot = emptyMethodAnnots + ((2, Order.Before) -> Assume(gt))
    val sampleAnnots = emptyAnnots + ("PPLTest.test1anno(II)I" -> sampleMethodAnnot)

    // .. read JSON strings. Convert to Anno data types .. put in loc2 anno ...
    //loc2anno.toMap

    val entries: List[JsValue] = ??? // get the individual annotations
    val annots: Annots = entries.foldLeft(emptyAnnots){case (annots:Annots, entry) =>
      val method: String      = ??? // get the method name
      val iindex: WALA.IIndex = ??? // get the instruction index
      val order: Order.t      = ??? // get the before/after
      val annot: Annotation   = ??? // get the actual annotation
      val old_method_annots: MethodAnnots = annots.getOrElse(method, default=emptyMethodAnnots)
      annots + (method -> (old_method_annots + ((iindex, order) -> List(annot))))
    }

    annots
  }
}

//- add annotation map in stepRegion for PHI and stepInstruction for instructions
//- address from a map from (instruction location -> list of annotations)
//- create two Option[Action] values from the list of annotations
//- call m.mapState with the result of Option[Action] for pre
//- call m.mapState with the result of Option[Action] for post

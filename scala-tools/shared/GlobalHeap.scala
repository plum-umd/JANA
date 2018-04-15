import Core._
import Util._
import scalaz._
import Scalaz._

//import scala.collection.{Map}
//import scala.collection.Map._
//import scala.collection.immutable.{Map=>IMap, Set, HashMap}

import edu.illinois.wala.Facade._

import collection.JavaConversions._

object GlobalHeap {
  
  //points-to graph
  var heap:WALA.HeapGraph = _
  
  //call graph
  var cg:WALA.CG = _
  
  var pk2ik = Map[WALA.PK, Option[WALA.IK]]()

  def getFields(t:WALA.TYPE) : List[WALA.Field] = {
    try {
    val fields = cg.getClassHierarchy().lookupClass(t).getAllFields().toList
    fields.foldLeft(List[WALA.Field]()) {
      case (l, field) =>
        WALA.getTypeType(field.getFieldTypeReference) match {
          case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
            field :: l
          case _ => l
        }
    }
    } catch {
      case _: Throwable => List[WALA.Field]()
    }
  }
  
  //return the corresponding pointerkey heap graph node of the field being accessed
  def getFieldPKs(node:WALA.CGNode, i:AccessI): List[WALA.PK] = {
    var ret = List[WALA.PK]()

    val f = cg.getClassHierarchy().resolveField(i.getDeclaredField());

    val pk = heap.getHeapModel().getPointerKeyForLocal(node, i.getRef())

    val it = heap.getPointerAnalysis().getPointsToSet(pk).iterator()

    while(it.hasNext) {
      val ik = it.next
      val fkey = heap.getHeapModel().getPointerKeyForInstanceField(ik, f)
      if(!pk2ik.containsKey(fkey)) {
        pk2ik += (fkey -> Some(ik))
      }
      ret ::= fkey
    }
    ret
  }
  
  def getPointsToSet(node:WALA.CGNode, i:AccessI): List[WALA.IK] = {
    val pk = heap.getHeapModel().getPointerKeyForLocal(node, i.getRef())
    heap.getPointerAnalysis().getPointsToSet(pk).toList
  }
  
  def getFieldPK(ik:WALA.IK, f:WALA.Field): WALA.PK = {
     val fkey = heap.getHeapModel().getPointerKeyForInstanceField(ik, f)
     if(!pk2ik.containsKey(fkey)) {
       pk2ik += (fkey -> Some(ik))
     }
     fkey
  }
  
  def getPointerKey(node:WALA.CGNode, vn:Int): WALA.PK = {
    heap.getHeapModel().getPointerKeyForLocal(node, vn)
  }
  
  def getPointsToSet(node:WALA.CGNode, vn:Int): List[WALA.IK] = {
    val pk = heap.getHeapModel().getPointerKeyForLocal(node, vn)
    heap.getPointerAnalysis().getPointsToSet(pk).toList
  }
  
  def getPointerKeyForStaticField(f:WALA.Field): WALA.PK = {
    val fkey = heap.getHeapModel().getPointerKeyForStaticField(f)
    if(!pk2ik.containsKey(fkey)) {
       pk2ik += (fkey -> None)
     }
    fkey
  }
  
  def getField(field:WALA.FieldReference): WALA.Field = {
    cg.getClassHierarchy().resolveField(field);
  }
  
  
  
  def getAliases(node:WALA.CGNode, vn:Int): List[WALA.PK] = {
    val pk = heap.getHeapModel().getPointerKeyForLocal(node, vn)
    
    val it = heap.getPointerAnalysis().getPointsToSet(pk).iterator()
    
    var l = Set[WALA.PK]()

    while(it.hasNext) {
      val ik = it.next
      val pkIt = heap.getPredNodes(ik)
      while(pkIt.hasNext()) {
        val pred = pkIt.next()
        pred match {
          case p: WALA.PK =>
            if(!l.contains(p) && !l.equals(pk)) {
              l += p
            }
          case _ =>
        }
      }
    }
    
    l.toList
  }

  //return the corresponding pointerkey heap graph node of the array being accessed (cuurently not disambiguate the array elements)
  def getArrayKeys(node:WALA.CGNode, i:ArrayReferenceI): List[WALA.PK] = {
    var ret = List[WALA.PK]()

    val pk = heap.getHeapModel().getPointerKeyForLocal(node, i.getArrayRef())

    val it = heap.getPointerAnalysis().getPointsToSet(pk).iterator()

    while(it.hasNext) {
      val ik = it.next
      val akey = heap.getHeapModel().getPointerKeyForArrayContents(ik)
      if(!pk2ik.containsKey(akey)) {
        pk2ik += (akey -> Some(ik))
      }
      ret ::= akey
    }
    ret
  }
  
  def getArrayKey(ik: WALA.IK): WALA.PK = {
    val akey = heap.getHeapModel().getPointerKeyForArrayContents(ik)
    if(!pk2ik.containsKey(akey)) {
      pk2ik += (akey -> Some(ik))
    }
    akey
  }
  
  def getPointsToSet(node:WALA.CGNode, i:ArrayReferenceI): List[WALA.IK] = {
    val pk = heap.getHeapModel().getPointerKeyForLocal(node, i.getArrayRef())
    heap.getPointerAnalysis().getPointsToSet(pk).toList
  }
  
  def getArrayLengthKeys(node:WALA.CGNode, i:Int): List[WALA.PK] = {
    var ret = List[WALA.PK]()

    val pk = heap.getHeapModel().getPointerKeyForLocal(node, i)

    val it = heap.getPointerAnalysis().getPointsToSet(pk).iterator()

    while(it.hasNext) {
      val ik = it.next
      val lkey = new com.ibm.wala.ipa.modref.ArrayLengthKey(ik)
      if(!pk2ik.containsKey(lkey)) {
        pk2ik += (lkey -> Some(ik))
      }
      ret ::= lkey
    }

    ret

  }
  
  def getPointsToSet(node:WALA.CGNode, i:ArrayLengthI): List[WALA.IK] = {
    val pk = heap.getHeapModel().getPointerKeyForLocal(node, i.getArrayRef())
    heap.getPointerAnalysis().getPointsToSet(pk).toList

  }
  
  def getLengthPK(ik:WALA.IK): WALA.PK = {
     val lkey = new com.ibm.wala.ipa.modref.ArrayLengthKey(ik)
     if(!pk2ik.containsKey(lkey)) {
       pk2ik += (lkey -> Some(ik))
     }
     lkey
  }
  
  def getArrayKeys(node:WALA.CGNode, vn:Int): List[WALA.PK] = {
    var ret = List[WALA.PK]()

    val pk = heap.getHeapModel().getPointerKeyForLocal(node, vn)

    val it = heap.getPointerAnalysis().getPointsToSet(pk).iterator()

    while(it.hasNext) {
      val ik = it.next
      val akey = heap.getHeapModel().getPointerKeyForArrayContents(ik)
      if(!pk2ik.containsKey(akey)) {
        pk2ik += (akey -> Some(ik))
      }
      ret ::= akey
    }
    ret
  }

}

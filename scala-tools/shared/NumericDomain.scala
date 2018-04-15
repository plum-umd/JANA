import parma_polyhedra_library._
import java.io._
import collection.JavaConversions._

import Util._
import Core._
import CommonLoopBounds._

trait NumericDomain {
  type rep
  val rep: PPL_Object
  val constraints: List[Constraint]
  val numDims: Long

  val isEmpty: Boolean
  def mapDimsWithRemoval(vmap: List[(DimIndex, DimIndex)]): NumericDomain
  def mapDims(vmap: List[(DimIndex, DimIndex)]): NumericDomain
  def joinDims(vid1: DimIndex, vid2: DimIndex): NumericDomain
  def addDims(dims: Int): NumericDomain
  def expandUpTo(maxvid: DimIndex): NumericDomain
  def expandUpToProject(maxvid: DimIndex): NumericDomain
  def widen(q: NumericDomain): NumericDomain
  def join(q: NumericDomain): NumericDomain
  def leq(q: NumericDomain): Boolean
  def projectTo(i: DimIndex): NumericDomain
  def projectTo(is: Iterable[DimIndex]): NumericDomain
  def copyDimension(d: DimIndex): NumericDomain
  def isDisjoint(q: NumericDomain) : Boolean
  def affineImage(v: Variable, e: Linear_Expression): NumericDomain
  def removeDims(dims: List[DimIndex]): NumericDomain
  def addConstraint(c: Constraint): NumericDomain
  def concat(inq: NumericDomain): NumericDomain
  def isConstraint(d: DimIndex): Boolean
}

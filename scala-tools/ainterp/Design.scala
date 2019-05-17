import Core._

object IBinding {
  case class NotFound(s: String) extends Exception(s)
}

trait IBinding[D,R] {
  def bind   (k: D, v: R): IBinding[D,R]
  def alloc  (k: D): IBinding[D,R]
  def unbind (k: D): IBinding[D,R]
  def apply  (k: D): Option[R]
  def get    (k: D): R // can throw NotFound
  def isBound(k: D): Boolean
}

/* Label moved to shared/Label.scala */
import Label._

object ILattice {
  def join[L <: ILattice[L]](ms: Iterable[L]): L = {
    ms.toList match {
      case Nil => throw LogicException("?")
      case h :: t => h.join(t)
    }
  }
}

trait ILattice[This <: ILattice[This]] { self: This =>
  def join (m2: This): This
  def widen(m2: This): This
  def leq  (m2: This): Boolean

  def isBottom: Boolean
  def isTop   : Boolean

  def isFeasible: Boolean = ! isBottom

  def join(ms: Iterable[This]): This = {
    ms.foldLeft(this){
      case (m1, m2) => m1.join(m2)
    }
  }

  def ⊔(ms: Iterable[This]) = join(ms)
  def ⊔(m2: This) = join(m2)
  def ⊑(m2: This) = leq(m2)
  def ⊥? = isBottom
  def ⊤? = isTop
}

abstract class IMachine[This <: IMachine[This]]
    extends    ILattice[This] { self: This =>

  def assume(c: Exp.Open): This
  def assert(c: Exp.Open): This
  def writevalue(l: Label, v: Term.Closed): This
}

abstract class IState[This <: IState[This]] { self: This =>

}

abstract class IFrame[This <: IFrame[This]] {
}

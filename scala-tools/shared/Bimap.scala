case class Bimap[K,V](
  val forward: Map[K,V],
  val backward: Map[V,K]
) extends Map[K,V] {
  def this() = this(Map[K,V](), Map[V,K]())

  def +[V1 >: V](kv: (K, V1)): Bimap[K,V1] =
    kv match { case (k,v) =>
      Bimap(
        forward = this.forward + (k -> v),
        backward = this.backward.asInstanceOf[Map[V1,K]] + (v -> k)
      )
    }

  def +/(kv: (V, K)): Bimap[K,V] = 
    kv match {
      case (v,k) => Bimap(
        forward = this.forward + (k -> v),
        backward = this.backward + (v -> k)
      )
    }


  def -(key: K): Bimap[K,V] = {
    val mv = forward.get(key)
    mv match {
      case Some(v) => Bimap(
        forward.filterKeys(_ != key),
        backward.filterKeys(_ != v)
      )
      case None => this
    }
  }


  def -/(v: V): Bimap[K,V] = {
    val mk = backward.get(v)
    mk match {
      case Some(k) => Bimap(
        forward.filterKeys(_ != k),
        backward.filterKeys(_ != v)
      )
      case None => this
    }
  }

  def back: Bimap[V,K] = Bimap(forward = this.backward, backward = this.forward)

  def applyBack(v: V) = getBack(v).get

  def get(key: K): Option[V] = forward.get(key)
  def getBack(v: V): Option[K] = backward.get(v)

  def iterator: Iterator[(K, V)] = forward.iterator
  def iteratorBack: Iterator[(V, K)] = backward.iterator
}

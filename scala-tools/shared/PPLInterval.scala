import parma_polyhedra_library._
import java.io._
import collection.JavaConversions._

import Util._
import Core._
import CommonLoopBounds._
import PPL._

case class PPLInterval(val rep: Rational_Box) extends NumericDomain {
  type rep = Rational_Box

  /*
   override def finalize() = {
   //printf("f")
   //rep.free()
   }
   */

  def this() = this(new Rational_Box(0, Degenerate_Element.UNIVERSE))

  def _mapRep(f: rep => rep): PPLInterval = {
    this.copy(rep = f(rep))
  }

  lazy val isEmpty: Boolean = {
    catchInterval(rep, {_.is_empty()})
  }

  lazy val constraints: List[Constraint] = rep.constraints().toList

  def affineImage(v: Variable, e: Linear_Expression): PPLInterval = _mapRep{inp =>
    val p = copyInterval(inp)
    p.affine_image(v, e, new Coefficient(1))
    p
  }

  def addDims(dims: Int): PPLInterval = _mapRep{inp =>
    val currentDims = inp.space_dimension()
    if (dims > 0) {
      val p = copyInterval(inp)
      p.add_space_dimensions_and_embed(dims)
      p
    } else {
      inp
    }
  }

  def copyDimension(d: DimIndex): PPLInterval = _mapRep{inp =>
    val p = copyInterval(inp)
    p.expand_space_dimension(new Variable(d), 1)
    p
  }

  def expandUpTo(maxvid: Core.DimIndex): PPLInterval = _mapRep{inp =>
    val currentDims = inp.space_dimension()
    if (currentDims <= maxvid) {
      val p = copyInterval(inp)
      p.add_space_dimensions_and_embed(maxvid - currentDims + 1)
      p
    } else {
      copyInterval(inp)
    }
  }

  def expandUpToProject(maxvid: Core.DimIndex): PPLInterval = _mapRep{inp =>
    val currentDims = inp.space_dimension()
    if (currentDims <= maxvid) {
      val p = copyInterval(inp)
      p.add_space_dimensions_and_project(maxvid - currentDims + 1)
      p
    } else {
      copyInterval(inp)
    }
  }

  def mapDims(vmap: List[(Core.DimIndex, Core.DimIndex)])
      : PPLInterval = _mapRep{inp =>

    val d = inp.space_dimension()
    val p = copyInterval(inp)

    val pf = new Partial_Function()
    var mapped = Set[DimIndex]()

    vmap.foreach{
      case (i,j) =>
        mapped += i
        pf.insert(i,j)
    }

    (0L until d).foreach{i =>
      if (! mapped(i)) {
        pf.insert(i,i)
      }
    }

    p.map_space_dimensions(pf)
    p
  }

  def mapDimsWithRemoval(vmap: List[(Core.DimIndex, Core.DimIndex)])
      : PPLInterval = _mapRep{inp =>

    val d = inp.space_dimension()
    val p = copyInterval(inp)

    val pf = new Partial_Function()
    var mapped = Set[DimIndex]()

    var lhs = Set[DimIndex]()
    var rhs = Set[DimIndex]()

    vmap.foreach{
      case (i,j) =>
        mapped += i
        pf.insert(i,j)
        lhs = lhs + i
        rhs = rhs + j
    }

    if (Math.abs(lhs.size - rhs.size) > 1 | (0 until rhs.size).toSet != rhs) {
      throw new InterpException(s"dim map problem\n\tLHS = $lhs\n\tRHS = $rhs")
    }

    p.map_space_dimensions(pf)
    p
  }

  def removeDims(dims: List[DimIndex]): PPLInterval = _mapRep{inp =>
    val p = copyInterval(inp)
    val vars = new Variables_Set()
    vars.addAll(dims.map{new Variable(_)})
    p.remove_space_dimensions(vars)
    p
  }

  def projectTo(is: Iterable[Core.DimIndex]): PPLInterval = _mapRep{inp =>
    val p = copyInterval(inp)
    val vars_list = (0L to p.space_dimension() - 1)
      .filter { j => ! is.contains(j) }
      .map { v => new Variable(v) }
    val vars = new Variables_Set()
    vars.addAll(vars_list)
    p.remove_space_dimensions(vars)
    p
  }
  def projectTo(i: Core.DimIndex): PPLInterval = projectTo(List(i))

  def joinDims(vid1: Core.DimIndex, vid2: Core.DimIndex): PPLInterval = _mapRep{inp =>
    val retp = copyInterval(inp)
    val origin = new java.util.TreeSet[Variable]()
    origin.add(new Variable(vid2))
    retp.fold_space_dimensions(origin.asInstanceOf[Variables_Set], new Variable(vid1))
    retp
  }

  def leq(q: NumericDomain): Boolean = {
    q match {
      case interval: PPLInterval =>
        interval.rep.contains(this.rep)
      case _ =>
        throw NotImplemented("leq between two different numeric domains")
    }
    
  }

  lazy val numDims: Long = rep.space_dimension()

  def concat(inq: NumericDomain): PPLInterval = {
    inq match {
      case interval: PPLInterval =>
        _mapRep {inp =>
          val p = copyInterval(inp)
          p.concatenate_assign(interval.rep)
          p
        }
    }
    
  }
  
  def isConstraint(d: DimIndex): Boolean = {
    rep.constrains(new Variable(d))
  }

  def addConstraint(c: Constraint): PPLInterval = _mapRep {inp =>
    val p = copyInterval(inp)
//    println("before: " + p)
//    println(c)
    val left = c.left_hand_side()
    val symbol = c.kind()
    val right = c.right_hand_side()
    p.generalized_affine_image(left, symbol, right)
//    p.add_constraint(c)
    p.intersection_assign(inp)
//    println("after: " + p)
    p
  }

  def join(inq: NumericDomain): PPLInterval = {
//    inq.isInstanceOf[PPLInterval] match
    inq match {
      case interval: PPLInterval =>
        val d1 = this.numDims
        val d2 = interval.numDims
        val maxdim = d1.max(d2) - 1
    
        val p = this.expandUpToProject(maxdim)
        val q = interval.expandUpToProject(maxdim)
    
        p._mapRep{p2 =>
          p2.upper_bound_assign(q.rep)
          p2
        }
      case _ =>
        throw NotImplemented("join two different numeric domains")
    }
  }

  def widen(q: NumericDomain): PPLInterval =  _mapRep{p =>
    // Note the arguments are not symmetric as per 'require' below.
    // Also see http://bugseng.com/products/ppl/documentation/user/ppl-user-1.1-html/index.html#Widening_Operators
    q match {
      case interval: PPLInterval =>
        ifDebug {
          require(interval.rep.contains(p), "p ⊑ q")
        }
        val new_q = copyInterval(interval.rep)
        new_q.CC76_widening_assign(p,new By_Reference(0))
        new_q
      case _ =>
        throw NotImplemented("widen between two different numeric domains")
    }
    
  }

  // Are the two polies disjoint?
  def isDisjoint(q: NumericDomain): Boolean = {
    q match {
      case interval: PPLInterval => this.rep.is_disjoint_from(interval.rep)
      case _ => throw NotImplemented("widen between two different numeric domains")
    }
    
  }

  // Are they disjoint on a particular set of dimensions?
  def isDisjointOn(q: PPLInterval, ds: Set[DimIndex]) =
    this.projectTo(ds).isDisjoint(q.projectTo(ds))
}

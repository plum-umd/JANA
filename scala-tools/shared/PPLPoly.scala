import parma_polyhedra_library._
import java.io._
import collection.JavaConversions._

import Util._
import Core._
import CommonLoopBounds._
import PPL._

case class PPLPoly(val rep: C_Polyhedron) extends NumericDomain {
  type rep = C_Polyhedron

  /*
   override def finalize() = {
   //printf("f")
   //rep.free()
   }
   */

  def this() = this(new C_Polyhedron(0, Degenerate_Element.UNIVERSE))

  def _mapRep(f: rep => rep): PPLPoly = {
    this.copy(rep = f(rep))
  }

  lazy val isEmpty: Boolean = {
    catchPoly(rep, {_.is_empty()})
  }

  lazy val constraints: List[Constraint] = rep.constraints().toList

  def affineImage(v: Variable, e: Linear_Expression): PPLPoly = _mapRep{inp =>
    val p = copyPoly(inp)
    p.affine_image(v, e, new Coefficient(1))
    p
  }

  def addDims(dims: Int): PPLPoly = _mapRep{inp =>
    val currentDims = inp.space_dimension()
    if (dims > 0) {
      val p = copyPoly(inp)
      p.add_space_dimensions_and_embed(dims)
      p
    } else {
      inp
    }
  }

  def copyDimension(d: DimIndex): PPLPoly = _mapRep{inp =>
    val p = copyPoly(inp)
    p.expand_space_dimension(new Variable(d), 1)
    p
  }
  
  def isConstraint(d: DimIndex): Boolean = {
    rep.constrains(new Variable(d))
  }

  def expandUpTo(maxvid: Core.DimIndex): PPLPoly = _mapRep{inp =>
    val currentDims = inp.space_dimension()
    if (currentDims <= maxvid) {
      val p = copyPoly(inp)
      p.add_space_dimensions_and_embed(maxvid - currentDims + 1)
      p
    } else {
      copyPoly(inp)
    }
  }

  def expandUpToProject(maxvid: Core.DimIndex): PPLPoly = _mapRep{inp =>
    val currentDims = inp.space_dimension()
    if (currentDims <= maxvid) {
      val p = copyPoly(inp)
      p.add_space_dimensions_and_project(maxvid - currentDims + 1)
      p
    } else {
      copyPoly(inp)
    }
  }

  def mapDims(vmap: List[(Core.DimIndex, Core.DimIndex)])
      : PPLPoly = _mapRep{inp =>

    val d = inp.space_dimension()
    val p = copyPoly(inp)

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
      : PPLPoly = _mapRep{inp =>

    val d = inp.space_dimension()
    val p = copyPoly(inp)

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

  def removeDims(dims: List[DimIndex]): NumericDomain = _mapRep{inp =>
    val p = copyPoly(inp)
    val vars = new Variables_Set()
    vars.addAll(dims.map{new Variable(_)})
    p.remove_space_dimensions(vars)
    p
  }

  def projectTo(is: Iterable[Core.DimIndex]): PPLPoly = _mapRep{inp =>
    val p = copyPoly(inp)
    val vars_list = (0L to p.space_dimension() - 1)
      .filter { j => ! is.contains(j) }
      .map { v => new Variable(v) }
    val vars = new Variables_Set()
    vars.addAll(vars_list)
    p.remove_space_dimensions(vars)
    p
  }
  def projectTo(i: Core.DimIndex): PPLPoly = projectTo(List(i))

  def joinDims(vid1: Core.DimIndex, vid2: Core.DimIndex): PPLPoly = _mapRep{inp =>
    val retp = copyPoly(inp)
    val origin = new java.util.TreeSet[Variable]()
    origin.add(new Variable(vid2))
    retp.fold_space_dimensions(origin.asInstanceOf[Variables_Set], new Variable(vid1))
    retp
  }

  def leq(q: NumericDomain): Boolean = {
    q match {
      case poly: PPLPoly => poly.rep.contains(this.rep)
      case _ => throw NotImplemented("leq between two different numeric domains")
        
    }
    
  }

  lazy val numDims: Long = rep.space_dimension()

  def concat(inq: NumericDomain): NumericDomain = {
    inq match {
      case poly: PPLPoly =>
        _mapRep {inp =>       
          val p = copyPoly(inp)
          p.concatenate_assign(poly.rep)
          p
        }
      case _ => throw NotImplemented("concat not implemented")
    }
    
}

  def addConstraint(c: Constraint): NumericDomain = _mapRep {inp =>
    val p = copyPoly(inp)
    p.add_constraint(c)
    p
  }

  def join(inq: NumericDomain): PPLPoly = {
    inq match {
      case poly: PPLPoly =>
        val d1 = this.numDims
        val d2 = poly.numDims
        val maxdim = d1.max(d2) - 1
    
        val p = this.expandUpToProject(maxdim)
        val q = poly.expandUpToProject(maxdim)
    
        p._mapRep{p2 =>
          p2.poly_hull_assign(q.rep)
          p2
        }
      case _ => throw NotImplemented("join between tow different numeric domains")
    }
    
  }

  def widen(q: NumericDomain): PPLPoly =  _mapRep{p =>
    // Note the arguments are not symmetric as per 'require' below.
    // Also see http://bugseng.com/products/ppl/documentation/user/ppl-user-1.1-html/index.html#Widening_Operators
    q match {
      case poly: PPLPoly =>
        ifDebug {
          require(poly.rep.contains(p), "p ⊑ q")
        }
        val new_q = copyPoly(poly.rep)
        new_q.BHRZ03_widening_assign(p,new By_Reference(0))
        new_q
      case _ => throw NotImplemented("widen between tow different numeric domains")
    }
    
  }

  // Are the two polies disjoint?
  def isDisjoint(q: NumericDomain): Boolean = {
    q match {
      case poly: PPLPoly =>
        this.rep.is_disjoint_from(poly.rep)
      case _ => throw NotImplemented("isDisjoint between tow different numeric domains")
    }
  }

  // Are they disjoint on a particular set of dimensions?
  def isDisjointOn(q: PPLPoly, ds: Set[DimIndex]) =
    this.projectTo(ds).isDisjoint(q.projectTo(ds))
}

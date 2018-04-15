import parma_polyhedra_library._
import collection.JavaConversions._

import Util._
import Core._
import CommonLoopBounds._
import java.io._

//abstract class Region[R <: Region[R]] { self: R =>
//  type rep
//
//  val isEmpty: Boolean
//  def mapDimsWithRemoval(vmap: List[(DimIndex, DimIndex)]): R
//  def mapDims(vmap: List[(DimIndex, DimIndex)]): R
//  def joinDims(vid1: DimIndex, vid2: DimIndex): R
//  def addDims(dims: Int): R
//  def expandUpTo(maxvid: DimIndex): R
//  def expandUpToProject(maxvid: DimIndex): R
//  def widen(q: R): R
//  def join(q: R): R
//  def leq(q: R): Boolean
//  def projectTo(i: DimIndex): R
//  def projectTo(is: Iterable[DimIndex]): R
//}

object PPL {
  def init = {
    Util.findAndLoadNativeLib("ppl_java")
    Parma_Polyhedra_Library.initialize_library()
  }

  type Coeff = BigInt /* coefficients in linear expressions */

  case class NonLinear(s: String) extends Exception(s)
  case class Disjunctive(s: String) extends Exception(s)

  def PPLConstraintSymbolOfNumericRelation(op: Operator.NumericRelation): Relation_Symbol = op match {
    case Operator.NumericRelation.== => Relation_Symbol.EQUAL
    case Operator.NumericRelation.≠  => throw Disjunctive("not equal")
    case Operator.NumericRelation.<  => Relation_Symbol.LESS_THAN
    case Operator.NumericRelation.≤  => Relation_Symbol.LESS_OR_EQUAL
    case Operator.NumericRelation.>  => Relation_Symbol.GREATER_THAN
    case Operator.NumericRelation.≥  => Relation_Symbol.GREATER_OR_EQUAL
  }

  def PPLConstraintOfNumericRelation(op: Operator.NumericRelation): (Linear_Expression => Linear_Expression => Constraint) = {
    a => b => new Constraint(a, PPLConstraintSymbolOfNumericRelation(op), b)
  }

  def PPLExpressionOfNumericBinop(op: Operator.NumericBinop)
      : (Linear_Expression => Linear_Expression => Linear_Expression) = op match {
    case Operator.NumericBinop.+ => {a => b => new Linear_Expression_Sum(a,b)}
    case Operator.NumericBinop.- => {a => b => new Linear_Expression_Difference(a,b)}
    case Operator.NumericBinop.* => throw NonLinear("multiplication")
    case Operator.NumericBinop./ => throw NonLinear("division")
    case Operator.NumericBinop.% => throw NonLinear("modulus")
  }

  def PPLExpressionOfNumericUnop(op: Operator.NumericUnop)
      : (Linear_Expression => Linear_Expression) = op match {
    case Operator.NumericUnop.- => {a => new Linear_Expression_Unary_Minus(a)}
  }

  implicit class RelationSymbolClass(val r: Relation_Symbol) {
    import Relation_Symbol._

    def implicitToString = r match {
      case GREATER_THAN     => ">"
      case GREATER_OR_EQUAL => "≥"
      case LESS_OR_EQUAL    => "≤"
      case LESS_THAN        => "<"
      case EQUAL            => "="
      case NOT_EQUAL        => "≠"
    }
    def neg = r match {
      case GREATER_THAN     => Relation_Symbol.LESS_OR_EQUAL
      case GREATER_OR_EQUAL => Relation_Symbol.LESS_THAN
      case LESS_OR_EQUAL    => Relation_Symbol.GREATER_THAN
      case LESS_THAN        => Relation_Symbol.GREATER_OR_EQUAL
      case EQUAL            => Relation_Symbol.NOT_EQUAL
      case NOT_EQUAL        => Relation_Symbol.EQUAL
    }
  }

  implicit class ConstraintClass(val c: Constraint) {
    import Relation_Symbol._

    def neg: Constraint =
      new Constraint(c.left_hand_side(), c.kind().neg, c.right_hand_side())

    def negIntegral: List[Constraint] =
      (new Constraint(c.left_hand_side(), c.kind().neg, c.right_hand_side()))
        .integral

    def integral: List[Constraint] = {
      val a = c.left_hand_side()
      val ap1 = a.sum(LExp.LConst(1))
      val am1 = a.sum(LExp.LConst(-1))
      val b = c.right_hand_side()
      val bp1 = b.sum(LExp.LConst(1))
      val bm1 = b.sum(LExp.LConst(-1))
      c.kind() match {
        case GREATER_THAN => List(new Constraint(a, GREATER_OR_EQUAL, bp1))
        case LESS_THAN    => List(new Constraint(ap1, LESS_OR_EQUAL, b))
        case NOT_EQUAL    => List(
          new Constraint(a, LESS_OR_EQUAL, bm1),
          new Constraint(am1, GREATER_OR_EQUAL, b)
        )
        case _ => List(c)
      }
    }
  }

  trait LExp extends Linear_Expression {
    def ==(b: Linear_Expression): Constraint = new Constraint(this, Relation_Symbol.EQUAL, b)
    def <=(b: Linear_Expression): Constraint = new Constraint(this, Relation_Symbol.LESS_OR_EQUAL, b)
    def >=(b: Linear_Expression): Constraint = new Constraint(this, Relation_Symbol.GREATER_OR_EQUAL, b)
    def +(b: Linear_Expression): Linear_Expression = this.sum(b)
    def -(b: Linear_Expression): Linear_Expression = this.subtract(b)
    def *(b: Linear_Expression_Coefficient): Linear_Expression = this.times(b.argument())
  }

  implicit class Linear_Expression_Class(val le: Linear_Expression) {
    def ==(b: Linear_Expression): Constraint = new Constraint(le, Relation_Symbol.EQUAL, b)
    def <=(b: Linear_Expression): Constraint = new Constraint(le, Relation_Symbol.LESS_OR_EQUAL, b)
    def >=(b: Linear_Expression): Constraint = new Constraint(le, Relation_Symbol.GREATER_OR_EQUAL, b)
    def + (b: Linear_Expression): Linear_Expression = le.sum(b)
    def - (b: Linear_Expression): Linear_Expression = le.subtract(b)
    def * (b: Linear_Expression_Coefficient): Linear_Expression = le.times(b.argument())
  }

  object LExp {
    case class LConst(v: Long)     extends Linear_Expression_Coefficient(new Coefficient(v)) with LExp
    case class LVar  (i: DimIndex) extends Linear_Expression_Variable   (new Variable   (i)) with LExp

    def flatten(dims: Int, le: Linear_Expression): LinearArray = {
      val rec = flatten(dims,_:Linear_Expression)
      le match {
        case e: Linear_Expression_Difference  => rec(e.left_hand_side()) - rec(e.right_hand_side())
        case e: Linear_Expression_Sum         => rec(e.left_hand_side()) + rec(e.right_hand_side())
        case e: Linear_Expression_Unary_Minus => rec(e.argument).neg
        case e: Linear_Expression_Times       => rec(e.linear_expression()) * e.coefficient().getBigInteger()
        case e: Linear_Expression_Variable    => new LinearArray(dims, e.argument().id(), 1)
        case e: Linear_Expression_Coefficient => new LinearArray(dims, e.argument().getBigInteger())
      }
    }
  }

  def edgeSpecOfPoly(vid: DimIndex, p: PPL_Object): LoopEdgeSpec = {
    p match {
      case polyhedron: Polyhedron =>
        val dims = polyhedron.space_dimension().toInt
        val cons = polyhedron.minimized_constraints().toList
        val (les, others) = cons.map{c : Constraint =>
          val le = c.left_hand_side()
          val re = c.right_hand_side()
          val diff = LExp.flatten(dims, re) - LExp.flatten(dims, le)
          val vidcoeff : Coeff = -1 * diff.linearCoefficients(vid.toInt)
          val ret = diff + (new LinearArray(dims, vid, vidcoeff))
          if (vidcoeff > 0) {
            (vidcoeff, c.kind(), ret)
          } else {
            if (c.kind() != Relation_Symbol.EQUAL && c.kind() != Relation_Symbol.NOT_EQUAL) {
                (vidcoeff * -1, c.kind().neg, ret.neg)
            } else {
                (vidcoeff * -1, c.kind(), ret.neg)
            }
          }
        }.partition{case (v, _, _) => v != 0}
    
        LoopEdgeSpec(
          loopvar = vid.toInt,
          loopvarRelations = les.map{case (x,y,z) => SolvedLinearConstraint(x,vid.toInt,y,z)},
          otherRelations = others.map{case (_, rel, ret) => LinearConstraint(rel,ret)}
        )
      case _ => throw NotImplemented("edgeSpecOfPoly not implemented")
    }
    
  }

  def transSpecOfPoly(vid_old: DimIndex, vid_new: DimIndex, p: PPL_Object) : LoopTransSpec = {
    p match {
      case polyhedron: Polyhedron =>
        edgeSpecOfPoly(vid_new, polyhedron) match {
          case LoopEdgeSpec (_, l, others) =>
            LoopTransSpec(
              loopvarOld = vid_old.toInt,
              loopvarNew = vid_new.toInt,
              loopvarRelations = l,
              otherRelations = others)
        }
      case _ => throw NotImplemented("transSpecOfPoly not implemented")
    }
    
  }
  
  def copyPoly(inp: PPL_Object): PPL_Object = {
    inp match {
      case polyhedron: C_Polyhedron =>
        catchPoly(polyhedron, {new C_Polyhedron(_)})  
      case _ => throw NotImplemented("copyPoly not implemented")
    }
      
  }

  def copyPoly(inp: C_Polyhedron): C_Polyhedron = {
        catchPoly(inp, {new C_Polyhedron(_)})    
  }

  def catchPoly[A](inp: C_Polyhedron, f: C_Polyhedron => A): A = {
    try {
      f(inp)
    } catch {
      case e: Exception =>
        println("===catchPoly===")
        println(inp)
        println("===============")
        println(inp.ascii_dump())
        println("===============")
        throw e
    }
    
  }
  
  def copyInterval(inp: Rational_Box): Rational_Box = {
    catchInterval(inp, {new Rational_Box(_)})
  }
  
  def catchInterval[T](inp: Rational_Box, f: Rational_Box => T): T = {
    try {
      f(inp)
    } catch {
      case e: Exception =>
        println("===catchInterval===")
        println(inp)
        println("===============")
        throw e
    }
  }

  def isEmpty(inp: PPL_Object): Boolean = {
    inp match {
      case polyhedron: C_Polyhedron =>
        catchPoly(polyhedron, {_.is_empty()})
      case _ => throw NotImplemented("isEmpty not implemented")
    }
    
  }

  /* Given a polyhedron on a map (described by a list of tuples of
   * varindices), renames the dimensions as specified by the map. Any
   * dimension not mapped is removed.
   */
  def mapDimsWithRemoval(inp: C_Polyhedron, vmap: List[(DimIndex, DimIndex)]): C_Polyhedron = {

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

    /*ifDebug{
      println("MAP DIMS")
      println(s"inp = $inp")
      println(s"vmap = $vmap")
      println(s"lhs = $lhs")
      println(s"rhs = $rhs")
    }*/

    if (Math.abs(lhs.size - rhs.size) > 1 | (0 until rhs.size).toSet != rhs) {
      throw new InterpException(s"dim map problem\n\tLHS = $lhs\n\tRHS = $rhs")
    }

    p.map_space_dimensions(pf)
    p
  }

  /* Given a polyhedron on a map (described by a list of tuples of
   * varindices), renames the dimensions as specified by the map.
   */
  def mapDims(inp: C_Polyhedron, vmap: List[(DimIndex,DimIndex)]): C_Polyhedron = {
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

  /* Given a polyhedron and two dimensions, creates a new dimension that
   * includes all values that the two given dimensions can take.
   */
  def joinDims(inp: C_Polyhedron, vid1: DimIndex, vid2: DimIndex): C_Polyhedron = {
    val retp = copyPoly(inp)
    val origin = new java.util.TreeSet[Variable]()
    origin.add(new Variable(vid2))
    retp.fold_space_dimensions(origin.asInstanceOf[Variables_Set], new Variable(vid1))
    retp
  }

  def addDims(inp: C_Polyhedron, dims: Int): C_Polyhedron = {
    val currentDims = inp.space_dimension()
    if (dims > 0) {
      val p = copyPoly(inp)
      p.add_space_dimensions_and_embed(dims)
      p
    } else {
      inp
    }
  }

  def expandUpTo(inp: C_Polyhedron, maxvid: DimIndex): C_Polyhedron = {
    val currentDims = inp.space_dimension()
    if (currentDims <= maxvid) {
      val p = copyPoly(inp)
      p.add_space_dimensions_and_embed(maxvid - currentDims + 1)
      p
    } else {
      inp
    }
  }

  def expandUpToProject(inp: C_Polyhedron, maxvid: DimIndex) : C_Polyhedron = {
    val currentDims = inp.space_dimension()
    if (currentDims <= maxvid) {
      val p = copyPoly(inp)
      p.add_space_dimensions_and_project(maxvid - currentDims + 1)
      p
    } else {
      inp
    }
  }

  def widen(p: C_Polyhedron, q: C_Polyhedron): C_Polyhedron = {
    // Note the arguments are not symmetric as per 'require' below.
    // Also see http://bugseng.com/products/ppl/documentation/user/ppl-user-1.1-html/index.html#Widening_Operators

    //ifDebug{
    require(q.contains(p), "p ⊑ q")
    //    }
    val new_q = copyPoly(q)
    new_q.BHRZ03_widening_assign(p,new By_Reference(0))
    new_q
  }

  def widen(
    p: Iterable[PPL_Object],
    q: Iterable[PPL_Object]): List[PPL_Object] = {
    disjunctsOfPPS(widen(PPSOfDisjuncts(p), PPSOfDisjuncts(q)))
  }

  def widen(
    p: PPL_Object,
    q: PPL_Object): PPL_Object = {
    (p, q) match {
      case (poly1: Pointset_Powerset_C_Polyhedron, poly2: Pointset_Powerset_C_Polyhedron) =>
        require(poly2.contains(poly1), "poly1 ⊑ poly2")
    
        val new_q = new Pointset_Powerset_C_Polyhedron(poly2)
        ConfigManager.widen_alg match {
          case None | Some("BHZ03_H79_H79") =>
            new_q.BHZ03_H79_H79_widening_assign(poly1)
          case Some("BHZ03_BHRZ03_BHRZ03") =>
            new_q.BHZ03_BHRZ03_BHRZ03_widening_assign(poly1)
          case _ => throw NotImplemented("widen algorithm not supported")
        }
//        new_q.BHZ03_BHRZ03_BHRZ03_widening_assign(poly1)
        
        reduce(new_q)
      case (box1: Rational_Box, box2: Rational_Box) =>
        require(box2.contains(box1), "box1 ⊑ box2")
    
        val new_b = copyInterval(box2)
        new_b.CC76_widening_assign(box1, new By_Reference(0))
        new_b
      case (_, _) => throw NotImplemented("widen not implemented")
    }
    
  }

  def reduce(p: Pointset_Powerset_C_Polyhedron): Pointset_Powerset_C_Polyhedron = {
    val new_p = new Pointset_Powerset_C_Polyhedron(p)
    new_p.omega_reduce()
    new_p.pairwise_reduce()
    new_p
  }

  def PPSOfDisjuncts(ps: Iterable[PPL_Object]): PPL_Object = {
    ps.head match {
      case polyhedron: C_Polyhedron =>
        val ret = new Pointset_Powerset_C_Polyhedron(polyhedron)
        ps.tail.foldLeft(ret) {
          case (pp_poly, poly: C_Polyhedron) => 
            pp_poly.add_disjunct(poly)
            pp_poly
          case (_, _) => throw NotImplemented("PPSOfDisjuncts not implemented")
        }
      case rational_box: Rational_Box =>
        ps.tail.foldLeft(rational_box) {
          case (pp_box, box: Rational_Box) => 
            box.upper_bound_assign(pp_box)
            box
          case (_, _) => throw NotImplemented("PPSOfDisjuncts not implemented")
        }
      case _ => 
        println(ps.head)
        throw NotImplemented("PPSOfDisjuncts not implemented")
    }
    
  }

  def disjunctsOfPPS(pps: PPL_Object): List[PPL_Object] = {
    pps match {
      case pp_polyhedron: Pointset_Powerset_C_Polyhedron =>
        var ret = List[C_Polyhedron]()
        var p = pp_polyhedron.begin_iterator()
        while (! p.equals(pp_polyhedron.end_iterator())) {
          ret = ret ++ List(new C_Polyhedron(p.get_disjunct()))
          p.next()
        }
        ret
      case rational_box: Rational_Box =>
        List[Rational_Box](rational_box)
      case _ => throw NotImplemented("disjunctsOfPPS not implemented")
    }
    
  }

  def polyHull(in_p1: C_Polyhedron, in_p2: C_Polyhedron): C_Polyhedron = {
    val d1 = in_p1.space_dimension()
    val d2 = in_p2.space_dimension()
    val maxvid = d1.max(d2) - 1
    val p1 = expandUpToProject(in_p1, maxvid)
    val p2 = expandUpToProject(in_p2, maxvid)
    val p = copyPoly(p1)
    p.poly_hull_assign(p2)
    p
  }

  def leq(p1: PPL_Object, p2: PPL_Object): Boolean = {
    (p1, p2) match {
      case (poly1: C_Polyhedron, poly2: C_Polyhedron) =>
        poly2.contains(poly1)
      case (poly1: Pointset_Powerset_C_Polyhedron, poly2: Pointset_Powerset_C_Polyhedron) =>
        poly2.contains(poly1)
     case (box1: Rational_Box, box2: Rational_Box) =>
        box2.contains(box1)
      case (_, _) => throw NotImplemented("leq not implemented")
    }
    
  }

  def project_to_variable(inp: C_Polyhedron, i: DimIndex) : C_Polyhedron = project_to_variables(inp, List(i))
  def project_to_variables(inp: C_Polyhedron, is: List[DimIndex]) : C_Polyhedron = {
    val p = copyPoly(inp)
    val vars_list = ((0L to (p.space_dimension() - 1)).filter { j => ! is.contains(j) }).map { v => new Variable(v) }
    val vars = new Variables_Set()
      vars.addAll(vars_list)
    p.remove_space_dimensions(vars)
    p
  }

  case class LinearArray (const: Coeff, lins: scala.Array[Coeff]) {
    /* This is a flattened version of ppl's Linear_Expression, composed of
     * the constant coefficient followed by an array of coefficients of
     * the various variables a linear_expression mentions.
     */

    // ConstantCoefficient + Var0 * Coefficient0 + Var1 * Coefficient1 + ...

    val size : Int = lins.length
    val constantCoefficient : Coeff = const
    val linearCoefficients : scala.Array[Coeff] = lins

    def this(a: LinearArray) = this(a.constantCoefficient, a.linearCoefficients.clone)

    def this(initsize: Int) = this(0, scala.Array.fill[Coeff](initsize)(0))

    def this(initsize: Int, vid: DimIndex, c: Coeff) = {
      this(0, scala.Array.fill[Coeff](initsize)(0))
      this.linearCoefficients(vid.toInt) = c
    }

    def this(initsize: Int, c: Coeff) = {
      this(c, scala.Array.fill[Coeff](initsize)(0))
    }

    override def clone(): LinearArray = new LinearArray(this)

    override def toString: String = {
      val nonzeros = linearCoefficients.zipWithIndex.filter{case (c,i) => c != 0}.map{case (c,i) => (c, i.toLong)}

      val coeff_string = if (this.constantCoefficient != 0) {
        this.constantCoefficient.toString
      } else { "" }

      val lins_string = if (nonzeros.length == 0) { "0" } else {
        nonzeros.map{p => p.implicitToString}.mkString("")
      }

      coeff_string + lins_string
    }

    def +(b: LinearArray): LinearArray = combine(_ + _, b)
    def -(b: LinearArray): LinearArray = combine(_ - _, b)
    def *(b: Coeff): LinearArray = {
      return new LinearArray(
        this.constantCoefficient * b,
        this.linearCoefficients.map{a => a*b}
      )
    }
    def neg: LinearArray = this * (-1)

    def combine(op: (Coeff, Coeff) => Coeff, b: LinearArray): LinearArray = {
      if (this.size != b.size) {
        throw new IllegalArgumentException
      }
      return new LinearArray(
        this.constantCoefficient + b.constantCoefficient,
        this.linearCoefficients.zip(b.linearCoefficients).map{case (a,b) => op(a,b)}
      )
    }
  }
}

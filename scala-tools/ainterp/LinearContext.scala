import parma_polyhedra_library._
import collection.JavaConversions._

import Util._
//import PPL._
//import PPL.LExp._
import Core._
import IBinding._
import Util._
//import elina._
import PPL._

import Label._

import scalaz._
import Scalaz._

import apron._

object LinearContext {
  def addConstraint(c: Tcons1): State[LinearContext,Unit] = {
    for {
      lc <- get
      _ <- put(lc.addConstraint(c))
    } yield Unit
  }
  
  def joinAll(lcs: Iterable[LinearContext]) : LinearContext = {
    lcs.tail.foldLeft(lcs.head) {
      case (lc, next) =>
        lc.join(next)
    }
  }
}

case class LinearContext(
  val domain: Manager,
  val abstract1: Abstract1
) extends IState[LinearContext]
    with ILattice[LinearContext]
{

  def this(d: Manager) = this(
    d,
    new Abstract1(d, new Environment())
  )
  

def printAbstract0() = {
    val abs = new Abstract0(domain, 10,0,false);
//    abs.fprint(domain);
}

  /* 
   * Pretty printing
   */
  override def toString = {
    try{
      val cons = abstract1.toLincons(domain)
      "LinearContext (" + abstract1.getSize(domain) + " dim(s)) with (" + cons.size + " conjunct(s)):" +
      (if (cons.length > 0) {
        "\n" + tab("   " + cons.mkString("\n ∧ "))
      } else {
        " no constraints"
      }
      )
    } catch {
	    case _: apron.OverflowException => "overflow"
}
  }
  def toStringWithLocals(implicit addr: WALA.IAddr) = {
    try{
      val cons = abstract1.toLincons(domain)
      "LinearContext (" + abstract1.getSize(domain) + " dim(s)) with (" + cons.size + " conjunct(s)):" +
      (if (cons.length > 0) {
        "\n" + tab("   " + cons.mkString("\n ∧ "))
      } else {
        " no constraints"
      }) 
    } catch {
	case _: apron.OverflowException => "overflow"
}
  }
  def simpleCoefficientToString(pre: String, c: Coefficient): String = {
    c.getBigInteger().intValue() match {
      case 1    => ""
      case (-1) => "-"
      case x if x > 0 => x + pre
      case x if x < 0 => x + pre
     }
  }
  
  def lexpToString(le: Texpr1Node): String = {
    le match {
      case d: Texpr1BinNode => lexpToString(d.getLeftArgument) + " " + d.getOperation + " " + lexpToString(d.getRightArgument)
      case d: Texpr1CstNode  => d.getConstant.toString()
      case d: Texpr1UnNode   => d.getOperation + " " + lexpToString(d.getArgument)
      case d: Texpr1VarNode => d.getVariable.toString()
    }
  }
  
  def lexpToStringWithLocals(le: Texpr1Node): String = {
    le match {
      case d: Texpr1BinNode => lexpToString(d.getLeftArgument) + " " + d.getOperation + " " + lexpToString(d.getRightArgument)
      case d: Texpr1CstNode  => d.getConstant.toString()
      case d: Texpr1UnNode   => d.getOperation + " " + lexpToString(d.getArgument)
      case d: Texpr1VarNode => d.getVariable.toString()
    }
  }
  
  def constraintToString(c: Tcons1): String = {
    c.toString()
  }
  def constraintToStringWithLocals(c: Tcons1)(implicit addr: WALA.IAddr): String = {
    c.toString()
//    lexpToStringWithLocals(c.left_hand_side()) + "\t" + c.kind().implicitToString + " " + lexpToStringWithLocals(c.right_hand_side())
  }
 
  /*
   * ILattice interface 
   */
  def isTop      = abstract1.isTop(domain)
  def isBottom() : Boolean = {
    try{
      abstract1.isBottom(domain)
    } catch {
      case _: apron.OverflowException => true
    }
  }


  def leq(c: LinearContext): Boolean = {
    val (c1, c2) = this.align(c)
    try{
      c1.abstract1.isIncluded(domain, c2.abstract1)
    } catch {
	    case exp: apron.OverflowException => true	  
	    case _: Throwable =>
	      println("c1")
	      c1.abstract1.getEnvironment.getVars.foreach { x => println(x)}
	      println("c2")
	      c2.abstract1.getEnvironment.getVars.foreach { x => println(x)}
	      ???
    }
  }
  
  def join(c: LinearContext): LinearContext = {
    val (c1, c2) = this.align(c)
    try{
        this.copy(abstract1 = c1.abstract1.joinCopy(domain, c2.abstract1))
    } catch{
	    case _: apron.OverflowException => 
	      this.copy(abstract1 = new Abstract1(domain, c1.abstract1.getEnvironment))
	    case _: Throwable =>
	      println("c1")
	      c1.abstract1.getEnvironment.getVars.foreach { x => println(x)}
	      println("c2")
	      c2.abstract1.getEnvironment.getVars.foreach { x => println(x)}
	      ???
    }
  }
  
  def widen(c: LinearContext): LinearContext = {
    val (c1, c2) = this.align(c)
    try{
        this.copy(abstract1 = c1.abstract1.widening(domain, c2.abstract1))
    } catch {
      case _: apron.OverflowException => 
        val new_abstract1 = new Abstract1(domain, c1.abstract1.getEnvironment)
        this.copy(abstract1 = new_abstract1)
    }
  }

  def isDisjointOn(c: LinearContext, labels: Set[Label]): Boolean = {
//    this.onLabels(labels).poly.isDisjoint(c.onLabels(labels).poly)
    ???
  }

  def isDisjoint(c: LinearContext): Boolean = {
//    this.poly.isDisjoint(c.poly)
    ???
  }

  /* 
   *  IBinding interface, and related
   */
  def bind(k: Label, v: Term.Closed): LinearContext = {
    if (isBound(k)) {
     embedNumeric(v) match {
       case Some(exp) =>
         var k_node = new Texpr1VarNode(k.toString());
         var e = new Texpr1BinNode(Texpr1BinNode.OP_SUB, k_node, exp);
         var c = new Tcons1(abstract1.getEnvironment, Tcons1.EQ, e)
         val expr = new Texpr1Intern(abstract1.getEnvironment, exp)
         try{
	         this.copy(abstract1 = abstract1.assignCopy(domain, k.toString(), expr, null))
         } catch {
		       case _: apron.OverflowException => this._alloc(k)
	       }
       case None =>         
         this
     }            
    } else {
      embedNumeric(v) match {        
        case Some(exp) =>
          var k_node = new Texpr1VarNode(k.toString());
          var e = new Texpr1BinNode(Texpr1BinNode.OP_SUB, k_node, exp);
          val env = abstract1.getEnvironment.add(List(k.toString()).toArray, List[String]().toArray)
          var c = new Tcons1(env, Tcons1.EQ, e)
          abstract1.changeEnvironment(domain, env, false)
          val expr = new Texpr1Intern(env, exp)
	        try{
	          this.copy(abstract1 = abstract1.assignCopy(domain, k.toString(), expr, null))
	        } catch {
	        	case _: apron.OverflowException => this._alloc(k)
	        }
        case _ =>           
          this
      }
    }
  }
  
  def unbind(k: Label): LinearContext = {
    try{    
      this.copy(abstract1 = this.abstract1.forgetCopy(domain, k.toString(),false))
    } catch {
      case ofe: apron.OverflowException =>
        this
      case iae:java.lang.IllegalArgumentException =>
        this
    }    
  }
  
  def unbind(l: List[Label]): LinearContext = {
    try{    
    this.copy(abstract1 = this.abstract1.forgetCopy(domain, l.map { x => x.toString() }.toArray,false))
    } catch {
      case _: apron.OverflowException =>
        this
    }
  }
  
  def apply(k: Label): Option[Term.Closed] =
    try {
      Some(this.get(k))
    } catch {
      case _: apron.OverflowException => None
    }
    
  def get(k: Label): Term.Closed = {
    Term.Linear(k)
  }


  def isBound(k: Label) = abstract1.getEnvironment.hasVar(k.toString())

  def alloc(k: Label): LinearContext = {    
      _alloc(k)
  }
  
//  def alloc(v: Var): LinearContext = {
//      _alloc(v)
//  }
  
  def alloc(k: List[Label])(implicit init: Boolean = false): LinearContext = {
      _alloc(k)(init)
  }
  
  def alloc(k: Set[Var]): LinearContext = {
      _alloc(k)
  }
  
  def allocAndInit(k: Label): LinearContext = {
    bind(k, Term.Constant(CIV(0)))
  }
  
  def bindWeak(k: Label, tmp: Label, v: Term.Closed): LinearContext = {
    val lc1 = this.bindEval(tmp, Expression.Term(v))
    val array = List(k.toString(), tmp.toString()).toArray
    lc1.copy(abstract1 = lc1.abstract1.foldCopy(domain, array))      
  }
  
  def numConstraints = 0//abstract1.toLincons(domain).length

  // Allocate a new unconstrained dimension for the given label.
  // Return the updated context and the new dimension number.
  def _alloc(k: Label): LinearContext = {
    if (abstract1.getEnvironment.hasVar(k.toString())) {
      try{    
        this.copy(abstract1 = this.abstract1.forgetCopy(domain, k.toString(),false))
      } catch {
        case _: apron.OverflowException =>
          this
      }      
    } else {
      val array = List(k.toString()).toArray
      val env = abstract1.getEnvironment.add(array, List[String]().toArray)
      this.copy(abstract1 = abstract1.changeEnvironmentCopy(domain, env, false))
    }
  }
  
  def _alloc(k: List[Label])(implicit init: Boolean = false): LinearContext = {
      val env = abstract1.getEnvironment.add(k.map{x => x.toString}.toArray, List[String]().toArray)
      this.copy(abstract1 = abstract1.changeEnvironmentCopy(domain, env, init))
  }
  
  def _alloc(k: Set[Var]): LinearContext = {
      val env = abstract1.getEnvironment.add(k.toArray, List[Var]().toArray)
      this.copy(abstract1 = abstract1.changeEnvironmentCopy(domain, env, false))
  }
  
  def getConstraintedLs(f: String => Boolean, binding: Map[Label, Term.Closed]): Set[Label] = {
    val constraintedVars = abstract1.getEnvironment.getVars.filter { v => !abstract1.isDimensionUnconstrained(domain, v) }.map { x => x.toString() }
    binding.filter { case (key, _) => constraintedVars.contains(key.toString()) && f(key.toString())}.map(_._1).toSet
  }
  
  def constraints(label: Label): Boolean = {
    isBound(label) match {
      case true =>
        try{
          !abstract1.isDimensionUnconstrained(domain, label.toString())
        } catch {
	        case _: apron.OverflowException => false
	      }
      case false => false
    }    
  }

  def embedNumeric(v: Term.Closed): Option[Texpr1Node] = {
    v match {
      case Term.Constant(CIV(x)) => 
        val expr = new Texpr1CstNode(new MpqScalar(x))
        Some(expr)
      case Term.Linear(l) => 
        val expr = new Texpr1VarNode(l.toString());
        Some(expr)
      case Term.Null() => None
      case _ => 
        println(v)
        ???
    }
  }

  /*
   Make sure bindings with the same name map to the same dimensions in
   each context. This should be called only after
   AbstractMachine.align which makes sure context at least contain the
   same set of dimension names.
   */
  def align(c2: LinearContext): (LinearContext, LinearContext) = {
      (this, c2)
  }
  
  def alignX(c2: LinearContext): (LinearContext, LinearContext) = {
      val vars1 = this.abstract1.getEnvironment.getVars
      val vars2 = c2.abstract1.getEnvironment.getVars
      // Align the second context's dimension map to the first.
      val c1 = this

      var flag = false
      if (vars1.toSet != vars2.toSet) {
        
        val (vs1, vs2) =
          (vars2.filter { v => !vars1.contains(v) }.foldLeft(List[Var]()) {
            case (l, variable) =>
              l :+ variable
          },
          vars1.filter { v => !vars2.contains(v) }.foldLeft(List[Var]()) {
            case (l, variable) =>
              l :+ variable
          }
          )
        (c1.alloc(vs1.toSet), c2.alloc(vs2.toSet))
      } else {
        (c1, c2)
      }
  }
  
  def alignConcat(c2: LinearContext): (LinearContext, LinearContext) = {
    val vars1 = this.abstract1.getEnvironment.getVars.toSet
    val vars2 = c2.abstract1.getEnvironment.getVars.toSet
    // Align the second context's dimension map to the first.
    val c1 = this

    if (vars1 != vars2) {
      if(ConfigManager.isTopDown && !ConfigManager.isHybrid)
        (c1.alloc(vars2.diff(vars1)), c2)
      else //concatenation in the bottom-up analysis requires alignment from both contexts
        (c1.alloc(vars2.diff(vars1)), c2.alloc(vars1.diff(vars2)))
    } else {
      (c1, c2)
    }
    
  }

  // Given a label and a constant value, promote the value to a linear
  // one achieving only the given constant value.
  def promoteConstant(l: Label, c: Term.Constant): LinearContext = {
    bind(l, c)
  }

  def filterDims(f: Label => Boolean, binding: Map[Label, _]): LinearContext = {
    val to_remove = binding.filter{case (l, _) => !f(l)}.map(_._1).map { x => x.toString }.toList
//    val exist = binding.map(_._1).map { x => x.toString }.toList
//    val current = this.abstract1.getEnvironment.getVars.map { x => x.toString() }.toList 
//    val diff = current.diff(exist)
//    println("diff: " + diff)
    removeDims(to_remove)
  }
  
  def keepLabels(l: List[Label]): LinearContext = {
//    val to_remove = binding.filter{case (l, _) => !f(l)}.map(_._1).map { x => x.toString }.toList
//    val exist = binding.map(_._1).map { x => x.toString }.toList
    val current = this.abstract1.getEnvironment.getVars.map { x => x.toString() }.toList 
    val diff = current.diff(l.map { x => x.toString() })
//    println("diff: " + diff)
    removeDims(diff)
  }
  
  def filterDims(l: List[Label], f: Label => Boolean, binding: Map[Label, _]): LinearContext = {
    val to_remove =binding.filter{case (l, _) => !f(l)}.map(_._1).map { x => x.toString }.toList ++l.map { x => x.toString() }
    removeDims(to_remove)
  }

//  def filterUnconstraintDims(f: String => Boolean): (LinearContext, List[String]) = {
//    val to_remove = abstract1.getEnvironment.getVars.map{x=>x.toString}.filter { x =>
//      !f(x)}.toList
//   val remove = to_remove.foldLeft(List[String]()){
//        case (l, s) =>
//	  if(abstract1.isDimensionUnconstrained(domain, s))
//              s :: l
//	  else l
//    }
//    (removeDims(remove), remove)
//  }

  def removeDims(list: List[String]): LinearContext = {
    // Given a list of labels, remove all the dimensions named by the
    // elements in the list.

    val l = list.toArray//.map { x => x.toString() }
    
    val env = abstract1.getEnvironment.remove(l)
    try{ 
      this.copy(abstract1 = abstract1.changeEnvironmentCopy(domain, env, true))
    } catch {
      case _: apron.OverflowException =>
        val new_abstract1 = new Abstract1(domain, env)
        this.copy(abstract1 = new_abstract1)
    }
  }
  
  def removeDims(s: String): LinearContext = {   
    val l = List(s).toArray
    val env = abstract1.getEnvironment.remove(l)
    try{
      this.copy(abstract1 = abstract1.changeEnvironmentCopy(domain, env, true))
	  }catch {
	    case _: apron.OverflowException => 
	    	val new_abstract1 = new Abstract1(domain, env)
	    	this.copy(abstract1 = new_abstract1)
    }
  }

  def concat(c2: LinearContext): LinearContext = {
    var (c_1, c_2) = this.alignConcat(c2)
  	try{
  	  this.copy(abstract1 = c_1.abstract1.meetCopy(domain, c_2.abstract1))
  	}catch {
	    case _: apron.OverflowException => c_1
    }  
  }

  def addConstraint(c: Tcons1): LinearContext = {
   try {
    this.copy(abstract1 = abstract1.meetCopy(domain, c))
   } catch {
     case _: apron.OverflowException => this
   }  
}

  def addConstraints(cs: List[Tcons1]): LinearContext = {
    cs.foldLeft(this){_.addConstraint(_)}
  }

  //TODO: fix it to use assume
  def assumeSingle(exp: Exp.Closed): LinearContext = {
    val cs = constraintsOfLogicalExpression(exp)
    if (cs.size != 1) {
      Log.println("WARNING: used assumeSingle even though expression resulted in more than 1 disjunct")
    }
    this.addConstraints(cs.head)
  }
  
  def assume(exp: Exp.Closed): List[LinearContext] = {
    for {
      c <- constraintsOfLogicalExpression(exp)
    } yield this.addConstraints(c)
  }

  def bindEval(k: Label, v: Exp.Closed): LinearContext = {
    val next_l = if (isBound(k)) this.unbind(k) else this
    // Might have to put the affineImage back here but it might be
    // this is only necessary for phi nodes which would not be
    // bindings to an expression, but instead bindings to terms
    // (covered by this.bind).
    
    val final_c = next_l._alloc(k)
    try {
      final_c.linearExpressionOfNumericExpression(v) match {
        case Some((coeff, le)) =>
          val cst = new Texpr1CstNode(new MpqScalar(coeff))
          val k_var = new Texpr1VarNode(k.toString())
          val left = new Texpr1BinNode(Texpr1BinNode.OP_MUL, cst, k_var)
          val e = new Texpr1BinNode(Texpr1BinNode.OP_SUB, left, le)         
          val c = new Tcons1(final_c.abstract1.getEnvironment, Tcons1.EQ, e)
          final_c.addConstraint(c)
        case _ => final_c
      }
    } catch {
      case NonLinear("modulus") =>
        throw NonLinear("modulus")
      case NonLinear(s) =>
        ifDebug {
          Log.println(s"WARNING: nonlinearity($s) in $v, will make unconstrained")
        }
        final_c
    }
  }

  //duplicate the dimension of l2 and make l1 bind with the new dimension
  def bindCopy(l1: Label, l2: Label): LinearContext = {
    val abs1 = abstract1.expandCopy(domain, l2.toString(), List(l1.toString()).toArray)    
    this.copy(abstract1 = abs1)    
  }
  
  //1. copy the dimension of l2; 2. make l1 bind with the new dimension; 3. remove the new dimension
  def copyBindRemove(l1: Label, l2: Label): LinearContext = {
    try{
      val abs1 = abstract1.expandCopy(domain, l2.toString(), List("tmp").toArray)
      val l_1 = this.copy(abstract1 = abs1)
      val l_2 = 
        if(this.isBound(l1)) {
          l_1
        } else {
          l_1.alloc(l1)
        }
      
      val tmp_node = new Texpr1VarNode("tmp")
      val l1_node = new Texpr1VarNode(l1.toString())
      var e = new Texpr1BinNode(Texpr1BinNode.OP_SUB, tmp_node, l1_node);
      
      val env = 
        if( l_2.abstract1.getEnvironment.getVars.map { x => x.toString() }.contains("tmp")) {
          l_2.abstract1.getEnvironment
        } else {
          l_2.abstract1.getEnvironment.add(List("tmp").toArray, List[String]().toArray)
        }
      var c = new Tcons1(env, Tcons1.EQ, e)
      val l_3 = l_2.addConstraint(c)
      
      l_3.removeDims("tmp")
    } catch {
      case _: apron.OverflowException => 
        if(this.isBound(l1)) {
          this
        } else {
          this.alloc(l1)
        }
    }
  }
  
  //1. copy the dimension of l2; 2. make l1 bind with the new dimension;
  def copyBind(l1: Label, l2: FrameHeapParameter, l2_copy: FrameHeapParameterCopy): LinearContext = {       
    val l = if(this.isBound(l2_copy)) {
      this.removeDims(l2_copy.toString())
    } else this
    val abs1 = l.abstract1.expandCopy(domain, l2.toString(), List(l2_copy.toString()).toArray)
    
    val l_1 = l.copy(abstract1 = abs1)
    val l_2 = 
      if(this.isBound(l1)) {
        l_1
      } else {
        l_1.alloc(l1)
      }
    
    val l2_copy_node = new Texpr1VarNode(l2_copy.toString())
    val l1_node = new Texpr1VarNode(l1.toString())
    var e = new Texpr1BinNode(Texpr1BinNode.OP_SUB, l2_copy_node, l1_node);
    var c = new Tcons1(l_2.abstract1.getEnvironment, Tcons1.EQ, e)
    l_2.addConstraint(c)
  }
  
  def weakUpdate(to: Label, from: List[Label]): LinearContext = {
    try{
      val (new_abs, _, fromCopy) =
        from.map { x => x.toString() }.foldLeft((this.abstract1, 0, List[String]())) {
          case ((abs, count, fromCopy), expandFrom) =>
            val tmp = "temp" + count
            //println(abs)
            //println(expandFrom)
            //println(tmp)
            (abs.expandCopy(domain, expandFrom, List(tmp).toArray), count+1, tmp::fromCopy)
        }
    
      val abs = new_abs.foldCopy(domain, (to.toString::fromCopy).toArray)
      this.copy(abstract1 = abs)
    } catch {
      case ofe: apron.OverflowException => 
        this.unbind(to)
      //case iae: java.lang.IllegalArgumentException =>
      //  this.unbind(to)
    }
  }

  def DNFOfLogicalExp(exp: Exp.Closed): List[List[Exp.Closed]] = {
    exp match {
      case Expression.Binop(e1, Operator.NumericRelation.≠, e2) => // convert ≠ into disjunction of < or >
        List(Expression.Binop(e1, Operator.NumericRelation.>, e2)) ::
        List(Expression.Binop(e1, Operator.NumericRelation.<, e2)) :: Nil
      case Expression.Binop(e1, r: Operator.NumericRelation, e2) => List(List(exp))
      case Expression.Binop(e1, r: Operator.LogicalRelation, e2) =>
        val disjuncts1 = DNFOfLogicalExp(e1)
        val disjuncts2 = DNFOfLogicalExp(e2)
        r match {
          case Operator.LogicalRelation.∧ => for {
            disjunct1 <- disjuncts1
            disjunct2 <- disjuncts2
          } yield disjunct1 ++ disjunct2
          case Operator.LogicalRelation.∨ => disjuncts1 ++ disjuncts2
          case op => throw NotImplemented(s"$op handling")
        }
      case x => throw InterpException(s"logical expression expected, got $exp instead")
    }

  }

  def linearExpressionOfNumericExpression(exp: Exp.Closed): Option[(Int, Texpr1Node)] = {
    // The returned long is the constant quotient under
    // linear_expression as PPL expressions do not allow such a thing.
    // This is used to handle division by constants by moving them to
    // the other side of (in)equalities.

    exp match {
      case Expression.Term(Term.Linear(l))        =>
        val exp = new Texpr1VarNode(l.toString());
          Some((1, exp))
      case Expression.Term(Term.Constant(CIV(c))) =>
        val exp = new Texpr1CstNode(new MpqScalar(c))
        Some((1, exp))
      case Expression.Unop(e1, r: Operator.NumericUnop) =>
        linearExpressionOfNumericExpression(e1) match {
          case Some((coeff, e1v)) => 
            r match {
              case Operator.NumericUnop.- =>
                val exp = new Texpr1UnNode(Texpr1UnNode.OP_NEG, e1v)
                Some((coeff, exp))
              case _ => None
            }
          case _ => None
            
        }
      case Expression.Binop(e1, Operator.NumericBinop.*, e2) =>
        (linearExpressionOfNumericExpression(e1), linearExpressionOfNumericExpression(e2)) match {
          case (Some((coeff1, e1v)), Some((coeff2, e2v))) =>
            val exp = new Texpr1BinNode(Texpr1BinNode.OP_MUL, e1v, e2v)
            Some((coeff1 * coeff2, exp))
          case (_, _) => None
        }

      case Expression.Binop(e1, Operator.NumericBinop./, e2) =>
        (linearExpressionOfNumericExpression(e1), linearExpressionOfNumericExpression(e2)) match {
          case (Some((coeff1, e1v)), Some((coeff2, e2v))) =>
            val exp = new Texpr1BinNode(Texpr1BinNode.OP_DIV, e1v, e2v)
            Some((coeff1 * coeff2, exp))
          case (_, _) => None
        }
      case Expression.Binop(e1, r: Operator.NumericBinop, e2) =>
        (linearExpressionOfNumericExpression(e1), linearExpressionOfNumericExpression(e2)) match {
          case (Some((coeff1, e1v)), Some((coeff2, e2v))) =>
            val exp = r match {
              case Operator.NumericBinop.+ => 
                new Texpr1BinNode(Texpr1BinNode.OP_ADD, e1v, e2v)
              case Operator.NumericBinop.- => 
                new Texpr1BinNode(Texpr1BinNode.OP_SUB, e1v, e2v)
              case Operator.NumericBinop.* => throw NonLinear("multiplication")
              case Operator.NumericBinop./ => throw NonLinear("division")
              case Operator.NumericBinop.% => 
                new Texpr1BinNode(Texpr1BinNode.OP_MOD, e1v, e2v)
            }
            Some((coeff1 * coeff2, exp))
          case (_, _) => None
        }
    }
  }

  def constraintOfNumericRelation(exp: Exp.Closed): Option[Tcons1] = {
    exp match {
      case Expression.Binop(e1, r: Operator.NumericRelation, e2) =>
        (linearExpressionOfNumericExpression(e1), linearExpressionOfNumericExpression(e2)) match {
          case (Some((coeff1, e1v)), Some((coeff2, e2v))) =>
            val c1 = new Texpr1CstNode(new MpqScalar(coeff1))
            val c2 = new Texpr1CstNode(new MpqScalar(coeff2))
            val e1 = new Texpr1BinNode(Texpr1BinNode.OP_MUL, c2, e1v);
            val e2 = new Texpr1BinNode(Texpr1BinNode.OP_MUL, c1, e2v);
            r match {
              case Operator.NumericRelation.== => //Relation_Symbol.EQUAL
                val left = new Texpr1BinNode(Texpr1BinNode.OP_SUB, e1, e2)
                Some(new Tcons1(abstract1.getEnvironment, Tcons1.EQ, left))
              case Operator.NumericRelation.≠  => throw Disjunctive("not equal")
              case Operator.NumericRelation.<  => //Relation_Symbol.LESS_THAN
                val left = new Texpr1BinNode(Texpr1BinNode.OP_SUB, e2, e1)
                Some(new Tcons1(abstract1.getEnvironment, Tcons1.SUP, left))
              case Operator.NumericRelation.≤  => //Relation_Symbol.LESS_OR_EQUAL
                val left = new Texpr1BinNode(Texpr1BinNode.OP_SUB, e2, e1)
                Some(new Tcons1(abstract1.getEnvironment, Tcons1.SUPEQ, left))
              case Operator.NumericRelation.>  => //Relation_Symbol.GREATER_THAN
                val left = new Texpr1BinNode(Texpr1BinNode.OP_SUB, e1, e2)
                Some(new Tcons1(abstract1.getEnvironment, Tcons1.SUP, left))
              case Operator.NumericRelation.≥  => //Relation_Symbol.GREATER_OR_EQUAL
                val left = new Texpr1BinNode(Texpr1BinNode.OP_SUB, e1, e2)
                Some(new Tcons1(abstract1.getEnvironment, Tcons1.SUPEQ, left))
            }
          case (_, _) => None
        }
      case _ => throw NotImplemented(
        s"Only numerical relations are presently supported as assertions. The expression '$exp' is not a numerical relation."
      )
    }
  }

  def constraintsOfLogicalExpression(exp: Exp.Closed): List[List[Tcons1]] = {
    // A set of disjuncts, where each disjunct is a set of conjuncts.
    for {
      disjuncts_exp: List[Exp.Closed] <- DNFOfLogicalExp(exp)
    } yield disjuncts_exp.foldLeft(List[Tcons1]()) {
      case (list, conjuncts_exp) =>
        constraintOfNumericRelation(conjuncts_exp) match {
          case Some(c) => c :: list
          case _ => list
        }
    }
  }
}

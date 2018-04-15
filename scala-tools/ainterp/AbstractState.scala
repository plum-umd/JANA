import Util._
import Core._
import PPL._
import PPL.LExp._
import WALAUtil._
import Label._
import Expression._
import Config._
import apron._
import edu.illinois.wala.Facade._

import parma_polyhedra_library._
import elina._
import scalaz._
import Scalaz._

import collection.JavaConversions._

object StateTypes {
  type AllBindings = Map[Label, Term.Closed]
  type StatePair   = (AllBindings, LinearContext)
}

import StateTypes._

object AbstractState {

  def join(states: Iterable[AbstractState]): Iterable[AbstractState] = {
    var visited = List[AbstractState]()

    var ret = List[AbstractState]()

    states.foreach { s_out =>
      if(!visited.contains(s_out)) {
        visited = s_out :: visited

        var s = s_out

        states.foreach { s_in =>
          if(!visited.contains(s_in)) {
            s = s.joinOrNot(s_in) match {
              case Some(joined:AbstractState) =>
                visited = s_in :: visited
                joined
              case None => s
            }
          }
        }
        ret = s :: ret
      }
    }
    ret
  }
  
  def alignX
    (m1: AbstractState, m2: AbstractState)
    (implicit init: Boolean = false)
      : (AbstractState, AbstractState) = {
    // The first machine's mapping of label to dimension might be
    // reordered, but the second can only be expanded.

    m1.alignX(m2)(init)
  }
  
  def alignToX
    (m1_in: AbstractState, ms: Iterable[AbstractState])
    (implicit init: Boolean = true)
      : (AbstractState, Iterable[AbstractState]) = {

    // Align ms to the first machine with the first machine preserving
    // the mapping of its variables.
    ms.foldRight[(AbstractState, List[AbstractState])]((m1_in, List[AbstractState]())){
      case (m, (m1:AbstractState, acc: List[AbstractState])) =>
        val (m1a: AbstractState, ma) = AbstractState.alignX(m1, m)(init)
        (m1a, ma :: acc)
    }
  }
  
  def alignManyX(ms: List[AbstractState]): Iterable[AbstractState] = {
    val temp = ms.size match {
      case 0 => List[AbstractState]()
      case 1 => List[AbstractState](ms.head)
      case 2 =>
        val (s1, s2) = AbstractState.alignX(ms.head, ms.tail.head)
        List[AbstractState](s1, s2)
      case _ =>
        val first_m = ms.head
        val tail_ms = ms.tail
        val (first_ma, tail_msa)   = AbstractState.alignToX(first_m, tail_ms)
        val (first_maa, tail_msaa) = AbstractState.alignToX(first_ma, tail_msa)
        // Do this twice for reasons.
        first_maa :: tail_msaa.toList
    }

    temp
  }
}

case class AbstractState(
  dispatch: AllBindings,
  linear  : LinearContext,
  ir      : WALA.IR,
  _isBottom: Boolean,
  updatedHeaps: List[FrameHeapRegister]
) extends ILattice[AbstractState] {

  def this(ir: WALA.IR) = this(
    dispatch = Map[Label,Term.Closed](),
    linear = 
      ConfigManager.domain match {
        case "POLY" => new LinearContext(new OptPoly(false))
        case "BOX" => new LinearContext(new Box())
        case "OCT" => new LinearContext(new OptOctagon())
      },
    // TODO Refactor: this should be a factory
    
    ir = ir,
    _isBottom = false,
    updatedHeaps = List[FrameHeapRegister]()
  )
  
  def updateHeaps(label: FrameHeapRegister): AbstractState = {
    if(hasUpdated(label)) {
      this
    } else {
      this.copy(updatedHeaps = label :: this.updatedHeaps)
    }
  }
  
  def hasUpdated(label: FrameHeapRegister): Boolean = {
    updatedHeaps.contains(label)
  }
  
  def mapState(f: AbstractState => AbstractState): AbstractState = {
    val new_state = f(this)
    new_state
  }

  def flatMap(f: AbstractState => Iterable[AbstractState]): AbstractState = {
    AbstractState.join(f(this)).head
  }

  def joinAllStates(): AbstractState = {
    this
  }
  
  val numConstraints: Int = linear.numConstraints
  val numLabels: Int = dispatch.size

  /* IBinding interface */ 
  type Binder = AbstractState
  def alloc(k: Label): AbstractState = {
    this.copy(
      linear = linear.alloc(k),
      dispatch = dispatch + (k -> Term.Linear(k)))
  }
  
  def alloc(k: List[Label])(implicit init: Boolean = false): AbstractState = {
    val new_dispatch = k.foldLeft(this.dispatch) {
      case (disp, l) =>
        disp + (l -> Term.Linear(l))
    }
    this.copy(
      linear = linear.alloc(k)(init),
      dispatch = new_dispatch)
  }
  
  def apply(k: Label): Option[Term.Closed] = {
    dispatch.get(k)
  }

  def bind(k: Label, v: Term.Closed): AbstractState = {
    val (new_c, new_v) = v match {
      case v: Term.Linear => 
        (linear.bind(k, v), Term.Linear(k))
      case _ =>
        if (linear.isBound(k)) {
          (linear.bind(k, v), Term.Linear(k))
        } else {
          (linear, v)
        }
    }
    copy(
      dispatch = dispatch + (k -> new_v),
      linear = new_c
    )
  }
  def get(k: Label): Term.Closed = dispatch(k)
  def isBound(k: Label): Boolean = dispatch.contains(k)
  def unbind(k: Label): AbstractState = this.copy(linear=linear.unbind(k))

  def unbind(l: List[Label]): AbstractState = this.copy(linear=linear.unbind(l))
  def unbind(f: Label => Boolean): AbstractState = {
    // Remove the labels for which f returns false.
    val lables = this.dispatch.filter{case (l, _) => f(l)}.map(_._1)
    unbind(lables.toList)
  }
  
  def onLabels(labels: Set[Label]): AbstractState = filter({l => labels.contains(l)})

  def filter(f2: Label => Boolean): AbstractState = {
    // Remove the labels for which f returns false.
    val map = this.dispatch.filter{case (l, term) => term match {
      case Term.Linear(_) => true
      case _ => false
      }
    }
    val ret = copy(
      dispatch = dispatch.filter{case (k,v) => f2(k)},
      linear = linear.filterDims(f2, map)
    )
    ret
  }
  
  def keepLabels(f: Label => Boolean): AbstractState = {
    // Remove the labels for which f returns false.
//    val map = this.dispatch.filter{case (l, term) => term match {
//      case Term.Linear(_) => true
//      case _ => false
//      }
//    }
    val new_dispatch = dispatch.filter{case (k,v) => f(k)}
    val labels = new_dispatch.map(_._1)
    val ret = copy(
      dispatch = new_dispatch, //dispatch.filter{case (k,v) => f(k)},
      linear = linear.keepLabels(labels.toList)//.filterDims(f, map)
    )
    ret
  }
  
  def filter(l: List[Label], f: Label => Boolean): AbstractState = {
    // Remove the labels for which f returns false.
    val map = this.dispatch.filter{case (l, term) => term match {
      case Term.Linear(_) => true
      case _ => false
      }
    }
    val ret = copy(
      dispatch = dispatch.--(l).filter{case (k,v) => f(k)},
      linear = linear.filterDims(l, f, map)
    )
    ret
  }
  
  def duplicateVars(): AbstractState = {
    dispatch.keySet.foldLeft(this) {
      case(s, l) =>
        val exp = Expression.Term(s.readvalue(l).get)
        s.bindEval(GhostLabel(l), exp)
    }
  }

  /* ILattice interface */
  def isTop    = false
  def isBottom = _isBottom || linear.⊥?
  
  // Given two abstract states, create a new one that contains the bindings and linear contexts
  // of both, interpreted as a cross product between the abstract state
  // representing each. Assume bindings are disjoint.
  def concat(s2_in: AbstractState): AbstractState = {
    val dis = this.dispatch ++ s2_in.dispatch
    val lin = this.linear.concat(s2_in.linear)
    this.copy(dispatch = dis, linear = lin)
  }

  def join(s2_in: AbstractState): AbstractState = {
    val (s1, s2) =
      if(!ConfigManager.isTopDown && ConfigManager.hasSummarizedDimensions) {
        this.alignX(s2_in)
      } else {
        this.align(s2_in)
      }
    
    val new_linear = s1.linear.join(s2.linear)
    val new_updatedHeaps = s1.updatedHeaps ::: s2.updatedHeaps.diff(s1.updatedHeaps)
    s1.copy(
      linear = new_linear,
      updatedHeaps = new_updatedHeaps
    )
  }
  
  def joinAlign(s2_in: AbstractState): AbstractState = {
    val (s1, s2) = this.alignX(s2_in)
    val new_linear = s1.linear.join(s2.linear)
    val new_updatedHeaps = s1.updatedHeaps ::: s2.updatedHeaps.diff(s1.updatedHeaps)
    s1.copy(
      linear = new_linear,
      updatedHeaps = new_updatedHeaps
    )
  }

  def joinOrNot(s2: AbstractState): Option[AbstractState] = {
    if(ConfigManager.join) {
      ConfigManager.joinPolicy match {
        case None => //no join policy, always join
          Some(this.join(s2))
        case Some(policy) =>          
          val conses_before = this.linear.getConstraintedLs(policy, this.dispatch) ++ s2.linear.getConstraintedLs(policy, this.dispatch)
          val temp = this.join(s2)
          val conses_after = temp.linear.getConstraintedLs(policy, this.dispatch)
          val ret = if (conses_before.subsetOf(conses_after)) {
            Some(temp)
          } else None
          ret
      }    
    } else {
      None
    }
  }

  def isDisjointOn(s2_in: AbstractState, labels: Set[Label]): Boolean =
    this.onLabels(labels).isDisjoint(s2_in.onLabels(labels))

  def isDisjoint(s2_in: AbstractState): Boolean = {
    val (s1, s2) = this.align(s2_in)
    s1.dispatch.toList.any{case (k, v1) => v1 != s2.dispatch(k)} ||
      s1.linear.isDisjoint(s2.linear)
  }
  
  def widen_leq(in_ps2: AbstractState): Boolean = {
    this.leq(in_ps2)
  }

  def widen(s2_in: AbstractState): AbstractState = {
    val (s1, s2) =
      if(!ConfigManager.isTopDown && ConfigManager.hasSummarizedDimensions) {
        this.alignX(s2_in)
      } else {
        this.align(s2_in)
      }
    this.copy(
      linear = s1.linear.widen(s2.linear)
    )
  }

  def leq(s2_in: AbstractState): Boolean = {
    val (s1, s2) =
      if(!ConfigManager.isTopDown && ConfigManager.hasSummarizedDimensions) {
        this.alignX(s2_in)
      } else {
        this.align(s2_in)
      }

    ifDebug{
      Log.println("leq")
      Log.println(tab("s1.linear = " + s1.linear))
      Log.println(tab("s2.linear = " + s2.linear))
    }
    s1.linear.leq(s2.linear)
  }

  def readvalueOrFail(l: Label): Term.Closed = {
    readvalue(l) match {
      case Some(v) => v
      case _ => throw InterpException(s"label $l not defined")
    }
  }

  def readvalue(bname:Label): Option[Term.Closed] = this(bname)

  def writevalue(vid_name: Label, v: Term.Closed): AbstractState =
    bind(vid_name, v)

  //promote a constant label to a linear label
  //alloc new linear label if not existing before
  def promoteOrAlloc(k: Label): AbstractState = {
    this.readvalue(k) match {
      case Some(c:Term.Constant) =>
        val new_context = linear.promoteConstant(k, c)
        this
          .copy(linear = new_context)
          .bind(k, Term.Linear(k))
      case Some(Term.Linear(_)) => this
      case None => alloc(k)
      case _ => throw NotImplemented("unimplemented case at promoteOrAlloc")
    }
  }
  
  def countLabelCopies(k: FrameHeapParameter): Int = {
    dispatch.keySet.foldLeft(0){
      case(i, key) =>
        key match {
          case FrameHeapParameterCopy(fhp, _) => if(fhp==k) i+1 else i
          case _ => i
        }
    }
  }
  
  def countTmpLabels(): Int = {
    dispatch.keySet.foldLeft(0){
      case(i, key) =>
        key match {
          case ft: FrameTemporary => i+1
          case _ => i
        }
    }
  }

  //bind l1 -> duplicate l2
  def bindCopy(l1: Label, l2: Label): AbstractState = {
    get(l2) match {
      case c: Term.Constant => this.bind(l1, c)
      case Term.Linear(_) => 
        this.copy(
          linear = linear.bindCopy(l1,l2),
          dispatch = dispatch + (l1 -> Term.Linear(l1))
        )
      case _ => ???
    }
  }
  
  //bind l1 -> duplicate l2 -> remove duplicate l2
  def copyBindRemove(l1: Label, l2: Label): AbstractState = {
    get(l2) match {
      case c: Term.Constant => this.bind(l1, c)
      case Term.Linear(_) => 
        this.copy(
          linear = linear.copyBindRemove(l1,l2),
          dispatch = dispatch + (l1 -> Term.Linear(l1))
        )
      case _ => ???
    }
  }
  
  def weakUpdate(to: Label, from: List[Label]): AbstractState = {
    this.copy(linear = linear.weakUpdate(to, from),dispatch = dispatch + (to -> Term.Linear(to)))
  }
  
  //bind l1 -> duplicate l2
  def copyBind(l1: Label, l2: FrameHeapParameter): AbstractState = {
    get(l2) match {
      case c: Term.Constant => this.bind(l1, c)
      case Term.Linear(_) => 
        val count = this.countLabelCopies(l2)
                
        val l2_copy = FrameHeapParameterCopy(l2, count)
                
        val ret = this.copy(
          linear = linear.copyBind(l1,l2,l2_copy),
          dispatch = dispatch + (l1 -> Term.Linear(l1)) + (l2_copy -> Term.Linear(l2_copy))
        )
        
        ret
      case _ => ???
    }
  }

  def bindEval(vid: Label, exp: Exp.Closed): AbstractState = {
    val ret = exp match {
      case Expression.Term(v) =>
        this.bind(vid, v)
      case _ =>
        this.copy(
          linear = linear.bindEval(vid, exp),
          dispatch = dispatch + (vid -> Term.Linear(vid))
        )
    }
    ret
  }
  
  def initHeap(label: Label): AbstractState = {
    readvalue(label) match {
      case None => alloc(label)
      case _ => this
    }
  }

  // If exp is entirely concrete, retuce it to a term, otherwise try
  // to reduce the concrete portions a bit. This last bit is not yet
  // implemented except by accident in some cases. Do this if
  // efficiency is necessary.
  def reduceExp(exp: Exp.Closed): Exp.Closed = {
    exp match {
      case e: Expression.Term[Term.Closed] => exp
      case Expression.Binop(v1, r: Operator.LogicalRelation, v2) =>
        val e1 = reduceExp(v1)
        val e2 = reduceExp(v2)
          (e1, e2) match {
          case (
            Expression.Term(Term.Constant(c1:BValue)),
            Expression.Term(Term.Constant(c2:BValue))
          ) =>
            Expression.Term(Term.Constant(
              BValue.getExpLogicalImp(r)(c1)(c2))
            )
          case _ => exp
        }
      case Expression.Binop(v1, r: Operator.NumericRelation, v2) =>
        val e1 = reduceExp(v1)
        val e2 = reduceExp(v2)
          (e1, e2) match {
          case (
            Expression.Term(Term.Constant(c1: IValue)),
            Expression.Term(Term.Constant(c2: IValue))
          ) =>
            Expression.Term(Term.Constant(
              IValue.getExpComparisonImp(r)(c1)(c2))
            )
          case _ => exp
        }
      case Expression.Binop(v1, r: Operator.NumericBinop, v2) =>
        val e1 = reduceExp(v1)
        val e2 = reduceExp(v2)
          (e1, e2) match {
          case (
            Expression.Term(Term.Constant(c1: IValue)),
            Expression.Term(Term.Constant(c2: IValue))
          ) =>
            Expression.Term(Term.Constant(
              IValue.getExpArithmeticImp(r)(c1)(c2))
            )
          case _ => exp
        }
    }
  }
  
  def assert(exp: Exp.Open): AbstractState = {
    // Add a constraint, but also check to make sure that the state
    // assuming the input constraint's negation was not satisfiable.
    
    assume(Exp.close(exp, this.dispatch)).head
  }

  def assume(exp: Exp.Closed): List[AbstractState] = {
    // Add a constraint.
    val (c,bot) = exp match {
      case Expression.Term(Term.Constant(CBV(false))) =>
        (List(linear), true)
      case Expression.Term(Term.Constant(CBV(true)))  =>
        (List(linear), isBottom)
      case _ =>
        (linear.assume(exp).toList, isBottom)
    }
    c.map{case cc =>
      this.copy(
        linear = cc,
        _isBottom = bot
      )
    }
  }

  def writeWeak(label:Label, v:Term): AbstractState = ???
  
  def bindWeak(k: Label, tmp: Label, v: Term.Closed): AbstractState = {
    this.copy(linear = linear.bindWeak(k, tmp, v))
  }
 
  def align(s2: AbstractState)(implicit init: Boolean = false): (AbstractState, AbstractState) = {
      (this, s2)
  }
  
  def alignX(s2: AbstractState)(implicit init: Boolean = false): (AbstractState, AbstractState) = {
    val s1 = this

    /* TODO: there has to be a cleaner way of doing all of this: */

    val b1 = s1.dispatch
    val b2 = s2.dispatch

    val labels = (b1.keys ++ b2.keys).toList
          
//    val (b1a, b2a, c1a, c2a) = labels.foldLeft((
//      b1,
//      b2,
//      s1.linear,
//      s2.linear)) {
//      case ((b1a, b2a, c1a, c2a), label) => 
//        val mv1 = b1.get(label)
//        val mv2 = b2.get(label)
//        if(isRetL(label)) {
//          (mv1, mv2) match {
//            case (Some(Term.Null()), Some(Term.Null())) =>
//              (
//                b1a + (label -> mv1.get),
//                b2a + (label -> mv2.get),
//                c1a,
//                c2a
//              )
//            case (None, Some(Term.Null())) =>
//              (
//                b1a + (label -> mv2.get),
//                b2a + (label -> mv2.get),
//                c1a,
//                c2a
//              )
//            case (Some(Term.Null()), None) =>
//              (
//                b1a + (label -> mv1.get),
//                b2a + (label -> mv1.get),
//                c1a,
//                c2a
//              )
//            case (Some(Term.Linear(l)), None) =>
//              (
//                b1a,
//                b2a+(label -> mv1.get),
//                c1a,
//                c2a.alloc(label)
//              )
//            case (None, Some(Term.Linear(l))) =>
//              (
//                b1a+(label -> mv2.get),
//                b2a,
//                c1a.alloc(label),
//                c2a
//              )
//            case (Some(Term.Linear(_)), Some(Term.Linear(_))) =>                
//              (b1a, b2a, c1a, c2a)
//            case (_, _) => (b1a, b2a, c1a, c2a)
//          }
//          
////        if (! init && ! isHeap(label) && (mv1 == None | mv2 == None)) {
////          throw new InterpException(s"label $label was undefined in one of my arguments; mv1 = $mv1, mv2 = $mv2")
////        }
//
//        val isLinearSomewhere = (s1.linear.isBound(label) || s2.linear.isBound(label))
//          (mv1, mv2) match {
//
//          case (Some(const1@Term.Constant(c)), None) =>
//            (
//              b1a+(label -> mv1.get),
//              b2a+(label -> mv1.get),
//              c1a,
//              c2a
//            )
//          case (None, Some(const2@Term.Constant(c))) =>
//            (
//              b1a+(label -> mv2.get),
//              b2a+(label -> mv2.get),
//              c1a,
//              c2a
//             )
//
//          case (Some(Term.Constant(c)), None) =>
//            (
//              b1a+(label -> mv1.get),
//              b2a+(label -> mv1.get),
//              c1a,
//              c2a
//            )
//          case (None, Some(Term.Constant(c))) =>
//            (
//              b1a+(label -> mv2.get),
//              b2a+(label -> mv2.get),
//              c1a,
//              c2a
//            )
//          case (Some(Term.Linear(l)), None) =>
//            label match {
//              case FrameHeapParameter(id, pk) =>
//                pk match {
//                  case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey =>
//                    val reg_label = FrameHeapRegister(id, pk)
//                    val c2a_init = 
//                      if(b2a.contains(reg_label)) {
//                         c2a.alloc(label).bind(label, Term.Linear(reg_label))
//                      }else {
//                         c2a.alloc(label)
//                      }
//                    val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(label))
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
//                    val c2a_init1 = c2a_init.assumeSingle(exp1 ≥ exp2)
//                    (
//                    b1a+(label -> mv1.get),
//                    b2a+(label -> mv1.get),
//                    c1a,
//                    c2a_init1
//                    )
//                  case _ =>
//                    val reg_label = FrameHeapRegister(id, pk)
//                    val c2a_init = 
//                      if(b2a.contains(reg_label)) {
//                        b2a.get(reg_label).get match {
//                          case Term.Constant(CIV(x)) =>
//                            c2a.alloc(label).bind(label, Term.Constant(CIV(x)))
//                          case Term.Constant(CBV(x)) =>
//                            c2a.alloc(label).bind(label, Term.Constant(CBV(x)))
//                          case Term.Linear(_) =>
//                            c2a.alloc(label).bind(label, Term.Linear(reg_label))
//                        }
//                      }else {
//                         c2a.alloc(label)
//                      }
//                    (
//                    b1a+(label -> mv1.get),
//                    b2a+(label -> mv1.get),
//                    c1a,
//                    c2a_init
//                    )
//                }
//              case FrameHeapRegister(id, pk) =>
//                val par_label = FrameHeapParameter(id, pk)
//                val c2a_init = 
//                  if(ConfigManager.isTopDown) {
//                    if(ConfigManager.isInterProc) {
//                      c2a.alloc(label)
//                    } else {
//                      c2a.alloc(label)
//                    }
//                  } else {
//                    if(b2a.contains(par_label)) {
//                       c2a.alloc(label).bind(label, Term.Linear(par_label))
//                    }else {
//                       c2a.alloc(label)
//                    }
//                  }
//                (
//                b1a+(label -> mv1.get),
//                b2a+(label -> mv1.get),
//                c1a,
//                c2a_init
//                )
//              case _ =>
//                (
//                b1a+(label -> mv1.get),
//                b2a+(label -> mv1.get),
//                c1a,
//                c2a.alloc(label)
//                )
//            }
//            
//          case (None, Some(Term.Linear(l))) =>
//            label match {
//              case FrameHeapParameter(id, pk) =>
//                pk match {
//                  case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey =>
//                    val reg_label = FrameHeapRegister(id, pk)
//                    val c1a_init = 
//                      if(b1a.contains(reg_label)) {
//                         c1a.alloc(label).bind(label, Term.Linear(reg_label))
//                      }else {
//                         c1a.alloc(label)
//                      }
//                    val exp1:Expression[Term.Closed] = Expression.Term(Term.Linear(label))
//                    val exp2:Expression[Term.Closed] = Expression.Term(Term.Constant(CIV(0)))
//                    val c1a_init1 = c1a_init.assumeSingle(exp1 ≥ exp2)
//                    (
//                    b1a+(label -> mv2.get),
//                    b2a+(label -> mv2.get),
//                    c1a_init1,
//                    c2a
//                    )
//                  case _ =>
//                    val reg_label = FrameHeapRegister(id, pk)
//                    val c1a_init = 
//                      if(b1a.contains(reg_label)) {
//                        b1a.get(reg_label).get match {
//                          case Term.Constant(CIV(x)) =>
//                            c1a.alloc(label).bind(label, Term.Constant(CIV(x)))
//                          case Term.Constant(CBV(x)) =>
//                            c1a.alloc(label).bind(label, Term.Constant(CBV(x)))
//                          case Term.Linear(_) =>
//                            c1a.alloc(label).bind(label, Term.Linear(reg_label))
//                        }                        
//                      }else {
//                         c1a.alloc(label)
//                      }
//                    (
//                    b1a+(label -> mv2.get),
//                    b2a+(label -> mv2.get),
//                    c1a_init,
//                    c2a
//                    )
//                }
//              case FrameHeapRegister(id, pk) =>
//                val par_label = FrameHeapParameter(id, pk)
//                val c1a_init = 
//                  if(ConfigManager.isTopDown) {
//                    if(ConfigManager.isInterProc) {
//                      c1a.alloc(label)
//                    } else {
//                      c1a.alloc(label)
//                    }
//                  } else {
//                    if(b1a.contains(par_label)) {
//                       c1a.alloc(label).bind(label, Term.Linear(par_label))
//                    }else {
//                       c1a.alloc(label)
//                    }
//                  }
//                (
//                b1a+(label -> mv2.get),
//                b2a+(label -> mv2.get),
//                c1a_init,
//                c2a
//                )
//              case _ =>
//                (
//                  b1a+(label -> mv2.get),
//                  b2a+(label -> mv2.get),
//                  c1a.alloc(label),
//                  c2a
//                )
//            }
//
//          case (
//            Some(Term.Constant(c1)),
//            Some(Term.Constant(c2))) if c1 == c2 =>
//            (
//              b1a+(label->mv1.get),
//              b2a+(label->mv2.get),
//              c1a,
//              c2a
//            )
//          case (
//            Some(const1@Term.Constant(c1)),
//            Some(const2@Term.Constant(c2))) if c1 != c2 =>
//            (
//              b1a+(label->Term.Linear(label)),
//              b2a+(label->Term.Linear(label)),
//              c1a.promoteConstant(label,const1),
//              c2a.promoteConstant(label,const2)
//            )
//          case (
//            Some(Term.Linear(l1)),
//            Some(Term.Linear(l2))) =>
//            (
//              b1a + (label -> mv1.get),
//              b2a + (label -> mv2.get),
//              c1a,
//              c2a
//            )
//          case (
//            Some(Term.Linear(l1)),
//            Some(const2@Term.Constant(c2))) =>
//            (
//              b1a + (label -> mv1.get),
//              b2a + (label -> mv1.get),
//              c1a,
//              c2a.promoteConstant(label, const2)
//            )
//          case (
//            Some(const1@Term.Constant(c1)),
//            Some(Term.Linear(l2))) =>
//            (
//              b1a + (label -> mv2.get),
//              b2a + (label -> mv2.get),
//              c1a.promoteConstant(label, const1),
//              c2a
//            )
//          case (Some(Term.Null()), Some(Term.Null())) =>
//            (
//              b1a + (label -> mv1.get),
//              b2a + (label -> mv2.get),
//              c1a,
//              c2a
//            )
//          case (None, Some(Term.Null())) =>
//            (
//              b1a + (label -> mv2.get),
//              b2a + (label -> mv2.get),
//              c1a,
//              c2a
//            )
//          case (Some(Term.Null()), None) =>
//            (
//              b1a + (label -> mv1.get),
//              b2a + (label -> mv1.get),
//              c1a,
//              c2a
//            )
//
//          case (_,_) => 
//            throw NotImplemented(s"unhandled case: $label -> $mv1 $mv2")
//        }
//          
//      } else {
//        (b1a, b2a, c1a, c2a)
//      }
//    }
//    
//    val (c1aa,c2aa) = c1a.alignX(c2a)
    
    val keys1 = b1.keySet
    val keys2 = b2.keySet

    val (b1a, b2a) = 
    if (keys1 != keys2) {
      
        (keys2.filter { v => !keys1.contains(v) }.foldLeft(b1) {
          case (map, key) =>
            map + (key -> b2.get(key).get)
        },
        keys1.filter { v => !keys2.contains(v) }.foldLeft(b2) {
          case (map, key) =>
            map + (key -> b1.get(key).get)
        }
        )
    } else {
      (b1, b2)
    }
    
    val (c1aa,c2aa) = s1.linear.alignX(s2.linear)
    (
      s1.copy(
        dispatch = b1a,
        linear = c1aa),
      s2.copy(
        dispatch = b2a,
        linear = c2aa)
    )
  }
  
  override def toString = {
    val labels = dispatch.toList.sortBy(_._1.toString)
    "AbstractState" + (if (isBottom) " IS BOTTOM" else "") + " with:\n" +
    tab(
      "labels =\n" + tab(
        labels.map{
          case (k,v) => k.toString + "\t-> " + v}
          .mkString("\n")
      ) + "\nlinear = " + linear.toString + "\nupdated heaps =" + updatedHeaps
    )
  }

  def toStringWithLocals(implicit addr: WALA.IAddr) = {
    val labels = dispatch.toList.sortBy(_._1.toStringWithLocals)
    "AbstractState" + (if (isBottom) " IS BOTTOM" else "") + " with:\n" +
    tab(
      "labels =\n" + tab(
        labels.map{
          case (k,v) => k.toStringWithLocals + "\t-> " + v + (if (WALAUtil.isSymbol(k)(addr.method)) "\t(from symbol table)" else "")}
          .mkString("\n")
      ) + "\nlinear = " + linear.toStringWithLocals + "\nupdated heaps = " + updatedHeaps
    )
  }
}

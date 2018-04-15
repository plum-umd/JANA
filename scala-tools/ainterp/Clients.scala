import Util._
import Core._
import CommonLoopBounds._
import AbstractMachine._
import WALAUtil._
import Config._
import StateTypes._
import Annotation._
import Exp._
import Label._

import edu.illinois.wala.Facade._

import collection.JavaConversions._

object Clients {
  
  var bound_check = Map[String, String]()
  
  var divided_by_zero_check = Map[String, String]()
  
  var field_access_check = Map[String, String]()
  
  var return_check = Map[String, String]()
  
  def checkDivideByZero(i: BinopI, m: Machine) : Unit = {
    val frame = m.topFrame
    val op = Operator.NumericRelation.==
    val exp = Expression.Binop(
      Expression.Term[Term](Term.Variable(frame.localLabel(i.getUse(1)))),
      op,
      Expression.Term[Term](Term.Constant(CIV(0)))
    )
    List(m.state).foreach { s =>       
      val cexp = exp.close(s.dispatch)                        
      val s_zero = s.assume(cexp).filter(_.isFeasible)
      
      val sig = m.cgnode.getMethod.getSignature + " " + i.toString()
      if(s_zero.isEmpty) {                              
        if(!divided_by_zero_check.contains(sig)) {
          divided_by_zero_check += (sig -> "SUCCESS")
        }
      } else {        
        divided_by_zero_check += (sig -> "FAIL")
      }
    }    
  }
  
  def checkArrayBound(i: ArrayReferenceI, m: Machine) : Unit = {
    val sig = m.cgnode.getMethod.getSignature + " " + i.toString()
    val frame = m.topFrame
    val pts = GlobalHeap.getPointsToSet(m.cgnode, i) //points-to set of the array variable         
    if(pts.length>0) {                                        
     List(m.state).foreach {  s =>
      var list = List[AbstractState]()                 
      val new_pts = 
        if(ConfigManager.unification) { //if unification, currently retrieving the first object in the points-to set
          List(pts(0))
        } else {
          pts
        }
      new_pts.foreach { ik =>
        val lkey = GlobalHeap.getLengthPK(ik)
        val pk = GlobalHeap.getPointerKey(m.cgnode,i.getArrayRef()) //pointer key                                                                      
        s.readvalue(frame.arrayLengthAccessPathLabel(pk)) match {
          case None =>
            s.readvalue(frame.regHeapLabel(lkey)) match {
              case None =>
                bound_check += (sig -> "FAIL")
              case _ =>
                val op = Operator.NumericRelation.<
                
                val exp = Expression.Binop(
                  Expression.Term[Term](Term.Variable(frame.localLabel(i.getIndex))),
                  op,
                  Expression.Term[Term](Term.Variable(frame.regHeapLabel(lkey)))
                )
                
                val cexp = exp.close(s.dispatch)
                        
                val sfalse = s.assume(!cexp).filter(_.isFeasible)
                
                if(ConfigManager.isTopDown) {
                  if(sfalse.isEmpty) {                              
                    if(!bound_check.contains(sig)) {
                      bound_check += (sig -> "SUCCESS")
                    }
                  } else {
                    bound_check += (sig -> "FAIL")
                  }
                } else {
                  val nodes = GlobalHeap.cg.getPredNodes(m.cgnode)

                  val callerAlive = nodes.foldLeft(false) {
                    case (flag, node) =>
                          if(EscapeAnalysis.liveAnalysis.mayBeLive(ik, node, -1))
                            true
                          else
                            flag                      
                  }
                  
                  if(!callerAlive) {
                    if(sfalse.isEmpty) {                              
                      if(!bound_check.contains(sig)) {
                        bound_check += (sig -> "SUCCESS")
                      }
                    } else {
                      bound_check += (sig -> "FAIL")
                    }
                  } else {
                    bound_check += (sig -> "FAIL")
                  }
                }
                
            }
          case _ => //the case array length access path exists
            if(s.linear.constraints(frame.arrayLengthAccessPathLabel(pk))){
              val op = Operator.NumericRelation.<
              
              val exp = Expression.Binop(
                Expression.Term[Term](Term.Variable(frame.localLabel(i.getIndex))),
                op,
                Expression.Term[Term](Term.Variable(frame.arrayLengthAccessPathLabel(pk)))
              )
              val cexp = exp.close(s.dispatch)
                      
              val sfalse = s.assume(!cexp).filter(_.isFeasible)
              
              if(sfalse.isEmpty) {                              
                if(!bound_check.keySet.contains(sig)) {
                  bound_check += (sig -> "SUCCESS")
                }
              } else {
                //TODO: can summarized objects be more precise than access paths?
                bound_check += (sig -> "FAIL")
              }
            } else {
              s.readvalue(frame.regHeapLabel(lkey)) match {
                case None =>
                    val sig = m.cgnode.getMethod.getSignature + " " + i.toString()
                    bound_check += (sig -> "FAIL")
                case _ =>
                  val op = Operator.NumericRelation.<
                  
                  val exp = Expression.Binop(
                    Expression.Term[Term](Term.Variable(frame.localLabel(i.getIndex))),
                    op,
                    Expression.Term[Term](Term.Variable(frame.regHeapLabel(lkey)))
                  )
                  
                  val cexp = exp.close(s.dispatch)
                          
                  val sfalse = s.assume(!cexp).filter(_.isFeasible)
                  
                  if(ConfigManager.isTopDown) {
                    if(sfalse.isEmpty) {                              
                      if(!bound_check.contains(sig)) {
                        bound_check += (sig -> "SUCCESS")
                      }
                    } else {
                      bound_check += (sig -> "FAIL")
                    }
                  } else {
                    val nodes = GlobalHeap.cg.getPredNodes(m.cgnode)
                    val callerAlive = nodes.foldLeft(false) {
                      case (flag, node) =>
                            if(EscapeAnalysis.liveAnalysis.mayBeLive(ik, node, -1))
                              true
                            else
                              flag                        
                    }
                    if(!callerAlive) {
                      if(sfalse.isEmpty) {                              
                        if(!bound_check.contains(sig)) {
                          bound_check += (sig -> "SUCCESS")
                        }
                      } else {
                        bound_check += (sig -> "FAIL")
                      }
                    } else {
                      bound_check += (sig -> "FAIL")
                    }
                  }                  
              }
            }
        }                    
      }
     }
    } else {
      bound_check += (sig -> "FAIL")
    }
  }
  
  def checkFieldAccess(i: GetI, m: Machine) : Unit = {
    val sig = m.cgnode.getMethod.getSignature + " " + i.toString()
    val frame = m.topFrame
    val field = GlobalHeap.getField(i.getDeclaredField()) //field reference
    if(!i.isStatic) {
      val pts = GlobalHeap.getPointsToSet(m.cgnode, i) //points-to set of base variable        
      if(pts.length>0) {                                 
        val ret  = List(m.state).foreach { s =>
          pts.foreach { ik =>               
            val fkey = GlobalHeap.getFieldPK(ik, field) //field key          
            val pk = GlobalHeap.getPointerKey(m.cgnode,i.getRef()) //pointer key                                                                    
            s.readvalue(frame.accessPathLabel(pk, field)) match {
              case None =>  //the case access path pk.field does not exist
                s.readvalue(frame.regHeapLabel(fkey)) match {
                  case None => //the case fkey does not exist as a regular heap label
                    field_access_check.get(sig) match {
                      case None  => 
                        field_access_check += (sig -> "FAIL")
                      case _ =>
                    }
                  case Some(Term.Linear(l)) =>
                    if(s.linear.constraints(l)) {
                      field_access_check.get(sig) match {
                        case None | Some("FAIL") =>
                          field_access_check += (sig -> "SUCCESS")
                        case _ =>
                      }
                    } else {
                      field_access_check.get(sig) match {
                        case None  => 
                          field_access_check += (sig -> "FAIL")
                        case _ =>
                      }
                    }
                  case _ =>
                    field_access_check.get(sig) match {
                      case None | Some("FAIL") =>
                        field_access_check += (sig -> "SUCCESS")
                      case _ =>
                    }
                }
              case Some(Term.Linear(l)) =>
                if(s.linear.constraints(l)) {
                  field_access_check.get(sig) match {
                    case None | Some("FAIL") =>
                      field_access_check += (sig -> "SUCCESS")
                    case _ =>
                  }
                } else {
                  s.readvalue(frame.regHeapLabel(fkey)) match {
                    case None => //the case fkey does not exist as a regular heap label
                      field_access_check.get(sig) match {
                        case None  => 
                          field_access_check += (sig -> "FAIL")
                        case _ =>
                      }
                    case Some(Term.Linear(l)) =>
                      if(s.linear.constraints(l)) {
                        field_access_check.get(sig) match {
                          case None| Some("FAIL") =>
                            field_access_check += (sig -> "SUCCESS")
                          case _ =>
                        }
                      } else {
                        field_access_check.get(sig) match {
                          case None  =>
                            field_access_check += (sig -> "FAIL")
                          case _ =>
                        }
                      }
                    case _ =>
                      field_access_check.get(sig) match {
                        case None | Some("FAIL") =>
                          field_access_check += (sig -> "SUCCESS")
                        case _ =>
                      }
                  }
                }
              case _ =>
                field_access_check.get(sig) match {
                  case None| Some("FAIL") =>
                    field_access_check += (sig -> "SUCCESS")
                  case _ =>
                }
            }
          }
        }
      } else {
        field_access_check += (sig -> "FAIL")
      }
    } else {
      val pk = GlobalHeap.getPointerKeyForStaticField(field)                              
      List(m.state).foreach { s =>
        s.readvalue(frame.regHeapLabel(pk)) match {
          case None => //the case fkey does not exist as a regular heap label 
            field_access_check.get(sig) match {
              case None  => 
                field_access_check += (sig -> "FAIL")
              case _ =>
            }
          case Some(Term.Linear(l)) => //the case fkey already exists as a regular heap label
            if(s.linear.constraints(l)) {
              field_access_check.get(sig) match {
                case None | Some("FAIL") =>
                  field_access_check += (sig -> "SUCCESS")
                case _ =>
              }
            } else {
              field_access_check.get(sig) match {
                case None =>
                  field_access_check += (sig -> "FAIL")
                case _ =>
              }
            }
          case _ =>
            field_access_check.get(sig) match {
              case None | Some("FAIL") =>
                field_access_check += (sig -> "SUCCESS")
              case _ =>
            }
         }
      }
    }    
  }
  
  def checkReturn(m: Machine) : Unit = {
    val frame = m.topFrame
    val retType = m.cgnode.getMethod.getReturnType()
    val sig = m.cgnode.getMethod.getSignature
    WALA.getTypeType(retType) match {
      case WALA.TypeType.TypeTypeIntegral | WALA.TypeType.TypeTypeChar | WALA.TypeType.TypeTypeBoolean =>
        List(m.state).foreach { s =>
          s.readvalue(frame.retLabel) match {
            case Some(Term.Linear(l)) =>
              if(s.linear.constraints(l)) {
                return_check.get(sig) match {
                  case None =>
                    return_check += (sig -> "SUCCESS")
                  case _ =>
                }
              } else {
                return_check.get(sig) match {
                  case None | Some("SUCCESS") =>
                    return_check += (sig -> "FAIL")
                  case _ =>
                }
              }
            case _ =>
              return_check.get(sig) match {
                case None =>
                  return_check += (sig -> "SUCCESS")
                case _ =>
              }
          }
        }
      case _ =>
    }
  }

}

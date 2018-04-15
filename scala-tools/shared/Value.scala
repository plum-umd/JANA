//the Value hierarchy... needs to express other types maybe
abstract class Value

import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.{IOperator => BOperator}
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator._
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.{IOperator => AOperator}
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.Operator._

object BValue {
  def getLogicalImp(op: AOperator): (BValue => BValue => BValue) = op match {
   case AND => _.and
   case OR  => _.or
   case XOR => _.xor
  }
  def getExpLogicalImp(op: Operator.LogicalRelation): (BValue => BValue => BValue) = op match {
   case Operator.LogicalRelation.∧ => _.and
   case Operator.LogicalRelation.∨ => _.or
   case Operator.LogicalRelation.⊕ => _.xor
  }
}

abstract class BValue extends Value {
  def eq (rhs:BValue): BValue
  def ne (rhs:BValue): BValue
  def and(rhs:BValue): BValue
  def or (rhs:BValue): BValue
  def xor(rhs:BValue): BValue
  def neg: BValue
}

trait BValueStatic {
  def btrue: BValue
  def bfalse: BValue
}

object IValue {
  def getArithmeticImp(op: AOperator): (IValue => IValue => IValue) = op match {
    case ADD => _.add
    case SUB => _.sub
    case MUL => _.mul
    case DIV => _.div
    case REM => _.rem
    case AND => _.and
    case OR  => _.or
    case XOR => _.xor
  }

  def getComparisonImp(op: BOperator): (IValue => IValue => BValue) = op match {
    case EQ => _.eq
    case NE => _.ne
    case LT => _.lt
    case LE => _.le
    case GT => _.gt
    case GE => _.ge
  }

  def getExpArithmeticImp[T](op: Operator.NumericBinop): (IValue => IValue => IValue) = op match {
    case Operator.NumericBinop.+ => _.add
    case Operator.NumericBinop.- => _.sub
    case Operator.NumericBinop.* => _.mul
    case Operator.NumericBinop./ => _.div
  }

  def getExpComparisonImp(op: Operator.NumericRelation): (IValue => IValue => BValue) = op match {
    case Operator.NumericRelation.== => _.eq
    case Operator.NumericRelation.≠ => _.ne
    case Operator.NumericRelation.< => _.lt
    case Operator.NumericRelation.≤ => _.le
    case Operator.NumericRelation.> => _.gt
    case Operator.NumericRelation.≥ => _.ge
  }
}

//abstract integer value specification
//anything that wants to call itself an integer must implement these 
//functions, and then define the semantics for what it means for that
//value to combine with anything else inheriting from IValue
abstract class IValue extends Value {
  def add(rhs:IValue): IValue
  def sub(rhs:IValue): IValue
  def mul(rhs:IValue): IValue
  def div(rhs:IValue): IValue
  def rem(rhs:IValue): IValue
  def and(rhs:IValue): IValue
  def  or(rhs:IValue): IValue
  def xor(rhs:IValue): IValue

  def eq(rhs:IValue): BValue
  def ne(rhs:IValue): BValue
  def gt(rhs:IValue): BValue
  def ge(rhs:IValue): BValue
  def lt(rhs:IValue): BValue
  def le(rhs:IValue): BValue
}

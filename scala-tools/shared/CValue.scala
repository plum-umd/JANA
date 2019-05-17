//concrete booleans
case class CBV(bval:Boolean) extends BValue {
  def eq(rhs:BValue): BValue = rhs match {
    case CBV(bval2) => return new CBV(this.bval == bval2)
  }
  def ne(rhs:BValue): BValue = rhs match {
    case CBV(bval2) => return new CBV(this.bval != bval2)
  }

  def and(rhs:BValue): BValue = rhs match {
    case CBV(bval2) => return new CBV(this.bval && bval2)
  }

  def or(rhs:BValue): BValue = rhs match {
    case CBV(bval2) => return new CBV(this.bval || bval2)
  }

  def xor(rhs:BValue): BValue = rhs match {
    case CBV(bval2) => return new CBV(this.bval ^ bval2)
  }

  def neg: BValue = new CBV(! this.bval)
  override def toString = bval.toString
}

object CBV extends BValueStatic {
  val btrue: BValue = CBV(true)
  val bfalse: BValue = CBV(false)
}

//concrete integer semantics
case class CIV(ival:Int) extends IValue {
  override def toString = ival.toString

  def add(rhs:IValue) : IValue = { rhs match {
    case CIV(ival2) => return new CIV(this.ival + ival2)
  } }

  def sub(rhs:IValue) : IValue = { rhs match {
      case CIV(ival2) => return new CIV(this.ival - ival2)
    }
  }

  def mul(rhs:IValue) : IValue = {  rhs match {
      case CIV(ival2) => return new CIV(this.ival * ival2)
    }
  }

  def div(rhs:IValue) : IValue = {  rhs match {
      case CIV(ival2) => return new CIV(this.ival / ival2)
    }
  }

  def rem(rhs:IValue) : IValue = {  rhs match {
      case CIV(ival2) => return new CIV(this.ival % ival2)
    }
  }

  def and(rhs:IValue) : IValue = {  rhs match {
      case CIV(ival2) => return new CIV(this.ival & ival2)
    }
  }

  def or(rhs:IValue) : IValue = {  rhs match {
      case CIV(ival2) => return new CIV(this.ival | ival2)
    }
  }

  def xor(rhs:IValue) : IValue = { rhs match {
      case CIV(ival2) => return new CIV(this.ival ^ ival2)
    }
  }

  def eq(rhs:IValue) : BValue = { rhs match {
      case CIV(ival2) => return new CBV(this.ival == ival2)
    }
  }
  def ne(rhs:IValue) : BValue = { rhs match {
      case CIV(ival2) => return new CBV(this.ival != ival2)
    }
  }

  def gt(rhs:IValue) : BValue = { rhs match {
    case CIV(ival2) => return new CBV(this.ival > ival2)
  } }

  def ge(rhs:IValue) : BValue = { rhs match {
      case CIV(ival2) => return new CBV(this.ival >= ival2)
    }
  }

  def lt(rhs:IValue) : BValue = { rhs match {
      case CIV(ival2) => return new CBV(this.ival < ival2)
    }
  }

  def le(rhs:IValue) : BValue = { rhs match {
      case CIV(ival2) => return new CBV(this.ival <= ival2)
    }
  }
}

object CIV {


}

case class Reference(addr:String) extends Value {
  def eq(rhs:Reference) : Boolean = {
    return this.addr == rhs.addr
  }

  def ne(rhs:Reference) : Boolean = {
    return this.eq(rhs) == false
  }
}
case class Object(field:List[Value]) extends Value {
}

//XXX should contain SSACFG for function a
case class Function(addr:String) extends Value {
}

case class Array(length:IValue) extends Value {
}

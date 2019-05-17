import Core._

import scala.util.parsing.combinator._

import collection.JavaConversions._

object Label {
  type FrameID = (Int, Int)

  object Parser extends Parsing[Label] {
    lazy val parser: PT[Label] = {
      castp(FrameDeferredRegister) |||
      castp(FrameDeferredReturn) |||
      castp(FrameDeferredField)
//      castp(FrameRegister) |||
//      castp(FrameReturn) |||
/*      castp(FrameParameter) |||
      castp(ShadowMethodPCCounter) |||
      castp(ShadowLoopPCCounter) |||*/
      /*FrameHeapParameter.parser |||
       FrameHeapRegister.parser |||
       FrameHeapTemporary.parser |||
       FrameHeapInit.parser |||
       FrameAccessPath.parser |||
       HeapLocation.parser*/
    }
    lexical.reserved ++=
      FrameDeferredRegister.lexical.reserved ++
      FrameDeferredReturn.lexical.reserved
  }

//  val isHeap: String => Boolean = {
//    case s: String =>
//      if(s.startsWith("FrameHeap") | s.startsWith("FrameAccessPath") | s.startsWith("FrameArrayLengthAccessPath"))
//        true
//      else
//        false
////    case _ => false
//  }
//  
//val isNotHeap: String => Boolean = {
//    case s: String =>
//      if(s.startsWith("FrameHeap") | s.startsWith("FrameAccessPath") | s.startsWith("FrameArrayLengthAccessPath"))
//        false
//      else
//        true
////    case _ => false
//  }
//
//  val notPar: String => Boolean = {
//    case s: String =>
//      if(s.startsWith("FrameParameter") | s.startsWith("FrameHeapParameterCopy") | s.startsWith("FrameHeapParameter"))
//        false
//      else
//        true
//  }
//  
//  val isRet: String => Boolean = {
//    case s: String =>
//      if(s.startsWith("FrameReturn") | s.startsWith("FrameHeapRegister"))
//        true
//      else
//        false
////    case _ => false
//  }
//
  val isParRet: String => Boolean = {
    case s: String =>
      if(s.startsWith("FrameParameter") | s.startsWith("FrameHeapParameterCopy") | s.startsWith("FrameReturn") | s.startsWith("FrameHeapRegister"))
        true
      else
        false
  }
//  
//  val isAccessPath: String => Boolean = {
//    case s: String =>
//      if(s.startsWith("FrameAccessPath") | s.startsWith("FrameArrayLengthAccessPath"))
//        true
//      else
//        false
//  }
//  
//  val isFilterPhi: (String, FrameID) => Boolean = {
//    case (s:String, id: FrameID) =>
//      if(s.startsWith("FrameParameter") | s.startsWith("FrameReturn") | s.startsWith("FrameHeapParameter") | s.startsWith("FrameHeapRegister") | s.startsWith("FrameHeapParameterCopy") | s.startsWith("FrameAccessPath") | s.startsWith("FrameArrayLengthAccessPath") | s.startsWith("Ghost")) {
//        true
//      } else if (s.startsWith("FrameRegister")) {
//        if(s.contains(id.toString()))
//          false
//        else
//          true
//      } else {
//        false
//      }
//  }
//  
//  val isNotLocal: (String, FrameID) => Boolean = {
//    case (s:String, id: FrameID) =>
//      if(s.startsWith("FrameRegister") | s.startsWith("FrameParameter") | s.startsWith("FrameReturn") | s.startsWith("FrameAccessPath") | s.startsWith("FrameArrayLengthAccessPath")) {
//        if(s.contains(id.toString()) | s.contains("(-1,-1)"))
//          false
//        else
//          true
//      } else {
//        true
//      }
//  }
//  
//  val isLocalOrHeapReachable: (String, FrameID, List[Label]) => Boolean = {
//    case (s:String, id: FrameID, list) =>
//      if(s.startsWith("FrameRegister") | s.startsWith("FrameParameter") | s.startsWith("FrameReturn") | s.startsWith("FrameAccessPath") | s.startsWith("FrameArrayLengthAccessPath")) {
//        if(s.contains(id.toString()) | s.contains("(-1,-1)"))
//          true
//        else
//          false
//      } else {
//        list.map { x => x.toString() }.contains(s)
//      }
//    case _ => throw NotImplemented("case match in isLocalOrHeapReachable not matched")
//  }
//  
  val all: String => Boolean = {
    case _ => true
  }
  
  val isHeapL: Label => Boolean = {
    case fhp:  FrameHeapParameter => true
    case fhr:  FrameHeapRegister  => true
    case fht:  FrameHeapTemporary => true
    case fhi:  FrameHeapInit      => true
    case fhpc: FrameHeapParameterCopy => true
    case ap: FrameAccessPath => true
    case ap: FrameArrayLengthAccessPath => true
    case GhostLabel(fhp: FrameHeapParameter) => true
    case GhostLabel(fhr: FrameHeapRegister) => true
    case GhostLabel(fht: FrameHeapTemporary) => true
    case GhostLabel(fhi: FrameHeapInit) => true
    case GhostLabel(fhc: FrameHeapParameterCopy) => true
    case GhostLabel(ap: FrameAccessPath) => true
    case GhostLabel(ap: FrameArrayLengthAccessPath) => true
    case _ => false
  }
  
  val notParL: Label => Boolean = {
    case fp:   FrameParameter     => false
    case fhpc: FrameHeapParameterCopy => false
    case fhp:  FrameHeapParameter => false
    case _ => true
  }
  
  val isAccessPathL: Label => Boolean = {
    case fap: FrameAccessPath => true
    case falap: FrameArrayLengthAccessPath => true
    case _ => false
  }
  
  val isRetL: Label => Boolean = {
    case fret: FrameReturn        => true
    case fhr:  FrameHeapRegister  => true
    case _ => false
  }

  val isParRetL: Label => Boolean = {
    case fp:   FrameParameter     => true
    case fret: FrameReturn        => true
    case fhr:  FrameHeapRegister  => true
    case fhpc: FrameHeapParameterCopy => true
    case _ => false
  }
  
  val isFilterPhiL: (Label, FrameID) => Boolean = {
    case (fp: FrameParameter, _)   => true
    case (fr: FrameReturn, _)   => true
    case (FrameRegister(frameid, _), id: FrameID)   => if(frameid==id) false else true
    case (fhp:  FrameHeapParameter, _) => true
    case (fhr:  FrameHeapRegister, _)  => true
    case (fhpc: FrameHeapParameterCopy, _) => true
    case (ap: FrameAccessPath, _) => true
    case (ap: FrameArrayLengthAccessPath, _) => true
    case (GhostLabel(fp: FrameParameter), _) => true
    case (GhostLabel(fret: FrameReturn), _) => true
    case (GhostLabel(fhp:  FrameHeapParameter), _) => true
    case (GhostLabel(fhr:  FrameHeapRegister), _) => true
    case (GhostLabel(fhpc: FrameHeapParameterCopy), _) => true
    case (GhostLabel(ap: FrameAccessPath), _) => true
    case (GhostLabel(ap: FrameArrayLengthAccessPath), _) => true
    case _ => false
  }
  
  val isNotLocalL: (Label, FrameID) => Boolean = {
    case (FrameRegister(frameid, _), id: FrameID)   => if(frameid==id) false else true
    case (FrameParameter(frameid, _), id: FrameID)   => if(frameid==id) false else true
    case (FrameReturn(frameid), id: FrameID)   => if(frameid==id) false else true
    case (FrameAccessPath(frameid, _, _), id: FrameID)   => if(frameid==id) false else true
    case (FrameArrayLengthAccessPath(frameid, _), id: FrameID)   => if(frameid==id) false else true
//    case (FrameHeapRegister(frameid, _), id: FrameID)   => if(frameid==id) false else true
    case _ => true
  }
  
  val isLocalOrHeapReachableL: (Label, FrameID, List[Label]) => Boolean = {
    case (FrameRegister(frameid, _), id: FrameID, _)   => if(frameid==id | frameid==(-1,-1)) true else false
    case (FrameParameter(frameid, _), id: FrameID, _)   => if(frameid==id | frameid==(-1,-1)) true else false
    case (FrameReturn(frameid), id: FrameID, _)   => if(frameid==id | frameid==(-1,-1)) true else false
    case (FrameAccessPath(frameid, _, _), id: FrameID, _)   => if(frameid==id | frameid==(-1,-1)) true else false
    case (FrameArrayLengthAccessPath(frameid, _), id: FrameID, _)   => if(frameid==id | frameid==(-1,-1)) true else false
    case (FrameHeapRegister(frameid, _), id: FrameID, _)   => if(frameid==id | frameid==(-1,-1)) true else false
    case (label, _, list) => list.contains(label)
    case _ => throw NotImplemented("case match in isLocalOrHeapReachable not matched")
  }
  
  val allL: Label => Boolean = {
    case _ => true
  }

  abstract class Label extends Ordered[Label] {
    // Labels are names for various data locations during the
    // execution of a java program.
    import scala.math.Ordered

    def toStringWithLocals(implicit addr: WALA.IAddr): String = toString
    def compare(l2: Label) = this.toString.compare(l2.toString)
    lazy val isParRet: Boolean = Label.isParRetL(this)
  }

  type FieldIndex = WALA.PK

  abstract class LocalLabel extends Label

  object FrameDeferredRegister extends Parsing[FrameDeferredRegister] {
    lazy val parser: PT[FrameDeferredRegister] = logOrNot(vid ^^ { FrameDeferredRegister(_) } )("FrameDeferredRegister")
  }
  case class FrameDeferredRegister(index: VarIndex) extends LocalLabel {
    // A frame's local register that is read from an annotation.
    // Unlike the below, it does not contain a frameid as this is not
    // know during parsing.
    override def toString = "FrameDeferredRegister" + s"v$index"
    override def toStringWithLocals
      (implicit addr: WALA.IAddr): String = {
      val ir = addr.method
      val iindex = if (addr.index >= 0) addr.index else 0
      val name = ir.getLocalNames(iindex, index.asInstanceOf[Int])
      this.toString + (if (null != name) "|" + name.toList.mkString("/") else "")
    }
  }

  object FrameDeferredField extends Parsing[FrameDeferredField] {
    lazy val parser: PT[FrameDeferredField] = logOrNot(
      (vid ~ keyword(".") ~ ident) ^^ { case (vid ~ _ ~ fname) => FrameDeferredField(vid, fname) }
    )("FrameDeferredField")
  }
  case class FrameDeferredField(index: VarIndex, fname: String) extends LocalLabel {
    override def toString = "FrameDeferredField" + s"v$index.$fname"
    override def toStringWithLocals
      (implicit addr: WALA.IAddr): String = {
      val ir = addr.method
      val iindex = if (addr.index >= 0) addr.index else 0
      val name = ir.getLocalNames(iindex, index.asInstanceOf[Int])
      this.toString + (if (null != name) "|" + name.toList.mkString("/") else "")
    }
  }

  object FrameRegister extends Parsing[FrameRegister] {
    lazy val parser: PT[FrameRegister] = "v" ~> (int ^^ { FrameRegister((0,0), _) } )
  }
  case class FrameRegister(frameid: FrameID, index: VarIndex) extends LocalLabel {

    // A frame's local register (v0, ...). 
    override def toString = "FrameRegister" + s"v$frameid" + s"v$index"
    override def toStringWithLocals
      (implicit addr: WALA.IAddr): String = {
      val ir = addr.method
      val iindex = if (addr.index >= 0) addr.index else 0
      val name = ir.getLocalNames(iindex, index.asInstanceOf[Int])
      this.toString + (if (null != name) "|" + name.toList.mkString("/") else "")
    }
  }

  object FrameDeferredReturn extends Parsing[FrameDeferredReturn] {
    lazy val parser: PT[FrameDeferredReturn] = "ret" ^^^ { FrameDeferredReturn() }
    lexical.reserved += "ret"
  }
  case class FrameDeferredReturn() extends LocalLabel {
    // Special label to store the value a frame returns.
    override def toString = "FrameDeferredReturn" + "ret"
  }

  object FrameReturn extends Parsing[FrameReturn] {
    lazy val parser: PT[FrameReturn] = "ret" ^^^ { FrameReturn((0,0)) }
    lexical.reserved += "ret"
  }
  case class FrameReturn(frameid: FrameID) extends LocalLabel {
    // Special label to store the value a frame returns.
    override def toString = "FrameReturn" + frameid + "ret"
  }

  object FrameParameter extends Parsing[FrameParameter] {
    lazy val parser: PT[FrameParameter] = "vin" ~> (int ^^ {FrameParameter((0,0), _)})
  }
  case class FrameParameter(frameid: FrameID, index: VarIndex) extends LocalLabel {
    // A distinction from FrameRegister used to designate the local
    // variables that were the frame's method's input parameters. Note
    // that these are not distinguished in the WALA IR from those we
    // use FrameRegister for.
    override def toString = "FrameParameter" + s"v$frameid" + s"v$index" + "ⁱⁿ"
    override def toStringWithLocals
      (implicit addr: WALA.IAddr): String = {
      val ir = addr.method
      val m = ir.getMethod()
      val aindex = if (m.isStatic()) index - 1 else index - 1
      val iindex = if (addr.index >= 0) addr.index else 0
      val name = ir.getMethod().getLocalVariableName(iindex, aindex.asInstanceOf[Int])
      this.toString + (if (null != name) s"|$name" else "")
    }
  }
  
  case class GhostLabel(label: Label) extends LocalLabel {
    override def toString = "GhostLabel" + "ghost(" + label.toString + ")"
  }

  case class FrameHeapParameter(frameid: FrameID, pk: WALA.PK) extends LocalLabel {
    //TODO: a better toString for Heap labels
//    override def toString = s"$pk(in)"
//    override def toString: String = {
//      pk match {
//        case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey => 
//          lkey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "arraylengthⁱⁿ: " + asin.getSite()
//            case _ => s"arraylengthⁱⁿ: $pk(in)"
//              //throw NotImplemented("case of heap toString no handled")
//          }
//        case akey: com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey =>
//          akey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "arrayⁱⁿ: " + asin.getSite()
//            case _ => s"arrayⁱⁿ: $pk(in)"
//              //throw NotImplemented("case of heap toString no handled")
//          }
//        case fkey: com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey =>
//          fkey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "fieldⁱⁿ: " + asin.getSite() + " " + fkey.getField()
//            case _ => s"fieldⁱⁿ: $pk(in)"
//              //throw NotImplemented("case of heap toString no handled")
//          }
//        case _ => s"$pk(in)"
//      }
//    }
    
    override def toString: String = {
      "FrameHeapParameter" + frameid + pk.hashCode
//      pk match {
//        case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey => 
//          val ik = lkey.getInstanceKey()
//          val pks = GlobalHeap.heap.getPredNodes(ik).toList
//          pks.foldLeft("length"){
//            case(s, pk) =>
//              pk match {
//                case lpk: com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey =>
//                  if(lpk.getNode.getIR != null) {
//
//                    val method_hash = lpk.getNode.getIR.hashCode
//                    frameid match {
//                      case (_, hash: Int) =>
//                        if(hash == method_hash) {
//                          if(s.equals("length"))
//                            s + "(v" + lpk.getValueNumber + ")"
//                          else{
//                            s + "(v" + lpk.getValueNumber + ")"
//                          }
//                        } else {
//                          s
//                        }                      
//                    }
//                  } else {
//                    s
//                  }
//                case _ => s
//              }
//          }
//        case akey: com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey =>
//          val ik = akey.getInstanceKey()
//          val pks = GlobalHeap.heap.getPredNodes(ik).toList
//          pks.foldLeft("element"){
//            case(s, pk) =>
//              pk match {
//                case lpk: com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey =>
//                  if(lpk.getNode.getIR != null) {
//                    val method_hash = lpk.getNode.getIR.hashCode
//                    frameid match {
//                      case (_, hash: Int) =>
//                        if(hash == method_hash) {
//                          if(s.equals("element"))
//                            s + "(v" + lpk.getValueNumber + ")"
//                          else{
//                            s + "(v" + lpk.getValueNumber + ")"
//                          }
//                        } else {
//                          s
//                        }                     
//                    }
//                  } else {
//                    s
//                  }
//                case _ => s
//              }
//          }
//        case fkey: com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey =>
//          val ik = fkey.getInstanceKey()
//          val pks = GlobalHeap.heap.getPredNodes(ik).toList
//          pks.foldLeft("fieldⁱⁿ: "){
//            case(s, pk) =>
//              pk match {
//                case lpk: com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey =>
//                  if(lpk.getNode.getIR != null) {
//                  val method_hash = lpk.getNode.getIR.hashCode
//                  frameid match {
//                    case (_, hash: Int) =>
//                      if(hash == method_hash) {
//                        if(s.equals("fieldⁱⁿ: "))
//                          s + "v" + lpk.getValueNumber
//                        else{
//                          s + " | v" + lpk.getValueNumber
//                        }
//                      } else {
//                        s
//                      }
//                      
//                  }
//                  } else {
//                    s
//                  }
//                case _ => s
//              }
//          }
//        case _ => s"$pk(in)"
//      }
    }

  }

    case class FrameHeapParameterCopy(label: FrameHeapParameter, id: Int) extends LocalLabel {

    override def toString: String = {
      "FrameHeapParameterCopy" + label.toString() + "@" + id
    }

  }
  
  case class FrameHeapRegister(frameid: FrameID, pk: WALA.PK) extends LocalLabel {
    //TODO: a better toString for Heap labels
//    override def toString = s"$pk"
//    override def toString: String = {
//      pk match {
//        case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey => 
//          lkey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "arraylength: " + asin.getSite()
//            case _ => s"arraylength: $pk(in)"
//              //throw NotImplemented("case of heap toString no handled")
//          }
//        case akey: com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey =>
//          akey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "array: " + asin.getSite()
//            case _ => s"array: $pk(in)"
//            //throw NotImplemented("case of heap toString no handled")
//          }
//        case fkey: com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey =>
//          fkey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "field: " + asin.getSite() + " " + fkey.getField()
//            case _ => s"field: $pk(in)"
//            //throw NotImplemented("case of heap toString no handled")
//          }
//        case _ => s"$pk"
//      }
//    }
    
    override def toString: String = {
      "FrameHeapRegister" + frameid + pk.hashCode
//      pk match {
//        case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey => 
//          val ik = lkey.getInstanceKey()
//          val pks = GlobalHeap.heap.getPredNodes(ik).toList
//          pks.foldLeft("length: "){
//            case(s, pk) =>
//              pk match {
//                case lpk: com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey =>
//                  if(lpk.getNode.getIR != null) {
//                  val method_hash = lpk.getNode.getIR.hashCode
//                  frameid match {
//                    case (_, hash: Int) =>
//                      if(hash == method_hash) {
//                        if(s.equals("length: "))
//                          s + "v" + lpk.getValueNumber
//                        else{
//                          s + " | v" + lpk.getValueNumber
//                        }
//                      } else {
//                        s
//                      }
//                      
//                  }
//                  } else {
//                    s
//                  }
//                case _ => s
//              }
//          }
//        case akey: com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey =>
//          val ik = akey.getInstanceKey()
//          val pks = GlobalHeap.heap.getPredNodes(ik).toList
//          pks.foldLeft("element: "){
//            case(s, pk) =>
//              pk match {
//                case lpk: com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey =>
//                  if(lpk.getNode.getIR != null) {
//                  val method_hash = lpk.getNode.getIR.hashCode
//                  frameid match {
//                    case (_, hash: Int) =>
//                      if(hash == method_hash) {
//                        if(s.equals("element: "))
//                          s + "v" + lpk.getValueNumber
//                        else{
//                          s + " | v" + lpk.getValueNumber
//                        }
//                      } else {
//                        s
//                      }
//                      
//                  }
//                  } else {
//                    s
//                  }
//                case _ => s
//              }
//          }
//        case fkey: com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey =>
//          val ik = fkey.getInstanceKey()
//          val pks = GlobalHeap.heap.getPredNodes(ik).toList
//          pks.foldLeft("field: "){
//            case(s, pk) =>
//              pk match {
//                case lpk: com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey =>
//                  if(lpk.getNode.getIR != null) {
//                  val method_hash = lpk.getNode.getIR.hashCode
//                  frameid match {
//                    case (_, hash: Int) =>
//                      if(hash == method_hash) {
//                        if(s.equals("field: "))
//                          s + "v" + lpk.getValueNumber
//                        else{
//                          s + " | v" + lpk.getValueNumber
//                        }
//                      } else {
//                        s
//                      }
//                      
//                  }
//                  } else {
//                    s
//                  }
//                case _ => s
//              }
//          }
//        case _ => s"$pk"
//      }
    }

  }

  //temporary heap field/array label; used for implicit field/array read (parameter passing)
  case class FrameHeapTemporary(frameid: FrameID, pk: WALA.PK) extends LocalLabel {
    //TODO: a better toString for Heap labels
//    override def toString = s"$pk(tmp)"
    override def toString: String = {
      "FrameHeapTemporary" + frameid + pk.hashCode + "(tmp)"
//      pk match {
//        case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey => 
//          lkey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "arraylength(tmp): " + asin.getSite()
//            case _ => throw NotImplemented("case of heap toString no handled")
//          }
//        case akey: com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey =>
//          akey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "array(tmp): " + asin.getSite()
//            case _ => throw NotImplemented("case of heap toString no handled")
//          }
//        case _ => s"$pk(tmp)"
//      }
    }

  }
  
  case class FrameTemporary(frameid: FrameID, i: Int) extends LocalLabel {
    override def toString = frameid + "tmp:" + i
  }
  
  case class FrameHeapInit(frameid: FrameID, pk: WALA.PK) extends LocalLabel {
    //TODO: a better toString for Heap labels
//    override def toString = s"$pk(init)"
    override def toString: String = {
      "FrameHeapInit" + frameid + pk.hashCode + "(init)"
//      pk match {
//        case lkey: com.ibm.wala.ipa.modref.ArrayLengthKey =>
//          lkey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "arraylength(init): " + asin.getSite()
//            case _ => throw NotImplemented("case of heap toString no handled")
//          }
//        case akey: com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey =>
//          akey.getInstanceKey() match {
//            case asin: com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode => "array(init): " + asin.getSite()
//            case _ => throw NotImplemented("case of heap toString no handled")
//          }
//        case _ => s"$pk(init)"
//      }
    }

  }
  
  case class FrameAccessPath(frameid: FrameID, pk: WALA.PK, field:WALA.Field) extends LocalLabel {
    override def toString = "FrameAccessPath" + frameid + pk.hashCode + " " + field  
//      override def toString = "FrameAccessPath" + frameid + pk + " " + field
}
  
  case class FrameArrayLengthAccessPath(frameid: FrameID, pk: WALA.PK) extends LocalLabel {
    override def toString = "FrameArrayLengthAccessPath" + frameid + pk.hashCode + ".length"
  }
  
  case class StaticUnmodeledFunction(frameid: FrameID, sig: String) extends LocalLabel {
    override def toString = "static: " + sig
  }
  
  case class DispatchUnmodeledFunction(frameid: FrameID, index: VarIndex, sig: String) extends LocalLabel {
    override def toString = "dispatch: v" + index + " " + sig
  }

  sealed abstract class ShadowLabel extends Label

  object ShadowMethodPCCounter extends Parsing[ShadowMethodPCCounter] {
    lazy val parser: PT[ShadowMethodPCCounter] =  "pc" ^^^ { ShadowMethodPCCounter((0,0)) }
    lexical.reserved += "pc"
  }

  case class ShadowMethodPCCounter(frameid: FrameID) extends ShadowLabel {
    override def toString = "pc"
  }

  object ShadowLoopPCCounter extends Parsing[ShadowLoopPCCounter] {
    lazy val parser: PT[ShadowLoopPCCounter] = "lc" ^^^ { ShadowLoopPCCounter((0,0), null) }
    lexical.reserved += "lc"
  }
  case class ShadowLoopPCCounter(frameid: FrameID, loophead: WALA.CFGNode) extends ShadowLabel {
    override def toString = "lc"
  }

  abstract class GlobalLabel extends Label
  case class HeapLocation(field: VarIndex) extends GlobalLabel

  //case class ArrayReference(ref: ArrayIndex, index: IndexIndex) extends Label
}

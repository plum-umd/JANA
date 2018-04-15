//import Label._
//import Core._
//
//object Packing {
//
//  var packs:List[Pack] = List[Pack]()
//  
//  var flag: Boolean = true
//  
//  var count: Int = 0
//  
//  var map:Bimap[Label, List[Int]] = new Bimap[Label, List[Int]]()
//  
//  var blockMap:Bimap[(Int, Int), Int] = new Bimap[(Int, Int), Int]()
//  
//  var current: Int = 0
//  
//  def findPack(label: Label) : Pack = {
//    ConfigManager.packing match {
//      case (None, _) =>
//        new Pack()
//      case (Some("RANDOM"), Some(i: Integer)) =>
//        if(map.contains(label)) {
//          packs.filter { p => map(label).contains(p.pack.head) }.head
//        } else {
//          val mod = count % i
//          map += (label -> List(mod))
//          count += 1
//          packs.filter { p => p.pack.head == mod }.head
//        }
//      case (Some("SYNTACTIC_BLOCK"), _) | (Some("SYNTACTIC_METHOD"), _) =>
//        
//        if(map.contains(label)) {
//          if(!map(label).contains(current)) {
//            val content = current :: map(label)
//            map += (label -> content)            
//          }
//        } else {
//          map += (label -> List(current))
//        }
//        packs.filter { p => p.pack.head == current }.head
//
//      case (_, _) => throw NotImplemented("packing policy not implemented")
//    }
//  }
//  
//  def setBlockCount(block: (Int, Int)) : Unit = {
//    blockMap.get(block) match {
//      case None =>
////        map.getBack(count) match {
////          case None =>
////            blockMap = blockMap -/ count
////          case _ =>
//            count = count + 1
////        }
//        
//        packs = new Pack(count) :: packs
//        blockMap += (block -> count)
//        current = count
//        
//      case Some(i) => current = i
//    }
//  }
//
//}
//
//case class Pack(
//  val pack: List[Int]
//) {
//
//def this() = this(
//  List[Int]()
//)
//
//def this(i:Int) = this(
//  List[Int](i)
//)
//
//def contains(label: Label) : Boolean = {
//  
//  if(this.isBase) {
//    return true
//  }
//  
//  ConfigManager.packing match {
//    case (None, _) =>
//      true
//    case (Some("RANDOM"), Some(i:Integer)) =>      
//      if(Packing.map.contains(label)) {
//        val head = this.pack.head
//        if(Packing.map(label).contains(head)) {
//          true
//        } else {
//          false
//        }
//      } else {
//         val mod =  Packing.count % i
//         Packing.map += (label -> List(mod))
//         Packing.count += 1
//         val head = this.pack.head
//         if(head == mod) {
//           true
//         } else {
//           false
//         }
//      }
//    case (Some("SYNTACTIC_BLOCK"), _) | (Some("SYNTACTIC_METHOD"), _) =>
//      if(Packing.map.contains(label)) {
//        val head = this.pack.head
//        if(Packing.map(label).contains(head)) {
//          true
//        } else {
//          if(head == Packing.current) {
//            val content = Packing.current :: Packing.map(label)
//            Packing.map += (label -> content)  
//            true
//          } else {
//            false
//          }
//        }
//      } else {
//         Packing.map += (label -> List(Packing.current))
//         val head = this.pack.head
//         if(head == Packing.current) {
//           true
//         } else {
//           false
//         }
//      }
//    case (_, _) => throw NotImplemented("packing policy not implemented")
//  }
//}
//
//def isEmpty : Boolean = pack.isEmpty
//
//def equals(other: Pack) : Boolean = {
//  if(pack.isEmpty) return other.pack.isEmpty
//  pack.head == other.pack.head
//}
//
//def isBase : Boolean = {
//  if(pack.isEmpty) return false
//  pack.head == -1
//}
//
//}
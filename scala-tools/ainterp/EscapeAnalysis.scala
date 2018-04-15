import edu.illinois.wala.Facade._

import collection.JavaConversions._

import com.ibm.wala.escape.FILiveObjectAnalysis
 
object EscapeAnalysis {

    val liveAnalysis = new FILiveObjectAnalysis(GlobalHeap.cg, GlobalHeap.heap, false)

    def mayBeLive(pk: WALA.PK, node: WALA.CGNode) : Boolean = {
      GlobalHeap.pk2ik.get(pk).get match {
        case None => true
        case Some(ik: WALA.IK) => liveAnalysis.mayBeLive(ik, node, -1)
      }
    }

}


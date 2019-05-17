import edu.illinois.wala.Facade._

import collection.JavaConversions._

import AInterp._
import CommandLine._
import WALA._
import WALAUtil._
import Label._
import Core._
import Config._

import java.io._

object IntraProc {
  
    var map = scala.collection.mutable.Map[CGNode,Machine]()
  
    //the top-down intra-procedural analysis for all the nodes in a call graph (except for WALA's fake root nodes)
    def solve(cg: WALA.CG)(implicit conf: AInterpConfig) : Unit = {
      WALA.createHeapMap(cg)
      val nodes = cg.iterator().toList      
      val isFakeRoot: String => Boolean = {
        case "com.ibm.wala.FakeRootClass.fakeRootMethod()V" => true
        case "com.ibm.wala.FakeRootClass.fakeWorldClinit()V" => true
        case _ => false
      }      

      nodes.filter(n => !isFakeRoot(n.getMethod.getSignature)).foreach { node =>
        Log.println("analyzing " + node.getMethod.getSignature)
         if(node.getIR!=null) {
          ConfigManager.checkArrayBound = false
          ConfigManager.checkFieldAccess = false
          val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node)
          val start = System.currentTimeMillis()
          val ret = AInterp.solveMethod(InterpContext(new EvalContext(), wcontext))(conf)
          val interval = System.currentTimeMillis() - start
          ret match {
            case Some(m) =>  map += (node -> m)
            case _ =>
          }
          val out = new PrintWriter(new BufferedWriter(new FileWriter("./files/intra-performance.txt", true)));
          out.write(node.getMethod.getSignature + "\t" + interval + "\n")
          out.close()
        } else {
          Log.println("WARNING: IR of " + node.getMethod.getSignature + " not found;")
        }
      }
    }

}

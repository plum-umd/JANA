import edu.illinois.wala.Facade._

import collection.JavaConversions._

import AInterp._
import CommandLine._
import WALA._
import WALAUtil._
import Label._
import SpecialLibHandling._
import Core._
import AbstractMachine.{Machine}

import util.control.Breaks._

//Extract recursions in the call graph and an estimation of if parallelization could help
object CallGraphAnalysis {

    val chain = scala.collection.mutable.ArrayBuffer.empty[CGNode]
    
    val printed = scala.collection.mutable.ArrayBuffer.empty[CGNode]

    def analyze() : Unit = {      
//      recursion(GlobalHeap.cg.getFakeRootNode)
      parallel()
    }

    def getCallees(node: CGNode) : List[CGNode] = {
	    return GlobalHeap.cg.getSuccNodes(node).toList
    }
    
    //find recursions in the application code
    def recursion(node: CGNode) : Unit= {
      chain += node
      val callees = getCallees(node)
      callees.foreach { callee =>
        val loader = callee.getMethod.getDeclaringClass.getClassLoader.getName.toString()
        if(loader.equals("Application")) {
          if(!chain.contains(callee)) {
            recursion(callee)
          } else {
            if(!printed.contains(callee)) {
              Log.println(callee.getIR.getMethod.getSignature)
              printed += callee
            }
          }
        }        
      }
      chain -= node
    }
    
    //compute a possible parallelization scheduling for inter-procedural bottom-up analysis
    def parallel() : Unit = {    
      val visited = scala.collection.mutable.ArrayBuffer.empty[CGNode]           
      var size = 0     
      do {        
        size = visited.size       
        val nodes = GlobalHeap.cg.iterator().toList        
        var count = 0       
        nodes.foreach { node =>         
            val loader = node.getMethod.getDeclaringClass.getClassLoader.getName.toString()
            if(loader.equals("Application") && !visited.contains(node)) {
              val succs = GlobalHeap.cg.getSuccNodes(node).toList
              var flag = true
              breakable {
                succs.foreach { succ => 
                  val succ_loader = succ.getMethod.getDeclaringClass.getClassLoader.getName.toString()
                  if(succ_loader.equals("Application") && !visited.contains(succ)) {
                    flag = false
                    break
                  }
                }
              }
              if(flag) {
                count += 1
                visited += node
              }
            }
          
        }        
        Log.println(count)
      } while(visited.size > size)      
    }

}

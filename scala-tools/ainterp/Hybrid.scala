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
import Config._

//the inter-procedural top-down and bottom-up analysis
object Hybrid {

    //the mapping from the call graph node to its summary machine
    var map = scala.collection.mutable.Map[CGNode,Machine]()

    var visited = List[CGNode]()

    def startSolving(context: InterpContext) : Unit = {
      
      //start with bottom-up analysis
      ConfigManager.globalHeapLabel = false
      ConfigManager.isTopDown = false
      ConfigManager.joinPolicy = None
      
      val node = context.wala.cgnode
      
      val clinit_nodes = GlobalHeap.cg.iterator().filter { node => node.getMethod.isClinit() }.toList
        
      clinit_nodes.foreach { clinit_node =>
            dfs(clinit_node)
      }
      
      dfs(node)

      //now switch to top-down analysis
      ConfigManager.globalHeapLabel = true
      ConfigManager.isTopDown = true
      ConfigManager.joinPolicy = None
            
      AInterp.solveMethod(context)(new AInterpConfig(true, false))
    }

    def unsolvedCallees(node: CGNode) : List[CGNode] = {
	    return GlobalHeap.cg.getSuccNodes(node).toList
    }
    
    def dfs(node: CGNode) : Unit= {
      visited ::= node
      var callees = unsolvedCallees(node)
      callees.foreach { callee =>
        if(!visited.contains(callee))
          dfs(callee)
      }
      
      val loader = node.getMethod.getDeclaringClass.getClassLoader.getName.toString()
      val skip = 
        if(ConfigManager.useLibraryModel) {
          if(!loader.equals("Application"))
            true
          else
            false
        } else if(ConfigManager.isHybrid) {
          if(loader.equals("Application"))
            true
          else {
              false  
          }
        }  else {
            false
        }        
          
      val nullIR =
        if(node.getIR!=null)
          false
        else
          true
          
      (skip, nullIR) match {
          case (false, false) =>
            val wcontext = new WALA.WALAContext(GlobalHeap.heap, GlobalHeap.cg, node) 
            
            ConfigManager.joinPolicy = None
            ConfigManager.isTopDown = false
            ifVerbose {
              Log.println("Starting to compute summary for " + node.getIR.getMethod.getSignature)
            }
            AInterp.solveMethod(InterpContext(new EvalContext(), wcontext))((new AInterpConfig(true, true))) match {
              case Some(m) =>
                val ret_m = m.filter(isParRetL)
                //TODO: filter the unconstrainted heap parameters?
//                val ret_m = ret_m1.mapState { s =>  
//                  s.dispatch.keys.foldLeft(s) {
//                    case (s1, key) =>
//                      key match {
//                        case fhpc: FrameHeapParameterCopy =>
//                          if(!s.linear.constraints(fhpc)) {
//                            s1.unbind(fhpc)
//                          } else {
//                            s1
//                          }
//                        case fhp: FrameHeapParameter =>
//                          if(!s.linear.constraints(fhp)) {
//                            s1.unbind(fhp)
//                          } else {
//                            s1
//                          }
//                        case _ =>
//                          s1
//                      }
//                      
//                    
//                  }
//                }
                val sum_m = ret_m.copy(state = ret_m.state.joinAllStates())
                
                map += (node -> sum_m)
                
                ifVerbose {
                  Log.println("Computed summary for " + node.getIR.getMethod.getSignature)
                  Log.println(sum_m)
                }
          
                ifDebug{ Log.println(map(node)) }
              case _ =>
            }
            
          case (_, _) => 
            ifVerbose {
              Log.println("WARNING: skipping the method " + node.getMethod.getSignature)
            }
        }

    }

}

import Util._
import Core._
import WALA._
import CommandLine._
import WALAUtil._
import Config._

import collection.JavaConversions._

import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._ // if you don't supply your own protocol

object CLI {
  
  var beforeAInterp : Long = 0
  
  def main(argv: scala.Array[String]) = {

    val (action, contexts) = processArgs(argv,
      toolName="Abstract Interpreter",
      toolDescription="Front-end for abstract interpretation and related analyses.")

    if (None == action || "ainterp" == action.get || "intra-bottomup" == action.get) { //intra-procedural bottom-up analysis
      
      contexts.foreach{ case (context) =>
        GlobalHeap.cg   = context.wala.cg
        GlobalHeap.heap = context.wala.heap
        
        val ret_projected = AInterp.solveMethod(context)(new AInterpConfig(false, true))
        
        Log.println("Final return is\n" + tab(ret_projected))
      }
    } else if ("ast" == action.get) {
      contexts.foreach{ context =>
        val InterpContext(econtext, wcontext) = context
        var expNode = new CFGAnnotations.ASTNodeAny(null, null, null)
        val controlTree = expNode.buildLoopTreeFromCFG(wcontext.cfg.entry(),
          wcontext)
        Log.println(controlTree.toString(1))
      }
    } else if ("interproc" == action.get | "inter-bottomup" == action.get) { //inter-procedural bottom-up analysis

      beforeAInterp = System.currentTimeMillis()
      contexts.foreach{
        case context =>
          GlobalHeap.cg   = context.wala.cg
          GlobalHeap.heap = context.wala.heap      
          
          if(ConfigManager.hasSummarizedDimensions)
            WALA.createHeapMap(context.wala.cg)
          
          val map = InterProc.startSolving(context.wala.cgnode)
      }
      
      val afterAInterp = System.currentTimeMillis()      
      OutPrint.printResult(afterAInterp-beforeAInterp)
      
    } else if ("inter-hybrid" == action.get) { //inter-procedural hybrid top-down & bottom-up analysis

      ConfigManager.isHybrid = true
      
      beforeAInterp = System.currentTimeMillis()
      
      contexts.foreach{
        case context =>
          GlobalHeap.cg   = context.wala.cg
          GlobalHeap.heap = context.wala.heap 
          
          if(ConfigManager.hasSummarizedDimensions)
            WALA.createHeapMap(context.wala.cg) 
            
          val map = Hybrid.startSolving(context)
      }
      
      val afterAInterp = System.currentTimeMillis()
      OutPrint.printResult(afterAInterp-beforeAInterp)
      
    } else if ("inter-topdown" == action.get) { //inter-procedural top-down analysis
      ifVerbose{
        Log.println("starting inter-procedural top-down analysis")
      }
      
      contexts.foreach{case context =>
        GlobalHeap.cg   = context.wala.cg
        GlobalHeap.heap = context.wala.heap

        //the globalHeapLabel option must set true for top-down analysis
        ConfigManager.globalHeapLabel = true

        if(ConfigManager.hasSummarizedDimensions) {
          WALA.createHeapMap(context.wala.cg)  
          
          ifDebug {
            Log.println("completed reachable heap map creation using escape analysis")
          }
        }
        
        beforeAInterp = System.currentTimeMillis()
        AInterp.solveMethod(context)(new AInterpConfig(true, false))

        val afterAInterp = System.currentTimeMillis()        
        OutPrint.printResult(afterAInterp-beforeAInterp)
      }
    } else if ("intra-topdown" == action.get) {
      ConfigManager.isInterProc = false
      ConfigManager.isTopDown = true
      contexts.foreach{case context =>
//        GlobalHeap.cg   = context.wala.cg
//        GlobalHeap.heap = context.wala.heap
        IntraProc.solve(context.wala.cg)(new AInterpConfig(false, false))
      }
    } else if ("cg-analysis" == action.get) {
      contexts.foreach{case context =>
        GlobalHeap.cg   = context.wala.cg
        GlobalHeap.heap = context.wala.heap
        CallGraphAnalysis.analyze()
      }
    } else {
      Log.println("invalid action.  please choose inter-topdown, inter-bottomup, inter-hybrid, intra-topdown, intra-bottomup, ast, and cg-analysis")
    }
  }
  
}

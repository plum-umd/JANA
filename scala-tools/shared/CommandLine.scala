import Util._

import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector
import com.ibm.wala.ipa.callgraph.propagation.ReceiverTypeContextSelector

import collection.JavaConversions._
import edu.illinois.wala.Facade._

import com.ibm.wala.ipa.callgraph.propagation._
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys

//import scalaz.Scalaz._

import Core._
import Annotation._
import AnnotationSerialize._
import Specification._

import java.io._

object CommandLine {

  def loadEntries(
    depsDir: String,
    sig: String,
    exclusions: String,
    localSig: String):
      List[InterpContext] = {
    implicit val config = CommandConfig.config(depsDir, sig, exclusions)
    
    val before = System.currentTimeMillis()

    val pa = (ConfigManager.context, ConfigManager.obj_rep) match {
      case (None, "ALLOCATION") | (Some("0-CFA"), "ALLOCATION") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() 
      case (None, "SMUSH_STRING") | (Some("0-CFA"), "SMUSH_STRING") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() 
        {
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_STRINGS)
        } 
      case (None, "TYPE") | (Some("0-CFA"), "TYPE") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() 
        {
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys.NONE)
        }
      case (Some("1-CFA"), "ALLOCATION") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() {
          override def cs = new nCFAContextSelector(1, new ContextInsensitiveSelector());
        }
      case (Some("1-CFA"), "SMUSH_STRING") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() 
        {
          override def cs = new nCFAContextSelector(1, new ContextInsensitiveSelector());
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_STRINGS)
        }
      case (Some("1-CFA"), "TYPE") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() 
        {
          override def cs = new nCFAContextSelector(1, new ContextInsensitiveSelector());
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys.NONE)
        }        
      case (Some("1-TYPE"), "ALLOCATION") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() {
          override def cs = new ReceiverTypeContextSelector();      
        }   
      case (Some("1-TYPE"), "SMUSH_STRING") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() {
          override def cs = new ReceiverTypeContextSelector();
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_STRINGS)
        }
      case (Some("1-TYPE"), "TYPE") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() {
          override def cs = new ReceiverTypeContextSelector();
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys.NONE)
        }
      case (None, "SMUSH_MANY") =>
        new edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder() 
        {
          override def instanceKeys = new ZeroXInstanceKeys(_options, _cha, theContextInterpreter, ZeroXInstanceKeys.ALLOCATIONS|ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_THROWABLES | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS | ZeroXInstanceKeys.SMUSH_MANY)
        }            
      case (_, _) => throw NotImplemented("Unsupported points-to analysis configuration")
    }
    
    val after = System.currentTimeMillis()
    
    val cg = pa.cg
    
//    val out_1 = new PrintWriter(new BufferedWriter(new FileWriter("./files/" + ConfigManager.outName + "/" +ConfigManager.outName + ".txt", true)));
//    out_1.write("\n")
//    out_1.write(depsDir + "\t")
//    out_1.write(sig + "\t")
//    out_1.close()    
    printConfig(depsDir, sig, after-before, cg.getNumberOfNodes, pa.heap.getNumberOfNodes)
//    val out_2 = new PrintWriter(new BufferedWriter(new FileWriter("./files/" + ConfigManager.outName + "/" +ConfigManager.outName + ".txt", true)));
//    out_2.write((after-before) + "\t")
//    out_2.write(cg.getNumberOfNodes + "\t")
//    out_2.write(pa.heap.getNumberOfNodes + "\t")
//    out_2.close()
    
    GlobalHeap.heap = pa.heap
    GlobalHeap.cg = cg
    if(localSig.equals("default")) {
      cg.getEntrypointNodes.toList.map{
        cgnode => new InterpContext(pa, cgnode)
      }
    } else {
      var l = List[InterpContext]()
      cg.iterator().toList.foreach{
        cgnode => 
          if(cgnode.getMethod.getSignature.equals(localSig)) {
            if(cgnode.getIR != null)
              l = new InterpContext(pa, cgnode) :: l
            else
              println("IR of " + localSig + " is not available")
          }
      }
      l
    }

  }

  import java.io.File
  import org.clapper.argot._
  import ArgotConverters._

  def loadFromCommandline(
    argv: scala.Array[String],
    toolName: String,
    toolDescription: String
  ): List[InterpContext] = {
    processArgs(argv, toolName, toolDescription)._2
  }

  /* Parse command line arguments and load the specified classes, and
   find all the specified methods. */
  def processArgs(
    argv: scala.Array[String],
    toolName: String,
    toolDescription: String
  ): (Option[String], List[InterpContext]) = {

    val parser = new ArgotParser(
      programName=toolName,
      compactUsage=true,
      preUsage=Some(toolName + ": " + toolDescription + "\nSOUCIS Team")
    )

    def dirconverter (s : String, opt: CommandLineArgument[File]) : File = {
      val f = new File(s)
      if (! f.exists) {
        Log.println("ERROR: Directory \"" + f.toString() + "\" not found.")
        System.exit(1)
      } else if (! f.isDirectory()) {
        Log.println("ERROR: File \"" + f.toString() + "\" is not a directory.")
        System.exit(1)
        }
      f
    }

    val usage = parser.flag[Any](
      namesOn=List("h", "?", "help"),
      description="Show this usage information."
    ) { (_,_) =>
      Log.println(parser.usageString())
      System.exit(0) }

    val actionOpt = parser.option[String](
      names=List("a", "action"),
      valueName="action",
      description="Type of abstract interpretation action to take: intra-bottomup (or ainterp), bounds, inter-bottomup (or interproc), intra-topdown, interbounds, or ast."
    )
    
    val hybrid = parser.option[String](
      names=List("h", "hybrid"),
      valueName="hybrid",
      description="Hybrid top-down bottom-up analysis."
    )
    
    val inline = parser.option[String](
      names=List("i", "inline"),
      valueName="inline",
      description="inline option."
    )
    
    val entriesOpts = parser.multiParameter[String](
      valueName="entry-method",
      description="Method to be analyzed in java locator format. In CLASS.METHOD(ARGS)RET format.",
      optional=true
    )
    val depdirsOpts = parser.multiOption[File](
      names=List("D", "dep"),
      valueName="path",
      description="Add a directory to locations of where .class files are retrieved. Default is [.] (current directory only)"
    ){ (x,y) => dirconverter(x,y) }
    val scopesOpts = parser.multiOption[String](
      names=List("S", "scope"),
      valueName="locator-pattern",
      description="Add the methods specified by regex pattern to the scope of the analysis." +
        "Default is [.*] (everything, which is NOT recommended)."
    )
    val spec = parser.option[String](
      names=List("s", "spec"),
      valueName="specification-file",
      description="Include annotations/specifications from the specified file."
    )
    val annotsOpts = parser.multiOption[String](
      names=List("A", "annot"),
      valueName="path",
      description="Include byte-code annotations from the specified file."
    )
    val exclusions = parser.option[String](
      names=List("E", "exclusion"),
      valueName="exclusion-file",
      description="Exclusion file for wala"
    )
    val csPolicy = parser.option[String](
      names=List("CS", "context"),
      valueName="cs-policy",
      description="Context-sensitive policy for wala call graph construction and points-to analysis"
    )
    val widenPolicy = parser.option[String](
      names=List("WIDEN", "widen"),
      valueName="widen-policy",
      description="Widen policy for numerical analysis"
    )
//    val packingPolicy = parser.option[String](
//      names=List("PACK", "packing"),
//      valueName="packing-policy",
//      description="Packing policy for numerical analysis"
//    )
    val outName = parser.option[String](
      names=List("OUT", "out"),
      valueName="out-name",
      description="Output file name"
    )    
    val index = parser.option[String](
      names=List("INDEX", "index"),
      valueName="index",
      description="Output file index"
    )
    val obj_repPolicy = parser.option[String](
      names=List("OBJ", "obj_rep"),
      valueName="object-representation-policy",
      description="Object represenation policy for wala call graph construction and points-to analysis"
    )
    val apPolicy = parser.option[String](
      names=List("AP", "ap"),
      valueName="access-path-policy",
      description="access path"
    )
    val sum_dimPolicy = parser.option[String](
      names=List("SUMDIM", "sum_dim"),
      valueName="sum-dim-policy",
      description="summarized dimension"
    )
    val joinPolicy = parser.option[String](
      names=List("JOIN", "join"),
      valueName="join-policy",
      description="join policy for numerical analysis"
    )   
    val clients = parser.option[String](
      names=List("CLIENT", "client"),
      valueName="clients",
      description="analysis clients"
    )
    val domain = parser.option[String](
      names=List("DOMAIN", "domain"),
      valueName="domain",
      description="domain for numerical analysis"
    )    
    val baseDomain = parser.option[String](
      names=List("BASE", "basedomain"),
      valueName="base domain",
      description="base domain for numerical analysis"
    )
    val widenAlg = parser.option[String](
      names=List("WIDEN_ALG", "widen_alg"),
      valueName="widen algorithm",
      description="widen algorithm for numerical analysis"
    )
    val localSig = parser.option[String](
      names=List("LOCAL", "local"),
      valueName="local-sig",
      description="The local method signature to perform the abstract interpretation"
    )
    val listMode = parser.flag[Boolean](
      namesOn=List("l", "list-entries"),
      description="List available entries."
    )
    val showMode = parser.flag[Boolean](
      namesOn=List("c", "show-code"),
      description="List available entries and their IR."
    )
    val debugMode = parser.flag[Boolean](
      namesOn=List("d", "debug"),
      description="Debug mode; print additional information."
    ){case (b, _) =>
        Core.debug = b
        b
    }
    val progressMode = parser.flag[Boolean](
      namesOn=List("p", "progress"),
      description="Print progress during computations."
    ){case (b, _) =>
        Core.progress = b
        b
    }
    val verboseMode = parser.flag[Boolean](
      namesOn=List("V", "verbose"),
      description="Verbose mode; print additional information."
    ){case (b, _) =>
        Core.verbose = b
        b
    }
    val deletePhisMode = parser.flag[Boolean](
      namesOn=List("no-clean-phis"),
      description="Keep un-needed variables after phi-assignment."
    ){case (b, _) =>
        Core.interpCleanPhis = ! b
        b
    }

    try {
      parser.parse(argv)
    } catch {
      case e: ArgotUsageException => Log.println(e.message)
        System.exit(1)
    }

    Core.ifDebug{
      showDebugInfo
    }

    val entries = entriesOpts.value.toList

    val exclusionsStr = exclusions.value match {
      case Some(s) => scala.io.Source.fromFile(s).mkString.filter(_ >= ' ')
      case None => "java/awt/.* javax/.* com/sun/.* com/apple/.* sun/.* apple/.*  org/eclipse/.*  apache/.*"
    }

    csPolicy.value match {
      case Some("1-CFA") | Some("1-TYPE") => ConfigManager.context = csPolicy.value
      case None | Some("0-CFA") =>
      case _ => throw NotImplemented("context sensitivity policy not supported")
    }
    
    actionOpt.value match {
      case Some("inter-topdown") => ConfigManager.isTopDown = true
      case Some("inter-bottomup") => ConfigManager.isTopDown = false
      case _ =>
    }
    
    hybrid.value match {
      case Some("true") => ConfigManager.isHybrid = true
      case _ =>
    }
    
    inline.value match {
      case Some("true") => ConfigManager.inline = true
      case _ =>
    }
    
    domain.value match {
      case Some("BOX") | Some("OCT") => ConfigManager.domain = domain.value.get
      case None | Some("POLY") =>
      case _ => throw NotImplemented("domain not supported")
    }
    
    baseDomain.value match {
      case None =>
      case Some("BOX") => ConfigManager.base_domain = baseDomain.value
      case _ => throw NotImplemented("base domain not supported")
    }
    
    outName.value match {
      case None =>
      case Some(s: String) => ConfigManager.setOutName(s)
      case _ => throw NotImplemented("out name not supported")
    }
    
    index.value match {
      case None =>
      case Some(s: String) => ConfigManager.setIndex(s)
      case _ => throw NotImplemented("out name not supported")
    }
    
    widenAlg.value match {
      case None =>
      case Some("BHZ03_H79_H79") | Some("BHZ03_BHRZ03_BHRZ03") => ConfigManager.widen_alg = widenAlg.value
      case _ => throw NotImplemented("widen algorithm not supported")
    }
    
//    packingPolicy.value match {
//      case None | Some("None") =>
//      case Some("SYNTACTIC_BLOCK") => ConfigManager.packing = (Some("SYNTACTIC_BLOCK"), None)
//      case Some("SYNTACTIC_METHOD") => ConfigManager.packing = (Some("SYNTACTIC_METHOD"), None)
//      case Some(s) => 
//        try {
//          ConfigManager.packing = (Some("RANDOM"), Some(s.toInt))
//        } catch {
//          case e: Exception => throw NotImplemented("packing policy not supported")
//        }    
//    }
    
    widenPolicy.value match {
      case Some(s) => 
        try {
          ConfigManager.widen = s.toInt
        } catch {
          case e: Exception => throw NotImplemented("widen policy not supported")
        }
      case None =>
    }
    
    obj_repPolicy.value match {
      case Some("ALLOCATION") | Some("SMUSH_STRING") | Some("SMUSH_MANY") | Some("TYPE")
        => ConfigManager.obj_rep = obj_repPolicy.value.get      
      case None => 
      case _ => throw NotImplemented("object representation policy not supported")
    }
    
    joinPolicy.value match {
      case Some("PAR_RET") => ConfigManager.joinPolicy = Some(Label.isParRet)
      case None | Some("ALWAYS") => 
      case _ => throw NotImplemented("join policy not supported")
    }
    
    apPolicy.value match {
      case None | Some("true") =>
      case Some("false") => ConfigManager.setHasAccessPath(false)
      case _ => throw NotImplemented("ap policy not supported")
    }
    
    sum_dimPolicy.value match {
      case None | Some("true") =>
      case Some("false") => ConfigManager.setHasSummarizedDimensions(false)
      case _ => throw NotImplemented("sum-dim policy not supported")
    }
    
    clients.value match {
      case Some("ALL") | None =>
        ConfigManager.checkArrayBound = true
        ConfigManager.checkDivideByZero = true
        ConfigManager.checkFieldAccess = true
        ConfigManager.checkReturn = false
      case _ => throw NotImplemented("client option not supported")
    }
    
    val localStr = localSig.value match {
      case Some(s) => s
      case None => "default"
    }

    val annotsFiles = annotsOpts.value.toList
    val annots: Annots = annotsFiles.foldLeft(emptyAnnots) {
      case (accum, filename) =>
        val content = scala.io.Source.fromFile(filename).mkString
        val annots: Annots = AnnotationSerialize.deserialize(content)
        Util.mapsMergeWith(accum, annots, {a1: MethodAnnots => a2: MethodAnnots =>
          Util.mapsMergeWith(a1, a2, {b1: List[Annotation] => b2: List[Annotation] => b1 ++ b2})})
    }

    // Specifications parsed from provided filename
    spec.value match {
      case Some(fn) =>
        val str = scala.io.Source.fromFile(fn).mkString
        Log.println("parsed specifications from file: " + fn)
        Specification.putSpecs(SpecificationSerialize.deserialize(str))
      case None =>
        Specification.putSpecs(Map[String, Specification]())
    }

    val depdirs = depdirsOpts.value.toList match {
      case List() => List(new File("."))
      case items => items
    }
    val scopes = scopesOpts.value.toList match {
      case List() =>
        Log.println("WARNING: scope of analysis is not restricted (-S option)\n  All of the java runtime will be included.\n  This might take a while to start up.")
        List(".*")
      case items => items
    }

    val contexsts = loadEntries(depdirs.mkString(" "),scopes.mkString(" "),exclusionsStr, localStr)
    val loadedMethods = contexsts.foldLeft(Map[String,InterpContext]()) {
      (m, context) => m + (context.wala.ir.getMethod().getSignature().toString() -> context) }

    def printAvailable(e: String) = {
      Log.println(
        (if (null != e) {"WARNING: Method \"" + e + "\" not found; available are:\n"} else "") +
          tab(loadedMethods.keys.toList.sorted.mkString("\n"))
      )
//      System.exit(if (null != e) 1 else 0)
//      ???
    }

    if (! listMode.value.isEmpty) {
      printAvailable(null)
    } else if (! showMode.value.isEmpty) {
      val methods = if (entries.size != 0) {
        
        entries.filter { k => !loadedMethods.contains(k) }.map{
          case k =>
            printAvailable(k)
        }
        
        entries.filter { k => loadedMethods.contains(k) }.map{
          case k =>
            (k -> loadedMethods(k))
        }
      } else loadedMethods
      Log.println(methods.map{case (k, v) => s"$k\n" + tab(v.wala.ir)}.mkString("\n\n"))
      System.exit(0)
    }

    entries.filter { k => !loadedMethods.contains(k) }.map { e =>
      printAvailable(e)
    }
    
    val loadedEntries = entries.filter { k => loadedMethods.contains(k) }.map { e =>
      loadedMethods.get(e) match {
        case Some(n) => n
      }
    }

    // Return pair: (action, contexts)
    (actionOpt.value, loadedEntries.map{e =>
      e.copy(
        eval = new EvalContext().copy(annots = annots))}
    )
  }
  
  def printConfig(depsDir : String, sig : String, ptsTime: Long, cgNodeNum: Int, ptsNodeNum: Int) : Unit = {
    
    val out = new PrintWriter(new BufferedWriter(new FileWriter("./files/" + ConfigManager.outName + "/" +ConfigManager.outName + ".txt", true)));
    out.write("\n")
    out.write(depsDir + "\t")
    out.write(sig + "\t")
    
    if(ConfigManager.isHybrid) {
      out.write("TD+BU\t")
    } else if(ConfigManager.isTopDown) {
      out.write("TD\t")
    } else {
      out.write("BU\t")
    }
    
    if(ConfigManager.hasAccessPath & ConfigManager.hasSummarizedDimensions) {
      out.write("AP+SO")
    } else {
      if(ConfigManager.hasAccessPath) {
        out.write("AP")
      } else {
        out.write("SO")
      }
    }
    
    ConfigManager.inline match {
      case true => out.write("+INLINE\t")
      case false => out.write("\t")
    }
    
    ConfigManager.context match {
      case Some(context) => out.write(context + "\t")
      case None => out.write("CI\t")
    }
       
    out.write(ConfigManager.obj_rep + "\t")
        
    out.write(ConfigManager.domain + "\t")
    
    out.write(ptsTime + "\t")
    out.write(cgNodeNum + "\t")
    out.write(ptsNodeNum + "\t")
        
    out.write("\t")
    
    out.close()
  }
}

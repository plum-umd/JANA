package edu.umd.soucis.bounds;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;

import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.viz.DotUtil;

import static com.ibm.wala.ipa.callgraph.impl.Util.makeVanillaZeroOneCFABuilder;

public class CliMain {
  public static void main(String  []args) throws IOException {
    String                inputFile = args[0];
    String                exclusionFile = args[1];
    String                methodDump = args[2];
    ClassHierarchy        ch = null;
    AnalysisScope         as = null;
    File                  exfile = new File(exclusionFile);
    
    try {
      as = AnalysisScopeReader.readJavaScope( inputFile, 
                                              exfile, 
                                              CliMain.class.getClassLoader());
    } catch(IOException e) {
      System.out.println("Could not open file");
      return;
    }

    try {
      ch = ClassHierarchy.make(as);
    } catch(ClassHierarchyException e) {
      System.out.println("class hierarchy exception "+e.toString());
      return;
    }

    List<Entrypoint>  eps = new LinkedList<Entrypoint>();
    MethodReference mr = StringStuff.makeMethodReference(methodDump);
    IMethod m = ch.resolveMethod(mr);
    eps.add(new DefaultEntrypoint(mr, ch));

    AnalysisOptions       o = new AnalysisOptions(as, eps);
    AnalysisCache         ac = new AnalysisCache();
    CallGraphBuilder      bu = makeVanillaZeroOneCFABuilder(o, ac, ch, as);
    CallGraph             cg = null;

    try {
      cg = bu.makeCallGraph(o, null);
    } catch(CancelException e) {
      System.out.println("exception");
      return;
    }
    
    if(cg != null) {
      //let's also iterate over the class hierarchy 
      IClassHierarchy ich = cg.getClassHierarchy(); 
      if(ich != null) {
        for(IClass cla : ich) {
          if(cla.getName().toString().equals("LSwapList")) {
            System.out.println("found SwapList class");
            Collection<Annotation>  annot = cla.getAnnotations();
            System.out.printf("%d\n", annot.size());
          }
        }
      } else {
        System.out.println("clas hierarchy null?!");
      }
      IR pir = ac.getSSACache().findOrCreateIR( m, 
                                                Everywhere.EVERYWHERE, 
                                                o.getSSAOptions());
      SymbolTable st = pir.getSymbolTable();
      //System.out.println(pir.toString());
      SSAInstruction  []instructions = pir.getInstructions();
      SSACFG          cfg = pir.getControlFlowGraph();
      /*System.out.printf("define void @foo() {\n");
      BasicBlock  entry = cfg.entry();
      int         first = entry.getFirstInstructionIndex();
      int         last = entry.getLastInstructionIndex();
      System.out.printf("%s\n", entry.toString());
      System.out.printf("}\n");*/
      MyVisitor v = new MyVisitor(st);
      for(SSAInstruction I : instructions) {
        //sometimes, there is a null instruction in the array
        if(I != null) {
          //I.visit(v);
        }
      }
      /*try {
        DotUtil.dotify(cg, null, "cg.dot", "cg.pdf", "/usr/bin/dot");
      } catch (WalaException e) {
        e.printStackTrace();
        return;
      }
      
      mr = StringStuff.makeMethodReference("Test.B(I)I");
      if(mr != null) {
        m = ch.resolveMethod(mr);
      
        if(m != null) {
          IR pir = ac.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, o.getSSAOptions());
          System.out.println(pir.toString());
        } else {
          System.out.println("couldn't look up method");
        }
      }*/ 
    }

    return;
  }
}

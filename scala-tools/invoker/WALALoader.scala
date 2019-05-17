import org.apache.commons.io.FilenameUtils
import collection.JavaConversions._
import java.io.File

import com.ibm.wala.ipa.cha.ClassHierarchy
import com.ibm.wala.ipa.cha.IClassHierarchy
import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.ipa.callgraph.AnalysisOptions
import com.ibm.wala.ipa.callgraph.AnalysisScope
import com.ibm.wala.types.ClassLoaderReference
import java.util.jar.JarFile
import java.io.IOException
import com.ibm.wala.ipa.cha.ClassHierarchyException

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.ipa.callgraph.AnalysisCache
import com.ibm.wala.ipa.callgraph.AnalysisOptions
import com.ibm.wala.ipa.callgraph.AnalysisScope
import com.ibm.wala.ipa.callgraph.impl.Everywhere
import com.ibm.wala.ipa.cha.ClassHierarchy
import com.ibm.wala.ipa.cha.ClassHierarchyException
import com.ibm.wala.util.config.AnalysisScopeReader
import com.ibm.wala.util.ref.ReferenceCleanser
import com.ibm.wala.classLoader._


import com.ibm.wala.util.graph.Graph
import com.ibm.wala.util.WalaException

import scala.collection.mutable.{Map=>MMap}

//import com.ibm.wala.examples.drivers.PDFTypeHierarchy

//class Dec[T] extends NodeDecorator[T] {
//  def getLabel(n: T): String = "label"
//}

import scala.sys.process._

object WALALoader {
  def scopeOfMethod(c: IMethod): String = {
    scopeOfClass(c.getDeclaringClass())
  }

  def scopeOfClass(c: IClass): String = {
    val temp = c.getName().toString()
    .replaceAll("^L", "")
    .replaceAll("/", ".")
    temp + ".*"
  }

  def loadFromFiles(fl: Seq[File]): List[Method] = {
    val scope = AnalysisScope.createJavaAnalysisScope()
    scope.addToScope(ClassLoaderReference.Primordial, new JarFile("../../environment/rt.jar"))

    fl.foreach { file =>
      val ext = FilenameUtils.getExtension(file.getPath())
      ext match {
        case "class" =>
          scope.addClassFileToScope(ClassLoaderReference.Application, new File(file.getPath))
        case "jar" =>
          scope.addToScope(ClassLoaderReference.Application, new JarFile(file.getPath))
        case _ => println("!!! I don't know how to load \"" + file.toString + "\"")
      }
    }

    val classMap: MMap[String, File] = MMap[String, File]()

    val modules = scope.getModules(scope.getApplicationLoader())
    modules.foreach{ m =>
      m match {
        case cm: ClassFileModule =>
          //println("module " + cm + " has class " + cm.getClassName())
          classMap += ("L" + cm.getClassName() -> cm.getFile())
        case _ => ()
      }

    }
    //println("modules = " + modules.toString)

    val cha = ClassHierarchy.make(scope);

    val cache = new AnalysisCache()
    ReferenceCleanser.registerCache(cache)
    val options = new AnalysisOptions()

    cha.toList.flatMap{cl =>
      val loader = cl.getClassLoader()
      //if (loader.isApplicationLoader()) {
      if (loader.getReference().equals(ClassLoaderReference.Application)) {
        val source = cl match {
          case bc: BytecodeClass[_] =>
            //println("bc(bytecode) = " + bc.toString)
            //println("container = " + bc.getContainer())
            val bcLoader    = bc.getClassLoader()
            val source      = bc.getSourceFileName()
            val classSource = bcLoader.getSourceFileName(bc)
            //println("source location = " + classSource)
            //println("source = " + source)
            source
          case _ =>
            //println("bc(???) = " + cl.toString)
            null
        }

        //println("cl = " + cl.getName())

        cl.getAllMethods().toList.flatMap{m =>
          //wipeSoftCaches()
          val ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions())

          classMap.get(cl.getName().toString()) match {
            case None =>
              //println(m.getName().toString() + " is ???")
              //Method(m, ir, Location.Unknown())
              List()
            case Some(f) =>
              if (null != ir && null != m) {
                //println("adding " + m + " of " + loader + " / " + cl)
                List(Method(m, ir, Location.ClassInFile(f)))
              } else {
                List()
              }

          }
        }
      } else {
        List()
      }
    }

  }

  def loadFromClass(d: File): List[Method] = {
    val scope = AnalysisScope.createJavaAnalysisScope()
    scope.addToScope(ClassLoaderReference.Primordial, new JarFile("../../environment/rt.jar"))
    scope.addClassFileToScope(ClassLoaderReference.Application, new File(d.getPath))
    val cha = ClassHierarchy.make(scope);

    val cache = new AnalysisCache()
    val ssaCache = cache.getSSACache()
    ReferenceCleanser.registerCache(cache)
    val options = new AnalysisOptions()

    cha.toList.flatMap{cl =>
      if (cl.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
        cl.getDeclaredMethods().toList.flatMap{m =>
          //wipeSoftCaches()
          val ir = ssaCache.findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions())
          if (null != ir && null != m) {
            List(Method(m, ir, Location.ClassInFile(d)))
          } else {
            List()
          }
        }
      } else {
        List()
      }
    }
  }

  def loadFromJar(d: File): List[Method] = {
    // http://stackoverflow.com/questions/976388/how-to-get-method-signatures-from-a-jar-file
    val scope = AnalysisScope.createJavaAnalysisScope()
    scope.addToScope(ClassLoaderReference.Primordial, new JarFile("../../environment/rt.jar"))
    scope.addToScope(ClassLoaderReference.Application, new JarFile(d.getPath))
    val cha = ClassHierarchy.make(scope);

    val cache = new AnalysisCache()
    ReferenceCleanser.registerCache(cache)
    val options = new AnalysisOptions()

    cha.toList.flatMap{cl =>
      if (cl.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
        cl.getAllMethods().toList.map{m =>
          //m.getSignature()
          val ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions())
          Method(m, ir, Location.ClassInJar(d))
        }
      } else {
        List()
      }
    }
  }

}
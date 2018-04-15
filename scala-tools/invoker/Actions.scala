import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
import javafx.event._
import javafx.scene.input.DragEvent
import java.nio.file.Path
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import javafx.stage.Window

import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._
import scalafx.scene.image._

import collection.JavaConversions._

import scalafx.beans.property._
import java.lang.management._

import Util._

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.FileOutputStream
import java.io.FileDescriptor

import java.io.{PrintWriter,File}
import java.nio.file.{Files,Path,FileSystems}
import java.nio.file.attribute.{PosixFilePermissions,PosixFilePermission,FileAttribute}
import java.nio.file.FileAlreadyExistsException

import scala.sys.process._

import Core._

case class AInterpParams(
  val sig  : String,
  val scope: String,
  val deps : String,
  val exc  : String,
  val entry: String
)

object Actions {
  def generateAnnotsFile(tmp: Path, taints: List[Taint]): Path = {
    val targetPath = tmp.resolve("taints")
    val taintWriter = new PrintWriter(targetPath.toFile)
    taints.foreach{ taint =>
      taintWriter.write(taint.m.method.getSignature.toString)
      taintWriter.write(", ")
      //taintWriter.write(taint.lineNum.toString)
      //taintWriter.write(" ")
      taintWriter.write(taint.defNum.toString)
      taintWriter.write(", ")
      taintWriter.write(taint.tag)
      taintWriter.write("\n")
    }
    taintWriter.close
    println("wrote " + targetPath.toString)
    targetPath
  }

  def ainterpCommon[A](opts: AInterpParams)(f: InterpContext => A): A = {
    val contexts = CommandLine.loadEntries(opts.deps, opts.scope, opts.exc, "", "default")
    val loadedMethods = contexts.foldLeft(Map[String,InterpContext]()) {
      (m, context) =>
      m + (context.wala.ir.getMethod().getSignature().toString() -> context)
    }
    loadedMethods.get(opts.sig) match {
      case Some(context) => f(context)
      case None => throw new Exception(s"cannot find $opts.sig")
    }
  }

  def ainterpCommonAll[A](opts: AInterpParams)(f: (InterpContext, List[InterpContext]) => A): A = {
    val contexts = CommandLine.loadEntries(opts.deps, opts.scope, opts.exc, "", "default")
    val loadedMethods = contexts.foldLeft(Map[String,InterpContext]()) {
      (m, context) =>
      m + (context.wala.ir.getMethod().getSignature().toString() -> context)
    }
    loadedMethods.get(opts.sig) match {
      case Some(context) => f(context, loadedMethods.values.toList)
      case None => throw new Exception(s"cannot find $opts.sig")
    }
  }

  def taint(tmp: Path, taints: List[Taint], opts:AInterpParams): Util.TaintResult = {
    val annots = generateAnnotsFile(tmp, taints)
    val output = annots.resolveSibling("output.json")

    val entryMethod = opts.entry
    val classPath = opts.deps

    val p = Process(Seq(
      Dirs.javaExe,
      "-classpath",  "../../taint/target/taint-jar-with-dependencies.jar", "main.java.soucis.taint.TaintAnalysis",
      "-c", classPath.toString,
      "-e", entryMethod.toString,
      "-a", annots.toString,
      "-o", output.toString))

    //println("will run: " + p.toString)

    Log.println(p.lines.mkString("\n"))

    Util.parseTaint(Util.slurpFile(output.toString))
  }

  def ainterp(opts:AInterpParams) = {
    ainterpCommon(opts){ context =>
      GlobalHeap.cg   = context.wala.cg
      GlobalHeap.heap = context.wala.heap
      val ret_projected = AInterp.solveMethod(context)
      Log.println("Final return is\n" + tab(ret_projected))
    }
  }

  def bounds(opts:AInterpParams) = {
    ainterpCommon(opts) { context =>
      GlobalHeap.cg   = context.wala.cg
      GlobalHeap.heap = context.wala.heap
      val bounds = Loops.loopSummary(context)
      val polynomialbounds = bounds.map{ case (h, blist) =>
        val bset = blist.map{ b =>
          PolynomialUtil.construct(b, context).toString
        }.toSet[String]

        (h.toString, bset)
      }

      polynomialbounds.keys.toList.sorted.foreach { k =>
        Log.println(k)
        polynomialbounds(k).toList.sorted.foreach { p =>
          Log.println(p)
        }
      }
    }
  }

  def interbounds(tmp: Path, opts: AInterpParams): File = {
    ainterpCommonAll(opts) { (context, contexts) =>
      val perms = PosixFilePermissions.fromString("rwxr-x---")
      val attr : FileAttribute[java.util.Set[PosixFilePermission]] =
        PosixFilePermissions.asFileAttribute(perms)
      val path = //FileSystems.getDefault().getPath("tmp")
        tmp
      Files.createDirectories(path, attr)
      val dir = Files.createTempDirectory(path, "out_", attr);
      val htmlFile = new File(dir.toFile(), "index.html")
      //val scriptFile = new File(dir.toFile(), "mksvg")
      val htmlWriter = new PrintWriter(htmlFile)
      //val scriptWriter = new PrintWriter(scriptFile)

      htmlWriter.write(FunctionBounds.htmlHeader());

      //contexts.foreach{case context =>
        GlobalHeap.cg   = context.wala.cg
        GlobalHeap.heap = context.wala.heap

        var oldDebug = Core.debug
        Core.debug = false
        InterProc.startSolving(context.wala.cgnode)
        Core.debug = oldDebug

        //val bounds = Loops.interprocBounds(context.wala.cgnode, dir, htmlWriter, scriptWriter)
        val bounds = Loops.interprocBounds(context.wala.cgnode, dir, htmlWriter)
        Log.println(bounds)
        Log.println(PolynomialUtil.construct(bounds, context))
      //}
      htmlWriter.write(FunctionBounds.htmlFooter());
      htmlWriter.close()
      //scriptWriter.close()
      Log.printlnErr("interbounds output: "+dir.toFile().getName())

      //val _ = Process(Seq("sh", "mksvg"), dir.toFile().getAbsoluteFile()).lines

      htmlFile.getAbsoluteFile()

    }
  }

  def interproc(opts: AInterpParams) = {
    ainterpCommonAll(opts){ (context, contexts) =>
      contexts.foreach{case context =>
        GlobalHeap.cg   = context.wala.cg
        GlobalHeap.heap = context.wala.heap
        val map = InterProc.startSolving(context.wala.cgnode)
        Log.println(map)
      }
    }
  }


}
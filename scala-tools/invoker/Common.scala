import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.graph.Graph
import com.ibm.wala.viz.DotUtil
import com.ibm.wala.viz.NodeDecorator

import scalafx.scene._
import scalafx.beans.property._

import scalafx.event.subscriptions.Subscription

import java.io.File
import java.nio.file.{Files,Path,FileSystems}

import scala.sys.process._

object Common {
  val taintStyleMapSingle = Map[String, String](
    "taint"  -> "tainted-row",
    "secret" -> "secret-row"
  )
  val taintStyleMap = Map[Set[String], String](
    Set("taint", "secret") -> "danger-row",
    Set("taint")           -> "tainted-row",
    Set("secret")          -> "secret-row"
  )

  def setClassViaSet[A,B](
    n: Node,
    os: ObjectProperty[Set[A]],
    m: Map[Set[B], String],
    f: A => B
  ): Subscription = {

    val allClasses = m.values

    allClasses.foreach{cl => n.styleClass -= cl}
    os.onInvalidate {
      allClasses.foreach{cl => n.styleClass -= cl}
    }

    if (null != os.get) {
      m.get(os.get.map(f)) match {
        case Some(c) => n.styleClass += c
        case None => ()
      }
    }

    os.onChange { (_, _, newSet) =>
      allClasses.foreach{cl => n.styleClass - cl}
      if (null != newSet) {
        m.get(newSet.map(f)) match {
          case Some(c) => n.styleClass += c
          case None => ()
        }
      } else {

      }
    }
  }

  def setClassViaValue[A,B](
    n: Node,
    os: ObjectProperty[A],
    m: Map[B, String],
    f: A => B
  ): Subscription = {
    val allClasses = m.values

    allClasses.foreach{cl => n.styleClass -= cl}
    os.onInvalidate {
      allClasses.foreach{cl => n.styleClass -= cl}
    }

    if (null != os.get) {
      m.get(f(os.get)) match {
        case Some(c) => n.styleClass += c
        case None => ()
      }
    }

    os.onChange { (_, _, newSet) =>
      allClasses.foreach{cl => n.styleClass - cl}
      if (null != newSet) {
        m.get(f(newSet)) match {
          case Some(c) => n.styleClass += c
          case None => ()
        }
      } else {

      }
    }
  }

}

abstract class Location
object Location {
  case class Unknown() extends Location
  class File(val file: java.io.File) extends Location {
  }
  case class ClassInFile(val classFile: java.io.File) extends File(classFile) {
  }
  case class ClassInJar (val jarFile: java.io.File)  extends File(jarFile) {
  }
}

case class Method(val method: IMethod, val ir: WALA.IR, val location: Location) {
  var dot: File = null

  override def toString = {
    method.getSelector().getName().toString + ": " +
    method.getDescriptor().toString
  }

  val sig = method.getSignature().toString()
    .replace("/", ".")

  def genDot(tmp: Path): Unit = {
    dot = if (null == ir) {
      null
    } else {
      val targetDot = tmp.resolve(sig + ".dt")
      val targetSvg = tmp.resolve(sig + ".svg")
      val targetPdf = tmp.resolve(sig + ".pdf")
      try {
        val cfg = ir.getControlFlowGraph().asInstanceOf[Graph[WALA.ICFGNode]]
        val dec = null
        //val dec = new NodeDecorator[WALA.ICFGNode]() {
        //def getLabel(n: WALA.ICFGNode): String = "label"
        //}
        //DotUtil.dotify(cfg, dec, targetDot.toString, targetPdf.toString, WALALoader.dotExe)
        DotUtil.writeDotFile(cfg, dec, null, targetDot.toString)
        val p = Process(Seq(Dirs.dotExe, "-Tsvg", "-o", targetSvg.toString, "-v", targetDot.toString))
          .run(ProcessLogger(
            {line: String => ()},
            {line: String => ()}
          )
        )
        //println("svg gen exit = " + p.exitValue())
        targetSvg.toFile
      } catch {
        case e: Throwable => e.printStackTrace()
          null
      }
    }
  }
}


case class Taint(val m: Method, val lineNum: Int, val defNum: Int, val tag: String, val codeLine: CodeWALALine) {
  override def toString = "[" + tag + "]"
}


import scalafx.Includes._
import scalafx.event._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.scene.layout._
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import scalafx.scene.input.TransferMode
import scalafx.geometry._
import scalafx.scene.image._
import scalafx.scene.control.SingleSelectionModel
import scalafx.beans.property._
import scalafx.scene.image._
import scalafx.collections._
import scalafx.beans.property._
import scalafx.scene.input._
import scalafx.stage.Stage
import scalafx.delegate.SFXDelegate

import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils

import scala.collection.mutable.{Map=>MMap}
//import javafx.scene.control.{TreeItem=>JTreeItem}

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom
import java.io.File

class ViewTab(val workArea: WorkArea) extends Tab {
  this.delegate.setUserData(this)
}

class ViewOpt
case class ViewOptFile  (f: File)   extends ViewOpt
case class ViewOptMethod(m: Method) extends ViewOpt

class WorkArea(file: String, funs: List[Method]) {
  val taints: ObservableBuffer[Taint] = new ObservableBuffer[Taint]()
  val tmpDir = Dirs.subTmpDir(file)

  override def toString = file

  def addTaint(t: Taint) = {
    taints.setAll(taints.iterator.toList ++ List(t))
  }
  def removeTaint(t: Taint) = {
    taints.setAll(taints.iterator.toList.filter{_ != t})
  }

  val viewPane = new TabPane() {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
  }

  val viewTabs:  MMap[ViewOpt, ViewTab] = MMap[ViewOpt,ViewTab]()
  val sigToView: MMap[String, ViewOptMethod] = MMap[String, ViewOptMethod]()

  def recordMethodView(vopt: ViewOptMethod, tab: ViewTabMethod): Unit = {
    viewTabs += (vopt -> tab)
    viewPane.tabs += tab
    viewPane.selectionModel.get.select(viewTabs(vopt))
    sigToView += (vopt.m.method.getSignature.toString -> vopt)
  }

  def updateSelectedMethod(m: Method): Unit = {
    m.location match {
      case fo: Location.File =>
        Controls.actionBar.scope.update(
          WALALoader.scopeOfClass(m.method.getDeclaringClass())
        )
        Controls.actionBar.dependencies.update(
          fo.file.getParent()
        )
        Controls.actionBar.signature.update(
          m.method.getSignature().toString
        )
      case _ => ()
    }
  }

  viewPane.selectionModel.get.selectedItem.onChange{ (_, _, i) =>
    if (null != i) {
      i.getUserData() match {
        case vtm: ViewTabMethod => updateSelectedMethod(vtm.m)
        case _ => ()
      }
    } else {
    }
  }

  val filesPane = new FileTreeView(this, new File(file))
  val filesTab = new TitledPane() {
    vgrow = Priority.ALWAYS
    text = "Files"
    content = filesPane
  }

  val funsPane = new MethodTreeView(this, new Location.File(new File(file)), funs)

  val funsTab = new TitledPane() {
    text = "Functions"
    vgrow = Priority.ALWAYS
    content = funsPane
  }

  val indexPane = new VBox() {
    hgrow = Priority.NEVER
    vgrow = Priority.ALWAYS
    children.addAll(filesTab, funsTab)
  }

  val root = new SplitPane() {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    items.addAll(indexPane, viewPane)
    setDividerPosition(0, 0.20)
    SplitPane.setResizableWithParent(indexPane, false);
  }

  val selectedMethod = funsPane.selectedMethod
  val selectedFile   = filesPane.selectedFile

  def addTaints(ts: List[(String, Int)], tag: String): Unit = {
    ts.foreach{ case (sig, line) =>
      sigToView.get(sig) match {
        case Some(vopt) =>
          val tab = viewTabs(vopt)
          tab.asInstanceOf[ViewTabMethod].taintLine(line, tag)
        case None => println("don't have " + sig + " open to highlight")
      }
    }
  }

  def removeTaints(ts: List[(String, Int)], tag: String): Unit = {
    ts.foreach{ case (sig, line) =>
      sigToView.get(sig) match {
        case Some(vopt) =>
          val tab = viewTabs(vopt)
          tab.asInstanceOf[ViewTabMethod].untaintLine(line, tag)
        case None => println("don't have " + sig + " open to unhighlight")
      }
    }
  }
}

object WorkArea {
  def collectFiles(d: File): List[File] = {
    val temp = if (d.exists) {
      if (d.isDirectory) {
        d.listFiles.toList.flatMap{collectFiles(_)}
      } else {
        val ext = FilenameUtils.getExtension(d.getPath());
        ext match {
          case "class" => List(d)
          case "jar"   => List(d)
          case _ => List()
        }
      }
    } else {
      List()
    }
    temp
  }

  def preloadMethods(d: File): List[Method] = {
    val files = collectFiles(d)
    WALALoader.loadFromFiles(files)
    /*val temp = if (d.exists) {
      if (d.isDirectory) {
        d.listFiles.toList.flatMap{preloadMethods(_)}
      } else {
        val ext = FilenameUtils.getExtension(d.getPath());
        ext match {
          case "class" => WALALoader.loadFromClass(d)
          case "jar"   => WALALoader.loadFromJar(d)
          case _ => List()
        }
      }
    } else {
      List()
    }
    temp*/
  }


}

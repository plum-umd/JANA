import scalafx.Includes._
import scalafx.application.JFXApp
//import scalafx.event._
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
import javafx.event._
import javafx.scene.input.DragEvent
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import javafx.stage.Window

import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._
import scalafx.scene.image._

import scalafx.scene.input._

import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils

import scala.collection.mutable.{Map=>MMap}
import javafx.scene.control.{TreeItem=>JTreeItem}

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom
import java.io.File

import scalafx.beans.property._

import java.io.File

case class FileItem(d: File, label: String) {
  def this(d: File) = this(d, d.toString)
  override def toString = label
}

class FilesTreeItem extends TreeItem[FileItem]
case class FilesTreeItemFile(val f: File) extends FilesTreeItem {

}

case class ViewTabFile(_workArea: WorkArea, f: File) extends ViewTab(_workArea) {
  val code = new TextArea()
  content = code
  
  val ext = FilenameUtils.getExtension(f.getPath());
  ext match {
    case "class" => ()
    case "java"  =>
      val contents = Util.slurpFile(f.getPath())
      code.text = contents
    case _ => println("don't know about this file type")
  }
}

object WorkAreaFiles {
  def constructFileTree(d: File)(implicit isRoot: Boolean = false): TreeItem[FileItem] = {
    val name = if (isRoot) {
      d.toString 
    } else {
      d.getName()
    }
    val item = FileItem(d, name)
    val temp = if (d.exists) {
      if (d.isDirectory) {
        val root = new TreeItem(item, new ImageView(Resources.iconFolder))
        root.children = d.listFiles.map{constructFileTree(_)(false)}
        root
      } else {
        val ext = FilenameUtils.getExtension(d.getPath());
        val icon = ext match {
          case "class" => Resources.iconBricks
          case "java"  => Resources.iconBricks
          case "ssa"   => Resources.iconBricks
          case "jar"   => Resources.iconBriefcase
          case "zip"   => Resources.iconBriefcase
          case _ => Resources.iconHelp
        }
        new TreeItem(item, new ImageView(icon))
      }
    } else {
      new TreeItem(item, new ImageView(Resources.iconError))
    }
    temp
  }
}

class FileTreeView(val workArea: WorkArea, f: File) extends TreeView[FileItem]() {
  root = WorkAreaFiles.constructFileTree(f)

  val selectedFile: ObjectProperty[Option[File]] = new ObjectProperty(this, "selectedFile", None)
  onMouseClicked = { mev: MouseEvent =>
    if (mev.button == MouseButton.PRIMARY && mev.clickCount == 2) {
      val selected = selectionModel.get.selectedItem.get
      if (null != selected) {
        loadFile(selected.value.get)
      }
    }
  }

  def loadFile(fi: FileItem): Unit = {
    val f = fi.d
    if (workArea.viewTabs.contains(ViewOptFile(f))) {
      workArea.viewPane.selectionModel.get.select(workArea.viewTabs(ViewOptFile(f)))
      return
    }
    // todo: create a file view tab
    val tab = new ViewTabFile(workArea, f)
    workArea.viewTabs += (ViewOptFile(f) -> tab)
  }

}

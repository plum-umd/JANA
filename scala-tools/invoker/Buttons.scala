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
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import javafx.stage.Window

import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._
import scalafx.scene.image._

import scala.sys.process._

import scalafx.event.{ActionEvent, Event}
import scalafx.scene.input._
import scalafx.scene.Node
import scalafx.scene.control.Button

import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils

import scala.collection.mutable.{Map=>MMap}
import javafx.scene.control.{TreeItem=>JTreeItem}

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom
import java.io.File

import scalafx.scene.web.WebView

import scalafx.beans.property._

import java.io.File

class IconButton(val img: Image, val tooltipString: String = null) extends Button {
  def this(img: Image) = this(img, null)

  graphic = new ImageView(img){
    fitWidth  = 12
    fitHeight = 12
  }
  padding = Insets(4)
  if (null != tooltipString) {
    tooltip = new Tooltip(tooltipString)
  }
}

class TinyIconButton(val img: Image, val tooltipString: String = null) extends Button {
  def this(img: Image) = this(img, null)

  graphic = new ImageView(img){
    fitWidth  = 8
    fitHeight = 8
  }
  padding = Insets(1)
  if (null != tooltipString) {
    tooltip = new Tooltip(tooltipString)
  }
}

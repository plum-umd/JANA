import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import scalafx.scene.input.TransferMode
import scalafx.geometry._
import scalafx.beans.property._
import scalafx.scene.web.WebView
import scalafx.event._

import javafx.stage.Window
import javafx.scene.input.DragEvent
import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler

import collection.JavaConversions._

import java.lang.management._

import java.time.Duration
import java.time.Period
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import java.nio.file.Path

class Result(val task: Task[_]) {
  override def toString = task.label + " " + task.target

  def showIn(n: TabPane): Unit = {
    n.id = "ResultPane"

    val outTab = new Tab(){
      closable = false
      text = "stdout"
      val ta = new TextArea() {
        text = task.logOut.get
        style = "-fx-font-family: 'monospace';"
      }
      content = ta
    }
    val errTab = new Tab(){
      closable = false
      text = "stderr"
      val ta = new TextArea() {
        text = task.logErr.get
        style = "-fx-font-family: 'monospace';"
      }
      content = ta
    }
    n.tabs ++= Seq(outTab, errTab)
  }
}

case class ResultFail(t: Task[_]) extends Result(t) {
  val e = t.exception.get

  override def showIn(n: TabPane): Unit = {
    super.showIn(n)

    n.id = "ResultPaneFail"

    val stackTab = new Tab(){
      closable = false
      text = "stack"
      val ta = new TextArea() {
        text = e.toString + "\n\n" + e.getStackTrace().mkString("\n")
        style = "-fx-font-family: 'monospace';"
      }
      content = ta
    }

    n.tabs ++= Seq(stackTab)
    n.selectionModel.get.select(stackTab)
  }
}

case class ResultText(t: Task[_]) extends Result(t) {
  override def showIn(n: TabPane): Unit = {
    super.showIn(n)
  }
}

class TaintRes()
class TaintResTag(val tag: String) extends TaintRes
case class TaintResRow(_tag: String, val sig: String, val lineNum: Int) extends TaintResTag(_tag)

case class ResultTaint(val workArea: WorkArea, t: Task[_], val res: Util.TaintResult) extends Result(t) {
  val textData = res.toList.map{ case (sig, places) =>
    sig + ":\n" + Util.tab(places.mkString("\n"))
  }.mkString("\n\n")

  override def showIn(n: TabPane): Unit = {
    super.showIn(n)

    val outputTab = new Tab() {
      closable = false
      text = "json"

      val newRoot = new TreeItem[TaintRes](new TaintRes())
      val ta = new TreeTableView[TaintRes] {
        id = "TaintView"
        showRoot = false
        root = newRoot
      }
      val colControls = new TreeTableColumn[TaintRes, Unit]("") {
        prefWidth = 30
      }
      val colTag = new TreeTableColumn[TaintRes, TaintRes]("tag") {
        prefWidth = 100
        cellFactory = { _ =>
          new TreeTableCell[TaintRes, TaintRes]() {
            item.onChange { (_, _, r) =>
              if (null != r) {
                r match {
                  case tag: TaintResRow =>
                    text = tag.tag
                    graphic = null
                  case tag: TaintResTag =>
                    text = tag.tag
                    graphic = new CheckBox() {
                      tooltip = new Tooltip("Highlight this tag in sourcecode.")
                      selected = true
                      workArea.addTaints(res(tag.tag), tag.tag)
                      onAction = { ae: ActionEvent =>
                        if (selected.get) {
                          workArea.addTaints(res(tag.tag), tag.tag)
                        } else {
                          workArea.removeTaints(res(tag.tag), tag.tag)
                        }
                        ae.consume()
                      }
                    }
                  case _ =>
                    text = null
                    graphic = null
                }
              } else {
                text = null
                graphic = null
              }
            }
          }
        }
        cellValueFactory = { i => i.value.value.get match {
          case tag: TaintResTag => i.value.value
          case _ => null
        } }
        /*i.value.value.get match {
            case tag: TaintResTag => StringProperty(tag.tag)
            case _ => StringProperty("")
          }
        }*/
      }
      val colLine = new TreeTableColumn[TaintRes, String]("L#") {
        prefWidth = 30
        cellValueFactory = { i =>
          i.value.value.get match {
            case row: TaintResRow => StringProperty(row.lineNum.toString)
            case _ => StringProperty("")
          }
        }
      }
      val colSig = new TreeTableColumn[TaintRes, String]("signature") {
        prefWidth = 300
        cellValueFactory = { i =>
          i.value.value.get match {
            case row: TaintResRow => StringProperty(row.sig)
            case _ => StringProperty("")
          }
        }
      }

      res.keys.foreach{ tag =>
        val newNode = new TreeItem[TaintRes](new TaintResTag(tag))
        newRoot.children += newNode
        res(tag).foreach{ case (sig, num) =>
          val newLine = new TreeItem[TaintRes](new TaintResRow(tag, sig, num))
          newNode.children += newLine
        }
        newNode.expanded = true
      }

      ta.columns ++= Seq(colControls, colTag, colLine, colSig)
      content = ta

    }

    n.tabs ++= Seq(outputTab)
    n.selectionModel.get.select(outputTab)
  }

}

case class ResultHTML(t: Task[_], val url: String) extends Result(t) {
  override def showIn(n: TabPane): Unit = {
    super.showIn(n)

    val htmlTab = new Tab(){
      closable = false
      text = s"html"
      content = new WebView() {
        engine.load(url)
      }
    }

    n.tabs ++= Seq(htmlTab)
    n.selectionModel.get.select(htmlTab)
  }
}

import scalafx.Includes._
import scalafx.application.JFXApp
//import scalafx.event._
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
//import javafx.event._
import scalafx.event.{ActionEvent, Event}
import javafx.scene.input.DragEvent
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import javafx.stage.Window

import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._

import collection.JavaConversions._

import java.lang.management._

import javafx.application.Platform
import javafx.concurrent.{Task=>fxTask}

import javafx.stage.FileChooser._

import java.io.File
import java.nio.file.Path

import scalafx.beans.property._
import scalafx.scene.image._

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom


class ActionButtonsBar(val actionBar: ActionBar) extends VBox {
  padding = Insets(2)
  spacing = 2
  hgrow = Priority.NEVER
  minWidth = 100

  val buttonAInterp = new Button(
    "ainterp", new ImageView(Resources.iconWindowGo)) {
    alignment = Pos.BaselineLeft
    onAction = {event: ActionEvent =>
      val task = Task(target = actionBar.signature.get.toString, label = "ainterp") {
        t: Task[_] => Actions.ainterp(actionBar.makeOpts)
      } { case (t: Task[_], result: Unit) =>
      } { case (t: Task[_], out: String, err: String, res: Unit) =>
          new ResultText(t)
      }
      Controls.monitor.addTaskWithResult(task)
      event.consume()
    }
  }

  val buttonInterproc = new Button(
    "interproc", new ImageView(Resources.iconWindows)) {
    alignment = Pos.BaselineLeft
    onAction = {event: ActionEvent =>
      val task = Task(
        target = actionBar.signature.get.toString,
        label = "interproc")
      { t: Task[_] =>
        Actions.interproc(actionBar.makeOpts)
      } { case (t, result) =>
      } { case (t: Task[_], out: String, err: String, res: Unit) =>
          new ResultText(t)
      }
      Controls.monitor.addTaskWithResult(task)
      event.consume()
    }
  }
  /*
   val buttonBounds = new Button(
   "bounds",
   new ImageView(Resources.iconWindowThunder)
   ) {
   alignment = Pos.BaselineLeft
   onAction = {e: ActionEvent =>
   val task = Task(
   target = signature.get.toString,
   label = "bounds"
   ) { _: Task[_] =>
   Actions.bounds(makeOpts)
   } { case (t, result) =>
   } { case (t: Task[_], out: String, err: String, res: Unit) =>
   new ResultText(t)
   }
   Controls.monitor.addTaskWithResult(task)
   e.consume()
   }
   }
   */

  val buttonInterBounds = new Button(
    "interbounds", new ImageView(Resources.iconWindowThunder)) {
    alignment = Pos.BaselineLeft
    onAction = {event: ActionEvent =>
      val tmp = Controls.selectedWorkArea.get.get.tmpDir
      val task = Task(
        target = actionBar.signature.get.toString,
        label = "interbounds"
      ){ _: Task[_] =>
        Actions.interbounds(tmp,actionBar.makeOpts)
      } { case (t, result) => ()
      } { case (t: Task[_], out: String, err: String, f: File) =>
          val url = f.toURI().toURL().toString
          println(s"url = $url")
          new ResultHTML(t, url = url)
      }
      Controls.monitor.addTaskWithResult(task)
      event.consume()
    }
  }

  val buttonTaint = new Button(
    "taint", new ImageView(Resources.iconWindowThunder)) {
    alignment = Pos.BaselineLeft
    onAction = {event: ActionEvent =>
      val workArea = Controls.selectedWorkArea.get.get
      val tmp = workArea.tmpDir
      val taints: List[Taint] = actionBar.taints.iterator.toList
      val task = Task(
        target = Controls.selectedWorkArea.get.get.toString,
        label = "taint"
      ){ _: Task[_] =>
        Actions.taint(tmp, taints, actionBar.makeOpts)
      } { case (t, result) => ()
      } { case (t: Task[_], out: String, err: String, tr: Util.TaintResult) =>
          new ResultTaint(workArea, t, tr)
      }
      Controls.monitor.addTaskWithResult(task)
      event.consume()
    }
  }

  val buttonImportTaint = new Button(
    "import-taint", new ImageView(Resources.iconWindowThunder)) {
      alignment = Pos.BaselineLeft
      onAction = {event: ActionEvent =>

        val workArea = Controls.selectedWorkArea.get.get

        val fileChooser = new javafx.stage.FileChooser();
        val selectedFile = fileChooser.showOpenDialog(null);

        if (null != selectedFile) {
          val task = Task(
            target = selectedFile.toString,
            label = "taint"
          ){ _: Task[_] =>
            Util.parseTaint(Util.slurpFile(selectedFile.getAbsolutePath()))
          } { case (t, result) => ()
          } { case (t: Task[_], out: String, err: String, tr: Util.TaintResult) =>
              new ResultTaint(workArea, t, tr)
          }
          Controls.monitor.addTaskWithResult(task)
        }
        event.consume()
    }
  }

  children += buttonAInterp
  children += buttonInterproc
  //buttons += buttonBounds
  children += buttonInterBounds
  children += buttonTaint
  children += buttonImportTaint

  /*
   buttons += new Button("computebounds all", new ImageView(Resources.iconArrowsDivide))
   buttons += new Button("compile",           new ImageView(Resources.iconBricks))
   buttons += new Button("decompile",         new ImageView(Resources.iconBricks))
   */

  children(0).disable <== ! actionBar.actionTarget
  children(1).disable <== ! actionBar.actionTarget
  children(2).disable <== ! actionBar.actionTarget
  children(3).disable <== ! actionBar.taintTarget 
  children(4).disable <== ! Controls.workAreaTarget

  //buttons(3).disable <== ! actionTarget
  //buttons(4).disable <== ! scriptTarget
  //buttons(5).disable <== ! codeTarget
  //buttons(6).disable <== ! codeTarget

  children.foreach{
    case c: javafx.scene.control.Button =>
      (new Button(c)).prefWidth = 100
    //c.contentDisplay = ContentDisplay.TOP
    //c.setPrefWidth(80)
    //c.setPrefHeight(60)
    //javafx.scene.control.ButtonBar.setButtonData(c, ButtonBar.ButtonData.Left)
    case c: javafx.scene.control.Label =>
      c.setPrefWidth(50)
      c.setPrefHeight(60)
    case c: javafx.scene.control.Separator =>
      c.setOrientation(Orientation.VERTICAL)
      c.setPrefWidth(10)
  }

}
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.event._
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
//import javafx.event._
//import javafx.scene.input.DragEvent
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
//import javafx.stage.Window

import scala.collection.mutable.{Map=>MMap}

//import javafx.scene.control.ScrollPane._
//import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._

import collection.JavaConversions._

//import java.lang.management._
//import javafx.application.Platform
//import javafx.concurrent.{Task=>fxTask}

import scalafx.beans.property._
import scalafx.scene.image._
import scalafx.collections._

//import javafx.beans.value._

object Controls {
  val stage = new JFXApp.PrimaryStage {
    title = "Invoker"
    width = 800
    height = 600
  }

  val resultWindows: MMap[Result, ResultWindow] = MMap()
  val workAreas:     MMap[String, WorkArea]     = MMap()

  val selectedWorkArea =
    new ObjectProperty[Option[WorkArea]](this, "selectedWorkArea", None)

  val workAreaTarget = new BooleanProperty(this, "workareatarget", false)
  workAreaTarget <== selectedWorkArea =!= None

  val actionBar: ActionBar = new ActionBar()
  val monitor:   Monitor   = new Monitor(stage)

  selectedWorkArea.onChange { (_, _, newSel) =>
    newSel match {
      case Some(wa) =>
        wa.taints.onChange { (a: ObservableBuffer[Taint], _) =>
          actionBar.taints.setAll(a)
        }
      case None     => actionBar.taints.clear()
      case null     => actionBar.taints.clear()
    }
  }

  val helpTab = new Tab(){
    userData = new ObjectProperty[String](this, "userData", "this is the help tab")
    text = "Info"
    content = new Label("Drop sources/classes/jars/folders/etc here") {
      alignmentInParent = Pos.Center
    }
  }

  var workTabs = new TabPane {
    tabs = Seq(helpTab)
  }

  def openResult(r: Result): Unit = {
    resultWindows.get(r) match {
      case None =>
        val nw = new ResultWindow(r)
        nw.show()
        resultWindows += (r -> nw)
      case Some(w) =>
        w.show()
        w.requestFocus()
    }
  }

  final val model = workTabs.selectionModel.get

  import javafx.scene.control.{Tab=>JTab}
  import javafx.beans.value.{ObservableValue=>JObservableValue}
  /*
  workTabs.getSelectionModel().selectedItemProperty().addListener(new ChangeListener[JTab] {
    override def changed(a: JObservableValue[_ <: JTab], old_tab: JTab, new_tab: JTab): Unit = {
      println("Tab Selection changed")
      println(s"old_tab = $old_tab")
      println(s"new_tab = $new_tab")
      println("raw userdata = " + new_tab.getUserData())
    }
  }
  )*/

  model.selectedItemProperty().onChange{(_, old_tab: JTab, new_tab: JTab) =>
    if (new_tab != helpTab && new_tab != null) {
      val data = new_tab.getUserData().asInstanceOf[WorkArea]
      selectedWorkArea.update(
        Some(data)
      )
    } else {
      selectedWorkArea.update(null)
    }
  }


}

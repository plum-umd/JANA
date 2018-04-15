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
import scalafx.event._
import scalafx.event.subscriptions.Subscription

import scalafx.scene.input.TransferMode
import scalafx.geometry._

import collection.JavaConversions._

import java.lang.management._

import javafx.application.Platform
import javafx.concurrent.{Task=>fxTask}
import java.io.File

import scalafx.beans.property._
import scalafx.scene.image._
import scalafx.collections._

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom

class TaintCloseCell extends TableCell[Taint,Taint] {
  item.onChange { (_, _, t: Taint) =>
    if (null != t) {
      val b = new TinyIconButton(Resources.iconCancel, "delete")
      graphic = b
      text = t.tag
      b.onAction = {e: ActionEvent =>
        t.codeLine.removeTaint(t)
        e.consume()
      }

    } else {
      graphic = null
      text = ""
    }
  }
}

class ActionBar() {
  val actionTarget = new BooleanProperty(this, "actionTarget", false)
  val scriptTarget = new BooleanProperty(this, "scriptTarget", false)
  val codeTarget   = new BooleanProperty(this, "codeTarget",   false)
  val taints: ObservableBuffer[Taint] = new ObservableBuffer[Taint]()
  val signature    = new StringProperty(this, "target", "")
  val exclusions   = new StringProperty(this, "exclusions",
    "java/awt/.* javax/.* com/sun/.* com/apple/.* sun/.* apple/.*  org/eclipse/.*  apache/.*"
  )
  val scope        = new StringProperty(this, "scope", "")
  val dependencies = new StringProperty(this, "dependencies", "")
  val entry        = new StringProperty(this, "entry", "")

  val taintTarget = new BooleanProperty(this, "taintTarget", false)
  taintTarget <== actionTarget && entry =!= ""

  val textSig = new TextField() {
    hgrow = Priority.ALWAYS
    text <==> signature
  }
  val setEntryButton = new IconButton(Resources.iconArrowRight) {
    tooltip = new Tooltip("Set this method as entry point for taint analysis.")
    onAction = { ae: ActionEvent =>
      entry.update(signature.get)
    }
    disable <== ! (signature =!= "")
  }

  val controlsSig = new HBox(textSig, setEntryButton) {
    spacing = 5
  }
  val textScope = new TextField() {
    hgrow = Priority.ALWAYS
    text <==> scope
  }
  val textDep = new TextField() {
    hgrow = Priority.ALWAYS
    text <==> dependencies
  }
  val textExc = new TextField() {
    hgrow = Priority.ALWAYS
    text <==> exclusions
  }

  val allBar = new ActionButtonsBar(this)


  val optBar = new TitledPane() {
    text = "ainterp options"
    collapsible = false
    padding = Insets(0)
    hgrow = Priority.ALWAYS
    prefWidth = 200
    content = 
      new GridPane() {
        padding = Insets(5)
        hgrow = Priority.ALWAYS
        hgap = 5
        vgap = 5
        add(new Label("signature") { minWidth = 80 }, 1, 1)
        add(controlsSig,                2, 1)
        add(new Label("scope"), 1, 2)
        add(textScope,          2, 2)
        add(new Label("dependencies"), 1, 3)
        add(textDep,                   2, 3)
        add(new Label("exclusions"), 1, 4)
        add(textExc,                 2, 4)
      }
  }

  def addTaint(t: Taint) = {
    taints.setAll(taints.iterator.toList ++ List(t))
  }
  def removeTaint(t: Taint) = {
    taints.setAll(taints.iterator.toList.filter{_ != t})
  }

  val taintsBox = new TableView[Taint]() {
    id = "TaintsView"
    var sub: Subscription = null
    hgrow = Priority.SOMETIMES
    rowFactory = { _ =>
      new TableRow[Taint]() {
        Common.setClassViaValue[Taint, String](this, item, Common.taintStyleMapSingle, { t => t.tag } )
        /* item.onChange { (_, _, t) =>
          if (null != sub) {
            sub.cancel
            sub = null
          }
          if (null != t) {
            sub = Common.setClassViaValue[Taint, String](this, t, Common.taintStyleMap, { t => t.tag } )
          }
         }*/

      }
    }
  }
  taints.onChange { (a: ObservableBuffer[Taint], _) =>
    taintsBox.items.get.setAll(a)
  }
  val colTaintTag    = new TableColumn[Taint,Taint]("tag") {
    prefWidth = 100
    cellFactory = { _: TableColumn[Taint,Taint] => new TaintCloseCell() }
    cellValueFactory = { t => ObjectProperty(t.value) }
  }
  val colTaintDef = new TableColumn[Taint,String]("D#") {
    prefWidth = 40
    cellValueFactory = { t => StringProperty(t.value.defNum.toString) }
  }
  val colTaintLineNum = new TableColumn[Taint,String]("L#") {
    prefWidth = 40
    cellValueFactory = { t => StringProperty(t.value.lineNum.toString) }
  }
  val colTaintMethod = new TableColumn[Taint,String]("Method") {
    prefWidth = 100
    cellValueFactory = { t => StringProperty(t.value.m.method.getSignature.toString) }
  }
  taintsBox.columns ++= Seq(colTaintTag, colTaintDef, colTaintLineNum, colTaintMethod)

  val mainEntry = new TextField() {
    hgrow = Priority.ALWAYS
    text <==> entry
  }
  val mainControls = new HBox(new Label("entry") { prefWidth = 60; padding = Insets(5) }, mainEntry)

  val taintsBar = new TitledPane() {
    hgrow = Priority.ALWAYS
    text = "taint options"
    content = new VBox(mainControls, taintsBox) {
      padding = Insets(5)
      spacing = 5
    }
    collapsible = false
    prefHeight <== allBar.height
  }

  //val taintControls = new VBox(mainControls, taintsBar) {
//    prefHeight <== allBar.height
//}

  /*
  taints.onChange { (_, _, newSel) =>
    taintsBox.items = newSel
    //taintsBox.text.update(newSel.mkString("\n"))
  }*/

  def makeOpts(): AInterpParams = AInterpParams(
    sig   = signature.get,
    scope = scope.get,
    deps  = dependencies.get,
    exc   = exclusions.get,
    entry = entry.get
  )

  val node = new HBox(allBar, optBar, taintsBar) {
    padding = Insets(2)
    spacing = 2
  }

//  taintsBox.ConstrainedResizePolicy = true
            
  optBar.prefWidth <== node.width * 0.40
  colTaintMethod.prefWidth = 200
  //node.width.get - optBar.width.get - allBar.width.get - colTaintTag.width.get - colTaintDef.width.get - colTaintLineNum.width.get

  node.setStyle("-fx-background-color: rgb(0,0,0);")

}
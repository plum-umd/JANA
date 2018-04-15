import scalafx.Includes._
import scalafx.application.JFXApp

import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
import scalafx.event._
import javafx.scene.input.DragEvent
import scalafx.scene.input.Dragboard
import scalafx.scene.control._

import javafx.stage.Window
import scalafx.scene.input._

import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._

import collection.JavaConversions._

import java.lang.management._

import javafx.application.Platform
import javafx.concurrent.{Task=>fxTask}

import scalafx.scene.image._
import scalafx.beans.property._
import scalafx.scene.control.cell._

import scalafx.animation._
import scalafx.util.Duration

class TaskProgressCell[A] extends TableCell[Task[A],Double] {
  val pbar = new ProgressBar()

  val tline = new Timeline {
    cycleCount = Timeline.Indefinite
    keyFrames = Seq(
      at (0 s) {pbar.progressProperty() -> 0d},
      at (1 s) {pbar.progressProperty() -> 1d}
    )
  }

  item.onInvalidate { graphic = null }

  item.onChange { (_, _, newItem: Double) =>
    if (null != newItem) {
      graphic = pbar
      pbar.progress = newItem
      if (newItem < 1.0) {
        spin
      } else {
        finish
      }
    } else {
      graphic = null
    }
  }

  /*override def updateItem(item: Double, empty: Boolean) = {
    //super.updateItem(item, empty)
    if (! empty) {
      //setGraphic(pbar)
      graphic = pbar
      pbar.progress = item
      if (item < 1.0) {
        spin
      } else {
        finish
      }
    } else {
      //setGraphic(null)
      graphic = null
    }
  }*/

  def spin: Unit = tline.playFromStart()
  def finish: Unit = {
    tline.pause()
    tline.jumpTo(1 s)
  }

}

class TaskOpenCell[A] extends javafx.scene.control.TableCell[Task[A],Option[Result]] {
  override def updateItem(item: Option[Result], empty: Boolean) = {

    super.updateItem(item, empty)
    if (! empty && item != None) {
      val b = new IconButton(Resources.iconZoom, "show results")
      setGraphic(b)
      b.onAction = {e: ActionEvent =>
        Controls.openResult(item.get)
        e.consume()
      }
    } else {
      setGraphic(null)
    }
  }
}

class TasksTable() extends TableView[Task[_]]() {
  val colTaskProg = new TableColumn[
    Task[_],
    Double
  ] {
    text = ""
    prefWidth = 50
    cellValueFactory = { t => t.value.progress }

    cellFactory = { _: scalafx.scene.control.TableColumn[Task[_],Double] =>
      val pbar = new TaskProgressCell[Task[_]]()
      //new TableCell(pbar.asInstanceOf[javafx.scene.control.TableCell[Task[_],Double]])
      pbar.asInstanceOf[TableCell[Task[_],Double]]
    }
  }
  val colTaskName    = new TableColumn[Task[_],String]("task") {
    prefWidth = 50
    cellValueFactory = _.value.labelProp
  }
  val colTaskTarget    = new TableColumn[Task[_],String]("target") {
    prefWidth = 150
    cellValueFactory = _.value.targetProp
  }
  val colTaskElapsed = new TableColumn[Task[_],String]("elapsed"){
    cellValueFactory = _.value.elapsedString
    prefWidth = 50
  }
  val colTaskStarted = new TableColumn[Task[_],String]("started"){
    cellValueFactory = _.value.startedString
    prefWidth = 50
  }

  colTaskTarget.prefWidth <== this.width
    - colTaskProg.width - colTaskName.width - colTaskElapsed.width
    - colTaskStarted.width

  columns ++= Seq(
    colTaskProg, colTaskName, colTaskTarget, colTaskElapsed, colTaskStarted)
}

class TasksTableDone() extends TableView[Task[_]]() {
  val colTaskOpen    = new TableColumn[Task[_],Option[Result]]("") {
    prefWidth = 30
    cellFactory = { _: scalafx.scene.control.TableColumn[Task[_],Option[Result]] =>
      val cell = new TaskOpenCell[Task[_]]()
      new TableCell(
        cell.asInstanceOf[javafx.scene.control.TableCell[Task[_],Option[Result]]]
      )
    }
    cellValueFactory = {a => ObjectProperty(a.value.result) }
  }
  val colTaskName    = new TableColumn[Task[_],String]("task") {
    prefWidth = 50
    cellValueFactory = _.value.labelProp
  }
  val colTaskTarget    = new TableColumn[Task[_],String]("target") {
    prefWidth = 150
    cellValueFactory = _.value.targetProp
  }
  val colTaskElapsed = new TableColumn[Task[_],String]("elapsed"){
    cellValueFactory = _.value.elapsedString
    prefWidth = 50
  }
  val colTaskStarted = new TableColumn[Task[_],String]("started"){
    cellValueFactory = _.value.startedString
    prefWidth = 50
  }
  val colTaskFinished = new TableColumn[Task[_],String]("finished"){
    cellValueFactory = _.value.finishedString
    prefWidth = 50
  }

  colTaskTarget.prefWidth <== this.width
    - colTaskName.width - colTaskElapsed.width - colTaskStarted.width
    - colTaskFinished.width

  columns ++= Seq(
    colTaskOpen, colTaskName, colTaskTarget,
    colTaskElapsed, colTaskStarted, colTaskFinished
  )

  onMouseClicked = { mev: MouseEvent =>
    if (mev.button == MouseButton.PRIMARY && mev.clickCount == 2) {
      val task = selectionModel.get.selectedItem.get
      if (null != task) {
        if (None != task.result) {
          Controls.openResult(task.result.get)
        }
      }
    }
  }

}

class Monitor(parent: Stage) {
  val runningTasks = new TasksTable() {
    vgrow = Priority.ALWAYS
  }
  val doneTasks    = new TasksTableDone() {
    vgrow = Priority.ALWAYS
  }
  val queuedTasks  = new TasksTable() {
    vgrow = Priority.ALWAYS
  }

  val doneClearButton = new Button("Clear"){
    onAction = {event: ActionEvent =>
      doneTasks.items.get.clear()
      event.consume()
    }
  }

  val doneButtons = new ButtonBar(){
    buttons = Seq(doneClearButton)
  }

  val queuedPane = new TitledPane{
    expanded = false
    text = "Queued"
    content = queuedTasks
    vgrow = Priority.ALWAYS
  }

  val runningPane = new TitledPane{
    expanded = true
    text = "Running"
    content = runningTasks
    vgrow = Priority.ALWAYS
  }

  val donePane = new TitledPane{
    expanded = true
    text = "Done"
    content = new VBox(doneButtons, doneTasks){
      padding = Insets(0)
    }
    vgrow = Priority.ALWAYS
  }

  val tasksPane = new VBox() {
    //orientation = Orientation.VERTICAL
    children ++= Seq(queuedPane, runningPane, donePane)
  }

  tasksPane.vgrow = Priority.ALWAYS

  val myScene = new Scene {
    stylesheets += Resources.styleMain
  }

  tasksPane.prefHeight <== myScene.height
  //queuedPane.prefHeight  <== tasksPane.height - runningPane.height - donePane.height
  //runningPane.prefHeight <== tasksPane.height - queuedPane.height  - donePane.height
  //donePane.prefHeight    <== tasksPane.height - queuedPane.height  - runningPane.height

  val monitors = 0.until(Monitor.availableProcessors).map{i =>
    new Label(i.toString) {
      //text = Monitor.operatingSystemMXBean.getSystemLoadAverage().toString
      text = "cpu"+i.toString
      //maxWidth = Double.MaxValue
      hgrow    = Priority.ALWAYS
      padding  = Insets(2)
    }
  }

  val boxes = new GridPane() {
    gridLinesVisible = true
    style = "-fx-background-color: rgb(0,0,0);"
  }
  //boxes.hgrow            = Priority.ALWAYS
  //boxes.scaleShape       = true

  val all = new BorderPane() {
    style = "-fx-background-color: rgb(50,50,50);"

    top = boxes
    center = tasksPane

    //hgrow    = Priority.ALWAYS
    //maxWidth = Double.MaxValue
    //scaleShape = true
  }
  myScene.content = all

  monitors.zipWithIndex.foreach{case (m, i) => boxes.add(m, i, 0)}

  val col = new ColumnConstraints()
  boxes.columnConstraints = monitors.map{_ => col}

  val stage: Stage = new Stage {
    title  = "Work"
    width  = 300
    height = 600
    x = parent.getX() + parent.getWidth() + 1
    y = parent.getY()
    scene = myScene

    width onChange show
    height onChange show
  }

  all.prefWidth <== stage.width
  all.prefHeight <== stage.height
  stage.initOwner(parent)
  stage.show()

  def addTask[A](t: Task[A]): Unit = {
    runningTasks.items.get += t

    val task = new fxTask[Unit](){
      override def call() = {
        t.run
        runningTasks.items.get -= t
        doneTasks.items.get    += t
        t.progress.update(1.0)
      }
    }
    val thread = new Thread(task, "whatever")
    thread.setDaemon(true)
    thread.start()
  }

  def addTaskWithResult[A](t: Task[A]): Unit = {
    runningTasks.items.get += t

    val task = new fxTask[Unit](){
      override def call() = {
        val r = t.runWithResult
        if (null != r) {
          Platform.runLater{new Runnable() {
            def run() = {
              Controls.openResult(r)
            }
          }
          }
        }
        runningTasks.items.get -= t
        doneTasks.items.get    += t
        t.progress.update(1.0)
      }
    }
    val thread = new Thread(task, "whatever")

    thread.setDaemon(true)
    thread.start()
  }
}
  

object Monitor {
  val operatingSystemMXBean:OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()
  val runtimeMXBean         = ManagementFactory.getRuntimeMXBean()
  val availableProcessors   = operatingSystemMXBean.getAvailableProcessors()
}
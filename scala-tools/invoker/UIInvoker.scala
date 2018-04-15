import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.Group
import scalafx.stage.Stage
import scalafx.scene.layout._
import javafx.scene.input.DragEvent
import java.io.File
import scalafx.scene.input.Dragboard
import scalafx.scene.control._
import javafx.stage.Window

import javafx.scene.control.ScrollPane._
import javafx.event.{EventHandler}
import scalafx.stage.WindowEvent
import scalafx.scene.input.TransferMode
import scalafx.geometry._
import scalafx.scene.image._

import collection.JavaConversions._

import scalafx.beans.property._
import java.lang.management._

import org.apache.commons.io.FilenameUtils

object Invoker extends JFXApp {
  PPL.init

  val menu = Menu.createMenuBar

  Controls.stage.scene = new Scene {
    stylesheets += Resources.styleMain
    onDragOver = new EventHandler[DragEvent]{
      override def handle(event: DragEvent) {
        val db: Dragboard = event.getDragboard()
        if (db.hasFiles()) {
          event.acceptTransferModes(TransferMode.COPY)
        } else {
          event.consume()
        }
      }
    }
    onDragDropped = new EventHandler[DragEvent]{
      override def handle(event: DragEvent) {
        val db: Dragboard = event.getDragboard()
        val success = false
        if (db.hasFiles()) {
          val success = true
          val filePath: String = null
            db.getFiles().map{file =>
              val filePath = file.getAbsolutePath()
              println(filePath)
              handleDrop(filePath)
            }
        }
          event.setDropCompleted(success)
        event.consume()
      }
    }

    root = new BorderPane {
      top    = Controls.actionBar.node
      center = Controls.workTabs
    }

    root
  }

  Controls.stage.onCloseRequest = { we: WindowEvent =>
    println("closing")
    scalafx.application.Platform.exit()
  }

  Controls.stage.show()

  def handleDrop(file: String): Unit = {
    if (Controls.workAreas.contains(file)) return

    val task = Task(target = file, label = "loading"){ _ : Task[_] =>
      WorkArea.preloadMethods(new File(file))

    }{ case (_, funs) =>
        val newArea = new WorkArea(file, funs)
        Controls.actionBar.actionTarget <== newArea.selectedMethod =!= None

        val f = new File(file).getPath()
        val basename  = FilenameUtils.getBaseName(f)
        val extension = FilenameUtils.getExtension(f)

        val newTab = new Tab{
          content = newArea.root
          closable = true
          id = file
          text = basename + (if ("" != extension) { "." + extension } else { "" })
          userData = newArea
        }

        Controls.workTabs.getSelectionModel().select(newTab)
        Controls.workTabs += newTab
        Controls.workTabs.tabs -= Controls.helpTab
    }

    Controls.monitor.addTask(task)
  }
}

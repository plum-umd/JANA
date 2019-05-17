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

import collection.JavaConversions._

import java.lang.management._

import scalafx.beans.property._
import scalafx.scene.web.WebView

import java.time.Duration
import java.time.Period
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

case class Task[A]
  (val target: String, val label: String)
  (val f: Task[A] => A)
  (val fUI: (Task[A], A) => Unit)
  (implicit val fResult: (Task[A], String, String, A) => Result = null) {

  override def toString = label

  val targetProp     = new StringProperty(this, "target", target)
  val labelProp      = new StringProperty(this, "label", label)
  val startedString  = new StringProperty(this, "started", "-")
  val finishedString = new StringProperty(this, "finished", "-")
  val elapsedString  = new StringProperty(this, "elapsed", "-")

  val running  = new BooleanProperty(this, "running", false)
  val progress = new ObjectProperty[Double](this, "progress", 0.0)

  val started  = new ObjectProperty(this, "started", LocalDateTime.now())
  val finished = new ObjectProperty(this, "finished", LocalDateTime.now())
  val elapsed  = new ObjectProperty(this, "elapsed", Duration.ZERO)

  val logOut = new StringProperty(this, "logOut", "")
  val logErr = new StringProperty(this, "logErr", "")

  var exception: Option[Throwable] = None
  var result:    Option[Result]    = None

  started.onChange{(_,_,n) =>
    startedString.update(Util.formatShort.format(n))
  }
  finished.onChange{(_,_,n:LocalDateTime) =>
    finishedString.update(Util.formatShort.format(n))
    elapsed.update(Duration.between(started.get, n))
  }
  elapsed.onChange{(_,_,n:Duration) =>
    elapsedString.update(elapsed.get.toString)
  }

  def runResult(a: A): Result = if (null != fResult) { fResult(this, logOut.get, logErr.get, a) } else {null}
  def runNonUI: A = f(this)
  def runUI(a: A): Unit = fUI(this, a)

  import javafx.application.Platform

  def runWithResult: Result = {
    started.update(LocalDateTime.now())

    val (aOpt, out, err) = Log.capture[A]{ runNonUI }

    logOut.update(out)
    logErr.update(err)
    finished.update(LocalDateTime.now())

    if (aOpt.isLeft) {
      val a = aOpt.left.get

      Platform.runLater{new Runnable() {
        def run() = try {
          runUI(a)
        } catch {
          case e: Throwable =>
            println("tasks UI computation failed: " + e.toString)
            e.printStackTrace()
        }
      }}

      result = Some(runResult(a))
    } else {
      this.exception = Some(aOpt.right.get)
      result = Some(new ResultFail(this))
    }
    result.get
  }

  def run() = {
    started.update(LocalDateTime.now())

    val (_, out, err) = Log.capture{
      try {
        val temp = runNonUI

        Platform.runLater{new Runnable() {

          def run() = try {
            runUI(temp)
          } catch {
            case e: Throwable =>
              println("task failed: " + e.toString)
              e.printStackTrace()
          }

        }}
      } catch {
        case e: Throwable =>
          println("task failed: " + e.toString)
          e.printStackTrace()
      }
    }

    logOut.update(out)
    logErr.update(err)

    finished.update(LocalDateTime.now())
  }

}

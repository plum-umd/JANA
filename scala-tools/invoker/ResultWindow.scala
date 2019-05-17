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

import javafx.application.Platform
import javafx.concurrent.{Task=>fxTask}

import scalafx.beans.property._
import scalafx.scene.control.cell._

import scalafx.animation._
import scalafx.util.Duration
import scalafx.scene.web.WebView

class ResultWindow(r: Result) extends Stage() {
  val resultDisplay = new TabPane() {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
  }

  title = "Result: " + r.toString
  val newScene = new Scene() {
    stylesheets += Resources.styleMain
    root = resultDisplay
  }

  r.showIn(resultDisplay)

  scene = newScene

  // TODO: try to resize this window to fit content of its tabs
  
  /*
  resultDisplay.applyCss()
  resultDisplay.layout()

  var mW = 0.0
  var mH = 0.0

  scene = newScene
  show()

  resultDisplay.tabs.toList.foreach{
    t => println("t.content = " + t.content.get)
    //t.content.get.applyCss()
    //t.content.get.layout()
    t.content.get match {
      case tf: javafx.scene.control.ScrollPane =>
        val w = tf.getWidth()
        val h = tf.getHeight()
        println(s"w = $w, h = $h")
        val wp = tf.getPrefWidth()
        val hp = tf.getPrefHeight()
        println(s"wp = $wp, hp = $hp")
        val wpar = tf.getBoundsInParent.getWidth()
        val hpar = tf.getBoundsInParent.getHeight()
        println(s"wpar = $wpar, hpar = $hpar")
        val wloc = tf.getBoundsInLocal.getWidth()
        val hloc = tf.getBoundsInLocal.getHeight()
        println(s"wloc = $wloc, hloc = $hloc")
        mW = mW.max(w)
        mH = mH.max(w)
      case _ => ()
    }
  }
   */

  resultDisplay.prefWidth = 600
  resultDisplay.prefHeight = 400

}

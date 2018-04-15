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
//import scalafx.stage.Window
import javafx.stage.Window

import javafx.scene.control.ScrollPane._
//import scalafx.event.EventHandler
import javafx.event.EventHandler
import scalafx.scene.input.TransferMode
import scalafx.geometry._

import collection.JavaConversions._

//import de.codecentric.centerdevice.MenuToolkit

import java.lang.management._

object Menu {
  def createMenuBar: MenuBar = {
    //val tk = MenuToolkit.toolkit();

    val appMenu = new Menu("Invoker")
    val aboutItem = new MenuItem("About Invoker");
    val prefsItem = new MenuItem("Preferences...");
    appMenu.getItems().addAll(
      aboutItem,
      new SeparatorMenuItem(),
      prefsItem,
      new SeparatorMenuItem(),
      //tk.createHideMenuItem("Invoker"),
      //tk.createHideOthersMenuItem(),
      //tk.createUnhideAllMenuItem(),
      new SeparatorMenuItem()
      //tk.createQuitMenuItem("Invoker")
    );
    val fileMenu = new Menu("File");
    val newItem = new MenuItem("New...");
    fileMenu.getItems().addAll(
      newItem,
      new SeparatorMenuItem(),
      //tk.createCloseWindowMenuItem(),
      new SeparatorMenuItem(),
      new MenuItem("TBD")
    );
    val editMenu = new Menu("Edit");
    editMenu.getItems().addAll(
      new MenuItem("TBD")
    );
    val viewMenu = new Menu("View");
    viewMenu.getItems().addAll(
      new MenuItem("Main"),
      new MenuItem("Work")
    );
    val windowMenu = new Menu("Window");
    windowMenu.getItems().addAll(
      //tk.createMinimizeMenuItem(),
      //tk.createZoomMenuItem(),
      //tk.createCycleWindowsItem(),
      new SeparatorMenuItem())
      //tk.createBringAllToFrontItem());
    val helpMenu = new Menu("Help");
    helpMenu.getItems().addAll(
      new MenuItem("TBD")
    );

    val bar = new MenuBar() {
      useSystemMenuBar = true
        menus = Seq(appMenu, fileMenu, editMenu, viewMenu, windowMenu, helpMenu);
    }

    //tk.setGlobalMenuBar(bar)
    //tk.setApplicationMenu(appMenu)
    //tk.autoAddWindowMenuItems(windowMenu);

    bar
  }

}
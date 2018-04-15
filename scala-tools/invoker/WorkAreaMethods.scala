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
import scalafx.scene.image._
import scalafx.scene.control.Button
import scalafx.scene.web.WebView
import scalafx.beans.property._
import scalafx.scene._
import scalafx.event._
import scalafx.scene.input._

import scala.sys.process._

import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils

import scala.collection.mutable.{Map=>MMap}

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom
import java.io.File

class PopupTitledPane(val innerContent: Node, parent: ViewTabMethod) extends TitledPane {
  style = "-fx-font-family: 'monospace'; -fx-background-color: rgb(0,0,0);";
  collapsible = false
  vgrow = Priority.ALWAYS
  hgrow = Priority.ALWAYS

  padding = Insets(0)

  val buttonBar = new HBox() {
    padding = Insets(0)
    spacing = 0
  }
  val buttonMaximize = new IconButton(Resources.iconArrowsOut, "maximize")
  val buttonMinimize = new IconButton(Resources.iconArrowsIn, "minimize")
  val buttonHide  = new IconButton(Resources.iconCancel, "hide")
  val buttonPopup = new IconButton(Resources.iconDouble, "pop-out into a new window")

  buttonMaximize.onAction = {ae: ActionEvent =>
    parent.maximizePane(this)
    ae.consume()
  }

  buttonMinimize.onAction = {ae: ActionEvent =>
    parent.minimizePane(this)
    ae.consume()
  }

  buttonHide.onAction = {ae: ActionEvent =>
    parent.hidePane(this)
    ae.consume()
  }

  buttonBar.children ++= Seq(buttonMaximize)//, buttonPopup, buttonHide)

  graphic = buttonBar

  content = new VBox(buttonBar, innerContent) {
    padding = Insets(0)
  }

  def showMaximized: Unit = {
    buttonBar.children.setAll(buttonMinimize)//, buttonPopup, buttonHide)
  }

  def showNormalized: Unit = {
    buttonBar.children.setAll(buttonMaximize)//, buttonPopup, buttonHide)
  }
}

class FunItem(val loc: Location, val name: String) {
  override def toString = name
}
class FunItemPackage(loc: Location, name: String) extends FunItem(loc, name) {
}
class FunItemMethod(val m: Method) extends FunItem(m.location, "") {
  override def toString = m.toString
}

class FunsTreeItem extends TreeItem[FunItem]
case class FunsTreeItemMethod(m: Method) extends FunsTreeItem {
  graphic = new ImageView(Resources.iconBricks)
  value   = new FunItemMethod(m)
}
case class FunsTreeItemPackage(f: Location, val name: String) extends FunsTreeItem {
  graphic = new ImageView(Resources.iconBriefcase)
  value   = new FunItemPackage(f, name)
}

case class ViewTabMethod(_workArea: WorkArea, m: Method) extends ViewTab(_workArea) {
  text = m.method.getSignature().toString()

  val fsource = m.location match {
    case f: Location.File =>
      val path      = FilenameUtils.getPath(f.file.getPath())
      val basename  = FilenameUtils.getBaseName(f.file.getPath())
      val extension = FilenameUtils.getExtension(f.file.getPath())
      "/" + path + basename + ".java"
    case _ => null
  }

  val sourcePane = new SourceView(workArea, new File(fsource))
  val ssaPane    = new WALAView(workArea, m)

  val classPane  = new TextArea() {
    id = "BytecodeView"
    text = "class"
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    //style = "-fx-background-color: rgb(0,0,0);"
  }

  val cfgPane    = new WebView() {
    id = "CFGView"
    //style = "-fx-background-color: rgb(0,0,0);"
    //zoom = 0.75
    if (null != m.dot) {
      val url = m.dot.toURI().toURL()
      engine.load(url.toString)
    }
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
  }

  val sourceTPane = new PopupTitledPane(sourcePane, this){
    text = "source"
  }

  def taintLine(line: Int, t: String): Unit = {
    //println("highlighting line " + line + " in " + m.method.getSignature.toString)
    sourcePane.taintLine(line, t)
  }

  def untaintLine(line: Int, t: String): Unit = {
    //println("highlighting line " + line + " in " + m.method.getSignature.toString)
    sourcePane.untaintLine(line, t)
  }

  val classTPane = new PopupTitledPane(classPane, this){
    text = "bytecode"
  }
  val ssaTPane = new PopupTitledPane(ssaPane, this){
    text = "wala/ir"
  }
  val cfgTPane = new PopupTitledPane(cfgPane, this){
    text = "wala/cfg"
  }

  val viewsPane = new SplitPane() {
    items += sourceTPane
    //items += classTPane
    items += ssaTPane
    items += cfgTPane
    vgrow = Priority.ALWAYS
    hgrow = Priority.ALWAYS
    //setDividerPosition(0,0.25)
    //setDividerPosition(1,0.50)
    //setDividerPosition(2,0.75)
    setDividerPosition(0,0.333)
    setDividerPosition(1,0.666)
  }

  var tempItems = viewsPane.items.toList

  def hidePane(m: PopupTitledPane): Unit = {
    viewsPane.items -= m
  }

  def showPane(m: PopupTitledPane): Unit = {
    viewsPane.items += m
  }

  def maximizePane(m: PopupTitledPane): Unit = {
    tempItems = viewsPane.items.toList
    //viewsPane.items.clear()
    /*val temp = new java.util.ArrayList[javafx.scene.Node](1)
     temp.add(m)*/
    //    viewsPane.items.clear()

    viewsPane.items.setAll(m)
    m.autosize()
    m.showMaximized
  }

  def minimizePane(m: PopupTitledPane): Unit = {
    //    viewsPane.items.clear()
    viewsPane.items.setAll(tempItems)
    m.autosize()
    m.showNormalized
    viewsPane.setDividerPosition(0,0.333)
    viewsPane.setDividerPosition(1,0.666)
    //viewsPane.setDividerPosition(2,0.75)
  }

  /*
  classPane.text =
    try {
      Util.slurpFile(f.getPath())
    } catch {
      case e: Throwable => "Could not display file " + f.toString
    }
   */

  //val resultsPane = new ResultsPane()


  content = new SplitPane() {
    vgrow = Priority.ALWAYS
    sourceTPane.prefHeight <== this.height
    classTPane.prefHeight  <== this.height
    ssaTPane.prefHeight    <== this.height
    cfgTPane.prefHeight    <== this.height

    /*
    val resultsItem = new TitledPane() {
      text = "results"
      content = resultsPane
    }
     */

    orientation = Orientation.VERTICAL
    //items += resultsItem
    items += viewsPane
    //setDividerPosition(0,0.20)
    //SplitPane.setResizableWithParent(resultsPane, false);
  }
}

class MethodTreeView(val workArea: WorkArea, loc: Location, funs: List[Method])
    extends TreeView[FunItem]() {

  val setEntry = new MenuItem("Set as taint entry.") {
    onAction = { ae: ActionEvent =>
      val item = selectionModel.get.selectedItem.get
      item.value.get match {
        case m: FunItemMethod =>
          Controls.actionBar.entry.update(m.m.method.getSignature.toString)
          //println("setting entry to " + m.m.method.getSignature.toString)
        case _ => ()
          //println("don't know what entry is")
      }
    }
  }

  val tempMenu = new ContextMenu(setEntry) {
    autoFix  = true
    autoHide = true
  }
  onContextMenuRequested = { cme: ContextMenuEvent =>
    val item = selectionModel.get.selectedItem.get
    item.value.get match {
      case m: FunItemMethod => tempMenu.show(this, cme.screenX, cme.screenY)
      case _ => ()
    }
    cme.consume()
  }
  

  /*cellFactory = {_ =>
    new TreeCell[FunItem]() {
      contextMenu = 
      item.onChange { (_, _, n) =>
        if (null != n) {
          n match {
            case tim: FunItemMethod  => text = tim.toString
            case tip: FunItemPackage => text = tip.toString
            case f: FunItem => text = f.toString
            case _ => text = ""
          }
          //value.update(n)
        }
      }
    }
   }*/

  showRoot = false

  val selectedMethod: ObjectProperty[Option[Method]] =
    new ObjectProperty(this, "selectedMethod", None)

  type ClassSig = (Atom, IClass)
  type FunSig   = (Atom, IClass, IMethod)

  val funH: MMap[ClassSig, TreeItem[FunItem]] = MMap[ClassSig, TreeItem[FunItem]]()

  val funsRoot = new TreeItem[FunItem](
    new FunItem(loc, "Methods"),
    new ImageView(Resources.iconBriefcase)
  )

  val funsEntries = new TreeItem[FunItem](
    new FunItem(loc, "Entries"),
    new ImageView(Resources.iconBriefcase))
  funsEntries.children += new TreeItem[FunItem](
    new FunItem(loc, "I don't know how to find these")
  )

  val funsAll = new TreeItem[FunItem](
    new FunItem(loc, "Packages"),
    new ImageView(Resources.iconBriefcase)
  )

  val funsOther = new TreeItem[FunItem](
    new FunItem(loc, "Unpackaged"),
    new ImageView(Resources.iconBriefcase)
  )

  funsRoot.children ++= Seq(funsEntries, funsOther, funsAll)
  root = funsRoot

  onMouseClicked = { mev: MouseEvent =>
    if (mev.button == MouseButton.PRIMARY && mev.clickCount == 2) {
      val new_sel = selectionModel.get.selectedItem.get
      if (null != new_sel) {

        new_sel.value.get match {
          case c: FunItemMethod =>
            c.m.location match {
              case fo: Location.File =>
                selectedMethod.update(Some(c.m))
                selectMethod(c.m)
              case _ => println("!!! I don't know where this method is located")
            } 
          case _ =>
            selectedMethod.update(None)
        }
      }
    }
  }

  val sfuns = funs.sortWith { case (a,b) => b.toString > a.toString }

  sfuns.foreach{case m =>
    val s = getClassSig(m.method)
    if (null != s._1) {
      val p = getPackage(m.location, getClassSig(m.method))
      val newItem = new FunsTreeItemMethod(m)
      p.children += newItem
    } else {
      val newItem = new FunsTreeItemMethod(m)
      funsOther.children += newItem
    }
  }

  funsAll.children.setAll(funsAll.children.sortWith { (a, b) => b.value.toString > a.value.toString })

  def getClassSig(m: IMethod): ClassSig = {
    val c = m.getDeclaringClass()
    val p = c.getName().getPackage()
    (p,c)
  }

  def getPackage(d: Location, i: ClassSig): TreeItem[FunItem] = {
    val (p,c) = i
    if (! funH.contains(i)) {
      val newItem = new FunsTreeItemPackage(d, p.toString + "/" + c.getName().getClassName())
      funH += (i -> newItem)
      funsAll.children += newItem
    }
    funH(i)
  }

  def selectMethod(m: Method): Unit = {
    m.location match {
      case fo: Location.File =>
        if (workArea.viewTabs.contains(ViewOptMethod(m))) {
          val p = workArea.viewTabs(ViewOptMethod(m))
          if (! workArea.viewPane.tabs.contains(p)) {
            workArea.viewPane.tabs += p
          } else {
          }
          workArea.viewPane.selectionModel.get.select(workArea.viewTabs(ViewOptMethod(m)))
          return
        }
        Controls.monitor.addTask(Task(
          target = m.method.getSignature.toString,
          label = "generate method info"
        )
          { _: Task[_] =>
            m.genDot(workArea.tmpDir)
            Process(Seq(Dirs.javapExe, "-c", fo.file.toString)).lines.mkString("\n")
          } { case (_, sourceCode) =>
              val tab = new ViewTabMethod(workArea, m)
              tab.classPane.text = sourceCode
              workArea.recordMethodView(ViewOptMethod(m), tab)

          })
      case _ => println("cannot select this method since I don't know where it is")
    }
  }

}
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
import scalafx.scene.input._
import scalafx.beans.property._
import scalafx.beans.property.PropertyIncludes
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableHashSet
import scalafx.collections.ObservableSet
import scalafx.collections.ObservableSet._
import scalafx.event._
import scalafx.event.subscriptions.Subscription

import javafx.scene.input.DragEvent
import javafx.stage.Window
import javafx.scene.control.ScrollPane._
import javafx.event.EventHandler
import javafx.scene.control.{TreeItem=>JTreeItem}
import javafx.scene.control.{TreeTableColumn=>JTreeTableColumn}

import java.io.File
import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import scala.collection.mutable.{Map=>MMap}
import scala.language.implicitConversions

import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.util.strings.Atom

class TaintMenuItem(
  val add: Boolean,
  val cell: TreeTableCell[_,_],
  val taintTag: String
) extends MenuItem() {

  text = taintTag

  onAction = {ae: ActionEvent =>
    cell.tableRow.get.item.get match {
      case wc: CodeWALALine =>
        if (! wc.defines.isEmpty) {
          val taint = wc.makeTaint(taintTag)
          if (add) {
            wc.addTaint(taint)
          } else {
            wc.removeTaint(taint)
          }
        } else {
          println("instruction does not define a value: " + wc.toString)
        }
      case x => println("unhandled type to add taint: " + x.toString)
    }
    ae.consume()
  }
}

class CodeCell[A,B]() extends TreeTableCell[A,B]() {
  val self = this

  item.onChange { (_, _, s) =>
    alignment = Pos.TopLeft
    if (null != s) {
      text = s.toString
    }
    padding = Insets(0)
  }
  item.onInvalidate { text = "" }

  val cell = this

  val itemAddTaint    = new Menu("Add taint")
  val itemRemoveTaint = new Menu("Remove taint")
  val taintTaint   = new TaintMenuItem(true, this, "taint")
  val taintSecret  = new TaintMenuItem(true, this, "secret")
  val taintTaintRem   = new TaintMenuItem(false, this, "taint")
  val taintSecretRem  = new TaintMenuItem(false, this, "secret")

  itemAddTaint.items    ++= Seq(taintTaint, taintSecret)
  itemRemoveTaint.items ++= Seq(taintTaintRem, taintSecretRem)
  val m = new ContextMenu(itemAddTaint, itemRemoveTaint)
  contextMenu = m

  /*
  onContextMenuRequested = { ev: ContextMenuEvent =>
    println("context menu requested:" + ev.toString)
  }*/

}

/* ins: this is where we can add additional highlighting */
class CodePart(val workArea: WorkArea, val text: String) {
  override def toString = text
  val highlighted = new BooleanProperty(this, "highlighted", false)
  val taints      = new ObjectProperty[Set[String]](this, "taints", Set[String]())
  /*
   *taints.onChange { (v, _) =>
   *  highlighted.update(v.seq.size > 0)
   *}
   */
  val textProp = new ObjectProperty(this, "code", text)
}

case class CodeClass(
  _workArea: WorkArea,
  _text: String
) extends CodePart(_workArea, _text)

class CodeLine(
  _workArea: WorkArea,
  val num: Int,
  _text: String
) extends CodePart(_workArea, _text) {
  val numProp = new StringProperty(this, "line number", num.toString)
}

class CodeWALALine(
  _workArea: WorkArea,
  val m: Method,
  val ins: WALA.I,
  val lineNum: Int,
  val bbNum: Int,
  _text: String
) extends CodeLine(_workArea, lineNum, _text) {

  def makeTaint(t: String): Taint = {
    new Taint(m, lineNum, defines.get, t, this)
  }

  val defines = if (null != ins) {
    if (ins.getNumberOfDefs() == 1) {
      Some(ins.getDef())
    } else {
      None
    }
  } else {
    None
  }

  val taintAnnots = new ObjectProperty[Set[Taint]](this, "taintAnnts", Set[Taint]())

  val bbNumProp       = new StringProperty(this, "bb number", bbNum.toString)
  val hiddenBBNumProp = StringProperty("BB" + bbNumProp.get + " ...")
  //var csub = taintAnnots.onChange { (_, _, newVal: Set[Taint]) =>
  //  textProp.update(newVal.mkString("") + "" + text)
  //}
  //taintAnnots.onInvalidate { _ => if (null != csub) { csub.cancel; csub = null} }

  def addTaint(t: Taint) = {
    if (! taintAnnots.get.contains(t)) {
      workArea.addTaint(t)
      taintAnnots.update(taintAnnots.get + t)
    }
  }
  def removeTaint(t: Taint) = {
    workArea.removeTaint(t)
    taintAnnots.update(taintAnnots.get.filter{_ != t})
  }

}
case class CodeWALAMethodParameter(
  _workArea: WorkArea,
  _m: Method,
  val parId: Int,
  _text: String
) extends CodeWALALine(_workArea, _m, null, 0, 0, _text) {

  override val defines = Some(parId)
}


case class CodeSourceLine(
  _workArea: WorkArea,
  n: Int,
  val oNum: Option[Int],
  _text: String
) extends CodeLine(_workArea, n, _text) {

  val originLineNumProp = new StringProperty(this, "bb number",
    oNum match {
      case Some(n) => n.toString
      case _ => "?"
    }
  )
}

case class CodeMethod(
  _workArea: WorkArea,
  _text: String
) extends CodePart(_workArea, _text)

class CodeView(
  val workArea: WorkArea
) extends TreeTableView[CodePart] {

  id = "CodeView"
  hgrow = Priority.ALWAYS
  vgrow = Priority.ALWAYS

  rowFactory = { _ => new TreeTableRow[CodePart]() {
    var sub    : Subscription = null

    item.onChange { (_,_,i) =>
      if (null != sub) {
        sub.cancel
        sub = null
      }
      if (null != i) {
        sub = Common.setClassViaSet[String, String](this, i.taints, Common.taintStyleMap, { a => a } )
      }
    }
  } }

  //  columnResizePolicy = {_:TreeTableView.ResizeFeatures[_] => false}
  sortPolicy = {_: TreeTableView[_] => false}

  val colControls = new TreeTableColumn[CodePart,Unit]("") {
    sortable = false
    prefWidth = 20
//    cellValueFactory = new ObjectProperty(this, "controls", ()
  }
  val colLineNum = new TreeTableColumn[CodePart,String]("L#") {
    sortable = false
    prefWidth = 30
    cellFactory = { _ => new CodeCell() }
    cellValueFactory = {temp =>
      val cp = temp.value.value.get
      cp match {
        case cm: CodeWALAMethodParameter =>
          new ObjectProperty(this, "line", "")

        case cl: CodeLine =>
          if (-1 == cl.num) {
            new ObjectProperty(this, "line", "")
          } else {
            cl.numProp
          }
        case cm: CodeMethod =>
          new ObjectProperty(this, "line", "")

        case cc: CodeClass =>
          new ObjectProperty(this, "line", "")
      }
    }
  }

  val colText = new TreeTableColumn[CodePart,String]("code") {
    sortable = false
    cellFactory = { _ => new CodeCell[CodePart, String]() }
    cellValueFactory = {feats =>
      val item = feats.value
      if (0 == item.children.length || item.expanded.get) {
        item.value.get.textProp
      } else {
        item.value.get match {
          case cm: CodeWALAMethodParameter => cm.textProp
          case cl: CodeWALALine => cl.hiddenBBNumProp
          case cl: CodeSourceLine => cl.textProp
          case cm: CodeMethod => cm.textProp
          case cc: CodeClass => cc.textProp
        }
      }
    }
  }

  columns ++= Seq(colControls, colLineNum, colText)
  //colText.prefWidth <== this.width - colControls.width - colLineNum.width - colBBNum.width

  val lineRoot: TreeItem[CodePart] = null
}

class WALAView(
  _workArea: WorkArea,
  m: Method
) extends CodeView(_workArea) {
  id = "WALAView"

  val colBBNum = new TreeTableColumn[CodePart,String]("B#") {
    sortable = false
    prefWidth = 30
    cellFactory = { _ => new CodeCell() }
    cellValueFactory = {temp =>
      temp.value.value.get match {
        case cm: CodeWALAMethodParameter => new ObjectProperty(this, "parameter", "")
        case cl: CodeWALALine => cl.bbNumProp
        case cm: CodeMethod => new ObjectProperty(this, "block", "")
      }
    }
  }

  columns.insert(1, colBBNum)

  if (null != m) {
    setMethod(m)
  } else {
    println("I don't know where to find: " + m.toString())
  }

  rowFactory = { _ => new TreeTableRow[CodePart]() {
    var sub: Subscription = null
    sub = item.onChange { (_, _, i) =>
      if (null != sub) {
        sub.cancel
        sub = null
      }

      i match {
        case wi: CodeWALALine =>
          sub = Common.setClassViaSet[Taint, String](this, wi.taintAnnots, Common.taintStyleMap, { a => a.tag})
        case _ => ()
      }
    } }
  }

  def setMethod(m: Method): Unit = {
    val newRoot = new TreeItem[CodePart](CodeMethod(workArea, m.method.getSignature().toString))

    val pars = 0 until m.method.getNumberOfParameters()
    pars.foreach { _parNum =>
      val parNum = _parNum + 1
      val typ = m.method.getParameterType(_parNum)
      val newItem = new TreeItem[CodePart](CodeWALAMethodParameter(workArea, m, parNum, "parameter v" + parNum.toString + " (" + typ.toString + ")"))
      newRoot.children += newItem
    }

    val cfg = m.ir.getControlFlowGraph()

    // TODO: I'm not sure exception blocks are being shown via the below:

    (0 until cfg.getNumberOfNodes()).foreach{ nodeNum =>
      val bb = cfg.getBasicBlock(nodeNum)
      val bbNum = bb.getNumber()

      val allins = bb.iteratePhis().toList ++ bb.iterateNormalInstructions().toList

      if (allins.length > 0) {
        val head = allins.head
        val tail = allins.tail

        val newBlock = new TreeItem[CodePart](new CodeWALALine(workArea, m, head, head.iindex, bbNum, head.toString))

        tail.foreach{ ins =>
          val newLine = new TreeItem[CodePart](new CodeWALALine(workArea, m, ins, ins.iindex, bbNum, ins.toString))
          newBlock.children += newLine
        }

        newRoot.children += newBlock
        newBlock.expanded = true
      }
    }
    newRoot.expanded = true
    root = newRoot
  }
}

class SourceView(
  _workArea: WorkArea,
  f: File
) extends CodeView(_workArea) {

  id = "SourceView"

  val lineMap: MMap[Int, CodeSourceLine] = MMap[Int, CodeSourceLine]()

  var hasOrigin = false

  val colOriginLineNum = new TreeTableColumn[CodePart,String]("OL#") {
    sortable = false
    prefWidth = 30
    cellFactory = { _ => new CodeCell() }
    cellValueFactory = {temp =>
      temp.value.value.get match {
        case cl: CodeSourceLine => cl.originLineNumProp
        case cc: CodeClass => new ObjectProperty(this, "origin line", "")
      }
    }
  }

  columns.insert(1, colOriginLineNum)

  if (null != f) {
    try {
      setSource(Util.slurpFile(f.toString))
    } catch {
      case e: Throwable => "Could not display file " + f.toString
    }
  }

  def taintLine(n: Int, t: String): Unit = {
    lineMap.get(n) match {
      case Some(scl) => scl.taints.update(scl.taints.get + t)
      // println("tainting " + scl.toString + " with " + t)
      case _ => println("cannot find line number " + n)
    }
  }

  def untaintLine(n: Int, t: String): Unit = {
    lineMap.get(n) match {
      case Some(scl) => scl.taints.update(scl.taints.get - t)
      // println("UNtainting " + scl.toString + " with " + t)
      case _ => println("cannot find line number " + n)
    }
  }

  def setSource(s: String): Unit = {
    val newRoot = new TreeItem[CodePart](new CodeClass(workArea, f.toString))
    hasOrigin = false
    lineMap.clear

    var codeLines: List[CodeSourceLine] = List[CodeSourceLine]()

    var lnum = 1
    s.split('\n').foreach{line =>
      // .*/* N+ */.*
      val originRE = """.*\/\* +(\d+) +\*\/.*""".r

      val olineNum: Option[Int] = line match {
        case originRE(olnum) =>
          hasOrigin = true
          Some(olnum.toInt)
        case _ => None
      }

      val codeLine = CodeSourceLine(workArea, lnum, olineNum, line)
      val newLine = new TreeItem[CodePart](codeLine)
      codeLines = codeLines ++ List(codeLine)

      newRoot.children += newLine
      lnum = lnum + 1
    }

    if (hasOrigin) {
      codeLines.foreach{ codeLine =>
        codeLine.oNum match {
          case Some(ln) => lineMap += (ln -> codeLine)
          case None => ()
        }

      }
    } else {
      codeLines.foreach { codeLine =>
        lineMap += (codeLine.num -> codeLine)
      }
    }

    newRoot.expanded = true
    root = newRoot
  }

}


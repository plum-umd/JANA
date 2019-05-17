import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.io.FileInputStream
import java.io.File

object Resources {
  val dirIcons = "resources/icons"

  def loadImage(f: String) = {
    try {new Image(new FileInputStream(f))}
    catch {
      case e: Exception =>
        println(e.toString)
        throw e
    }
  }

  val styleMain = new File("resources/style.css").toURI().toURL().toExternalForm()

  val iconArrowRight = loadImage(dirIcons + "/arrow_right.png")
  val iconArrowsOut = loadImage(dirIcons + "/arrow_out.png")
  val iconArrowsIn  = loadImage(dirIcons + "/arrow_in.png")
  val iconDouble    = loadImage(dirIcons + "/application_double.png")

  val iconCancel    = loadImage(dirIcons + "/cancel.png")
  val iconZoom      = loadImage(dirIcons + "/zoom.png")
  val iconFolder    = loadImage(dirIcons + "/folder.png")
  val iconHelp      = loadImage(dirIcons + "/help.png")
  val iconError     = loadImage(dirIcons + "/bullet_error.png")
  val iconBricks    = loadImage(dirIcons + "/bricks.png")
  val iconBriefcase = loadImage(dirIcons + "/briefcase.png")

  val iconWindows       = loadImage(dirIcons + "/application_cascade.png")
  val iconWindowGo      = loadImage(dirIcons + "/application_go.png")
  val iconWindowThunder = loadImage(dirIcons + "/application_lightning.png")

  val iconArrowsDivide = loadImage(dirIcons + "/arrow_divide.png")

}

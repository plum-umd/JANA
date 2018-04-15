import collection.JavaConversions._

import java.nio.file.{Files,Path,FileSystems}

import org.apache.commons.io.FilenameUtils

import scala.sys.process._

object Dirs {
  val dotExe   = Process("which dot").!!.trim
  val javaExe  = Process("which java").!!.trim
  val javapExe = Process("which javap").!!.trim

  val fs   = FileSystems.getDefault
  val base = FileSystems.getDefault.getPath(".invoker")
  base.toFile.mkdirs

  val tmp = base.resolve("tmp")
  tmp.toFile.mkdirs

  def subTmpDir(n: String): Path = {
    val temp = n.replace("/", ".")
    val newDir = tmp.resolve(temp)
    newDir.toFile.mkdirs
    newDir
  }
}

import java.io._
import java.util.zip._
import scala.collection.JavaConversions._

import java.nio.file._

import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.LocalDateTime

case class NotFound(s: String) extends Exception(s)
case class InitException(s: String) extends Exception(s)

abstract class Resource {
  def exists: Boolean
  def toFile: Entry.EntryFile
  def getPath: String
  def inside(file: String): Resource
}
case class Unknown(f: File) extends Resource {
  def this(f: String) = this(new File(f))

  def exists = false
  def toFile = throw NotFound(f.toString)
  def getPath = f.getPath()
  def inside(f: String) = this
}

object Log {
  var out: PrintStream = null
  var err: PrintStream = null

  reset

  def println(s: Any): Unit = {
    out.println(s.toString)
  }
  def print(s: Any): Unit = {
    out.print(s.toString)
  }
  def printlnErr(s: Any): Unit = {
    err.println(s.toString)
  }
  def printErr(s: Any): Unit = {
    err.print(s.toString)
  }

  def reset: Unit = {
    out = System.out
    err = System.err
  }

  def capture[A](f: => A): (Either[A, Throwable], String, String) = {
    val psOut = out
    val psErr = err

    val baosOut = new ByteArrayOutputStream()
    val baosErr = new ByteArrayOutputStream()
    val capOut = new PrintStream(baosOut)
    val capErr = new PrintStream(baosErr)

    out = capOut
    err = capErr

    val ret = try {
      Left(f)
    } catch {
      case e: Throwable => Right(e)
    } finally {
      out = psOut
      err = psErr
    }

    (ret, baosOut.toString, baosErr.toString)
  }
}

object Resource {
  def ofString(s: String): Resource = {
    val f = new File(s)
    if (f.isDirectory()) {
      Container.Dir(f)
    } else if (f.isFile()) {
      val p = f.getPath()
      p match {
        case Util.extension(fn, "jar") => Container.Zip(new ZipFile(p))
        case Util.extension(fn, "zip") => Container.Zip(new ZipFile(p))
        case _ => Entry.EntryFile(f)
      }
    } else {
      Unknown(f)
    }
  }
}

abstract class Container extends Resource {
}
object Container {
  case class Dir(file: File) extends Container {
    def this(file: String) = this(new File(file))
    def exists = true
    def toFile = throw NotFound(s"file is a directory, not a file")
    def getPath = file.getPath()

    def inside(inner: String): Resource = {
      val full = file.getPath() + File.separator + inner
      Resource.ofString(full)
    }
  }
  case class Zip(file: ZipFile) extends Container {
    val entries: Map[String,ZipEntry] =
      file.entries().toList.foldLeft(Map[String,ZipEntry]()){
        case (m, e) => m + (e.getName() -> e)
      }

    def toFile = new Entry.EntryFile(file.getName())
    def getPath = file.getName()

    override def toString = "Zip(" + this.getPath + ")"

    def this(file: String) = this(new ZipFile(file))

    def exists = true

    def inside(inner: String): Resource = {
      if (entries.contains(inner)) {
        Entry.EntryZip(file, entries(inner))
      } else {
        new Unknown(file.getName() + File.separator + inner)
      }
    }
  }
}


object Entry {
  case class EntryFile(file: File) extends Resource {
    def this(f: String) = this(new File(f))

    def toFile = this
    def getPath = file.getPath()
    def exists = true

    def inside(f: String) =
      if (file.toPath().endsWith("/" + f)) this else Unknown(new File(f))
  }

  case class EntryZip(zip: ZipFile, entry: ZipEntry) extends Resource {
    override def toString =
      "EntryZip " + entry.getName() + "\n" + "(inside): " + zip.getName()

    def getPath = zip.getName()
    def exists = true
    def toFile: EntryFile = {
      val temp = File.createTempFile(zip.toString, Util.baseName(entry.toString))
      temp.delete()
      val s = zip.getInputStream(entry)
      Files.copy(s, temp.toPath())
      Entry.EntryFile(temp)
    }
    def inside(f: String) = if (entry.getName() == f) this else Unknown(new File(f))
  }
}

object Util {
  val extension = """(.*)[.]([^.]*)""".r

  def baseName(f: String): String = {
    val p = new File(f)
    p.getName()
  }

  val osType =
    (System.getProperty("os.name").toLowerCase() + "_"
      + System.getProperty("os.arch"))
      .replace(' ', '_')

  def isZip(f: File): Boolean = {
    Files.probeContentType(f.toPath()) match {
      case x => Log.println(s"filetype of $f is $x")
    }
    false
  }

  val username = System.getenv("USER")

  val pathPwd = Resource
    .ofString(System.getProperty("user.dir"))
    .asInstanceOf[Container]
  val pathsClass =
    pathsSeperated(System.getProperty("java.class.path"))
      .map{Resource.ofString(_)}

  val pathsPwd = pathsAncestors(pathPwd.getPath)
    .map{Resource.ofString(_)}
    .filter(_.exists).asInstanceOf[List[Container]]

  def pathsAncestors(p: String) =
    p.split(File.separator)
      .inits.map(_.mkString(File.separator))
      .toList
  def pathsSeperated(p: String) =
    p.split(File.pathSeparator)
      .toList

  def findUnique(paths: List[Container], f: String): Resource = {
    val filenames = findOnPaths(paths, f)
    if (filenames.size == 0) {
      throw new NotFound("could not find file " + f
        + " on these paths:\n" + tab(paths.mkString("\n")))
    }
    if (filenames.size > 1) {
      throw new Exception("multiple paths found for " + f
        + ":\n" + tab(filenames.mkString("\n")))
    }
    filenames(0)
  }

  def findOne(paths: List[Resource], f: String): Resource = {
    val filenames = findOnPaths(paths, f)
    if (filenames.size == 0) {
      throw new NotFound("could not find file " + f
        + " on these paths:\n" + tab(paths.mkString("\n")))
    }
    filenames(0)
  }

  def findOnPaths(paths: List[Resource], f: String): List[Resource] = {
    paths
      .map(_.inside(f))
      .filter(_.exists)
  }

  def findAndLoadLib(l: String) = {
    val lib = System.mapLibraryName(l)
    val res = findOne(pathsClass,lib)
    val filename = res.toFile
    Core.ifVerbose{
      Log.println(s"loading $l:\n" +
        tab(s"from    : $filename\n" +
          s"(origin): $res"
        )
      )
    }
    System.load(filename.getPath)
  }

  def findAndLoadNativeLib(l: String) = {
    val lib = s"native/$osType/" + System.mapLibraryName(l)
    val res = findOne(pathsClass,lib)
    val filename = res.toFile
    Core.ifVerbose{
      Log.println(s"loading $l:\n" +
        tab(s"from    : $filename\n" +
          s"(origin): $res"
        )
      )
    }
    System.load(filename.getPath)
  }

  def tab(s: Any) = {
    s.toString.split("\n").map{ p => "\t" + p }.mkString("\n")
  }

  def mapInvert[A,B](m: Map[A,B]): Map[B,A] = {
    m.toList.foldLeft(Map[B,A]()) {
      case (m, (k,v)) => m + (v -> k)
    }
  }

  def showDebugInfo = {
    Log.println(s"os:\t$osType")
    Log.println(s"pathPwd:\t$pathPwd")
    Log.println(s"pathsClass:\n" + tab(pathsClass.mkString("\n")))
  }

  def mapsMergeWith[A,B](m1: Map[A,B], m2: Map[A,B], f: B => B => B): Map[A,B] = {
    m2.toList.foldLeft(m1){ case (m, (k,v2)) =>
      m.get(k) match {
        case Some(v1) => m + (k -> (f(v1)(v2)))
        case None => m + (k -> v2)
      }
    }
  }

  def setsUnion[A](sets: Iterable[Set[A]])(implicit default: Set[A] = Set()): Set[A] = {
    if (sets.isEmpty) default else 
      sets.tail.foldLeft(sets.head){case (accum, aset) => accum ++ aset}
  }

  def setsIntersection[A](sets: Iterable[Set[A]])(implicit default: Set[A] = Set()): Set[A] = {
    if (sets.isEmpty) default else
      sets.tail.foldLeft(sets.head){case (accum, aset) => accum.intersect(aset)}
  }

  import java.nio.file.Files
  import java.nio.charset.StandardCharsets

  def slurpFile(filename: String): String = {
    val encoding = StandardCharsets.UTF_8
    Files.readAllLines(Paths.get(filename), encoding).mkString("\n");
  }

  val formatStyleLong = FormatStyle.FULL
  val formatStyleShort = FormatStyle.SHORT

  val formatLong  = DateTimeFormatter.ofLocalizedTime(formatStyleLong)
  val formatShort = DateTimeFormatter.ofLocalizedTime(formatStyleShort)

  def shortTimestamp: String = {
    formatShort.format(LocalDateTime.now)
  }
  def longTimestamp: String = {
    formatLong.format(LocalDateTime.now)
  }

  type TaintResult = Map[String, List[(String, Int)]]

  def parseTaint(jsonString: String): TaintResult = {
    val jsonVal = Json.parse(jsonString)

    val taintVal = (jsonVal \ "taint" \ "results").as[List[JsValue]].map( v => ((v \ "method").as[String], (v \ "line").as[Int]))
    val secretVal = (jsonVal \ "secret" \ "results").as[List[JsValue]].map( v => ((v \ "method").as[String], (v \ "line").as[Int]))

    Map("taint" -> taintVal, "secret" -> secretVal)
  }
}

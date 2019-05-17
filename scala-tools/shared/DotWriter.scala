import java.io.{File,PrintWriter,FileWriter,BufferedWriter,OutputStream}
import java.nio.file.{Files,Path}
import java.lang.{Process,ProcessBuilder}
import java.lang.ProcessBuilder.{Redirect}

class DotWriter(
  val svgFile : File,
  var logFile : File,
  var process : Process,
  var outStream : OutputStream, // stream to DOTTY
  var outWriter : PrintWriter,   // stream to DOTTY
  var logWriter : PrintWriter
) {

  def this(fn_piece:String, outDir:Path) = {
    this(File.createTempFile("fun_"+fn_piece+"_", ".svg", outDir.toFile()),
      null, null, null, null, null);

    // the DOT file
    logFile = new File(svgFile.getAbsolutePath().replaceAll(".svg",".dot"))
    logWriter = new PrintWriter(logFile)

    // the DOTTY process
    val pb = new ProcessBuilder("dot", "-Tsvg");
    pb.redirectErrorStream(true);
    pb.redirectOutput(Redirect.appendTo(svgFile));
    process = pb.start();
    outStream = process.getOutputStream();
    outWriter = new PrintWriter(outStream);
  }

  def write(s:String) = {
    outWriter.write(s)
    logWriter.write(s)
  }
  def close() = {
    logWriter.close()
    outWriter.close()
    outStream.flush()
    outStream.close()
    process.waitFor()
  }
  def getOutputFileName() : String = {
    return svgFile.getName()
  }
  def getOutputFilePath() : String = {
    return svgFile.getAbsolutePath()
  }
}

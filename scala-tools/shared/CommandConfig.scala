import com.typesafe.config.ConfigFactory

import java.io._

case class ConfigException(s:String) extends Exception(s)

object CommandConfig {
  val file_java_runtime = Util.findUnique(
    Util.pathsPwd,
    "environment" + File.separator + "rt.jar"
  ).getPath

  def config(dep: String, sig: String, exclusions: String) = {
    ConfigFactory.load().withFallback(ConfigFactory.parseString("""

wala {
  jre-lib-path = """ + file_java_runtime + """
  exclusions: " """ + exclusions + """ "
  dependencies.binary += """ + "\"" + dep + """"
  entry {
    signature-pattern = """ + "\"" + sig + """"
  }
}
    """)).resolve()
  }
}

name := "invoker"

organization := "edu.umd.soucis"

version := "0.1"

scalaVersion := "2.11.7"

isSnapshot := true

unmanagedClasspath in Runtime += baseDirectory.value / "../lib"
unmanagedClasspath in (Compile, runMain) += baseDirectory.value / "../lib"
//javaOptions in run += "-Djava.library.path=" + baseDirectory.value + "/../lib"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

//Define dependencies. These ones are only required for Test and Integration Test scopes.
libraryDependencies ++= Seq(
  "org.clapper" %% "argot" % "1.0.3",
  "com.ibm.wala" % "com.ibm.wala.shrike" % "1.3.8-SNAPSHOT",
  "com.ibm.wala" % "com.ibm.wala.util" % "1.3.8-SNAPSHOT",
  "com.ibm.wala" % "com.ibm.wala.core" % "1.3.8-SNAPSHOT",
  "edu.illinois.wala" %% "walafacade" % "0.1.2",
  "com.bugseng.ppl" % "ppl" % "1.1" from "file://" + baseDirectory.value + "/../lib/ppl_java.jar",
  "edu.umd.soucis" %% "scalashared" % "0.1",
  "edu.umd.soucis" %% "ainterp" % "0.1",
  "org.scalafx" %% "scalafx" % "8.0.60-R10-SNAPSHOT",
//  "de.codecentric.centerdevice" % "centerdevice-nsmenufx" % "2.0.0",
  "commons-io" % "commons-io" % "2.4"
//  "org.fxmisc.richtext" % "richtextfx" % "1.0.0-SNAPSHOT"
//  "commons-lang" % "commons-lang" % "2.3"
)

fork in run := true

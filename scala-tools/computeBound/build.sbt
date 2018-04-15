name := "computebound"

organization := "edu.umd.soucis"

version := "0.1"

scalaVersion := "2.11.7"

isSnapshot := true

unmanagedClasspath in Runtime += baseDirectory.value / "../lib"
unmanagedClasspath in (Compile, runMain) += baseDirectory.value / "../lib"
//javaOptions in run += "-Djava.library.path=" + baseDirectory.value + "/../lib"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
//Define dependencies. These ones are only required for Test and Integration Test scopes.
libraryDependencies ++= Seq(
  "org.clapper" %% "argot" % "1.0.3",
  "com.ibm.wala" % "com.ibm.wala.shrike" % "1.3.10-SNAPSHOT",
  "com.ibm.wala" % "com.ibm.wala.util" % "1.3.10-SNAPSHOT",
  "com.ibm.wala" % "com.ibm.wala.core" % "1.3.10-SNAPSHOT",
  "edu.illinois.wala" %% "walafacade" % "0.1.2",
  "com.bugseng.ppl" % "ppl" % "1.3" from "file://" + baseDirectory.value + "/../lib/ppl_java.jar",
//  "edu.joana" % "api" % "1.0",
//  "edu.joana" % "sdg" % "1.0",
//  "soucis" % "taint" % "0.0.1",
  "edu.umd.soucis" %% "scalashared" % "0.1"
)

fork in run := true

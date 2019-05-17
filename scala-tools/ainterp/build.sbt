lazy val commonSettings = Seq(
  organization := "edu.umd.soucis",
  scalaVersion := "2.11.7",
  isSnapshot   := true,

  unmanagedResourceDirectories in Compile  += { baseDirectory.value / "/../lib" },
  unmanagedClasspath in Runtime            +=   baseDirectory.value / "/../lib",
  unmanagedClasspath in (Compile, runMain) +=   baseDirectory.value / "/../lib",

  resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",

  libraryDependencies ++= Seq(
    "org.clapper"       %% "argot"               % "1.0.3",
    "com.ibm.wala"       % "com.ibm.wala.shrike" % "1.3.10-SNAPSHOT",
    "com.ibm.wala"       % "com.ibm.wala.util"   % "1.3.10-SNAPSHOT",
    "com.ibm.wala"       % "com.ibm.wala.core"   % "1.3.10-SNAPSHOT",
    "edu.illinois.wala" %% "walafacade"          % "0.1.2",
    "com.bugseng.ppl"    % "ppl"                 % "1.3" from "file://" + baseDirectory.value + "/../lib/ppl_java.jar",
    "edu.umd.soucis"    %% "scalashared"         % "0.1",
    "edu.umd.soucis" 	%% "computebound" 	 % "0.1",
    "org.scalaz"        %% "scalaz-core"         % "7.1.4",
    "net.jcazevedo"     %% "moultingyaml"        % "0.1",
    "com.typesafe.play" %% "play-json"           % "2.3.9",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
//    "com.fasterxml.jackson.core"       % "jackson-core"            % "2.1.1",
//    "com.fasterxml.jackson.core"       % "jackson-annotations"     % "2.1.1",
//    "com.fasterxml.jackson.core"       % "jackson-databind"        % "2.1.1",
//    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.1.1"
  ),

  fork in run := true,

  assemblyMergeStrategy in assembly := {
    case PathList("plugin.properties") => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val ainterp = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
  name := "ainterp",
    version := "0.1",
    mainClass in assembly := Some("CLI"),
    mainClass in (Compile, run) := Some("CLI"),
    assemblyOutputPath in assembly := baseDirectory.value / "/../AInterp.jar"
)

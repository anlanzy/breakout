import sbt.Keys._

name := "breakout"


val scalaV = "2.12.6"
val projectName = "breakout"
val projectVersion = "2019.01.31"
val projectMainClass = "com.neo.sk.breakout.Boot"

def commonSettings = Seq(
  version := projectVersion,
  scalaVersion := scalaV,
  scalacOptions ++= Seq(
    //"-deprecation",
    "-feature"
  )
)

import sbtcrossproject.{crossProject, CrossType}

lazy val root = (project in file("."))
  .aggregate(frontend, backend)
  .settings(name := projectName)



// Scala-Js frontend
lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "frontend")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false )
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "io.suzaku" %%% "diode" % "1.1.2",
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1",
      //"com.lihaoyi" %%% "upickle" % "0.6.6",
      "com.lihaoyi" %%% "scalatags" % "0.6.5",
      "org.scala-lang.modules" %% "scala-swing" % "2.0.1",
      "org.seekloud" %%% "byteobject" % "0.1.1",
      "org.seekloud" %% "essf" % "0.0.1-beta3"
      //"org.scala-js" %%% "scalajs-java-time" % scalaJsJavaTime
      //"com.lihaoyi" %%% "utest" % "0.3.0" % "test"
    )
  )
  .dependsOn(sharedJs)

// Akka Http based backend
lazy val backend = (project in file("backend"))
  .enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(projectMainClass),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "backend")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("breakout" -> projectMainClass),
    packJvmOpts := Map("breakout" -> Seq("-Xmx64m", "-Xms32m")),
    packExtraClasspath := Map("breakout" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies ++ Dependencies.grpcSeq
    //libraryDependencies ++= Dependencies.backendDependencies
  )
  .settings {
  (resourceGenerators in Compile) += Def.task {
    val fastJsOut = (fastOptJS in Compile in frontend).value.data
    val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
    Seq(
      fastJsOut,
      fastJsSourceMap
    )
  }.taskValue
}
  .settings((resourceGenerators in Compile) += Def.task {
  Seq(
    (packageJSDependencies in Compile in frontend).value
    //(packageMinifiedJSDependencies in Compile in frontend).value
  )
}.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in frontend).value,
    watchSources ++= (watchSources in frontend).value
  )
  .dependsOn(sharedJvm)



lazy val shared = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("shared"))
  .settings(name := "shared")
  .settings(commonSettings: _*)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js









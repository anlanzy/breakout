import sbt.Keys._

name := "hiStream"


val scalaV = "2.11.8"
val scalaXmlV = "1.0.4"
val akkaV = "2.4.9"
val hikariCpV = "2.4.3"
val slickV = "3.1.1"
val logbackV = "1.1.3"
val nscalaTimeV = "2.10.0"
val codecV = "1.9"
val sprayV = "1.3.3"
val playComponentV = "2.5.4"
val postgresJdbcV = "9.4.1208"
val playJsonForAkkaHttp = "1.7.0"


val scalaJsDomV = "0.9.0"
val scalaJsjqueryV = "0.9.0"
val upickleV = "0.4.2"

lazy val root = (project in file("."))
  .aggregate(frontend, backend)


// Scala-Js frontend
lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "hiStream_Frontend")
  .settings(commonSettings: _*)
  .settings(skip in packageJSDependencies := false )
  .settings(
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomV,
      "be.doeraene" %%% "scalajs-jquery" % scalaJsjqueryV,
      "com.lihaoyi" %%% "upickle" % upickleV,
      "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
    )
  )
  .dependsOn(sharedJs)



val projectMainClass = "com.neo.sk.hiStream.Boot"

// Akka Http based backend
lazy val backend = (project in file("backend"))
  .settings(
    Revolver.settings.settings,
    mainClass in Revolver.reStart := Some(projectMainClass)
  ).settings(name := "hiStream_Backend")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    packSettings,

    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("hiStream" -> projectMainClass),
    packJvmOpts := Map("hiStream" -> Seq("-Xmx256m", "-Xms64m")),
    packExtraClasspath := Map("hiStream" -> Seq("."))
  )
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaV,
      "org.scala-lang.modules" % "scala-xml_2.11" % scalaXmlV,
      "com.typesafe.akka" %% "akka-actor" % akkaV withSources() withSources(),
      "com.typesafe.akka" %% "akka-slf4j" % akkaV,
      "com.typesafe.akka" %% "akka-stream" % akkaV,
      "com.typesafe.akka" %% "akka-http-core" % akkaV withSources(),
      "com.typesafe.akka" %% "akka-http-experimental" % akkaV withSources(),
      "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
      "de.heikoseeberger" %% "akka-http-play-json" % playJsonForAkkaHttp,
      "io.spray" %% "spray-caching" % sprayV withSources(),
      "com.typesafe.slick" %% "slick" % slickV withSources(),
      "com.typesafe.slick" %% "slick-codegen" % slickV,
      "com.typesafe.play" %% "play-ws" % playComponentV,
      "com.typesafe.play" %% "play-json" % playComponentV,
      "com.zaxxer" % "HikariCP" % hikariCpV,
      "ch.qos.logback" % "logback-classic" % logbackV withSources(),
      "com.github.nscala-time" %% "nscala-time" % nscalaTimeV,
      "commons-codec" % "commons-codec" % codecV,
      "com.lihaoyi" %% "upickle" % upickleV,
      "org.postgresql" % "postgresql" % postgresJdbcV
    ),
    (resourceGenerators in Compile) <+=
    (fastOptJS in Compile in frontend, packageScalaJSLauncher in Compile in frontend)
      .map { (f1, f2) => Seq(f1.data, f2.data) },
    watchSources <++= (watchSources in frontend)
  )
  .dependsOn(sharedJvm)



lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(name := "hiStream_shared")
  .settings(commonSettings: _*)


lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

def commonSettings = Seq(
  version := "1.2.0",
  scalaVersion := scalaV
)






logLevel := Level.Warn
val sbtRevolverV = "0.7.2"
val sbtAssemblyV = "0.13.0"
val sbtPackV = "0.7.9"
val sbtScalaJsV = "0.6.12"
val coursierV = "1.0.0-M14"

addSbtPlugin("io.get-coursier" % "sbt-coursier" % coursierV)

addSbtPlugin("io.spray" % "sbt-revolver" % sbtRevolverV)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % sbtAssemblyV)

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % sbtPackV)

addSbtPlugin("org.scala-js" % "sbt-scalajs" % sbtScalaJsV)



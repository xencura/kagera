import sbt._
import sbt.Keys._
import spray.revolver.RevolverPlugin.Revolver
import com.trueaccord.scalapb.{ ScalaPbPlugin => PB }
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends Build {

  import Dependencies._
  import Formatting._

  val commonScalacOptions = Seq(
    "-encoding",
    "utf8",
    "-target:jvm-1.8",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked",
    "-deprecation",
    "-Xlog-reflective-calls"
  )

  lazy val basicSettings = Seq(
    organization := "io.kagera",
    scalaVersion := "2.11.8",
    scalacOptions := commonScalacOptions,
    incOptions := incOptions.value.withNameHashing(true)
  )

  lazy val scalaPBSettings = PB.protobufSettings ++ Seq(
    PB.runProtoc in PB.protobufConfig := (args => com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray))
  )

  lazy val defaultProjectSettings =
    basicSettings ++ formattingSettings ++ Revolver.settings ++ SonatypePublish.settings ++ scalaPBSettings

  lazy val api = Project("api", file("api"))
    .settings(defaultProjectSettings: _*)
    .settings(
      name := "kagera-api",
      libraryDependencies ++= Seq(scalaGraph, catsCore, fs2Core, shapeless, scalatest % "test")
    )

  lazy val visualization = Project("visualization", file("visualization"))
    .dependsOn(api)
    .settings(defaultProjectSettings: _*)
    .settings(name := "kagera-visualization", libraryDependencies ++= Seq(scalaGraph, scalaGraphDot))

  lazy val akka = Project("akka", file("akka"))
    .dependsOn(api)
    .settings(
      defaultProjectSettings ++ Seq(
        name := "kagera-akka",
        libraryDependencies ++= Seq(
          scalaReflect,
          akkaActor,
          akkaPersistence,
          akkaSlf4j,
          scalaGraph,
          akkaTestkit % "test",
          scalatest % "test"
        )
      )
    )

  lazy val demo = (crossProject.crossType(CrossType.Full) in file("demo"))
    .settings(defaultProjectSettings: _*)
    .settings(
      unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala",
      libraryDependencies ++= Seq("com.lihaoyi" %%% "scalatags" % "0.4.6", "com.lihaoyi" %%% "upickle" % "0.4.2")
    )
    .jsSettings(
      jsDependencies ++= Seq(
        "org.webjars.bower" % "cytoscape" % cytoscapeVersion
          / s"$cytoscapeVersion/dist/cytoscape.js"
          minified s"$cytoscapeVersion/dist/cytoscape.min.js"
          commonJSName "cytoscape"
      ),
      libraryDependencies ++= Seq("org.scala-js" %%% "scalajs-dom" % "0.8.0")
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "de.heikoseeberger" %% "akka-http-upickle" % "1.10.1",
        akkaHttp,
        akkaPersistenceQuery,
        akkaPersistenceCassandra
      ),
      name := "demo-app",
      mainClass := Some("io.kagera.demo.Main")
    )

  lazy val demoJs = demo.js
  lazy val demoJvm = demo.jvm
    .dependsOn(api, visualization, akka)
    .settings(
      // include the compiled javascript result from js module
      (resources in Compile) += (fastOptJS in (demoJs, Compile)).value.data,
      // include the javascript dependencies
      (resources in Compile) += (packageJSDependencies in (demoJs, Compile)).value
    )

  lazy val root = Project("kagera", file("."))
    .aggregate(api, akka, visualization)
    .settings(defaultProjectSettings)
    .settings(publish := {})
}

import sbt._
import Keys._
import sbtassembly.AssemblyKeys
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import spray.revolver.RevolverPlugin.Revolver

object Build extends Build {

  import Dependencies._
  import Formatting._
  import AssemblyKeys._

  val targetScalaVersion = "2.11.7"

  val basicScalacOptions = Seq(
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
    organization := "io.process",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := targetScalaVersion,
    scalacOptions := basicScalacOptions,
    incOptions := incOptions.value.withNameHashing(true)
  )

  lazy val basicProjectSettings = basicSettings ++ formattingSettings ++ Revolver.settings

  lazy val common = (crossProject.crossType(CrossType.Pure) in file("common"))
    .settings(basicProjectSettings: _*)
    .settings(name := "statebox-common")
    .jvmSettings(libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.3")
    .jsSettings(libraryDependencies += "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.3")

  lazy val commonJs = common.js
  lazy val commonJvm = common.jvm

  lazy val stateboxApi = Project("statebox-api", file("statebox"))
    .settings(basicProjectSettings: _*)
    .settings(mainClass := Some("io.statebox.Main"))
    .settings(
      libraryDependencies ++= Seq(
        akkaActor,
        akkaPersistence,
        akkaSlf4j,
        akkaHttp,
        ficus,
        graph,
        graphDot,
        logback,
        scalaTime,
        akkaTestkit % "test",
        scalatest % "test"
      )
    )
    .dependsOn(commonJvm)

  lazy val frontend = Project("statebox-frontend", file("frontend"))
    .enablePlugins(ScalaJSPlugin)
    .settings(persistLauncher in Compile := true)
    .settings(basicProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.1",
        "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.3",
        "com.lihaoyi" %%% "scalatags" % "0.5.1"
      )
    )
    .dependsOn(commonJs)

  lazy val root = Project("statebox", file(".")).aggregate(stateboxApi, frontend)
}

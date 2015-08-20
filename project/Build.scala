import sbt._
import Keys._
import sbtassembly.AssemblyKeys
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

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
    version := "0.1.0",
    scalaVersion := targetScalaVersion,
    scalacOptions := basicScalacOptions,
    incOptions := incOptions.value.withNameHashing(true)
  )

  lazy val appSettings = basicSettings ++ dependencySettings ++ formattingSettings

  lazy val statebox = Project("statebox-api", file("statebox"))
    .settings(appSettings: _*)
    .settings(assemblyJarName := "statebox.jar")
    .settings(mainClass := Some("io.statebox.Main"))
    .settings(
      libraryDependencies ++=
        compile(akkaActor, akkaPersistence, akkaSlf4j, akkaHttp, ficus, graph, logback, scalaTime) ++
          test(akkaTestkit, scalatest)
    )

  lazy val frontend = Project("statebox-frontend", file("frontend"))
    .enablePlugins(ScalaJSPlugin)
    .settings(persistLauncher in Compile := true)
    .settings(appSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(
          "org.scala-js" %%% "scalajs-dom" % "0.8.1",
          "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.3",
          "com.lihaoyi" %%% "scalatags" % "0.5.1"
        )
    )

  lazy val root = Project("statebox", file("."))
    .settings(basicSettings: _*)
    .aggregate(statebox, frontend)
}

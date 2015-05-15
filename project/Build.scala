import sbt._
import Keys._
import sbtassembly.AssemblyKeys
import spray.revolver.RevolverPlugin.Revolver
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends Build {

  import BuildInfo._
  import Dependencies._
  import Formatting._
  import AssemblyKeys._

  val targetScalaVersion = "2.11.5"

  // ScalacOptions
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

  lazy val libSettings = basicSettings ++ dependencySettings ++ formattingSettings
  lazy val appSettings = libSettings ++ Revolver.settings

  lazy val root = Project("statebox", file("."))
    .settings(basicSettings: _*)
    .aggregate(statebox, frontend)

  lazy val statebox = Project("statebox-api", file("statebox"))
    .settings(appSettings: _*)
    .settings(assemblyJarName := "statebox.jar")
    .settings(mainClass := Some("io.statebox.Main"))
    .settings(buildInfoGeneratorSettings("io.statebox"): _*)
    .settings(javaOptions in Revolver.reStart := List("-Dconfig.file=../etc/local.conf"))
    .settings(
      libraryDependencies ++=
        compile(
          akkaActor,
          akkaPersistence,
          akkaSlf4j,
          sprayCan,
          ficus,
          graph,
          sprayRouting,
          sprayClient,
          sprayJson,
          logback,
          logstashLogbackEncoder,
          scalaTime
        ) ++
          test(akkaTestkit, sprayTestkit, scalatest)
    )

  lazy val frontend = Project("statebox-frontend", file("frontend"))
    .enablePlugins(ScalaJSPlugin)
    .settings(persistLauncher in Compile := true)
    .settings(appSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(
          "org.scala-js" %%% "scalajs-dom" % "0.8.0",
          "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.1-2",
          "com.lihaoyi" %%% "scalatags" % "0.5.1"
        )
    )
}

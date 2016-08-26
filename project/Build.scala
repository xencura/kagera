import sbt._
import sbt.Keys._
import spray.revolver.RevolverPlugin.Revolver
import com.trueaccord.scalapb.{ ScalaPbPlugin => PB }

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
    basicSettings ++ formattingSettings ++ Revolver.settings ++ Sonatype.settings ++ scalaPBSettings

  //  lazy val common = (crossProject.crossType(CrossType.Pure) in file("common"))
  //    .settings(defaultProjectSettings: _*)
  //    .settings(name := "kagera-common")
  //    .jvmSettings(libraryDependencies += scalazCore)
  //    .jsSettings(libraryDependencies += "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.3")

  //  lazy val commonJs = common.js
  //  lazy val commonJvm = common.jvm

  //  lazy val frontend = Project("frontend", file("frontend"))
  //    .dependsOn(commonJs)
  //    .enablePlugins(ScalaJSPlugin)
  //    .settings(defaultProjectSettings ++ Seq(
  //      persistLauncher in Compile := true,
  //      libraryDependencies ++= Seq(
  //      "org.scala-js"                    %%% "scalajs-dom" % "0.8.1",
  //      "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.3",
  //      "com.lihaoyi"                     %%% "scalatags"   % "0.5.1")
  //    ))

  lazy val api = Project("api", file("api"))
    .settings(defaultProjectSettings: _*)
    .settings(name := "kagera-api", libraryDependencies ++= Seq(graph, shapeless, scalaReflect, scalatest % "test"))

  lazy val visualization = Project("visualization", file("visualization"))
    .dependsOn(api)
    .settings(defaultProjectSettings: _*)
    .settings(name := "kagera-visualization", libraryDependencies ++= Seq(graph, graphDot))

  lazy val akkaImplementation = Project("akka", file("akka"))
    .dependsOn(api)
    .settings(
      defaultProjectSettings ++ Seq(
        name := "kagera-akka",
        mainClass := Some("io.kagera.akka.Main"),
        libraryDependencies ++= Seq(
          akkaActor,
          akkaPersistence,
          akkaSlf4j,
          graph,
          akkaTestkit % "test",
          scalatest % "test"
        )
      )
    )

  lazy val root = Project("kagera", file("."))
    .aggregate(api, akkaImplementation, visualization)
    .settings(defaultProjectSettings)
    .settings(publish := {})
}

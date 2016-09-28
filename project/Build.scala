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
    basicSettings ++ formattingSettings ++ Revolver.settings ++ Sonatype.settings ++ scalaPBSettings

  lazy val api = Project("api", file("api"))
    .settings(defaultProjectSettings: _*)
    .settings(name := "kagera-api", libraryDependencies ++= Seq(graph, shapeless, scalatest % "test"))

  lazy val visualization = Project("visualization", file("visualization"))
    .dependsOn(api)
    .settings(defaultProjectSettings: _*)
    .settings(name := "kagera-visualization", libraryDependencies ++= Seq(graph, graphDot))

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
          graph,
          akkaTestkit % "test",
          scalatest % "test"
        )
      )
    )

  lazy val analyse = Project("analyse", file("analyse"))
    .dependsOn(akka)
    .settings(
      defaultProjectSettings ++ Seq(
        resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven",
        name := "kagera-analyse",
        libraryDependencies ++= Seq(akkaAnalyticsCassandra, akkaHttp)
      )
    )

  lazy val demo = (crossProject.crossType(CrossType.Full) in file("demo"))
    .settings(defaultProjectSettings: _*)
    .settings(
      unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala",
      libraryDependencies ++= Seq("com.lihaoyi" %%% "scalatags" % "0.4.6")
    )
    .jsSettings(libraryDependencies ++= Seq("org.scala-js" %%% "scalajs-dom" % "0.8.0"))
    .jvmSettings(
      libraryDependencies ++= Seq(akkaHttp, akkaPersistenceQuery, akkaPersistenceCassandra),
      name := "demo-app",
      mainClass := Some("io.kagera.demo.Main")
    )

  lazy val demoJs = demo.js
  lazy val demoJvm = demo.jvm
    .dependsOn(api, visualization, akka, analyse)
    .settings(
      // include javascript compiled resources from js module
      (resources in Compile) += (fastOptJS in (demoJs, Compile)).value.data
    )

  lazy val root = Project("kagera", file("."))
    .aggregate(api, akka, visualization)
    .settings(defaultProjectSettings)
    .settings(publish := {})
}

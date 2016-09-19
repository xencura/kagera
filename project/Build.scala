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

  lazy val demo = Project("demo", file("demo"))
    .dependsOn(api, visualization, akka, analyse)
    .settings(
      defaultProjectSettings ++ Seq(
        libraryDependencies ++= Seq(akkaHttp, akkaPersistenceQuery, akkaPersistenceCassandra),
        name := "kagera-demo-app",
        mainClass := Some("io.kagera.demo.Main")
      )
    )

  lazy val root = Project("kagera", file("."))
    .aggregate(api, akka, visualization)
    .settings(defaultProjectSettings)
    .settings(publish := {})
}

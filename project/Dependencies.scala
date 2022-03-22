import sbt.Keys._
import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val akkaVersion = "2.6.19"
  val akkaHttpVersion = "10.2.9"
  val sprayVersion = "1.3.2"
  val scalazVersion = "7.1.3"
  val zioVersion = "1.0.13"
  val zioActorsVersion = "0.0.9"
  val cytoscapeVersion = "3.2.5"

  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.7.0"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaHttpUpickle = "de.heikoseeberger" %% "akka-http-upickle" % "1.39.2"
  val akkaCoordination = "com.typesafe.akka" %% "akka-coordination" % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
  val akkaPki = "com.typesafe.akka" %% "akka-pki" % akkaVersion
  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % "1.5.0")
  val akkaInmemoryJournal = "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2"

  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.3.5"

  val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5"

  val scalaGraph = Def.setting("org.scala-graph" %%% "graph-core" % "1.13.3")
  val scalaGraphDot = "org.scala-graph" %% "graph-dot" % "1.13.3"
  val scalaJsDom = Def.setting("org.scala-js" %%% "scalajs-dom" % "2.1.0")
  val laminar = Def.setting("com.raquo" %%% "laminar" % "0.14.2")
  val d3 = Def.setting("com.github.xencura.scala-js-d3v4" %%% "scala-js-d3v4" % "be1e1c8")
  val scalaTags = Def.setting("com.lihaoyi" %%% "scalatags" % "0.11.1")

  val fs2Core = Def.setting("co.fs2" %%% "fs2-core" % "3.2.5")
  val catsCore = Def.setting("org.typelevel" %%% "cats-core" % "2.7.0")
  val catsEffect = Def.setting("org.typelevel" %%% "cats-effect" % "3.3.8")

  val zioCore = "dev.zio" %% "zio" % zioVersion
  val zioInteropCats = "dev.zio" %% "zio-interop-cats" % "3.2.9.1"
  val zioTest = "dev.zio" %% "zio-test" % zioVersion
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion
  val zioActors = "dev.zio" %% "zio-actors" % zioActorsVersion
  val zioActorsPersistence = "dev.zio" %% "zio-actors-persistence" % zioActorsVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.5"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val scalatest = "org.scalatest" %% "scalatest" % "3.2.11"
}

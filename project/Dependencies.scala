import sbt.Keys._
import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val akkaVersion = "2.5.32"
  val akkaHttpVersion = "10.1.12"
  val sprayVersion = "1.3.2"
  val scalazVersion = "7.1.3"
  val zioVersion = "1.0.3"
  val zioActorsVersion = "0.0.7+25-34551a32-SNAPSHOT"
  val cytoscapeVersion = "2.7.9"

  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.1.12"
  val akkaInmemoryJournal = "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2"

  val akkaAnalyticsCassandra = "com.github.krasserm" %% "akka-analytics-cassandra" % "0.3.1"
  val akkaAnalyticsKafka = "com.github.krasserm" %% "akka-analytics-kafka" % "0.3.1"

  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.2.6"

  val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.103"


  val scalaGraph = Def.setting("org.scala-graph" %%% "graph-core" % "1.13.2")
  val scalaGraphDot = "org.scala-graph" %% "graph-dot" % "1.13.0"

  val fs2Core = "co.fs2" %% "fs2-core" % "2.5.9"
  val catsCore = "org.typelevel" %% "cats-core" % "2.6.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.4"

  val zioCore = "dev.zio" %% "zio" % zioVersion
  val zioInteropCats = "dev.zio" %% "zio-interop-cats" % "2.1.4.0"
  val zioTest = "dev.zio" %% "zio-test" % zioVersion
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion
  val zioActors = "dev.zio" %% "zio-actors" % zioActorsVersion
  val zioActorsPersistence = "dev.zio" %% "zio-actors-persistence" % zioActorsVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val scalatest = "org.scalatest" %% "scalatest" % "3.2.9"
}

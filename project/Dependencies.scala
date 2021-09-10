import sbt.Keys._
import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val akkaVersion = "2.6.16"
  val akkaHttpVersion = "10.1.12"
  val sprayVersion = "1.3.2"
  val scalazVersion = "7.1.3"
  val cytoscapeVersion = "3.2.5"

  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.2.6"
  val akkaHttpUpickle = "de.heikoseeberger" %% "akka-http-upickle" % "1.37.0"
  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % "1.4.1")
  val akkaInmemoryJournal = "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2"

  val akkaAnalyticsCassandra = "com.github.krasserm" %% "akka-analytics-cassandra" % "0.3.1"
  val akkaAnalyticsKafka = "com.github.krasserm" %% "akka-analytics-kafka" % "0.3.1"

  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.3.5"

  val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5"

  val scalaGraph = "org.scala-graph" %% "graph-core" % "1.13.1"
  val scalaGraphDot = "org.scala-graph" %% "graph-dot" % "1.13.0"
  val scalaJsDom = Def.setting("org.scala-js" %%% "scalajs-dom" % "1.0.0")
  val scalaTags = Def.setting("com.lihaoyi" %%% "upickle" % "1.4.1")

  val fs2Core = "co.fs2" %% "fs2-core" % "3.1.2"
  val catsCore = "org.typelevel" %% "cats-core" % "2.6.1"

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.5"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val scalatest = "org.scalatest" %% "scalatest" % "3.2.9"
}

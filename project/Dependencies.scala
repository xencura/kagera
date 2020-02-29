import sbt.Keys._
import sbt._

object Dependencies {

  val akkaVersion = "2.4.12"
  val akkaHttpVersion = "2.4.11"
  val sprayVersion = "1.3.2"
  val scalazVersion = "7.1.3"
  val cytoscapeVersion = "2.7.9"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaQuery = "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion
  val akkaInmemoryJournal = "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.3.14"

  val akkaAnalyticsCassandra = "com.github.krasserm" %% "akka-analytics-cassandra" % "0.3.1"
  val akkaAnalyticsKafka = "com.github.krasserm" %% "akka-analytics-kafka" % "0.3.1"

  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.2.6"

  val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.18"
  val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion

  val scalaGraph = "com.assembla.scala-incubator" %% "graph-core" % "1.10.1"
  val scalaGraphDot = "com.assembla.scala-incubator" %% "graph-dot" % "1.10.1"

  val fs2Core = "co.fs2" %% "fs2-core" % "0.9.1"
  val catsCore = "org.typelevel" %% "cats-core" % "0.7.2"

  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val scalaReflect = "org.scala-lang" % "scala-reflect" % "2.11.12"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1"
}

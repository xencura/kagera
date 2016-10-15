import sbt.Keys._
import sbt._

object Dependencies {

  val akkaVersion = "2.4.11"
  val sprayVersion = "1.3.2"
  val scalazVersion = "7.1.3"
  val cytoscapeVersion = "2.7.9"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion

  val akkaAnalyticsCassandra = "com.github.krasserm" %% "akka-analytics-cassandra" % "0.3.1"
  val akkaAnalyticsKafka = "com.github.krasserm" %% "akka-analytics-kafka" % "0.3.1"

  val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.18"
  val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.10"

  val scalaGraph = "com.assembla.scala-incubator" %% "graph-core" % "1.10.1"
  val scalaGraphDot = "com.assembla.scala-incubator" %% "graph-dot" % "1.10.1"

  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val scalaReflect = "org.scala-lang" % "scala-reflect" % "2.11.8"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.0"
}

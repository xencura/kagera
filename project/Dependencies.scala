import sbt.Keys._
import sbt._

object Dependencies {

  lazy val dependencySettings = Seq(
    resolvers := Seq(
      "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
      "Spray Repository" at "http://repo.spray.io/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
    )
  )

  val akkaVersion = "2.4-M3"
  val sprayVersion = "1.3.2"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % "1.0"

  val graphConstrained = "com.assembla.scala-incubator" %% "graph-core" % "1.9.0"
  val graph = "com.assembla.scala-incubator" %% "graph-core" % "1.9.4"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1"
  val scalaTime = "com.github.nscala-time" %% "nscala-time" % "1.6.0"
}

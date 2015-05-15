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

  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.2"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val sprayClient = "io.spray" %% "spray-client" % sprayVersion
  val sprayRouting = "io.spray" %% "spray-routing" % sprayVersion
  val sprayUtil = "io.spray" %% "spray-util" % sprayVersion
  val sprayCan = "io.spray" %% "spray-can" % sprayVersion
  val sprayTestkit = "io.spray" %% "spray-testkit" % sprayVersion
  val sprayJson = "io.spray" %% "spray-json" % "1.3.1"
  val graph = "com.assembla.scala-incubator" %% "graph-core" % "1.9.1"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
  val logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "3.5"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1"
  val scalaTime = "com.github.nscala-time" %% "nscala-time" % "1.6.0"

  def appendAll(scope: String, deps: Seq[ModuleID]) = deps map (_ % scope)

  def compile(deps: ModuleID*) = appendAll("compile", deps)
  def provided(deps: ModuleID*) = appendAll("provided", deps)
  def test(deps: ModuleID*) = appendAll("test", deps)
  def runtime(deps: ModuleID*) = appendAll("runtime", deps)
  def container(deps: ModuleID*) = appendAll("container", deps)
}

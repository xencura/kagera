import sbt._
import Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

object SonatypePublish {

  protected def isSnapshot(s: String) = s.trim endsWith "SNAPSHOT"

  protected val nexus = "https://oss.sonatype.org/"
  protected val ossSnapshots = "Sonatype OSS Snapshots" at nexus + "content/repositories/snapshots/"
  protected val ossStaging = "Sonatype OSS Staging" at nexus + "service/local/staging/deploy/maven2/"

  val settings = Seq(
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/merlijn/kagera")),
    pomExtra := (
      <scm>
        <url>git@github.com:merlijn/kagera.gi</url>
        <connection>scm:git:git@github.com:merlijn/kagera</connection>
      </scm>
        <developers>
          <developer>
            <id>merlijn</id>
            <name>Merlijn van Ittersum</name>
          </developer>
        </developers>
    ),
    publishMavenStyle := true,
    publishTo := Some(if (isSnapshot(version.value)) ossSnapshots else ossStaging),
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleaseStep(action = Command.process("publishSigned", _)),
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges
    )
  )
}

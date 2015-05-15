import sbt._
import sbt.Keys._
import sbtbuildinfo.Plugin._
import com.typesafe.sbt.SbtGit.GitKeys._

object BuildInfo extends Plugin {
  def buildInfoGeneratorSettings(targetPackage: String) = buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := targetPackage,
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      BuildInfoKey.map(gitHeadCommit) { case (key, value) =>
        key -> value.getOrElse("-")
      },
      BuildInfoKey.action("buildTime") {
        buildTime
      }
    )
  )

  def buildTime = {
    import java.text.SimpleDateFormat
    import java.util._

    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS '(UTC)'")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(new Date())
  }
}

import scala.Console._

import sbt._
import Keys._

/*
 * Shamelessly stolen from the Play framework, all credits to them!
 *
 * See:
 * https://github.com/playframework/Play20/blob/master/framework/src/sbt-plugin/src/main/scala/PlayCommands.scala
 * https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/core/utils/Color.scala
 */
object ShellPrompt extends Plugin {
  override def settings = Seq(shellPrompt := { state =>
    val extracted = Project.extract(state)

    import extracted._
    import com.typesafe.sbt.SbtGit._

    val project = Colors.blue(extracted get Keys.name)
    val reader = extracted get GitKeys.gitReader
    val dir = extracted get baseDirectory
    val branch =
      if (isGitRepo(dir)) Some(Colors.green(s"(${reader.withGit(_.branch)}) "))
      else None

    s"""${branch.getOrElse("")}${project}: """
  })

  @scala.annotation.tailrec
  private def isGitRepo(dir: File): Boolean = {
    if (dir.listFiles().map(_.getName).contains(".git")) true
    else {
      val parent = dir.getParentFile
      if (parent == null) false
      else isGitRepo(parent)
    }
  }
}

object Colors {
  import scala.Console._

  lazy val isANSISupported = {
    Option(System.getProperty("sbt.log.noformat"))
      .map(_ != "true")
      .orElse {
        Option(System.getProperty("os.name"))
          .map(_.toLowerCase)
          .filter(_.contains("windows"))
          .map(_ => false)
      }
      .getOrElse(true)
  }

  def red(str: String): String = if (isANSISupported) (RED + str + RESET) else str
  def blue(str: String): String = if (isANSISupported) (BLUE + str + RESET) else str
  def cyan(str: String): String = if (isANSISupported) (CYAN + str + RESET) else str
  def green(str: String): String = if (isANSISupported) (GREEN + str + RESET) else str
  def magenta(str: String): String = if (isANSISupported) (MAGENTA + str + RESET) else str
  def white(str: String): String = if (isANSISupported) (WHITE + str + RESET) else str
  def black(str: String): String = if (isANSISupported) (BLACK + str + RESET) else str
  def yellow(str: String): String = if (isANSISupported) (YELLOW + str + RESET) else str
}

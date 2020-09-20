import Dependencies._

inThisBuild(
  List(
    organization := "io.github.xencura",
    homepage := Some(url("https://github.com/xencura/kagera")),
    licenses := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    developers := List(
      Developer("nightscape", "Martin Mauch", "martin.mauch@gmail.com", url("https://github.com/nightscape"))
    )
  )
)

val commonScalacOptions = Seq(
  "-encoding",
  "utf8",
  "-target:jvm-1.8",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-deprecation",
  "-Xlog-reflective-calls"
)

lazy val basicSettings =
  Seq(
    crossScalaVersions := Seq("2.13.6", "2.12.14"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions := commonScalacOptions
  )

lazy val defaultProjectSettings = basicSettings

lazy val api = project
  .in(file("api"))
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-api",
    libraryDependencies ++= Seq(collectionCompat, scalaGraph, catsCore, fs2Core, scalatest % "test")
  )

lazy val visualization = project
  .in(file("visualization"))
  .dependsOn(api)
  .settings(defaultProjectSettings: _*)
  .settings(name := "kagera-visualization", libraryDependencies ++= Seq(scalaGraph, scalaGraphDot))

lazy val execution = project
  .in(file("execution"))
  .dependsOn(api)
  .settings(
    defaultProjectSettings ++ Seq(
      name := "kagera-execution",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        scalaGraph,
        scalatest % "test"
      )
    )
  )

lazy val akka = project
  .in(file("akka"))
  .dependsOn(api, execution)
  .settings(
    defaultProjectSettings ++ Seq(
      name := "kagera-akka",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        akkaActor,
        akkaPersistence,
        akkaSlf4j,
        akkaStream,
        akkaQuery,
        scalaGraph,
        akkaInmemoryJournal % "test",
        akkaTestkit % "test",
        scalatest % "test"
      ),
      PB.protocVersion := "3.17.3",
      Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value)
    )
  )

lazy val demo = (crossProject(JSPlatform, JVMPlatform) in file("demo"))
  .enablePlugins(JSDependenciesPlugin)
  .settings(defaultProjectSettings: _*)
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "shared" / "main" / "scala",
    libraryDependencies ++= Seq(scalaTags.value, upickle.value)
  )
  .jsSettings(
    jsDependencies ++= Seq(
      "org.webjars.bower" % "cytoscape" % cytoscapeVersion
        / s"$cytoscapeVersion/dist/cytoscape.js"
        minified s"$cytoscapeVersion/dist/cytoscape.min.js"
        commonJSName "cytoscape"
    ),
    libraryDependencies ++= Seq(scalaJsDom.value)
  )
  .jvmSettings(
    libraryDependencies ++= Seq(akkaHttpUpickle, akkaHttp, akkaQuery, akkaPersistenceCassandra),
    name := "demo-app",
    mainClass := Some("io.kagera.demo.Main")
  )

lazy val demoJs = demo.js
lazy val demoJvm = demo.jvm
  .dependsOn(api, visualization, akka)
  .settings(
    // include the compiled javascript result from js module
    Compile / resources += (demoJs / Compile / fastOptJS).value.data,
    // include the javascript dependencies
    Compile / resources += (demoJs / Compile / packageJSDependencies).value
  )

lazy val root = Project("kagera", file("."))
  .aggregate(api, akka, execution, visualization)
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultProjectSettings)
  .settings(
    publish := {},
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      BuildInfoKey.map(git.gitHeadCommit) { case (key, value) =>
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

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
    crossScalaVersions := Seq("3.0.2", "2.13.6", "2.12.14"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions := commonScalacOptions
  )

lazy val defaultProjectSettings = basicSettings

lazy val api = project
  .in(file("api"))
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-api",
    libraryDependencies ++= Seq(
      collectionCompat,
      scalaGraph.cross(CrossVersion.for3Use2_13),
      catsCore,
      fs2Core,
      scalatest % "test"
    )
  )

lazy val visualization = project
  .in(file("visualization"))
  .dependsOn(api)
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-visualization",
    libraryDependencies ++= Seq(
      scalaGraph.cross(CrossVersion.for3Use2_13),
      scalaGraphDot.cross(CrossVersion.for3Use2_13)
    )
  )

lazy val scalaReflect = Def.setting(if (scalaVersion.value >= "3.0.0") {
  Seq[ModuleID]()
} else {
  Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
})
lazy val akka = project
  .in(file("akka"))
  .dependsOn(api)
  .settings(
    defaultProjectSettings ++ Seq(
      name := "kagera-akka",
      libraryDependencies ++= Seq(
        akkaActor.cross(CrossVersion.for3Use2_13),
        akkaPersistence.cross(CrossVersion.for3Use2_13),
        akkaSlf4j.cross(CrossVersion.for3Use2_13),
        akkaStream.cross(CrossVersion.for3Use2_13),
        akkaQuery.cross(CrossVersion.for3Use2_13),
        scalaGraph.cross(CrossVersion.for3Use2_13),
        (akkaInmemoryJournal % "test").cross(CrossVersion.for3Use2_13),
        (akkaTestkit % "test").cross(CrossVersion.for3Use2_13),
        scalatest % "test"
      ) ++ scalaReflect.value,
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
    libraryDependencies ++= Seq(scalaJsDom.value.cross(CrossVersion.for3Use2_13))
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      akkaHttpUpickle.cross(CrossVersion.for3Use2_13),
      akkaHttp.cross(CrossVersion.for3Use2_13),
      akkaQuery.cross(CrossVersion.for3Use2_13),
      akkaPersistenceCassandra.cross(CrossVersion.for3Use2_13)
    ),
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
  .aggregate(api, akka, visualization)
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

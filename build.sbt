import Dependencies._

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
    organization := "io.kagera",
    crossScalaVersions := Seq("2.13.4", "2.12.12"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions := commonScalacOptions
  )

lazy val defaultProjectSettings = basicSettings ++ SonatypePublish.settings

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

lazy val akka = project
  .in(file("akka"))
  .dependsOn(api)
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
      PB.protocVersion := "-v2.6.1",
      Compile / PB.targets := Seq(
        //PB.gens.java("2.6.1") -> (sourceManaged in Compile).value
        scalapb.gen() -> (Compile / sourceManaged).value
      )
    )
  )

lazy val demo = (crossProject(JSPlatform, JVMPlatform) in file("demo"))
  .enablePlugins(JSDependenciesPlugin)
  .settings(defaultProjectSettings: _*)
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "shared" / "main" / "scala",
    libraryDependencies ++= Seq("com.lihaoyi" %%% "scalatags" % "0.9.1", "com.lihaoyi" %%% "upickle" % "1.1.0")
  )
  .jsSettings(
    jsDependencies ++= Seq(
      "org.webjars.bower" % "cytoscape" % cytoscapeVersion
        / s"$cytoscapeVersion/dist/cytoscape.js"
        minified s"$cytoscapeVersion/dist/cytoscape.min.js"
        commonJSName "cytoscape"
    ),
    libraryDependencies ++= Seq("org.scala-js" %%% "scalajs-dom" % "1.0.0")
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "de.heikoseeberger" %% "akka-http-upickle" % "1.32.0",
      akkaHttp,
      akkaQuery,
      akkaPersistenceCassandra
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

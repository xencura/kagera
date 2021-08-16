import Dependencies._

val commonScalacOptions = Seq(
  "-encoding",
  "utf8",
  "-target:jvm-1.8",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:higherKinds",
  "-unchecked",
  "-deprecation",
  "-Xlog-reflective-calls"
)

lazy val defaultProjectSettings =
  Seq(
    organization := "io.kagera",
    crossScalaVersions := Seq("2.13.6", "2.12.14"),
    scalaVersion := crossScalaVersions.value.head,
    githubOwner := "xencura",
    githubRepository := "kagera",
    scalacOptions := commonScalacOptions
  )

githubTokenSource := TokenSource.GitConfig("github.token")

lazy val api = crossProject(JSPlatform, JVMPlatform)
  .in(file("api"))
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-api",
    libraryDependencies ++= Seq(collectionCompat, scalaGraph, catsCore, catsEffect, fs2Core, scalatest % "test")
  )

lazy val visualization = project
  .in(file("visualization"))
  .dependsOn(api.jvm)
  .settings(defaultProjectSettings: _*)
  .settings(name := "kagera-visualization", libraryDependencies ++= Seq(scalaGraph, scalaGraphDot))

lazy val visualizationJs = crossProject(JSPlatform, JVMPlatform)
  .in(file("visualization-js"))
  .dependsOn(api)
  .enablePlugins(JSDependenciesPlugin)
  .settings(defaultProjectSettings: _*)
  .settings(
    resolvers += "jitpack" at "https://jitpack.io",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "shared" / "main" / "scala",
    libraryDependencies ++= Seq("com.lihaoyi" %%% "scalatags" % "0.9.1", "com.lihaoyi" %%% "upickle" % "1.1.0")
  )
  .jsSettings(
    jsDependencies ++= Seq(
    ),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.0.0",
      "com.github.fdietze.scala-js-d3v4" %%% "scala-js-d3v4" % "23be8f92a3"
    )
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

lazy val execution = project
  .in(file("execution"))
  .dependsOn(api.jvm)
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
  .dependsOn(api.jvm, execution)
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
      PB.protocVersion := "-v3.15.1",
      Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value)
    )
  )

lazy val zio = project
  .in(file("zio"))
  .dependsOn(api, execution)
  .settings(
    defaultProjectSettings ++ Seq(
      name := "kagera-zio",
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        zioCore,
        zioInteropCats,
        zioActors,
        zioActorsPersistence,
        scalaGraph,
        zioTest % "test",
        zioTestSbt % "test"
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
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
  .dependsOn(api.jvm, visualization, akka)
  .settings(
    // include the compiled javascript result from js module
    Compile / resources += (demoJs / Compile / fastOptJS).value.data,
    // include the javascript dependencies
    Compile / resources += (demoJs / Compile / packageJSDependencies).value
  )

lazy val root = Project("kagera", file("."))
  .aggregate(api.jvm, akka, execution, visualization, zio)
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

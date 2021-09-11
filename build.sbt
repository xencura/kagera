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
    crossScalaVersions := Seq("2.13.6"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions := commonScalacOptions
  )

lazy val defaultProjectSettings = basicSettings

lazy val api = crossProject(JSPlatform, JVMPlatform)
  //.withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("api"))
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-api",
    libraryDependencies ++= Seq(collectionCompat, scalaGraph.value, catsCore.value, fs2Core.value, scalatest % "test")
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsSettings(scalaJSLinkerConfig ~= {
    _.withModuleKind(ModuleKind.CommonJSModule)
  })

lazy val visualization = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .enablePlugins(ScalaJSBundlerPlugin)
  .in(file("visualization"))
  .dependsOn(api)
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-visualization",
    libraryDependencies ++= Seq(
      scalaGraph.value,
      "com.lihaoyi" %%% "scalatags" % "0.9.1",
      "com.lihaoyi" %%% "upickle" % "1.1.0"
    )
  )
  .jsConfigure(_.enablePlugins(JSDependenciesPlugin, ScalaJSBundlerPlugin))
  .jsSettings(
    resolvers += "jitpack" at "https://jitpack.io",
    webpackBundlingMode := BundlingMode.LibraryAndApplication(),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    libraryDependencies ++= Seq(scalaJsDom.value, d3.value, laminar.value),
    jsDependencies ++= Seq(
      "org.webjars.bower" % "cytoscape" % cytoscapeVersion
        / s"$cytoscapeVersion/dist/cytoscape.js"
        minified s"$cytoscapeVersion/dist/cytoscape.min.js"
        commonJSName "cytoscape"
    )
  )
  .jvmSettings(libraryDependencies ++= Seq(scalaGraphDot))

lazy val execution = project
  .in(file("execution"))
  .dependsOn(api.jvm)
  .settings(
    defaultProjectSettings ++ Seq(
      name := "kagera-execution",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        scalaGraph.value,
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
        akkaCoordination,
        akkaRemote,
        akkaCluster,
        akkaClusterTools,
        akkaPki,
        scalaGraph.value,
        akkaInmemoryJournal % "test",
        akkaTestkit % "test",
        scalatest % "test"
      ),
      PB.protocVersion := "3.17.3",
      Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value)
    )
  )

lazy val demo = (crossProject(JSPlatform, JVMPlatform) in file("demo"))
  .dependsOn(api, visualization)
  .settings(defaultProjectSettings: _*)
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "shared" / "main" / "scala",
    libraryDependencies ++= Seq(scalaTags.value, upickle.value)
  )
  .jsConfigure(_.enablePlugins(JSDependenciesPlugin, ScalaJSBundlerPlugin))
  .jsSettings(
    webpackBundlingMode := BundlingMode.LibraryAndApplication(),
    libraryDependencies ++= Seq(scalaJsDom.value)
  )
  .jvmSettings(
    libraryDependencies ++= Seq(akkaHttpUpickle, akkaHttp, akkaQuery, akkaPersistenceCassandra),
    name := "demo-app",
    mainClass := Some("io.kagera.demo.Main")
  )

lazy val demoJs = demo.js
lazy val demoJvm = demo.jvm
  .dependsOn(api.jvm, visualization.jvm, akka)
  .settings(
    // include the compiled javascript result from js module
    Compile / resources += (demoJs / Compile / fastOptJS).value.data,
    // include the javascript dependencies
    Compile / resources += (demoJs / Compile / packageJSDependencies).value
  )

lazy val root = Project("kagera", file("."))
  .aggregate(api.jvm, akka, execution, visualization.jvm, visualization.js, demo.jvm, demo.js)
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

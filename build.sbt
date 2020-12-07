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
  "-language:higherKinds",
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

lazy val api = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .enablePlugins(ScalaJSBundlerPlugin)
  .in(file("api"))
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-api",
    libraryDependencies ++= Seq(collectionCompat, scalaGraph.value, catsCore, catsEffect, fs2Core, scalatest % "test"),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val visualization = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .enablePlugins(ScalaJSBundlerPlugin)
  .in(file("visualization"))
  .dependsOn(api)
  .settings(defaultProjectSettings: _*)
  .settings(
    name := "kagera-visualization",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      scalaGraph.value,
      "com.lihaoyi" %%% "scalatags" % "0.9.1",
      "com.lihaoyi" %%% "upickle" % "1.1.0"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.0.0",
      "com.github.xencura.scala-js-d3v4" %%% "scala-js-d3v4" % "766d13e0c1"
    )
  )
  .jvmSettings(libraryDependencies ++= Seq(scalaGraphDot))

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
        scalaGraph.value,
        akkaInmemoryJournal % "test",
        akkaTestkit % "test",
        scalatest % "test"
      ),
      PB.protocVersion := "3.17.3",
      Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value)
    )
  )

lazy val zio = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .enablePlugins(ScalaJSBundlerPlugin)
  .in(file("zio"))
  .dependsOn(api)
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
        scalaGraph.value,
        zioTest % "test",
        zioTestSbt % "test"
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )
  )

lazy val demo = (crossProject(JSPlatform, JVMPlatform) in file("demo"))
  .enablePlugins(JSDependenciesPlugin, ScalaJSBundlerPlugin)
  .dependsOn(api, visualization)
  .settings(defaultProjectSettings: _*)
  .settings(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "shared" / "main" / "scala",
    libraryDependencies ++= Seq(scalaTags.value, upickle.value)
  )
  .jsSettings(
    webpackBundlingMode := BundlingMode.LibraryAndApplication(),
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
  .dependsOn(api.jvm, visualization.jvm, akka)
  .settings(
    // include the compiled javascript result from js module
    Compile / resources += (demoJs / Compile / fastOptJS).value.data,
    // include the javascript dependencies
    Compile / resources += (demoJs / Compile / packageJSDependencies).value
  )

lazy val root = Project("kagera", file("."))
  .aggregate(api.jvm, akka, visualization.jvm, zio.jvm)
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

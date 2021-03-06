import Dependencies._

name := "talos"
sonatypeProfileName := "org.vaslabs"
version in ThisBuild := sys.env.getOrElse("VASLABS_PUBLISH_VERSION", "SNAPSHOT")

val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  organization := "org.vaslabs.talos",
  organizationName := "vaslabs",
  scmInfo := Some(ScmInfo(url("https://github.com/vaslabs/talos"), "scm:git@github.com:vaslabs/talos.git")),
  developers := List(
    Developer(
      id    = "vaslabs",
      name  = "Vasilis Nicolaou",
      email = "vaslabsco@gmail.com",
      url   = url("http://vaslabs.org")
    )
  ),
  publishMavenStyle := true,
  licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT")),
  homepage := Some(url("https://talos.vaslabs.org")),
  startYear := Some(2018)
)

scalaVersion := "2.12.7"

lazy val compilerSettings = {
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:postfixOps",              //Allow postfix operator notation, such as `1 to 10 toList'
    "-language:implicitConversions",
    "-Ypartial-unification",
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard",               // Warn when non-Unit expression results are unused.
    "-Ywarn-unused:imports",
    "-Xfatal-warnings"
  )
}


lazy val talosEvents =
  (project in file("events")).settings(
    libraryDependencies ++= libraries.Akka.all ++ libraries.ScalaTest.all
  ).settings(
    compilerSettings
  ).settings(publishSettings)

lazy val talosKamon =
  (project in file("kamon")).settings(
    libraryDependencies ++= libraries.Kamon.all ++ libraries.ScalaTest.all ++ libraries.Akka.all
  ).settings(compilerSettings)
    .settings(publishSettings)
  .dependsOn(talosEvents)

lazy val hystrixReporter =
  (project in file("hystrix-reporter")).settings(
    libraryDependencies ++=
      libraries.Akka.allHttp ++ libraries.Kamon.all ++ libraries.Circe.all ++ libraries.ScalaTest.all
  ).settings(compilerSettings)
    .settings(publishSettings)
  .dependsOn(talosKamon)

lazy val talosExamples =
  (project in file("examples"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(UniversalPlugin)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    packageName in Docker := "talos-demo",
    version in Docker := version.value,
    maintainer in Docker := "Vasilis Nicolaou",
    dockerBaseImage := "openjdk:8-alpine",
    dockerExposedPorts := Seq(8080),
    maintainer := "vaslabsco@gmail.com",
    dockerUsername := Some("vaslabs"),
  )
  .settings(
    libraryDependencies ++=
      libraries.Akka.allHttp ++ libraries.Kamon.all ++ libraries.Circe.all ++ libraries.ScalaTest.all
  ).settings(noPublishSettings)
  .dependsOn(hystrixReporter)

lazy val noPublishSettings = Seq(
  publish := {},
  skip in publish := true,
  publishLocal := {},
  publishArtifact in Test := false
)

lazy val talos =
  (project in file("."))
  .settings(noPublishSettings)
  .aggregate(talosEvents, talosKamon, hystrixReporter, talosExamples)
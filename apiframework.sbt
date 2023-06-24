enablePlugins(ScalaJSPlugin)

name := "Roll20 API Framework"

organization := "com.lkroll"

version := "0.12.0-SNAPSHOT"

scalaVersion := "2.13.10"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/Bathtor/api-framework"))
scmInfo := Some(ScmInfo(url("https://github.com/Bathtor/api-framework"), "git@github.com:Bathtor/api-framework.git"))
developers := List(
  Developer(id = "lkroll",
            name = "Lars Kroll",
            email = "bathtor@googlemail.com",
            url = url("https://github.com/Bathtor")
  )
)
publishMavenStyle := true

// Add sonatype repository settings
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
publishTo := sonatypePublishToBundle.value

import ReleaseTransformations._

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

scalacOptions ++= Seq(
  "-feature",
  "-language:implicitConversions",
  "-deprecation",
  //"-Xfatal-warnings",
  "-Xlint"
)

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies += "com.lkroll" %%% "roll20-api-facade" % "1.2.4"
libraryDependencies += "com.lkroll" %%% "roll20-core" % "0.13.3"
libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.12.0"
libraryDependencies += "org.scalactic" %%% "scalactic" % "3.2.16"
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.16" % "test"
libraryDependencies += "com.lihaoyi" %%% "fastparse" % "2.3.+" % "provided" // needed for TemplateVars parsing
libraryDependencies += "org.rogach" %%% "scallop" % "4.1.0" % "provided" // needed for ScallopConfig based commands
libraryDependencies += "com.lkroll" %%% "roll20-sheet-model" % "0.12.0-SNAPSHOT" % "provided" // needed for APIOutputTemplate fields

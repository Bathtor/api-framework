enablePlugins(ScalaJSPlugin)

name := "Roll20 API Framework"

organization := "com.lkroll.roll20"

version := "0.7.0-SNAPSHOT"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
    "-P:scalajs:suppressExportDeprecations"
)

libraryDependencies += "com.lkroll.roll20" %%% "roll20-api-facade" % "1.2.0-SNAPSHOT"
libraryDependencies += "com.lkroll.roll20" %%% "roll20-core" % "0.12.+"
libraryDependencies += "org.scalactic" %%% "scalactic" % "3.0.4" % "test"
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
libraryDependencies += "com.lihaoyi" %%% "fastparse" % "1.+" % "provided" // needed for TemplateVars parsing
libraryDependencies += "org.rogach" %%% "scallop" % "3.1.+" % "provided" // needed for ScallopConfig based commands
enablePlugins(ScalaJSPlugin)

name := "Roll20 API Framework"

organization := "com.larskroll.roll20"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq(
    "-P:scalajs:suppressExportDeprecations"
)

libraryDependencies += "com.larskroll.roll20" %%% "roll20-api-facade" % "1.0-SNAPSHOT"
libraryDependencies += "com.larskroll.roll20" %%% "roll20-sheet-framework" % "0.4-SNAPSHOT"
libraryDependencies += "org.rogach" %%% "scallop" % "3.1.0"

scalaVersion := "2.12.4"
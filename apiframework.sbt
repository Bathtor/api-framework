enablePlugins(ScalaJSPlugin)

name := "Roll20 API Framework"

organization := "com.lkroll.roll20"

version := "0.10.0"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
    "-P:scalajs:suppressExportDeprecations"
)

resolvers += Resolver.bintrayRepo("lkrollcom", "maven")

libraryDependencies += "com.lkroll.roll20" %%% "roll20-api-facade" % "1.2.1"
libraryDependencies += "com.lkroll.roll20" %%% "roll20-core" % "0.13.0"
libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.6.7"
libraryDependencies += "org.scalactic" %%% "scalactic" % "3.0.4" % "test"
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
libraryDependencies += "com.lihaoyi" %%% "fastparse" % "1.+" % "provided" // needed for TemplateVars parsing
libraryDependencies += "org.rogach" %%% "scallop" % "3.1.+" % "provided" // needed for ScallopConfig based commands
libraryDependencies += "com.lkroll.roll20" %%% "roll20-sheet-model" % "[0.11.0,)" % "provided" // needed for APIOutputTemplate fields

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayPackageLabels := Seq("roll20", "api")
bintrayOrganization := Some("lkrollcom")
bintrayRepository := "maven"

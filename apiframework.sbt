enablePlugins(ScalaJSPlugin)

name := "Roll20 API Framework"

organization := "com.lkroll.roll20"

version := "0.10.1"

scalaVersion := "2.12.11"

resolvers += Resolver.bintrayRepo("lkrollcom", "maven")

libraryDependencies += "com.lkroll.roll20" %%% "roll20-api-facade" % "1.2.2"
libraryDependencies += "com.lkroll.roll20" %%% "roll20-core" % "0.13.1"
libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.9.+"
libraryDependencies += "org.scalactic" %%% "scalactic" % "3.2.0" % "test"
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.0" % "test"
libraryDependencies += "com.lihaoyi" %%% "fastparse" % "2.3.+" % "provided" // needed for TemplateVars parsing
libraryDependencies += "org.rogach" %%% "scallop" % "3.5.+" % "provided" // needed for ScallopConfig based commands
libraryDependencies += "com.lkroll.roll20" %%% "roll20-sheet-model" % "0.11.2" % "provided" // needed for APIOutputTemplate fields

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayPackageLabels := Seq("roll20", "api")
bintrayOrganization := Some("lkrollcom")
bintrayRepository := "maven"

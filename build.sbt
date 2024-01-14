name := "flac-to-mp3"
version := "1.4"
scalaVersion := "2.13.12"

libraryDependencies += "commons-io" % "commons-io" % "2.11.0"

assembly / mainClass := Some("org.bruchez.olivier.flactomp3.FlacToMp3")

assembly / assemblyJarName := "flac-to-mp3.jar"

ThisBuild / scalafmtOnCompile := true

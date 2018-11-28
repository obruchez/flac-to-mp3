name := "flac-to-mp3"
version := "1.0"
scalaVersion := "2.12.7"

libraryDependencies += "commons-io" % "commons-io" % "2.6"

mainClass in assembly := Some("org.bruchez.olivier.flactomp3.FlacToMp3")

assemblyJarName in assembly := "flac-to-mp3.jar"

scalafmtOnCompile in ThisBuild := true

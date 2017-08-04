name := "flac-to-mp3"
version := "1.0"
scalaVersion := "2.12.3"

libraryDependencies += "commons-io" % "commons-io" % "2.5"

mainClass in assembly := Some("org.bruchez.olivier.flactomp3.FlacToMp3")

assemblyJarName in assembly := "flac-to-mp3.jar"

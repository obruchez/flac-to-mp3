package org.bruchez.olivier.flactomp3

import java.nio.file.Path

import scala.sys.process._
import scala.util._

/*
 Known ffmpeg problems:

  - AAC output files (.m4a, .mp4, etc.) won't be able to contain "custom" tags (ReplayGain, MusicBrainz, etc.)
 */

object Ffmpeg {
  // ffmpeg exposes Ogg comments at the stream level, unlike FLAC/MP3/M4A which expose them at
  // the format level. "-map_metadata 0" therefore copies nothing from an Ogg source. The
  // stream-level form must stay conditional: on a FLAC source it would drop every tag.
  private val OggExtensions = Set("ogg", "opus", "oga", "spx")

  def convert(srcPath: Path, dstPath: Path)(implicit arguments: Arguments): Try[Unit] = {
    val extension =
      srcPath.getFileName.toString.toLowerCase.split('.').lastOption.getOrElse("")
    val metadataSource = if (OggExtensions.contains(extension)) "0:s:0" else "0"

    // Copying metadata should be the default behaviour since ffmpeg 3.2
    val cmd = Seq("ffmpeg", "-i", srcPath.toString, "-map_metadata", metadataSource) ++
      arguments.ffmpegArguments :+
      "-y" :+
      dstPath.toString

    val stringProcessLogger = newStringProcessLogger

    Try(cmd.!!(stringProcessLogger)) match {
      case Success(_) =>
        Success(())
      case Failure(throwable) =>
        // ffmpeg outputs everything to the stderr, but merge both stdout and stderr just in case
        val outputAndErrorStrings =
          stringProcessLogger.outputString + stringProcessLogger.errorString

        // Include all ffmpeg output for debug purpose
        Failure(new Exception(s"ffmpeg error: ${throwable.getMessage}\n" + outputAndErrorStrings))
    }
  }

  trait OutputAndErrorStrings {
    def outputString: String
    def errorString: String
  }

  private def newStringProcessLogger: ProcessLogger with OutputAndErrorStrings =
    new ProcessLogger with OutputAndErrorStrings {
      private val outputStringBuilder = new StringBuilder
      private val errorStringBuilder = new StringBuilder

      override def outputString: String = outputStringBuilder.toString
      override def errorString: String = errorStringBuilder.toString

      override def out(s: => String): Unit = synchronized {
        outputStringBuilder.append(s + "\n"); ()
      }
      override def err(s: => String): Unit = synchronized {
        errorStringBuilder.append(s + "\n"); ()
      }
      override def buffer[T](f: => T): T = f
    }
}

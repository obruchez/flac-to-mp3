package org.bruchez.olivier

import java.nio.file.Path

import scala.sys.process._
import scala.util._

/*
 Known ffmpeg problems:

  - metadata from .ogg source files won't be copied to output files
  - AAC output files (.m4a, .mp4, etc.) won't be able to contain "custom" tags (ReplayGain, MusicBrainz, etc.)
 */

object Ffmpeg {
  def convert(srcPath: Path, dstPath: Path)(implicit arguments: Arguments): Try[Unit] = {
    // Copying metadata should be the default behaviour since ffmpeg 3.2
    val cmd = Seq("ffmpeg", "-i", srcPath.toString, "-map_metadata", "0") ++
      arguments.formatSpecificFfmpegArguments :+
      "-y" :+
      dstPath.toString

    val stringProcessLogger = newStringProcessLogger

    Try(cmd.!!(stringProcessLogger)) match {
      case Success(_) =>
        Success(())
      case Failure(throwable) =>
        // ffmpeg outputs everything to the stderr, but merge both stdout and stderr just in case
        val outputAndErrorStrings = stringProcessLogger.outputString + stringProcessLogger.errorString

        // Include all ffmpeg output for debug purpose
        Failure(new Exception(s"ffmpeg error: ${throwable.getMessage}\n" + outputAndErrorStrings))
    }
  }

  trait OutputAndErrorStrings {
    def outputString: String
    def errorString: String
  }

  private def newStringProcessLogger: ProcessLogger with OutputAndErrorStrings = new ProcessLogger with OutputAndErrorStrings {
    private val outputStringBuilder = new StringBuilder
    private val errorStringBuilder = new StringBuilder

    override def outputString: String = outputStringBuilder.toString
    override def errorString: String = errorStringBuilder.toString

    override def out(s: => String): Unit = synchronized { outputStringBuilder.append(s + "\n"); () }
    override def err(s: => String): Unit = synchronized { errorStringBuilder.append(s + "\n"); () }
    override def buffer[T](f: => T): T = f
  }
}

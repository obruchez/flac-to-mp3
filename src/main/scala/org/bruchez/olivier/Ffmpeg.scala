package org.bruchez.olivier

import java.nio.file.Path

import scala.sys.process._
import scala.util.Try

object Ffmpeg {
  def convert(srcPath: Path, dstPath: Path)(implicit arguments: Arguments): Try[Unit] = {
    // Copying metadata should be the default behaviour since ffmpeg 3.2
    val cmd = Seq("ffmpeg", "-i", srcPath.toString, "-map_metadata", "0") ++
      arguments.formatSpecificFfmpegArguments :+
      dstPath.toString

    Try(cmd.!!)
  }
}

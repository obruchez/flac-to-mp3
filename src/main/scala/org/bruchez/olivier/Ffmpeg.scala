package org.bruchez.olivier

import java.nio.file.Path

import scala.sys.process._
import scala.util.Try

object Ffmpeg {
  def convert(srcPath: Path, dstPath: Path)(implicit arguments: Arguments): Try[Unit] = {
    val cmd = Seq("ffmpeg", "-i", srcPath.toString) ++ arguments.ffmpegArguments :+ dstPath.toString

    Try(cmd.!!)
  }
}

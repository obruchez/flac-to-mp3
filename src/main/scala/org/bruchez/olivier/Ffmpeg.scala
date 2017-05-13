package org.bruchez.olivier

import java.nio.file.Path

import scala.util.Try

object Ffmpeg {
  def convert(srcPath: Path, dstPath: Path)(implicit arguments: Arguments): Try[Unit] = {
    // @todo
    Try(())
  }
}

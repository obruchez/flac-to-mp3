package org.bruchez.olivier

import java.nio.file._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ActionGroup(actions: Seq[Action])

sealed trait Action {
  def execute()(implicit arguments: Arguments): Future[Unit]
}

case class ConvertFileAction(srcFile: Path, dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments): Future[Unit] = Future {
    Ffmpeg.convert(srcFile, dstFile).get
    Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
  }
}

case class CopyFileAction(srcFile: Path, dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments): Future[Unit] = Future {
    Files.copy(srcFile, dstFile)
    Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
  }
}

case class RemoveFileAction(dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments): Future[Unit] = Future {
    val defaultTrashPath = arguments.trashPath.resolve(dstFile.relativize(arguments.dstPath))

    @annotation.tailrec
    def nonExistingTrashPath(suffix: Option[Int] = None): Path = {
      val path = Paths.get(defaultTrashPath.toString + suffix.map(s => s".trash$s").getOrElse(""))
      if (!Files.exists(path)) path else nonExistingTrashPath(suffix = Some(suffix.getOrElse(-1) + 1))
    }

    val actualTrashPath = nonExistingTrashPath()

    Files.move(dstFile, actualTrashPath)
  }
}

case class RemoveSymbolicLinkAction(dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments): Future[Unit] = Future {
    // The delete method will delete the symbolic link, not the target file
    Files.delete(dstFile)
  }
}

/*
 Algorithm idea:
 - get files from source path
 - get files from destination path
 - method to compare both group of files => return ActionGroups
 - Actions can be parallelized only if they're part of the same group, so groups will be processed one after the other
 - this will allow for the following actions to be executed safely: symbolic links removal, file deletion, conversions

File comparison:
 - take source files and generate expected list of destination files (.flac -> .m4a, .mpp -> .m4a, etc.)
 - compare expected destination files and actual destination files => generate actions
 */

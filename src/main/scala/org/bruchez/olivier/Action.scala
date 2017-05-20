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
    if (arguments.noop) {
      println(s"Converting $srcFile to $dstFile")
    } else {
      Ffmpeg.convert(srcFile, dstFile).get
      Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
    }
  }
}

case class CopyFileAction(srcFile: Path, dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments): Future[Unit] = Future {
    if (arguments.noop) {
      println(s"Copying $srcFile to $dstFile")
    } else {
      Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
      Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
    }
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

    if (arguments.noop) {
      println(s"Moving $dstFile to trash ($actualTrashPath)")
    } else {
      Files.move(dstFile, actualTrashPath)
    }
  }
}

case class RemoveSymbolicLinkAction(dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments): Future[Unit] = Future {
    if (arguments.noop) {
      println(s"Removing symbolic link $dstFile")
    } else {
      // The delete method will delete the symbolic link, not the target file
      Files.delete(dstFile)
    }
  }
}

object Action {
  def executeActions(actions: Seq[Action])(implicit arguments: Arguments): Unit = {

    val groupedActions = actions.grouped(arguments.threadCount).toSeq

    for (groupedAction <- groupedActions) {

    }


    // @todo
  }
}



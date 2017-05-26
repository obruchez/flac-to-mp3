package org.bruchez.olivier

import java.nio.file._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util._

case class ExecutionError(error: String)

case class ActionGroupExecutionErrors(name: String, executionErrors: Seq[ExecutionError])

case class ActionGroup(name: String, actions: Seq[Action], parallelExecution: Boolean) {
  def execute()(implicit arguments: Arguments): ActionGroupExecutionErrors = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val groupedActions = actions.grouped(if (parallelExecution) arguments.threadCount else 1).toSeq

    val results =
      for (groupedAction <- groupedActions) yield {
        val future = Future.sequence(ActionGroup.lift(groupedAction.map(_.execute())))

        Await.result(future, 1 hour)
      }

    ActionGroupExecutionErrors(
      name = this.name,
      executionErrors = results.flatten.flatMap(_.failed.toOption).map { throwable =>
        ExecutionError(error = throwable.getMessage.trim)
      })
  }
}

sealed trait Action {
  def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit]
}

case class ConvertFileAction(srcFile: Path, dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    if (arguments.noop) {
      println(s"Converting $srcFile to $dstFile")
    } else {
      Files.createDirectories(dstFile.getParent)
      Ffmpeg.convert(srcFile, dstFile).get
      Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
    }
  }
}

case class CopyFileAction(srcFile: Path, dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    if (arguments.noop) {
      println(s"Copying $srcFile to $dstFile")
    } else {
      Files.createDirectories(dstFile.getParent)
      Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
      Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
    }
  }
}

case class RemoveFileAction(dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    val defaultTrashPath = arguments.trashPath.resolve(arguments.dstPath.relativize(dstFile))

    @annotation.tailrec
    def nonExistingTrashPath(suffix: Option[Int] = None): Path = {
      val path = Paths.get(defaultTrashPath.toString + suffix.map(s => s".trash$s").getOrElse(""))
      if (!Files.exists(path)) path else nonExistingTrashPath(suffix = Some(suffix.getOrElse(-1) + 1))
    }

    val actualTrashPath = nonExistingTrashPath()

    if (arguments.noop) {
      println(s"Moving $dstFile to trash ($actualTrashPath)")
    } else {
      Files.createDirectories(actualTrashPath.getParent)
      Files.move(dstFile, actualTrashPath)
    }
  }
}

case class RemoveSymbolicLinkAction(dstFile: Path) extends Action {
  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    if (arguments.noop) {
      println(s"Removing symbolic link $dstFile")
    } else {
      // The delete method will delete the symbolic link, not the target file
      Files.delete(dstFile)
    }
  }
}

case class RemoveEmptyDirectoriesAction(dstPath: Path) extends Action {
  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    for {
      directory <- FileUtils.allFilesInPath(dstPath).filter(Files.isDirectory(_))
    } {
      if (Files.exists(directory) && FileUtils.emptyDirectory(directory)) {
        if (arguments.noop) {
          println(s"Removing directory $directory")
        } else {
          FileUtils.deleteDirectoryAndAllParentDirectoriesIfEmpty(directory)
        }
      }
    }
  }
}

object ActionGroup {
  private def lift[A](futures: Seq[Future[A]])(implicit ec: ExecutionContext): Seq[Future[Try[A]]] =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) })
}

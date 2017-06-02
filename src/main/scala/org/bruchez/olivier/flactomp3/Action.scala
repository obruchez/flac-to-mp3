package org.bruchez.olivier.flactomp3

import java.nio.file._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
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
        val actionResultsFuture = Future.sequence {
          ActionGroup.lift(groupedAction.map(action => action -> action.executeOrNoop()))
        }

        val actionResults = Await.result(actionResultsFuture, 1 hour)

        for ((action, actionResult) <- actionResults) {
          actionResult match {
            case Success(_) => println(s"Success: ${action.description}")
            case Failure(_) => println(s"Failure: ${action.description} (error will be reported at end of execution)")
          }
        }

        actionResults
      }

    ActionGroupExecutionErrors(
      name = this.name,
      executionErrors = results.flatten.flatMap(_._2.failed.toOption).map { throwable =>
        ExecutionError(error = throwable.getMessage.trim)
      })
  }
}

sealed trait Action {
  def executeOrNoop()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] =
    if (arguments.noop) {
      Future { println(s"Noop: $description") }
    } else {
      execute()
    }

  def description: String

  protected def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit]
}

case class ConvertFileAction(srcFile: Path, dstFile: Path) extends Action {
  override val description: String = s"converting $srcFile to $dstFile"

  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    Files.createDirectories(dstFile.getParent)
    Ffmpeg.convert(srcFile, dstFile).get
    Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
  }
}

case class CopyFileAction(srcFile: Path, dstFile: Path) extends Action {
  override val description: String = s"copying $srcFile to $dstFile"

  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    Files.createDirectories(dstFile.getParent)
    Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    Files.setLastModifiedTime(dstFile, Files.getLastModifiedTime(srcFile))
  }
}

case class RemoveFileAction(dstFile: Path) extends Action {
  override val description: String = s"moving $dstFile to trash"

  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    val defaultTrashPath = arguments.trashPath.resolve(arguments.dstPath.relativize(dstFile))

    @annotation.tailrec
    def nonExistingTrashPath(suffix: Option[Int] = None): Path = {
      val path = Paths.get(defaultTrashPath.toString + suffix.map(s => s".trash$s").getOrElse(""))
      if (!Files.exists(path)) path else nonExistingTrashPath(suffix = Some(suffix.getOrElse(-1) + 1))
    }

    val actualTrashPath = nonExistingTrashPath()

    Files.createDirectories(actualTrashPath.getParent)
    Files.move(dstFile, actualTrashPath)
  }
}

case class RemoveSymbolicLinkAction(dstFile: Path) extends Action {
  override val description: String = s"removing symbolic link $dstFile"

  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    // The delete method will delete the symbolic link, not the target file
    Files.delete(dstFile)
  }
}

case class RemoveEmptyDirectoriesAction(dstPath: Path) extends Action {
  override val description: String = s"removing empty directories in $dstPath"

  override def execute()(implicit arguments: Arguments, ec: ExecutionContext): Future[Unit] = Future {
    for {
      directory <- FileUtils.allFilesInPath(dstPath).filter(Files.isDirectory(_))
    } {
      if (Files.exists(directory) && FileUtils.emptyDirectory(directory)) {
        FileUtils.deleteDirectoryAndAllParentDirectoriesIfEmpty(directory)
      }
    }
  }
}

object ActionGroup {
  private def lift[A](futures: Seq[(Action, Future[A])])(implicit ec: ExecutionContext): Seq[Future[(Action, Try[A])]] =
    futures.map {
      case (action, future) =>
        future.map { action -> Success(_) }.recover { case t => action -> Failure(t) }
    }
}

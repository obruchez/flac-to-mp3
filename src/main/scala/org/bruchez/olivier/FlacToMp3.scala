package org.bruchez.olivier

import java.nio.file._

import scala.util._

object FlacToMp3 {
  def main(args: Array[String]): Unit = {
    //FileUtils.dumpExtensionStatistics(java.nio.file.Paths.get(args(0)))
    //System.exit(0)

    Arguments(args) match {
      case Failure(throwable) =>
        System.err.println(throwable.getMessage)
        System.exit(-1)
      case Success(arguments) =>
        //println(s"arguments = $arguments")
        convert()(arguments)
    }
  }

  def convert()(implicit arguments: Arguments): Unit = {
    println("Parsing source files...")
    val srcPaths = FileUtils.allFilesInPath(arguments.srcPath).
      filterNot(p => FileUtils.osMetadataFile(p) || Files.isDirectory(p))
    println(s"Source file count: ${srcPaths.size}")

    println("Parsing destination files...")
    val dstPaths = FileUtils.allFilesInPath(arguments.dstPath).
      filterNot(Files.isDirectory(_))
    println(s"Destination file count: ${dstPaths.size}")

    val actionGroups = this.actionGroups(srcPaths, dstPaths)

    for (actionGroup <- actionGroups) {
      actionGroup.execute()
    }
  }

  /*
  File comparison:
   - take source files and generate expected list of destination files (.flac -> .m4a, .mpp -> .m4a, etc.)
   - compare expected destination files and actual destination files => generate actions
   */

  private def actionGroups(srcPaths: Seq[Path],
                           dstPaths: Seq[Path])(implicit arguments: Arguments): Seq[ActionGroup] = {
    val expectedDestinationPathsBySourcePath = this.expectedDestinationPathsBySourcePath(srcPaths)
    val expectedDestinationPaths = expectedDestinationPathsBySourcePath.values.toSet

    val removeSymbolicLinkActions = dstPaths.filter(Files.isSymbolicLink).map(RemoveSymbolicLinkAction)

    val removeFileActions = dstPaths.filterNot(expectedDestinationPaths.contains).map(RemoveFileAction)

    // @todo
    val convertFileActions = Seq[Action]()

    // @todo
    val copyFileActions = Seq[Action]()

    Seq(
      ActionGroup("Symbolic link removal", removeSymbolicLinkActions, parallelExecution = true),
      // Do not delete files in parallel, as we're actually moving them to a trash folder and want to avoid name collisions
      ActionGroup("File removal", removeFileActions, parallelExecution = false),
      ActionGroup("File conversion", convertFileActions, parallelExecution = true),
      ActionGroup("File copy", copyFileActions, parallelExecution = true))
  }

  private def expectedDestinationPathsBySourcePath(srcPaths: Seq[Path])(implicit arguments: Arguments): Map[Path, Path] = {
    val paths =
      for {
        srcPath <- srcPaths
        (_, srcExtensionOption) = FileUtils.baseNameAndExtension(srcPath)
      } yield {
        // @todo
        val expectedPath = srcPath

        srcPath -> expectedPath
      }

    Map(paths: _*)
  }
}

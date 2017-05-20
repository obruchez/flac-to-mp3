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
        convert(arguments)
    }
  }

  def convert(arguments: Arguments): Unit = {
    println("Parsing source files...")
    val srcPaths = FileUtils.allFilesInPath(arguments.srcPath).filterNot(FileUtils.osMetadataFile)
    println(s"Source file count: ${srcPaths.size}")

    println("Parsing destination files...")
    val dstPaths = FileUtils.allFilesInPath(arguments.dstPath)
    println(s"Destination file count: ${dstPaths.size}")

    val actionGroups = this.actionGroups(srcPaths, dstPaths)

    for (actionGroup <- actionGroups) {
      Action.executeActions(actionGroup.actions)(arguments)
    }
  }

  /*
  File comparison:
   - take source files and generate expected list of destination files (.flac -> .m4a, .mpp -> .m4a, etc.)
   - compare expected destination files and actual destination files => generate actions
   */

  def actionGroups(srcPaths: Seq[Path], dstPaths: Seq[Path]): Seq[ActionGroup] = {
    val removeSymbolicLinkActions = dstPaths.filter(Files.isSymbolicLink).map(RemoveSymbolicLinkAction)

    // @todo
    val removeFileActions = Seq[Action]()

    // @todo
    val convertFileActions = Seq[Action]()

    // @todo
    val copyFileActions = Seq[Action]()

    Seq(
      ActionGroup(removeSymbolicLinkActions),
      ActionGroup(removeFileActions),
      ActionGroup(convertFileActions),
      ActionGroup(copyFileActions))
  }
}

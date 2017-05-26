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

  private def actionGroups(srcPaths: Seq[Path],
                           dstPaths: Seq[Path])(implicit arguments: Arguments): Seq[ActionGroup] = {
    val sourceAndExpectedDestinationPaths = this.sourceAndExpectedDestinationPaths(srcPaths)
    val expectedDestinationPaths = sourceAndExpectedDestinationPaths.map(_._2).toSet

    def lastModified(path: Path): Long = Files.getLastModifiedTime(path).toMillis

    def mustConvert(path: Path): Boolean =
      FileUtils.baseNameAndExtension(path)._2.exists(arguments.inputExtensionsToConvert.contains)

    val filesToConvertOrCopy =
      for {
        (srcPath, dstPath) <- sourceAndExpectedDestinationPaths
        if !Files.exists(dstPath) || Files.isSymbolicLink(dstPath) || lastModified(srcPath) > lastModified(dstPath)
      } yield (srcPath, dstPath)

    val (filesToConvert, filesToCopy) = filesToConvertOrCopy.partition(srcAndDstPaths => mustConvert(srcAndDstPaths._1))

    val removeSymbolicLinkActions = dstPaths.filter(Files.isSymbolicLink).map(RemoveSymbolicLinkAction)

    val removeFileActions = dstPaths.filterNot(expectedDestinationPaths.contains).map(RemoveFileAction)

    val convertFileActions = filesToConvert.map { case (srcPath, dstPath) => ConvertFileAction(srcPath, dstPath) }

    val copyFileActions = filesToCopy.map { case (srcPath, dstPath) => CopyFileAction(srcPath, dstPath) }

    Seq(
      ActionGroup("Symbolic link removal", removeSymbolicLinkActions, parallelExecution = true),
      // Do not delete files in parallel, as we're actually moving them to a trash folder and want to avoid name collisions
      ActionGroup("File removal", removeFileActions, parallelExecution = false),
      ActionGroup("File conversion", convertFileActions, parallelExecution = true),
      ActionGroup("File copy", copyFileActions, parallelExecution = true),
      ActionGroup("Empty directory removal", Seq(RemoveEmptyDirectoriesAction(arguments.dstPath)), parallelExecution = false))
  }

  private def sourceAndExpectedDestinationPaths(srcPaths: Seq[Path])(implicit arguments: Arguments): Seq[(Path, Path)] =
    for {
      srcPath <- srcPaths
      (_, srcExtensionOption) = FileUtils.baseNameAndExtension(srcPath)
    } yield {
      val defaultExpectedPath = arguments.dstPath.resolve(arguments.srcPath.relativize(srcPath))

      val expectedPath =
        if (srcExtensionOption.exists(arguments.inputExtensionsToConvert.contains)) {
          FileUtils.withExtension(defaultExpectedPath, arguments.outputFormat.extension)
        } else {
          defaultExpectedPath
        }

      srcPath -> expectedPath
    }
}

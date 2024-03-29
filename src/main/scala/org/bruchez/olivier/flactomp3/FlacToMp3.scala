package org.bruchez.olivier.flactomp3

import java.nio.file._

import scala.util._

object FlacToMp3 {
  def main(args: Array[String]): Unit = {
    // FileUtils.dumpExtensionStatistics(java.nio.file.Paths.get(args(0)))
    // System.exit(0)

    Arguments(args) match {
      case Failure(throwable) =>
        System.err.println(throwable.getMessage)
        System.err.println()
        System.err.println(Arguments.usage)
        System.exit(-1)
      case Success(arguments) =>
        convert()(arguments)
    }
  }

  // scalastyle:off method.length
  def convert()(implicit arguments: Arguments): Unit = {
    println("Parsing source files...")

    val srcPaths =
      FileUtils
        .allFilesInPath(arguments.srcPath, recursive = true)
        .filterNot(p => FileUtils.osMetadataFile(p) || Files.isDirectory(p))

    println(s"Source file count: ${srcPaths.size}")
    println()

    println("Parsing destination files...")

    val dstPaths =
      FileUtils
        .allFilesInPath(arguments.dstPath, recursive = true)
        .filterNot(Files.isDirectory(_))
        .filterNot(_.startsWith(arguments.trashPath))

    println(s"Destination file count: ${dstPaths.size}")
    println()

    val actionGroups = this.actionGroups(srcPaths, dstPaths)

    val totalActionCount = actionGroups.map(_.actions.size).sum

    println(s"Actions to perform ($totalActionCount):")
    for (actionGroup <- actionGroups) {
      println(s" - ${actionGroup.name} count: ${actionGroup.actions.size}")
    }
    println()

    val allActionGroupExecutionErrors = actionGroups.map(_.execute())
    println()

    val totalErrorCount = allActionGroupExecutionErrors.map(_.executionErrors.size).sum

    if (totalErrorCount > 0) {
      println(s"Execution errors ($totalErrorCount):")
      println()

      for (executionError <- allActionGroupExecutionErrors.flatMap(_.executionErrors)) {
        println(executionError.error)
        println()
      }
    }

    println(s"Execution error counts ($totalErrorCount):")
    for (actionGroupExecutionErrors <- allActionGroupExecutionErrors) {
      println(
        s" - ${actionGroupExecutionErrors.name}: ${actionGroupExecutionErrors.executionErrors.size}"
      )
    }
  }

  private def actionGroups(srcPaths: Seq[Path], dstPaths: Seq[Path])(implicit
      arguments: Arguments
  ): Seq[ActionGroup] = {
    val sourceAndExpectedDestinationPaths = this.sourceAndExpectedDestinationPaths(srcPaths)
    val expectedDestinationPaths = sourceAndExpectedDestinationPaths.map(_._2).toSet

    def lastModified(path: Path): Long = Files.getLastModifiedTime(path).toMillis

    def mustConvert(path: Path): Boolean =
      FileUtils.baseNameAndExtension(path)._2.exists(arguments.inputExtensionsToConvert.contains)

    val filesToConvertOrCopy =
      for {
        (srcPath, dstPath) <- sourceAndExpectedDestinationPaths
        if !Files.exists(dstPath) || Files.isSymbolicLink(dstPath) || lastModified(
          srcPath
        ) > lastModified(dstPath) || arguments.force
      } yield (srcPath, dstPath)

    val (filesToConvert, filesToCopy) =
      filesToConvertOrCopy.partition(srcAndDstPaths => mustConvert(srcAndDstPaths._1))

    val removeSymbolicLinkActions =
      dstPaths.filter(Files.isSymbolicLink).map(RemoveSymbolicLinkAction)

    val removeFileActions =
      if (!arguments.noDelete) {
        dstPaths.filterNot(expectedDestinationPaths.contains).map(RemoveFileAction)
      } else {
        Seq()
      }

    val convertFileActions = filesToConvert.map { case (srcPath, dstPath) =>
      ConvertFileAction(srcPath, dstPath)
    }

    val copyFileActions = {
      if (!arguments.convertedOnly) {
        filesToCopy.map { case (srcPath, dstPath) =>
          CopyFileAction(srcPath, dstPath)
        }
      } else {
        Seq()
      }
    }
    // scalastyle:on method.length

    Seq(
      ActionGroup("Symbolic link removal", removeSymbolicLinkActions, parallelExecution = true),
      // Do not delete files in parallel, as we're actually moving them to a trash folder and want to avoid name collisions
      ActionGroup("File removal", removeFileActions, parallelExecution = false),
      ActionGroup("File conversion", convertFileActions, parallelExecution = true),
      ActionGroup("File copy", copyFileActions, parallelExecution = true),
      ActionGroup(
        "Empty directories removal check",
        Seq(RemoveEmptyDirectoriesAction(arguments.dstPath)),
        parallelExecution = false
      )
    )
  }

  private def sourceAndExpectedDestinationPaths(
      srcPaths: Seq[Path]
  )(implicit arguments: Arguments): Seq[(Path, Path)] =
    (for {
      srcPath <- srcPaths
      (_, srcExtensionOption) = FileUtils.baseNameAndExtension(srcPath)
    } yield {
      lazy val defaultExpectedPath = expectedDestinationPath(srcPath)

      if (srcExtensionOption.exists(arguments.inputExtensionsToConvert.contains)) {
        // File to convert => change extension
        Seq(
          srcPath -> FileUtils.withExtension(defaultExpectedPath, arguments.outputFormat.extension)
        )
      } else if (CoverArt.coverArt(srcPath)) {
        // Cover art => copy to expected destination, as well as sub-directories if needed
        (srcPath +: CoverArt.expectedCoverArtSubLocations(srcPath)).map(path =>
          srcPath -> expectedDestinationPath(path)
        )
      } else {
        Seq(srcPath -> defaultExpectedPath)
      }
    }).flatten

  private def expectedDestinationPath(srcPath: Path)(implicit arguments: Arguments): Path =
    arguments.dstPath.resolve(arguments.srcPath.relativize(srcPath))
}
